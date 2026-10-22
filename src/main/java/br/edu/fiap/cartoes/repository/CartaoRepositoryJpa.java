package br.edu.fiap.cartoes.repository;
import br.edu.fiap.cartoes.entity.Cartao;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Adaptador JPA: injete EntityManager, escreva JPQL parametrizada e retorne entidades.
 * Não chamar controller nem gerar token nesta camada. Gravações participam da transação aberta pelo use case.
 */
@ApplicationScoped
public class CartaoRepositoryJpa implements CartaoRepository {
    @Override
    public Cartao salvar(Cartao cartao) {
        return null;
    }

    @Override
    public List<Cartao> listarPorUsuario(Long usuarioId) {
        return List.of();
    }
    // TODO: implementar a interface depois de declarar suas assinaturas.

}
