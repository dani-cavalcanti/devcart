package com.devcart.pedidosservice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import com.devcart.pedidosservice.model.ItemPedido;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Report;
import net.jqwik.api.Reporting;
import net.jqwik.api.Tag;
import net.jqwik.api.constraints.IntRange;

/**
 * PBT do mapeamento {@link ItemPedidoResponse#fromEntity}. Rodam so com {@code -Ppbt}.
 * A suite de exemplos usa quantidade 1 (onde preco == subtotal) e nao distingue os campos.
 */
@Tag("pbt")
@Label("ItemPedidoResponse — mapeamento preco/subtotal (jqwik)")
class ItemPedidoResponsePropertyTest {

    @Provide
    Arbitrary<BigDecimal> precosNaoNegativos() {
        return Arbitraries.bigDecimals()
                .between(BigDecimal.ZERO, new BigDecimal("100000"))
                .ofScale(20);
    }

    @Property(tries = 500)
    @Report(Reporting.FALSIFIED)
    @Label("no DTO, subtotal == precoUnitario x quantidade e precoUnitario == preco da entidade")
    void mapeamentoPreservaPrecoESubtotal(
            @ForAll @IntRange(min = 1, max = 100_000) int quantidade,
            @ForAll("precosNaoNegativos") BigDecimal preco) {

        ItemPedido item = new ItemPedido("p1", "Produto", quantidade, preco);

        ItemPedidoResponse dto = ItemPedidoResponse.fromEntity(item);

        assertThat(dto.precoUnitario())
                .as("precoUnitario do DTO %s != preco da entidade %s", dto.precoUnitario(), preco)
                .isEqualByComparingTo(preco);
        assertThat(dto.subtotal())
                .as("subtotal do DTO %s != preco %s x qtd %s", dto.subtotal(), preco, quantidade)
                .isEqualByComparingTo(preco.multiply(BigDecimal.valueOf(quantidade)));
    }
}
