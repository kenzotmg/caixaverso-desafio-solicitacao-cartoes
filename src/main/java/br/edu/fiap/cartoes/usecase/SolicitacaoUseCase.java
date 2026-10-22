package br.edu.fiap.cartoes.usecase;
import br.edu.fiap.cartoes.entity.SolicitacaoCartao;
import br.edu.fiap.cartoes.entity.Cartao;
import br.edu.fiap.cartoes.entity.TipoCartao;
import br.edu.fiap.cartoes.entity.Usuario;
import br.edu.fiap.cartoes.exception.RecursoNaoEncontradoException;
import br.edu.fiap.cartoes.exception.RegraNegocioException;
import br.edu.fiap.cartoes.repository.SolicitacaoCartaoRepository;
import br.edu.fiap.cartoes.repository.CartaoRepository;
import br.edu.fiap.cartoes.repository.TipoCartaoRepository;
import br.edu.fiap.cartoes.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import br.edu.fiap.cartoes.dto.*;
import br.edu.fiap.cartoes.entity.StatusSolicitacao;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Camada de aplicação a implementar: coordena uma operação, suas regras e transação.
 * O controller já chama estas assinaturas. Na migração para Clean, substituam
 * a dependência de DTO HTTP por entradas/saídas da aplicação e portas de saída.
 */
@ApplicationScoped
public class SolicitacaoUseCase {

    private final UsuarioRepository usuarioRepository;

    private final TipoCartaoRepository tipoCartaoRepository;

    private final SolicitacaoCartaoRepository solicitacaoRepository;

    private final CartaoRepository cartaoRepository;

    @Inject
    public SolicitacaoUseCase(UsuarioRepository usuarioRepository, TipoCartaoRepository tipoCartaoRepository, SolicitacaoCartaoRepository solicitacaoRepository, CartaoRepository cartaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.tipoCartaoRepository = tipoCartaoRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.cartaoRepository = cartaoRepository;
    }

    /** Buscar titular e tipo por JPA; tipo indisponível ou pedido pendente duplicado gera 409. */
    public SolicitacaoResponse criar(Long usuarioId, SolicitacaoRequest request) {
        TipoCartao tipoCartao = tipoCartaoRepository.buscarPorId(request.tipoCartaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não localizado o tipo de cartão com id: " + request.tipoCartaoId()));

        if(solicitacaoRepository.existsPorUsuarioIdAndStatus(usuarioId, tipoCartao)) {
            throw new RegraNegocioException("Outro pedido PENDENTE para o mesmo titular e tipo.");
        }

        Usuario usuario = usuarioRepository.buscaPorId(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não localizado usuário de Id: " + usuarioId));

        SolicitacaoCartao solicitacaoCartao = new SolicitacaoCartao(usuario, tipoCartao);

        return SolicitacaoResponse.de(solicitacaoRepository.salvar(solicitacaoCartao));
    }

    /** JPQL própria com filtro de titular e status opcional, nunca listar todos os usuários. */
    public List<SolicitacaoResponse> listar(Long usuarioId, StatusSolicitacao status) {
        if(usuarioId == null) {
            throw new RecursoNaoEncontradoException("Não localizadas solicitações para o usuário.");
        }
        return solicitacaoRepository.listarSolicitacoes(usuarioId, status)
                .stream()
                .map(SolicitacaoResponse::de)
                .toList();

    }

    /** Buscar por id E titular; retornar 404 para pedido de outro usuário. */
    public SolicitacaoResponse buscar(Long usuarioId, Long id) {
        SolicitacaoCartao solicitacao = solicitacaoRepository.listarSolicitacaoPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Não localizada solicitação de Id: " + id));

        if(!usuarioId.equals(solicitacao.getUsuario().getId())) {
            throw new RecursoNaoEncontradoException("Não localizada solicitação de Id: " + id);
        }

        return SolicitacaoResponse.de(solicitacao);
    }

    /** Em transação, buscar o próprio pedido e chamar cancelar; estado diferente de PENDENTE gera 409. */
    @Transactional
    public SolicitacaoResponse cancelar(Long usuarioId, Long id) {
        SolicitacaoCartao solicitacao = buscarPropriaComBloqueio(usuarioId, id);
        solicitacao.cancelar();
        return SolicitacaoResponse.de(solicitacao);
    }

    /** Em uma transação, bloquear o pedido para escrita, conferir titular/estado, criar cartão e marcar EMITIDA. */
    @Transactional
    public CartaoResponse emitir(Long usuarioId, Long id) {
        SolicitacaoCartao solicitacao = buscarPropriaComBloqueio(usuarioId, id);
        solicitacao.marcarEmitida();
        Cartao cartao = cartaoRepository.salvar(new Cartao(solicitacao));
        return CartaoResponse.de(cartao);
    }

    private SolicitacaoCartao buscarPropriaComBloqueio(Long usuarioId, Long id) {
        return solicitacaoRepository.buscarPorIdETitularComBloqueio(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação não encontrada"));
    }
}
