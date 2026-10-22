package br.edu.fiap.cartoes.repository;
import br.edu.fiap.cartoes.entity.SolicitacaoCartao;
import br.edu.fiap.cartoes.entity.StatusSolicitacao;
import br.edu.fiap.cartoes.entity.TipoCartao;
import br.edu.fiap.cartoes.exception.EtapaNaoImplementadaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador JPA: injete EntityManager, escreva JPQL parametrizada e retorne entidades.
 * Não chamar controller nem gerar token nesta camada. Gravações participam da transação aberta pelo use case.
 */
@ApplicationScoped
public class SolicitacaoCartaoRepositoryJpa implements SolicitacaoCartaoRepository {

    private final EntityManager entityManager;

    @Inject
    public SolicitacaoCartaoRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public SolicitacaoCartao salvar(SolicitacaoCartao solicitacaoCartao) {

        SolicitacaoCartao novaSolicitacao = SolicitacaoCartao.de(solicitacaoCartao);

        try {
            entityManager.persist(novaSolicitacao);

            entityManager.flush();
            return novaSolicitacao.devolveComId();

        } catch (RuntimeException erro) {
            throw new EtapaNaoImplementadaException("ERRO INTERNO");
        }
    }

    @Override
    public boolean existsPorUsuarioIdAndStatus(Long usuarioId, TipoCartao tipoCartao) {
        return entityManager.createQuery("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM SolicitacaoCartao s
        WHERE s.usuario.id = :usuarioId
          AND s.tipoCartao = :tipoCartao
          AND s.status = :status
        """, Boolean.class)
                .setParameter("usuarioId", usuarioId)
                .setParameter("tipoCartao", tipoCartao)
                .setParameter("status", StatusSolicitacao.PENDENTE)
                .getSingleResult();
    }

    @Override
    public Optional<SolicitacaoCartao> listarSolicitacaoPorId(Long id) {
        return entityManager.createQuery("""
                SELECT s
                FROM SolicitacaoCartao s
                JOIN FETCH s.tipoCartao
                JOIN FETCH s.usuario
                WHERE s.id = :id
                """, SolicitacaoCartao.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<SolicitacaoCartao> buscarPorIdETitularComBloqueio(Long id, Long usuarioId) {
        return entityManager.createQuery("""
                SELECT s
                FROM SolicitacaoCartao s
                JOIN FETCH s.tipoCartao
                JOIN FETCH s.usuario
                WHERE s.id = :id AND s.usuario.id = :usuarioId
                """, SolicitacaoCartao.class)
                .setParameter("id", id)
                .setParameter("usuarioId", usuarioId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<SolicitacaoCartao> listarSolicitacoes(Long usuarioId, StatusSolicitacao status) {
        return entityManager.createQuery("""
                                SELECT s
                                FROM SolicitacaoCartao s
                                JOIN FETCH s.tipoCartao
                                WHERE s.usuario.id = :usuarioId
                                  AND (:status IS NULL OR s.status = :status)
                                ORDER BY s.solicitadaEm DESC
                                """,
                        SolicitacaoCartao.class)
                .setParameter("usuarioId", usuarioId)
                .setParameter("status", status)
                .getResultStream()
                .toList();
    }
}
