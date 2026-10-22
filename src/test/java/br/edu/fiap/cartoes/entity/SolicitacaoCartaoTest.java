package br.edu.fiap.cartoes.entity;

import br.edu.fiap.cartoes.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SolicitacaoCartaoTest {
    @Test
    void deveCancelarSolicitacaoPendente() {
        SolicitacaoCartao solicitacao = novaSolicitacao();
        solicitacao.cancelar();
        assertEquals(StatusSolicitacao.CANCELADA, solicitacao.getStatus());
    }

    @Test
    void deveMarcarSolicitacaoPendenteComoEmitida() {
        SolicitacaoCartao solicitacao = novaSolicitacao();
        solicitacao.marcarEmitida();
        assertEquals(StatusSolicitacao.EMITIDA, solicitacao.getStatus());
    }

    @Test
    void deveRejeitarTransicoesDeEstadosFinaisSemAlterarEstado() {
        for (StatusSolicitacao status : new StatusSolicitacao[]{
                StatusSolicitacao.CANCELADA, StatusSolicitacao.EMITIDA}) {
            SolicitacaoCartao solicitacao = new SolicitacaoCartao(
                    10L, null, null, Instant.parse("2026-10-20T12:00:00Z"), status);
            assertThrows(RegraNegocioException.class, solicitacao::cancelar);
            assertThrows(RegraNegocioException.class, solicitacao::marcarEmitida);
            assertEquals(status, solicitacao.getStatus());
        }
    }

    @Test
    void devePreservarDadosAoReconstruirSolicitacao() {
        Instant data = Instant.parse("2026-10-20T12:00:00Z");
        SolicitacaoCartao solicitacao = new SolicitacaoCartao(
                10L, null, null, data, StatusSolicitacao.EMITIDA);
        SolicitacaoCartao copia = solicitacao.devolveComId();
        assertAll(
                () -> assertEquals(10L, copia.getId()),
                () -> assertEquals(data, copia.getSolicitadaEm()),
                () -> assertEquals(StatusSolicitacao.EMITIDA, copia.getStatus())
        );
    }

    private SolicitacaoCartao novaSolicitacao() {
        return new SolicitacaoCartao(
                new Usuario("Titular", "titular@example.com", "hash-de-teste"),
                new TipoCartao("ESSENCIAL", "Para o dia a dia", true));
    }
}
