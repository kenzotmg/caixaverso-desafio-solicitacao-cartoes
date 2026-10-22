package br.edu.fiap.cartoes.controller;

import br.edu.fiap.cartoes.entity.Cartao;
import br.edu.fiap.cartoes.entity.SolicitacaoCartao;
import br.edu.fiap.cartoes.entity.StatusSolicitacao;
import br.edu.fiap.cartoes.entity.TipoCartao;
import br.edu.fiap.cartoes.entity.Usuario;
import br.edu.fiap.cartoes.security.JwtService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TransicoesSolicitacaoIntegracaoTest {
    @Inject EntityManager entityManager;
    @Inject JwtService jwtService;

    private Long usuarioId;
    private Long outroUsuarioId;
    private Long solicitacaoId;
    private String token;
    private String outroToken;

    @BeforeEach
    void prepararDados() {
        QuarkusTransaction.requiringNew().run(() -> {
            String identificador = UUID.randomUUID().toString();
            Usuario usuario = new Usuario("Titular", identificador + "@example.com", "hash-de-teste");
            Usuario outro = new Usuario("Outro titular", identificador + "-outro@example.com", "hash-de-teste");
            entityManager.persist(usuario);
            entityManager.persist(outro);
            TipoCartao tipo = entityManager.createQuery(
                    "SELECT t FROM TipoCartao t WHERE t.nome = :nome", TipoCartao.class)
                    .setParameter("nome", "ESSENCIAL").getSingleResult();
            SolicitacaoCartao solicitacao = new SolicitacaoCartao(usuario, tipo);
            entityManager.persist(solicitacao);
            entityManager.flush();
            usuarioId = usuario.getId();
            outroUsuarioId = outro.getId();
            solicitacaoId = solicitacao.getId();
            token = jwtService.emitir(usuario).accessToken();
            outroToken = jwtService.emitir(outro).accessToken();
        });
    }

    @AfterEach
    void limparDados() {
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createQuery("DELETE FROM Cartao c WHERE c.solicitacao.id = :id")
                    .setParameter("id", solicitacaoId).executeUpdate();
            entityManager.createQuery("DELETE FROM SolicitacaoCartao s WHERE s.id = :id")
                    .setParameter("id", solicitacaoId).executeUpdate();
            entityManager.createQuery("DELETE FROM Usuario u WHERE u.id IN :ids")
                    .setParameter("ids", List.of(usuarioId, outroUsuarioId)).executeUpdate();
        });
    }

    @Test
    void deveCancelarSomenteUmaVezESemEmitirCartao() {
        given().auth().oauth2(token).when().patch(caminho("cancelar"))
                .then().statusCode(200)
                .body("id", equalTo(solicitacaoId.intValue()))
                .body("status", equalTo("CANCELADA"));
        given().auth().oauth2(token).when().patch(caminho("cancelar"))
                .then().statusCode(409);
        given().auth().oauth2(token).when().post(caminho("emitir"))
                .then().statusCode(409);
        conferirPersistencia(StatusSolicitacao.CANCELADA, 0);
    }

    @Test
    void deveEmitirExatamenteUmCartaoAtivo() {
        given().auth().oauth2(token).when().post(caminho("emitir"))
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("solicitacaoId", equalTo(solicitacaoId.intValue()))
                .body("tipoCartaoNome", equalTo("ESSENCIAL"))
                .body("emitidoEm", notNullValue())
                .body("status", equalTo("ATIVO"));
        given().auth().oauth2(token).when().post(caminho("emitir"))
                .then().statusCode(409);
        given().auth().oauth2(token).when().patch(caminho("cancelar"))
                .then().statusCode(409);
        conferirPersistencia(StatusSolicitacao.EMITIDA, 1);
    }

    @Test
    void deveOcultarSolicitacaoDeOutroTitularEInexistente() {
        for (Long id : List.of(solicitacaoId, Long.MAX_VALUE)) {
            given().auth().oauth2(outroToken).when()
                    .patch("/api/solicitacoes/" + id + "/cancelar").then().statusCode(404);
            given().auth().oauth2(outroToken).when()
                    .post("/api/solicitacoes/" + id + "/emitir").then().statusCode(404);
        }
        conferirPersistencia(StatusSolicitacao.PENDENTE, 0);
    }

    @Test
    void deveExigirTokenEIdPositivo() {
        given().when().patch(caminho("cancelar")).then().statusCode(401);
        given().when().post(caminho("emitir")).then().statusCode(401);
        given().auth().oauth2(token).when().patch("/api/solicitacoes/0/cancelar")
                .then().statusCode(400);
        given().auth().oauth2(token).when().post("/api/solicitacoes/0/emitir")
                .then().statusCode(400);
        conferirPersistencia(StatusSolicitacao.PENDENTE, 0);
    }

    @Test
    void deveDesfazerMudancaDeEstadoQuandoPersistenciaDoCartaoFalha() {
        // Forca uma violacao UNIQUE para exercitar rollback depois de marcarEmitida.
        QuarkusTransaction.requiringNew().run(() ->
                entityManager.persist(new Cartao(entityManager.find(SolicitacaoCartao.class, solicitacaoId))));

        given().auth().oauth2(token).when().post(caminho("emitir"))
                .then().statusCode(409);

        conferirPersistencia(StatusSolicitacao.PENDENTE, 1);
    }

    @Test
    void deveSerializarDuasEmissoesConcorrentes() throws Exception {
        conferirConcorrencia(false);
        conferirPersistencia(StatusSolicitacao.EMITIDA, 1);
    }

    @Test
    void deveSerializarCancelamentoEEmissaoConcorrentes() throws Exception {
        conferirConcorrencia(true);
        QuarkusTransaction.requiringNew().run(() -> {
            StatusSolicitacao status = entityManager.find(SolicitacaoCartao.class, solicitacaoId).getStatus();
            assertTrue(status == StatusSolicitacao.CANCELADA || status == StatusSolicitacao.EMITIDA);
            assertEquals(status == StatusSolicitacao.EMITIDA ? 1L : 0L, contarCartoes());
        });
    }

    private void conferirConcorrencia(boolean cancelar) throws Exception {
        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de inicio");
                return given().auth().oauth2(token).when().post(caminho("emitir")).statusCode();
            });
            var segunda = executor.submit(() -> {
                if (!inicio.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout de inicio");
                return cancelar
                        ? given().auth().oauth2(token).when().patch(caminho("cancelar")).statusCode()
                        : given().auth().oauth2(token).when().post(caminho("emitir")).statusCode();
            });
            inicio.countDown();
            List<Integer> respostas = List.of(
                    primeira.get(30, TimeUnit.SECONDS), segunda.get(30, TimeUnit.SECONDS));
            assertEquals(1, respostas.stream().filter(codigo -> codigo == 409).count());
            assertEquals(1, respostas.stream().filter(codigo -> codigo == 200 || codigo == 201).count());
        }
    }

    private String caminho(String operacao) {
        return "/api/solicitacoes/" + solicitacaoId + "/" + operacao;
    }

    private void conferirPersistencia(StatusSolicitacao status, long quantidadeCartoes) {
        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(status, entityManager.find(SolicitacaoCartao.class, solicitacaoId).getStatus());
            assertEquals(quantidadeCartoes, contarCartoes());
        });
    }

    private long contarCartoes() {
        return entityManager.createQuery(
                "SELECT COUNT(c) FROM Cartao c WHERE c.solicitacao.id = :id", Long.class)
                .setParameter("id", solicitacaoId).getSingleResult();
    }
}
