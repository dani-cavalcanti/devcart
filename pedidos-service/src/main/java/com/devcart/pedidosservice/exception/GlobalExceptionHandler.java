package com.devcart.pedidosservice.exception;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Tratamento global padronizado. Toda excecao vira um {@link ApiError}
 * com timestamp, status, error, message e path.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PedidoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNaoEncontrado(PedidoNaoEncontradoException ex,
                                                        HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> handleRegraNegocio(RegraNegocioException ex,
                                                       HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(CatalogoIndisponivelException.class)
    public ResponseEntity<ApiError> handleCatalogoIndisponivel(CatalogoIndisponivelException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleRequisicaoInvalida(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacao(MethodArgumentNotValidException ex,
                                                    HttpServletRequest request) {
        String detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "Falha de validacao: " + detalhes, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError body = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}
