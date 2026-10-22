package br.edu.fiap.cartoes.repository;

import br.edu.fiap.cartoes.entity.TipoCartao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;

/** Adaptador JPA a implementar: buscar o tipo por ID ao criar uma solicitação. */
@ApplicationScoped
public class TipoCartaoRepositoryJpa implements TipoCartaoRepository {
    private final EntityManager entityManager;

    @Inject
    public TipoCartaoRepositoryJpa(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<TipoCartao> buscarPorId(Long id) {
        return Optional.ofNullable(entityManager.find(TipoCartao.class, id));
    }
}