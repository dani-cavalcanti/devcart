package com.devcart.catalogoservice.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(String id) {
        super("Produto nao encontrado: " + id);
    }
}
