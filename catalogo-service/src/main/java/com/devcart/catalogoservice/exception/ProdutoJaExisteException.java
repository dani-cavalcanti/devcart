package com.devcart.catalogoservice.exception;

public class ProdutoJaExisteException extends RuntimeException {

    public ProdutoJaExisteException(String nome) {
        super("Ja existe um produto com o nome: " + nome);
    }
}
