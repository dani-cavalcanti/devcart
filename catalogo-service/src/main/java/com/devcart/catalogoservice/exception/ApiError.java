package com.devcart.catalogoservice.exception;

import java.time.OffsetDateTime;

/**
 * Corpo JSON padronizado para todas as respostas de erro da API.
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
