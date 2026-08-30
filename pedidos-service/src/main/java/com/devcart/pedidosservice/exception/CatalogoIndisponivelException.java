package com.devcart.pedidosservice.exception;

/** O catalogo-service falhou, estourou o timeout de 2s ou o circuito esta aberto. */
public class CatalogoIndisponivelException extends RuntimeException {

    public CatalogoIndisponivelException(String produtoId, Throwable causa) {
        super("Catalogo indisponivel ao consultar o produto " + produtoId, causa);
    }
}
