package com.devcart.pedidosservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Report;
import net.jqwik.api.Reporting;
import net.jqwik.api.Tag;

/**
 * PBT do motor de calculo de {@link Pedido#recalcularTotal()}. Rodam so com {@code -Ppbt}.
 */
@Tag("pbt")
@Label("Pedido — recalculo de total (jqwik)")
class PedidoPropertyTest {

    @Provide
    Arbitrary<List<ItemPedido>> itensValidos() {
        Arbitrary<Integer> quantidade = Arbitraries.integers().between(1, 1_000);
        Arbitrary<BigDecimal> preco = Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(10);
        Arbitrary<ItemPedido> item = Combinators.combine(quantidade, preco)
                .as((q, p) -> new ItemPedido("produto-" + q, "Produto", q, p));
        return item.list().ofMinSize(1).ofMaxSize(20);
    }

    @Property(tries = 500)
    @Report(Reporting.FALSIFIED)
    @Label("recalcularTotal e idempotente: chamar N vezes = chamar 1 vez")
    void recalcularTotalEhIdempotente(@ForAll("itensValidos") List<ItemPedido> itens) {
        Pedido pedido = new Pedido("usuario-property");
        itens.forEach(pedido::adicionarItem);

        pedido.recalcularTotal();
        BigDecimal umaVez = pedido.getValorTotal();

        pedido.recalcularTotal();
        pedido.recalcularTotal();
        BigDecimal varias = pedido.getValorTotal();

        assertThat(varias)
                .as("recalcularTotal nao e idempotente: 1x=%s, 3x=%s", umaVez, varias)
                .isEqualByComparingTo(umaVez);
    }
}
