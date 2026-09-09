package com.devcart.carrinhoservice.exception;

/** Violacao de regra de negocio do carrinho (ex.: limite de quantidade). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
