package com.devcart.catalogoservice.contract;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devcart.catalogoservice.dto.ProdutoResponse;
import com.devcart.catalogoservice.service.ProdutoService;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

/**
 * Verificacao de contrato (PROVIDER). Sobe o catalogo-service de verdade (Tomcat em porta
 * aleatoria) e reproduz cada interacao do pact publicado pelo pedidos-service, conferindo
 * que a resposta real de {@code GET /produtos/{id}} continua compativel com o contrato.
 *
 * <p>{@link ProdutoService} e mockado por {@code @State} — o foco e o formato HTTP/JSON,
 * nao a persistencia. Fora da suite padrao (tag {@code contract}); roda com {@code -Pcontract}.
 */
@Tag("contract")
@Provider("catalogo-service")
@PactFolder("pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
class CatalogoProviderContractTest {

    @MockitoBean
    private ProdutoService produtoService;

    @LocalServerPort
    private int port;

    @BeforeEach
    void apontaParaOServidorLocal(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verificaContratoComOPedidosService(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("produto PROD-1 existe")
    void produtoProd1Existe() {
        when(produtoService.buscarPorId("PROD-1")).thenReturn(new ProdutoResponse(
                "PROD-1", "Teclado Mecanico", "Layout ABNT2", new BigDecimal("199.90"), 10));
    }
}
