package br.edu.fiap.cartoes.usecase;
import br.edu.fiap.cartoes.entity.Usuario;
import br.edu.fiap.cartoes.repository.UsuarioRepository;
import br.edu.fiap.cartoes.security.JwtService;
import br.edu.fiap.cartoes.security.SenhaBCrypt;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import br.edu.fiap.cartoes.dto.*;
import br.edu.fiap.cartoes.entity.StatusSolicitacao;
import br.edu.fiap.cartoes.exception.EtapaNaoImplementadaException;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;

/**
 * Camada de aplicação a implementar: coordena uma operação, suas regras e transação.
 * O controller já chama estas assinaturas. Na migração para Clean, substituam
 * a dependência de DTO HTTP por entradas/saídas da aplicação e portas de saída.
 */
@ApplicationScoped
public class AutenticacaoUseCase {
    /** Buscar e-mail normalizado, conferir BCrypt, negar genericamente ou emitir JWT. */
    private final UsuarioRepository usuarios;
    private final SenhaBCrypt senhas;
    private final JwtService token;

    @Inject
    public AutenticacaoUseCase(UsuarioRepository usuarios, SenhaBCrypt senhas, JwtService token) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.token = token;
    }

    public TokenResponse login(LoginRequest request) {
        // TODO: implementar sem colocar SQL ou regras no controller.
        Usuario usuario = usuarios.buscaPorEmail(request.email())
                .orElseThrow(() -> new NotAuthorizedException("Bearer"));

        if (!senhas.confere(request.senha(), usuario.getSenhaHash())) {
            throw new NotAuthorizedException("Bearer");
        }
        return token.emitir(usuario);
    }
}