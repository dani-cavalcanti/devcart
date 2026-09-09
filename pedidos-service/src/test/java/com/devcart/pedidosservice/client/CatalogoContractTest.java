package com.devcart.pedidosservice.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

import feign.Feign;
import feign.jackson.JacksonDecoder;

/**
 * Teste de contrato (CONSUMER). Descreve — do ponto de vista do pedidos-service — o que
 * ele espera do catalogo-service em {@code GET /produtos/{id}} e gera o pact
 * {@code target/pacts/pedidos-service-catalogo-service.json}.
 *
 * <p>Nao sobe o catalogo real: valida so que o {@link CatalogoFeignClient} de producao
 * fala HTTP/JSON conforme o contrato. A verificacao do lado do provider fica no
 * catalogo-service. Fora da suite padrao (tag {@code contract}); roda com {@code -Pcontract}.
 */
@Tag("contract")
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "catalogo-service", pactVersion = PactSpecVersion.V3)
class CatalogoContractTest {

    @Pact(consumer = "pedidos-service")
    RequestResponsePact produtoExistente(PactDslWithProvider builder) {
        return builder
                .given("produto PROD-1 existe")
                .uponReceiving("busca do produto PROD-1 pelo pedidos-service")
                    .path("/produtos/PROD-1")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(new PactDslJsonBody()
                            .stringType("id", "PROD-1")
                            .stringType("nome", "Teclado Mecanico")
                            .stringType("descricao", "Layout ABNT2")
                            .decimalType("preco", 199.90)
                            .integerType("estoque", 10))
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "produtoExistente")
    void catalogoFeignClientRespeitaOContrato(MockServer mockServer) {
        CatalogoFeignClient client = Feign.builder()
                .contract(new SpringMvcContract())
                .decoder(new JacksonDecoder())
                .target(CatalogoFeignClient.class, mockServer.getUrl() + "/produtos");

        ProdutoDto produto = client.buscarPorId("PROD-1");

        assertThat(produto.id()).isEqualTo("PROD-1");
        assertThat(produto.nome()).isEqualTo("Teclado Mecanico");
        assertThat(produto.descricao()).isEqualTo("Layout ABNT2");
        assertThat(produto.preco()).isEqualByComparingTo("199.90");
        assertThat(produto.estoque()).isEqualTo(10);
    }
}
