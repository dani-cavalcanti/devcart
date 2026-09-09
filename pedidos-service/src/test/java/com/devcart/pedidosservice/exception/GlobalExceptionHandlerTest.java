package com.devcart.pedidosservice.exception;

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
    void naoEncontrado_retorna404() {
        ResponseEntity<ApiError> resposta = handler.handleNaoEncontrado(
                new PedidoNaoEncontradoException(7L), requestPara("/pedidos/7"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().message()).isEqualTo("Pedido nao encontrado: 7");
        assertThat(resposta.getBody().path()).isEqualTo("/pedidos/7");
        assertThat(resposta.getBody().error()).isEqualTo("Not Found");
    }

    @Test
    void regraNegocio_retorna422() {
        ResponseEntity<ApiError> resposta = handler.handleRegraNegocio(
                new RegraNegocioException("estoque insuficiente"), requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resposta.getBody().message()).isEqualTo("estoque insuficiente");
    }

    @Test
    void catalogoIndisponivel_retorna503() {
        ResponseEntity<ApiError> resposta = handler.handleCatalogoIndisponivel(
                new CatalogoIndisponivelException("p1", new RuntimeException("timeout")),
                requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resposta.getBody().message()).isEqualTo("Catalogo indisponivel ao consultar o produto p1");
    }

    @Test
    void requisicaoInvalida_retorna400_headerAusente() throws Exception {
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", List.class), 0);
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-User-Id", parametro);

        ResponseEntity<ApiError> resposta = handler.handleRequisicaoInvalida(ex, requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message()).contains("X-User-Id");
    }

    @Test
    void requisicaoInvalida_retorna400_corpoIlegivel() {
        ResponseEntity<ApiError> resposta = handler.handleRequisicaoInvalida(
                new HttpMessageNotReadableException("json quebrado", new MockHttpInputMessage(new byte[0])),
                requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void validacao_concatenaErrosDeCampo() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "criarPedidoRequest");
        binding.addError(new FieldError("criarPedidoRequest", "itens", "o pedido deve conter ao menos um item"));
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", List.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, binding);

        ResponseEntity<ApiError> resposta = handler.handleValidacao(ex, requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message())
                .isEqualTo("Falha de validacao: itens: o pedido deve conter ao menos um item");
    }

    @Test
    void generico_retorna500ComMensagemFixa() {
        ResponseEntity<ApiError> resposta = handler.handleGenerico(
                new RuntimeException("detalhe interno"), requestPara("/pedidos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().message()).isEqualTo("Erro interno inesperado");
    }

    @SuppressWarnings("unused")
    private void metodoDummy(List<String> ignore) {
    }
}
