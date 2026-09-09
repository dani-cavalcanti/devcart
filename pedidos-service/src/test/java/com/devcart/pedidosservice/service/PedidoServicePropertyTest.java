package com.devcart.pedidosservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.context.ApplicationEventPublisher;

import com.devcart.pedidosservice.client.CatalogoClientService;
import com.devcart.pedidosservice.client.ProdutoDto;
import com.devcart.pedidosservice.dto.CriarPedidoRequest;
import com.devcart.pedidosservice.dto.ItemPedidoRequest;
import com.devcart.pedidosservice.exception.RegraNegocioException;
import com.devcart.pedidosservice.model.Pedido;
import com.devcart.pedidosservice.repository.PedidoRepository;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Report;
import net.jqwik.api.Reporting;
import net.jqwik.api.Tag;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;

/**
 * PBT da validacao de estoque em {@link PedidoService#criarPedido}. Rodam so com {@code -Ppbt}.
 * A suite de exemplos so cobre pedidos com o produto aparecendo UMA vez — o estoque e
 * validado linha a linha, sem somar quantidades do mesmo produto.
 */
@Tag("pbt")
@Label("PedidoService — validacao de estoque por produto (jqwik)")
class PedidoServicePropertyTest {

    /** Estoque E (2..50) + varias quantidades do MESMO produto, cada uma <= E, mas cuja soma > E. */
    @Provide
    Arbitrary<Tuple2<Integer, List<Integer>>> estoqueEQuantidadesDoMesmoProduto() {
        return Arbitraries.integers().between(2, 50).flatMap(estoque ->
                Arbitraries.integers().between(1, estoque)
                        .list().ofMinSize(2).ofMaxSize(6)
                        .filter(qs -> qs.stream().mapToInt(Integer::intValue).sum() > estoque)
                        .map(qs -> Tuple.of(estoque, qs)));
    }

    @Property(tries = 300)
    @Report(Reporting.FALSIFIED)
    @Label("estoque e validado pela SOMA das quantidades do mesmo produto")
    void estoqueValidaSomaDoMesmoProduto(
            @ForAll("estoqueEQuantidadesDoMesmoProduto") Tuple2<Integer, List<Integer>> caso) {

        int estoque = caso.get1();
        List<Integer> quantidades = caso.get2();

        PedidoRepository repo = mock(PedidoRepository.class);
        CatalogoClientService catalogo = mock(CatalogoClientService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        when(catalogo.buscarProduto(anyString())).thenReturn(CompletableFuture.completedFuture(
                new ProdutoDto("p1", "Produto", "desc", BigDecimal.TEN, estoque)));
        when(repo.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoService service = new PedidoService(repo, catalogo, publisher);

        List<ItemPedidoRequest> itens = quantidades.stream()
                .map(q -> new ItemPedidoRequest("p1", q))
                .toList();
        int somaPedida = quantidades.stream().mapToInt(Integer::intValue).sum();

        assertThatThrownBy(() -> service.criarPedido("user-1", new CriarPedidoRequest(itens)))
                .as("estoque %s, soma pedida %s do mesmo produto %s — deveria recusar",
                        estoque, somaPedida, quantidades)
                .isInstanceOf(RegraNegocioException.class);
    }
}
