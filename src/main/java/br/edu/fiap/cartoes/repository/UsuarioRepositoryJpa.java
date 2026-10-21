package br.edu.fiap.cartoes.repository;
import br.edu.fiap.cartoes.entity.Usuario;
import br.edu.fiap.cartoes.exception.EmailJaCadastradoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Adaptador JPA: injete EntityManager, escreva JPQL parametrizada e retorne entidades.
 * Não chamar controller nem gerar token nesta camada. Gravações participam da transação aberta pelo use case.
 */
@ApplicationScoped
public class UsuarioRepositoryJpa implements UsuarioRepository {
    // TODO: implementar a interface depois de declarar suas assinaturas.
    private static final String BUSCA_POR_EMAIL = """
                SELECT usuario FROM Usuario usuario
                WHERE lower(usuario.email) = lower(:email)
            """;

    private final EntityManager entityManager;

    @Inject
    public UsuarioRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public boolean existePorEmail(String email) {
        return buscaPorEmail(email).isPresent();
    }

    @Override
    @Transactional
    public Optional<Usuario> buscaPorEmail(String email) {
        return entityManager.createQuery(BUSCA_POR_EMAIL, Usuario.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional
    public Usuario salvar(Usuario usuario) {
        Usuario entity = usuario;
        try{
            entityManager.persist(entity);
            entityManager.flush();
            return entity;
        } catch (RuntimeException erro) {
            if (violouChaveUnica(erro)) {
                throw new EmailJaCadastradoException();
            }
            throw new IllegalStateException("Falhou", erro);
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
