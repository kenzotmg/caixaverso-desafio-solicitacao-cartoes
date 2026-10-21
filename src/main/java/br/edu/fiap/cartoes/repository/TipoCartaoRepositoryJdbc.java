package br.edu.fiap.cartoes.repository;

import br.edu.fiap.cartoes.entity.TipoCartao;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador JDBC: use AgroalDataSource, PreparedStatement e try-with-resources.
 * Não chamar controller nem gerar token nesta camada. Esta é a única implementação JDBC obrigatória.
 */
@ApplicationScoped
public class TipoCartaoRepositoryJdbc implements ConsultaTiposDisponiveis {
    @Inject
    AgroalDataSource dataSource;

    @Override
    public List<TipoCartao> listarDisponiveis() {
        String sql = """
                SELECT id, nome, descricao, disponivel
                FROM tipos_cartao
                WHERE disponivel = ?
                ORDER BY nome
                """;
        List<TipoCartao> tipos = new ArrayList<>();

        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, true);

            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    TipoCartao tipo = new TipoCartao(
                            resultSet.getString("nome"),
                            resultSet.getString("descricao"),
                            resultSet.getBoolean("disponivel")
                    );
                    tipo.setId(resultSet.getLong("id"));
                    tipos.add(tipo);
                }
            }
        } catch (SQLException erro) {
            throw new IllegalStateException("Nao foi possivel consultar os tipos de cartao.", erro);
        }

        return tipos;
    }
}
