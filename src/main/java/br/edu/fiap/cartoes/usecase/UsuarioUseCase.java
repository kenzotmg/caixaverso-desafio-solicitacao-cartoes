package br.edu.fiap.cartoes.usecase;
import br.edu.fiap.cartoes.entity.Usuario;
import br.edu.fiap.cartoes.exception.EmailJaCadastradoException;
import br.edu.fiap.cartoes.repository.UsuarioRepository;
import br.edu.fiap.cartoes.security.SenhaBCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import br.edu.fiap.cartoes.dto.*;
import br.edu.fiap.cartoes.entity.StatusSolicitacao;
import br.edu.fiap.cartoes.exception.EtapaNaoImplementadaException;
import jakarta.inject.Inject;

/**
 * Camada de aplicação a implementar: coordena uma operação, suas regras e transação.
 * O controller já chama estas assinaturas. Na migração para Clean, substituam
 * a dependência de DTO HTTP por entradas/saídas da aplicação e portas de saída.
 */
@ApplicationScoped
public class UsuarioUseCase {
    /** Normalizar e-mail, rejeitar duplicado, gerar hash BCrypt, persistir e mapear sem senha. */
    private final UsuarioRepository usuario;
    private final SenhaBCrypt senhas;

    @Inject
    public UsuarioUseCase(UsuarioRepository usuario, SenhaBCrypt senhas) {
        this.usuario = usuario;
        this.senhas = senhas;
    }

    public UsuarioResponse cadastrar(UsuarioRequest request) {
        // TODO: implementar sem colocar SQL ou regras no controller.
        if (usuario.existePorEmail(request.email())) {
            throw new EmailJaCadastradoException();
        }
        String hash = senhas.criarHash(request.senha());
        Usuario novoUsuario = new Usuario(request.nome(), request.email(), hash);
        return  UsuarioResponse.de(usuario.salvar(novoUsuario));
    }
}
