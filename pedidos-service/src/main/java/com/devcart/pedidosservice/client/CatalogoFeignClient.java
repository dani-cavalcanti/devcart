package com.devcart.pedidosservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente sincrono para o catalogo.
 * {@code name = "catalogo-service"} + Eureka/Spring Cloud LoadBalancer no classpath
 * fazem o Feign resolver o destino como {@code lb://catalogo-service}.
 * RestTemplate nao e utilizado em nenhum ponto.
 */
@FeignClient(name = "catalogo-service", path = "/produtos")
public interface CatalogoFeignClient {

    @GetMapping("/{id}")
    ProdutoDto buscarPorId(@PathVariable("id") String id);
}
