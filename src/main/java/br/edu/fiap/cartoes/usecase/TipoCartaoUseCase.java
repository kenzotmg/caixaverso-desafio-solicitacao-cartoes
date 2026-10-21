package br.edu.fiap.cartoes.usecase;

import br.edu.fiap.cartoes.dto.TipoCartaoResponse;
import br.edu.fiap.cartoes.entity.TipoCartao;
import br.edu.fiap.cartoes.repository.ConsultaTiposDisponiveis;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
/**
 * Camada de aplicação a implementar: coordena uma operação, suas regras e transação.
 * O controller já chama estas assinaturas. Na migração para Clean, substituam
 * a dependência de DTO HTTP por entradas/saídas da aplicação e portas de saída.
 */
@ApplicationScoped
public class TipoCartaoUseCase {
    @Inject
    ConsultaTiposDisponiveis consultaTiposDisponiveis;

    /** Delegar à única consulta JDBC e mapear a resposta. */
    public List<TipoCartaoResponse> listarDisponiveis() {
        List<TipoCartao> tipos = consultaTiposDisponiveis.listarDisponiveis();
        List<TipoCartaoResponse> respostas = new ArrayList<>();

        for (TipoCartao tipo : tipos) {
            respostas.add(new TipoCartaoResponse(
                    tipo.getId(),
                    tipo.getNome(),
                    tipo.getDescricao(),
                    tipo.isDisponivel()
            ));
        }

        return respostas;
    }
}
