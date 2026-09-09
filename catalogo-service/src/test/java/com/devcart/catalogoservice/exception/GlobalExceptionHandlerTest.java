package com.devcart.catalogoservice.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static HttpServletRequest requestPara(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    @Test
    void naoEncontrado_retorna404ComCorpoPadronizado() {
        ResponseEntity<ApiError> resposta = handler.handleNaoEncontrado(
                new ProdutoNaoEncontradoException("id-1"), requestPara("/produtos/id-1"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError corpo = resposta.getBody();
        assertThat(corpo).isNotNull();
        assertThat(corpo.status()).isEqualTo(404);
        assertThat(corpo.error()).isEqualTo("Not Found");
        assertThat(corpo.message()).isEqualTo("Produto nao encontrado: id-1");
        assertThat(corpo.path()).isEqualTo("/produtos/id-1");
        assertThat(corpo.timestamp()).isNotNull();
    }

    @Test
    void jaExiste_retorna409() {
        ResponseEntity<ApiError> resposta = handler.handleJaExiste(
                new ProdutoJaExisteException("Teclado"), requestPara("/produtos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody().message()).isEqualTo("Ja existe um produto com o nome: Teclado");
    }

    @Test
    void generico_retorna500ComMensagemFixa() {
        ResponseEntity<ApiError> resposta = handler.handleGenerico(
                new RuntimeException("stack trace secreto"), requestPara("/produtos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().message()).isEqualTo("Erro interno inesperado");
    }

    @Test
    void validacao_concatenaErrosDeCampo() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "produtoRequest");
        binding.addError(new FieldError("produtoRequest", "nome", "nome e obrigatorio"));
        binding.addError(new FieldError("produtoRequest", "preco", "preco deve ser maior que zero"));
        MethodParameter parametro = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", List.class), -1);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parametro, binding);

        ResponseEntity<ApiError> resposta = handler.handleValidacao(ex, requestPara("/produtos"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody().message())
                .isEqualTo("Falha de validacao: nome: nome e obrigatorio; preco: preco deve ser maior que zero");
    }

    @AfterEach
    void limparContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolvePath_semRequestESemContexto_retornaVazio() {
        // resolvePath tem fallback defensivo; sem RequestContextHolder ativo, retorna "".
        RequestContextHolder.resetRequestAttributes();
        ResponseEntity<ApiError> resposta = handler.handleNaoEncontrado(
                new ProdutoNaoEncontradoException("x"), null);

        assertThat(resposta.getBody().path()).isEqualTo("");
    }

    @Test
    void resolvePath_semRequestMasComContexto_usaRequestContextHolder() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/produtos/via-contexto");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        ResponseEntity<ApiError> resposta = handler.handleNaoEncontrado(
                new ProdutoNaoEncontradoException("x"), null);

        assertThat(resposta.getBody().path()).isEqualTo("/produtos/via-contexto");
    }

    @SuppressWarnings("unused")
    private void metodoDummy(List<String> ignore) {
    }
}
