package br.com.estoque.service;

// Erro de regra de negocio (ex.: estoque insuficiente). Vira HTTP 400.
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
