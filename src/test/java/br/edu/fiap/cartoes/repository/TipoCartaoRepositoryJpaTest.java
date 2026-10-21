package br.edu.fiap.cartoes.repository;

import br.edu.fiap.cartoes.entity.TipoCartao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoCartaoRepositoryJpaTest {
    private EntityManager entityManager;
    private TipoCartaoRepositoryJpa repository;

    @BeforeEach
    void prepararRepository() {
        entityManager = mock(EntityManager.class);
        repository = new TipoCartaoRepositoryJpa(entityManager);
    }

    @Test
    void deveRetornarTipoExistentePorId() {
        TipoCartao tipo = new TipoCartao("ESSENCIAL", "Para o dia a dia", true);
        tipo.setId(10L);
        when(entityManager.find(TipoCartao.class, 10L)).thenReturn(tipo);

        Optional<TipoCartao> resultado = repository.buscarPorId(10L);

        assertTrue(resultado.isPresent());
        assertSame(tipo, resultado.orElseThrow());
        verify(entityManager).find(TipoCartao.class, 10L);
        verifyNoMoreInteractions(entityManager);
    }

    @Test
    void deveRetornarOptionalVazioQuandoTipoNaoExiste() {
        when(entityManager.find(TipoCartao.class, 99L)).thenReturn(null);

        Optional<TipoCartao> resultado = repository.buscarPorId(99L);

        assertTrue(resultado.isEmpty());
        verify(entityManager).find(TipoCartao.class, 99L);
    }

    @Test
    void deveRetornarTipoMesmoQuandoIndisponivel() {
        TipoCartao tipo = new TipoCartao("EMPRESARIAL", "Fora de oferta", false);
        tipo.setId(30L);
        when(entityManager.find(TipoCartao.class, 30L)).thenReturn(tipo);

        Optional<TipoCartao> resultado = repository.buscarPorId(30L);

        TipoCartao encontrado = resultado.orElseThrow();
        assertSame(tipo, encontrado);
        assertFalse(encontrado.isDisponivel());
        verify(entityManager).find(TipoCartao.class, 30L);
    }
}
