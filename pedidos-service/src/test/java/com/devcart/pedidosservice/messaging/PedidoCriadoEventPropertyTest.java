package com.devcart.pedidosservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.devcart.pedidosservice.model.ItemPedido;
import com.devcart.pedidosservice.model.Pedido;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Report;
import net.jqwik.api.Reporting;
import net.jqwik.api.Tag;

/**
 * PBT do mapeamento {@link PedidoCriadoEvent#fromEntity}. Rodam so com {@code -Ppbt}.
 * A suite de exemplos usa quantidade 1 por linha (onde nº de linhas == soma das quantidades).
 */
@Tag("pbt")
@Label("PedidoCriadoEvent — quantidadeItens do evento (jqwik)")
class PedidoCriadoEventPropertyTest {

    @Provide
    Arbitrary<List<Integer>> quantidadesPorLinha() {
        return Arbitraries.integers().between(1, 1_000).list().ofMinSize(1).ofMaxSize(15);
    }

    @Property(tries = 500)
    @Report(Reporting.FALSIFIED)
    @Label("quantidadeItens do evento == soma das quantidades das linhas (nao o nº de linhas)")
    void quantidadeItensSomaAsLinhas(@ForAll("quantidadesPorLinha") List<Integer> quantidades) {
        Pedido pedido = new Pedido("user-1");
        quantidades.forEach(q ->
                pedido.adicionarItem(new ItemPedido("p" + q, "Produto", q, BigDecimal.ONE)));
        pedido.recalcularTotal();

        PedidoCriadoEvent evento = PedidoCriadoEvent.fromEntity(pedido);

        int somaEsperada = quantidades.stream().mapToInt(Integer::intValue).sum();
        assertThat(evento.quantidadeItens())
                .as("evento.quantidadeItens=%s, mas a soma das linhas e %s (%s linhas)",
                        evento.quantidadeItens(), somaEsperada, quantidades.size())
                .isEqualTo(somaEsperada);
    }
}
