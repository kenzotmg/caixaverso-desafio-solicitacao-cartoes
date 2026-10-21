package br.edu.fiap.cartoes.repository;

import br.edu.fiap.cartoes.entity.TipoCartao;
import io.agroal.api.AgroalDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoCartaoRepositoryJdbcTest {
    private TipoCartaoRepositoryJdbc repository;
    private AgroalDataSource dataSource;
    private String url;

    @BeforeEach
    void prepararBanco() throws SQLException {
        url = "jdbc:h2:mem:tipos_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        dataSource = mock(AgroalDataSource.class);
        when(dataSource.getConnection()).thenAnswer(invocation -> DriverManager.getConnection(url));
        repository = new TipoCartaoRepositoryJdbc();
        repository.dataSource = dataSource;

        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE tipos_cartao (
                        id BIGINT PRIMARY KEY,
                        nome VARCHAR(60) NOT NULL,
                        descricao VARCHAR(255) NOT NULL,
                        disponivel BOOLEAN NOT NULL
                    )
                    """);
        }
    }

    @AfterEach
    void encerrarBanco() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.execute("SHUTDOWN");
        }
    }

    @Test
    void deveListarSomenteDisponiveisOrdenadosPorNome() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tipos_cartao (id, nome, descricao, disponivel) VALUES
                    (20, 'VIAGEM', 'Para viagens', TRUE),
                    (30, 'EMPRESARIAL', 'Fora de oferta', FALSE),
                    (10, 'ESSENCIAL', 'Para o dia a dia', TRUE)
                    """);
        }

        List<TipoCartao> tipos = repository.listarDisponiveis();

        assertEquals(List.of("ESSENCIAL", "VIAGEM"),
                tipos.stream().map(TipoCartao::getNome).toList());
        assertAll(
                () -> assertEquals(10L, tipos.get(0).getId()),
                () -> assertEquals("Para o dia a dia", tipos.get(0).getDescricao()),
                () -> assertEquals(20L, tipos.get(1).getId()),
                () -> assertEquals("Para viagens", tipos.get(1).getDescricao()),
                () -> assertTrue(tipos.stream().allMatch(TipoCartao::isDisponivel))
        );
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaTipos() {
        assertTrue(repository.listarDisponiveis().isEmpty());
    }

    @Test
    void deveRetornarListaVaziaQuandoTodosEstaoIndisponiveis() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO tipos_cartao (id, nome, descricao, disponivel)
                    VALUES (30, 'EMPRESARIAL', 'Fora de oferta', FALSE)
                    """);
        }

        assertTrue(repository.listarDisponiveis().isEmpty());
    }

    @Test
    void devePropagarFalhaDeConexaoComCausaOriginal() throws SQLException {
        SQLException causa = new SQLException("Falha simulada de conexao");
        when(dataSource.getConnection()).thenThrow(causa);

        IllegalStateException erro = assertThrows(
                IllegalStateException.class, () -> repository.listarDisponiveis());

        assertSame(causa, erro.getCause());
    }
}
