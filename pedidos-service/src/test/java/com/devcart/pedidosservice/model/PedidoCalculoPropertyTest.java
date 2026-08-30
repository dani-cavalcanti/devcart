package com.devcart.pedidosservice.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Report;
import net.jqwik.api.Reporting;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple3;

/**
 * Property-Based Testing (jqwik) das regras de dominio do pedido:
 * <ul>
 *   <li>calculo do valor total ({@link Pedido#recalcularTotal()} + {@link ItemPedido#getSubtotal()});</li>
 *   <li>validacao de quantidade e preco no construtor de {@link ItemPedido}.</li>
 * </ul>
 *
 * <p>Quando uma propriedade falha, o jqwik imprime o <b>counterexample</b>:
 * a {@code Original Sample} (amostra que quebrou) e a {@code Shrunk Sample}
 * (contraexemplo minimo apos o shrinking). O {@link Report}({@link Reporting#FALSIFIED})
 * em cada propriedade torna esses parametros explicitos no relatorio de falha.
 */
class PedidoCalculoPropertyTest {

    // =========================================================== GERADORES (@Provide)

    /** Numeros negativos + zero: quantidades invalidas. */
    @Provide
    Arbitrary<Integer> quantidadesNaoPositivas() {
        return Arbitraries.oneOf(
                Arbitraries.just(0),
                Arbitraries.just(Integer.MIN_VALUE),
                Arbitraries.integers().between(Integer.MIN_VALUE + 1, -1));
    }

    /** Inteiros grandes e positivos, ate {@code Integer.MAX_VALUE}. */
    @Provide
    Arbitrary<Integer> quantidadesPositivas() {
        return Arbitraries.oneOf(
                Arbitraries.just(1),
                Arbitraries.just(Integer.MAX_VALUE),
                Arbitraries.integers().between(1, Integer.MAX_VALUE));
    }

    /**
     * Precos {@code >= 0}: inclui zero, decimais de escala extensa e valores grandes.
     * unscaled em [0, 10^24] e escala em [0, 50] -> ate ~75 digitos.
     */
    @Provide
    Arbitrary<BigDecimal> precosNaoNegativos() {
        Arbitrary<BigInteger> unscaled = Arbitraries.bigIntegers()
                .between(BigInteger.ZERO, BigInteger.TEN.pow(24));
        Arbitrary<Integer> escala = Arbitraries.integers().between(0, 50);
        return Combinators.combine(unscaled, escala).as((valor, s) -> new BigDecimal(valor, s));
    }

    /** Precos estritamente negativos: invalidos. */
    @Provide
    Arbitrary<BigDecimal> precosNegativos() {
        Arbitrary<BigInteger> unscaled = Arbitraries.bigIntegers()
                .between(BigInteger.ONE, BigInteger.TEN.pow(24));
        Arbitrary<Integer> escala = Arbitraries.integers().between(0, 50);
        return Combinators.combine(unscaled, escala).as((valor, s) -> new BigDecimal(valor, s).negate());
    }

    /** Lista de itens validos (quantidade &gt; 0, preco &gt;= 0) para montar um pedido. */
    @Provide
    Arbitrary<List<Tuple3<String, Integer, BigDecimal>>> itensValidos() {
        Arbitrary<Tuple3<String, Integer, BigDecimal>> item = Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(8),
                quantidadesPositivas(),
                precosNaoNegativos()
        ).as((id, quantidade, preco) -> Tuple.of(id, quantidade, preco));
        return item.list().ofMinSize(0).ofMaxSize(15);
    }

    // =========================================================== PROPRIEDADES

    /** O valor total do pedido nunca pode ser negativo nem NaN, e deve ser o somatorio exato. */
    @Property
    @Report(Reporting.FALSIFIED)
    void oValorTotalNuncaEhNegativoNemNaN(
            @ForAll("itensValidos") List<Tuple3<String, Integer, BigDecimal>> itens) {

        Pedido pedido = new Pedido("user-pbt");
        itens.forEach(t -> pedido.adicionarItem(new ItemPedido(t.get1(), t.get1(), t.get2(), t.get3())));

        pedido.recalcularTotal();
        BigDecimal total = pedido.getValorTotal();

        assertThat(total).isNotNull();
        assertThat(total.signum()).isGreaterThanOrEqualTo(0);          // nunca negativo
        assertThat(Double.isNaN(total.doubleValue())).isFalse();        // nunca NaN

        BigDecimal esperado = itens.stream()
                .map(t -> t.get3().multiply(BigDecimal.valueOf(t.get2())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(esperado);              // calculo exato
    }

    /** Subtotal do item = preco x quantidade, e nunca negativo quando os dados sao validos. */
    @Property
    @Report(Reporting.FALSIFIED)
    void subtotalDoItemEhPrecoVezesQuantidadeENuncaNegativo(
            @ForAll("quantidadesPositivas") int quantidade,
            @ForAll("precosNaoNegativos") BigDecimal preco) {

        ItemPedido item = new ItemPedido("p1", "Produto", quantidade, preco);

        assertThat(item.getSubtotal())
                .isEqualByComparingTo(preco.multiply(BigDecimal.valueOf(quantidade)));
        assertThat(item.getSubtotal().signum()).isGreaterThanOrEqualTo(0);
    }

    /** Quantidade &lt;= 0 SEMPRE dispara IllegalArgumentException. */
    @Property
    @Report(Reporting.FALSIFIED)
    void quantidadeMenorOuIgualAzeroDisparaIllegalArgumentException(
            @ForAll("quantidadesNaoPositivas") int quantidade,
            @ForAll("precosNaoNegativos") BigDecimal preco) {

        assertThatThrownBy(() -> new ItemPedido("p1", "Produto", quantidade, preco))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantidade");
    }

    /** Preco negativo SEMPRE dispara IllegalArgumentException. */
    @Property
    @Report(Reporting.FALSIFIED)
    void precoNegativoDisparaIllegalArgumentException(
            @ForAll("quantidadesPositivas") int quantidade,
            @ForAll("precosNegativos") BigDecimal preco) {

        assertThatThrownBy(() -> new ItemPedido("p1", "Produto", quantidade, preco))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precoUnitario");
    }
}
