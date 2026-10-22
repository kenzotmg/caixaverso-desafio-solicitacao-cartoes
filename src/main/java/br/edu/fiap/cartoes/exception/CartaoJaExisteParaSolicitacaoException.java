package br.edu.fiap.cartoes.exception;

public class CartaoJaExisteParaSolicitacaoException extends RegraNegocioException {
    public CartaoJaExisteParaSolicitacaoException(Long solicitacaoId) {
        super("Cartão já existente para solicitação " + solicitacaoId);
    }
}
