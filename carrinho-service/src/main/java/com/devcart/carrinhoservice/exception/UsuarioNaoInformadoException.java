package com.devcart.carrinhoservice.exception;

/** Header X-User-Id ausente ou em branco. */
public class UsuarioNaoInformadoException extends RuntimeException {

    public UsuarioNaoInformadoException() {
        super("Header X-User-Id e obrigatorio");
    }
}
