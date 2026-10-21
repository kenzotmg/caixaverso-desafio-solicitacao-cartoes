package br.edu.fiap.cartoes.usecase;

import br.edu.fiap.cartoes.dto.TipoCartaoResponse;
import br.edu.fiap.cartoes.entity.TipoCartao;
import br.edu.fiap.cartoes.repository.ConsultaTiposDisponiveis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TipoCartaoUseCaseTest {
    private ConsultaTiposDisponiveis consulta;
    private TipoCartaoUseCase useCase;

    @BeforeEach
    void prepararUseCase() {
        consulta = mock(ConsultaTiposDisponiveis.class);
        useCase = new TipoCartaoUseCase();
        useCase.consultaTiposDisponiveis = consulta;
    }

    @Test
    void deveMapearTodosOsCamposEPreservarOrdemDaConsulta() {
        TipoCartao essencial = new TipoCartao("ESSENCIAL", "Para o dia a dia", true);
        essencial.setId(10L);
        TipoCartao viagem = new TipoCartao("VIAGEM", "Para viagens", true);
        viagem.setId(20L);
        when(consulta.listarDisponiveis()).thenReturn(List.of(essencial, viagem));

        List<TipoCartaoResponse> respostas = useCase.listarDisponiveis();

        assertEquals(List.of(
                new TipoCartaoResponse(10L, "ESSENCIAL", "Para o dia a dia", true),
                new TipoCartaoResponse(20L, "VIAGEM", "Para viagens", true)
        ), respostas);
        verify(consulta).listarDisponiveis();
        verifyNoMoreInteractions(consulta);
    }

    @Test
    void deveRetornarListaVaziaQuandoConsultaNaoRetornaTipos() {
        when(consulta.listarDisponiveis()).thenReturn(List.of());

        assertTrue(useCase.listarDisponiveis().isEmpty());
        verify(consulta).listarDisponiveis();
    }

    @Test
    void devePropagarErroDaConsultaSemSubstituirPorListaVazia() {
        IllegalStateException causa = new IllegalStateException("Falha simulada de consulta");
        when(consulta.listarDisponiveis()).thenThrow(causa);

        IllegalStateException erro = assertThrows(
                IllegalStateException.class, () -> useCase.listarDisponiveis());

        assertSame(causa, erro);
        verify(consulta).listarDisponiveis();
    }
}
