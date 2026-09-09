package com.devcart.pedidosservice.client;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.devcart.pedidosservice.exception.CatalogoIndisponivelException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

/**
 * Fachada resiliente sobre o {@link CatalogoFeignClient}.
 *
 * <p>{@code @TimeLimiter} exige retorno assincrono ({@link CompletableFuture}) e aplica
 * o timeout estrito de 2s (configurado em {@code resilience4j.timelimiter.instances.catalogo}).
 * {@code @CircuitBreaker} envolve a chamada e contabiliza timeouts/erros como falha,
 * abrindo o circuito e acionando o {@code fallback}.
 */
@Service
public class CatalogoClientService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoClientService.class);
    private static final String INSTANCIA = "catalogo";

    private final CatalogoFeignClient catalogoFeignClient;

    public CatalogoClientService(CatalogoFeignClient catalogoFeignClient) {
        this.catalogoFeignClient = catalogoFeignClient;
    }

    @CircuitBreaker(name = INSTANCIA, fallbackMethod = "buscarProdutoFallback")
    @TimeLimiter(name = INSTANCIA)
    public CompletableFuture<ProdutoDto> buscarProduto(String produtoId) {
        return CompletableFuture.supplyAsync(() -> catalogoFeignClient.buscarPorId(produtoId));
    }

    @SuppressWarnings("unused")
    private CompletableFuture<ProdutoDto> buscarProdutoFallback(String produtoId, Throwable ex) {
        log.warn("Fallback do catalogo para produto {}: {}", produtoId, ex.toString());
        return CompletableFuture.failedFuture(new CatalogoIndisponivelException(produtoId, ex));
    }
}
