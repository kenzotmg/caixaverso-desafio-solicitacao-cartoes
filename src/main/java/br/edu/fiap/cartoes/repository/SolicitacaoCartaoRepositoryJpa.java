package br.edu.fiap.cartoes.repository;
import br.edu.fiap.cartoes.entity.SolicitacaoCartao;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Adaptador JPA: injete EntityManager, escreva JPQL parametrizada e retorne entidades.
 * Não chamar controller nem gerar token nesta camada. Gravações participam da transação aberta pelo use case.
 */
@ApplicationScoped
public class SolicitacaoCartaoRepositoryJpa implements SolicitacaoCartaoRepository {
    @Override
    public SolicitacaoCartao salvar(SolicitacaoCartao solicitacaoCartao) {
        return null;
    }

    @Override
    public List<SolicitacaoCartao> listarSolicitacoesPorUsuarioId(Long usuarioId) {
        return List.of();
    }

    @Override
    public List<SolicitacaoCartao> listarSolicitacoesPorStatus(String status) {
        return List.of();
    }
    // TODO: implementar a interface depois de declarar suas assinaturas.
}
