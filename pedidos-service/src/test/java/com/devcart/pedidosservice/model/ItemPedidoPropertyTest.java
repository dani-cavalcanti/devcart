package com.devcart.pedidosservice.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

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
 * PBT do invariante de dominio de {@link ItemPedido}. Rodam so com {@code -Ppbt}.
 * A suite de exemplos cobre 100% de Line/Branch e fica verde; estas propriedades
 * falsificam dois defeitos de fronteira.
 */
@Tag("pbt")
@Label("ItemPedido — invariantes de quantidade e preco (jqwik)")
class ItemPedidoPropertyTest {

    @Provide
    Arbitrary<Integer> quantidadesInvalidas() {
        return Arbitraries.oneOf(
                Arbitraries.just(0),
                Arbitraries.integers().between(Integer.MIN_VALUE, -1));
    }

    @Provide
    Arbitrary<BigDecimal> decimaisExtensos() {
        return Arbitraries.bigDecimals()
                .between(new BigDecimal("-1000000"), new BigDecimal("1000000"))
                .ofScale(30);
    }

    @Property(tries = 500)
    @Report(Reporting.FALSIFIED)
    @Label("quantidade <= 0 sempre dispara IllegalArgumentException")
    void quantidadeMenorOuIgualAZeroDisparaIllegalArgument(
            @ForAll("quantidadesInvalidas") int quantidade,
            @ForAll("decimaisExtensos") BigDecimal preco) {

        assertThatThrownBy(() -> new ItemPedido("produto-1", "Produto", quantidade, preco))
                .as("quantidade %s deveria ter sido rejeitada", quantidade)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Property(tries = 500)
    @Report(Reporting.FALSIFIED)
    @Label("preco negativo sempre dispara IllegalArgumentException")
    void precoNegativoDisparaIllegalArgument(
            @ForAll("decimaisExtensos") BigDecimal preco) {

        if (preco.signum() >= 0) {
            return; // so nos interessa preco negativo
        }
        assertThatThrownBy(() -> new ItemPedido("produto-1", "Produto", 1, preco))
                .as("preco %s deveria ter sido rejeitado", preco)
                .isInstanceOf(IllegalArgumentException.class);
    }
}
