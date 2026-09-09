package com.devcart.carrinhoservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static HttpServletRequest requestPara(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    @Test
    void headerAusente_retorna400ComMensagemFixa() throws Exception {
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", List.class), 0);
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-User-Id", parametro);

        ResponseEntity<ApiError> resposta = handler.handleHeaderAusente(ex, requestPara("/carrinho"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).isEqualTo("Header X-User-Id e obrigatorio");
        assertThat(resposta.getBody().path()).isEqualTo("/carrinho");
    }

    @Test
    void usuarioNaoInformado_retorna400() {
        ResponseEntity<ApiError> resposta = handler.handleHeaderAusente(
                new UsuarioNaoInformadoException(), requestPara("/carrinho"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().status()).isEqualTo(400);
    }

    @Test
    void corpoInvalido_retorna400ComMensagemAmigavel() {
        ResponseEntity<ApiError> resposta = handler.handleCorpoInvalido(
                new HttpMessageNotReadableException("erro cru", new MockHttpInputMessage(new byte[0])),
                requestPara("/carrinho/itens"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).isEqualTo("Corpo da requisicao ausente ou malformado");
    }

    @Test
    void regraNegocio_retorna422ComMensagemDaExcecao() {
        ResponseEntity<ApiError> resposta = handler.handleRegraNegocio(
                new RegraNegocioException("limite excedido"), requestPara("/carrinho/itens"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().message()).isEqualTo("limite excedido");
    }

    @Test
    void generico_retorna500() {
        ResponseEntity<ApiError> resposta = handler.handleGenerico(
                new IllegalStateException("boom"), requestPara("/carrinho"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().message()).isEqualTo("Erro interno inesperado");
    }

    @Test
    void validacao_concatenaErrosDeCampo() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "adicionarItemRequest");
        binding.addError(new FieldError("adicionarItemRequest", "quantidade", "deve ser maior que zero"));
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", List.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, binding);

        ResponseEntity<ApiError> resposta = handler.handleValidacao(ex, requestPara("/carrinho/itens"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message())
                .isEqualTo("Falha de validacao: quantidade: deve ser maior que zero");
    }

    @SuppressWarnings("unused")
    private void metodoDummy(List<String> ignore) {
    }
}
