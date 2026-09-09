package com.devcart.pedidosservice.exception;

/** Violacao de regra de negocio do pedido (ex.: estoque insuficiente). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
