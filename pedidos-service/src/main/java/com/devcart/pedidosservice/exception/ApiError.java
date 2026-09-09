package com.devcart.pedidosservice.exception;

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
