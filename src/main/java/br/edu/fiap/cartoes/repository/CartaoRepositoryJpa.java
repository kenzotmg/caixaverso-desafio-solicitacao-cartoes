package br.edu.fiap.cartoes.repository;
import br.edu.fiap.cartoes.entity.Cartao;
import br.edu.fiap.cartoes.exception.CartaoJaExisteParaSolicitacaoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.sql.SQLException;
import java.util.List;

/**
 * Adaptador JPA: injete EntityManager, escreva JPQL parametrizada e retorne entidades.
 * Não chamar controller nem gerar token nesta camada. Gravações participam da transação aberta pelo use case.
 */
@ApplicationScoped
public class CartaoRepositoryJpa implements CartaoRepository {

    private final EntityManager entityManager;


    @Inject
    public CartaoRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Cartao> listarPorUsuario(Long usuarioId) {
        String query = """
                SELECT c
                FROM Cartao c
                WHERE c.solicitacao.usuario.id = :usuarioId
                """;
        return entityManager.createQuery(query, Cartao.class).setParameter("usuarioId", usuarioId).getResultList();
    }

    @Override
    @Transactional
    public Cartao salvar(Cartao cartao) {
        Cartao entity = Cartao.de(cartao);
        try {
            entityManager.persist(entity);
            entityManager.flush();

            return entity.paraDominio();
        } catch (RuntimeException erro) {
            if(violouChaveUnica(erro)) {
                throw new CartaoJaExisteParaSolicitacaoException(cartao.getSolicitacao().getId());
            }
            throw new IllegalStateException("Erro ao persistir:", erro);
        }
    }

    private boolean violouChaveUnica(Throwable erro){
        Throwable atual = erro;
        while(atual != null){
            if(atual instanceof SQLException sql && "23505".equals(sql.getSQLState())){
                return true;
            }
            atual = atual.getCause();
        }
        return false;
    }
}
