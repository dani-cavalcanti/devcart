package com.devcart.pedidosservice.exception;

public class PedidoNaoEncontradoException extends RuntimeException {

    public PedidoNaoEncontradoException(Long id) {
        super("Pedido nao encontrado: " + id);
    }
}
