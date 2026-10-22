package br.edu.fiap.cartoes.exception;

public class CartaoJaExisteParaSolicitacaoException extends RuntimeException {
    public CartaoJaExisteParaSolicitacaoException(Long solicitacaoId) {
        super("Cartão já existente para solicitação " + solicitacaoId);
    }
}
