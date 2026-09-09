package com.devcart.carrinhoservice.exception;

import java.time.OffsetDateTime;

/** Corpo JSON padronizado para respostas de erro. */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
