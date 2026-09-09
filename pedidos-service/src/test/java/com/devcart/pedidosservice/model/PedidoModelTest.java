package com.devcart.pedidosservice.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.devcart.pedidosservice.dto.PedidoResponse;
import com.devcart.pedidosservice.messaging.PedidoCriadoEvent;

class PedidoModelTest {

    // ---------- ItemPedido ----------

    @Test
    void itemPedido_subtotalEhPrecoVezesQuantidade() {
        ItemPedido item = new ItemPedido("p1", "Produto", 3, new BigDecimal("9.90"));

        assertThat(item.getSubtotal()).isEqualByComparingTo("29.70");
        assertThat(item.getProdutoId()).isEqualTo("p1");
        assertThat(item.getNomeProduto()).isEqualTo("Produto");
        assertThat(item.getQuantidade()).isEqualTo(3);
        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("9.90");
    }

    @Test
    void itemPedido_aceitaPrecoZero() {
        ItemPedido item = new ItemPedido("p1", "Brinde", 2, BigDecimal.ZERO);

        assertThat(item.getSubtotal()).isEqualByComparingTo("0");
    }

    @Test
    void itemPedido_construtorProtegidoDoJpaNaoPreencheCampos() {
        ItemPedido item = new ItemPedido();

        assertThat(item.getId()).isNull();
        assertThat(item.getPedido()).isNull();
        assertThat(item.getProdutoId()).isNull();
    }

    @Test
    void itemPedido_toStringExpoeProdutoQuantidadeEPreco() {
        ItemPedido item = new ItemPedido("p1", "Produto", 3, new BigDecimal("9.90"));

        assertThat(item.toString())
                .contains("produtoId='p1'")
                .contains("quantidade=3")
                .contains("precoUnitario=9.90");
    }

    @Test
    void itemPedido_rejeitaQuantidadeZeroOuNegativa() {
        assertThatThrownBy(() -> new ItemPedido("p1", "P", 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantidade do item deve ser maior que zero");
        assertThatThrownBy(() -> new ItemPedido("p1", "P", -1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemPedido_rejeitaPrecoNuloOuNegativo() {
        assertThatThrownBy(() -> new ItemPedido("p1", "P", 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ser nulo nem negativo");
        assertThatThrownBy(() -> new ItemPedido("p1", "P", 1, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Pedido ----------

    @Test
    void pedido_adicionarItemVinculaAoPedidoEAcumula() {
        Pedido pedido = new Pedido("user-1");
        ItemPedido item = new ItemPedido("p1", "P1", 2, new BigDecimal("5.00"));

        pedido.adicionarItem(item);

        assertThat(pedido.getItens()).containsExactly(item);
        assertThat(item.getPedido()).isSameAs(pedido);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CRIADO);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("0"); // ainda nao recalculado
    }

    @Test
    void pedido_recalcularTotalSomaSubtotais() {
        Pedido pedido = new Pedido("user-1");
        pedido.adicionarItem(new ItemPedido("p1", "P1", 2, new BigDecimal("5.00")));
        pedido.adicionarItem(new ItemPedido("p2", "P2", 3, new BigDecimal("1.50")));

        pedido.recalcularTotal();

        assertThat(pedido.getValorTotal()).isEqualByComparingTo("14.50");
        assertThat(pedido.getQuantidadeTotalItens()).isEqualTo(5);
    }

    @Test
    void pedido_recalcularTotalSemItensZera() {
        Pedido pedido = new Pedido("user-1");

        pedido.recalcularTotal();

        assertThat(pedido.getValorTotal()).isEqualByComparingTo("0");
        assertThat(pedido.getQuantidadeTotalItens()).isZero();
    }

    @Test
    void pedido_setStatusAlteraEstado() {
        Pedido pedido = new Pedido("user-1");

        pedido.setStatus(StatusPedido.CONFIRMADO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
    }

    @Test
    void pedido_construtorProtegidoDoJpaAplicaDefaults() {
        Pedido pedido = new Pedido();

        assertThat(pedido.getId()).isNull();
        assertThat(pedido.getUsuarioId()).isNull();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CRIADO);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("0");
        assertThat(pedido.getItens()).isEmpty();
        assertThat(pedido.getCriadoEm()).isNull();
    }

    @Test
    void pedido_aoPersistirDefineCriadoEmApenasUmaVez() {
        Pedido pedido = new Pedido("user-1");

        pedido.aoPersistir();
        var primeiro = pedido.getCriadoEm();
        assertThat(primeiro).isNotNull();

        pedido.aoPersistir();
        assertThat(pedido.getCriadoEm()).isSameAs(primeiro);
    }

    // ---------- DTO / evento ----------

    @Test
    void pedidoResponse_fromEntityMapeiaItensEStatus() {
        Pedido pedido = new Pedido("user-1");
        pedido.adicionarItem(new ItemPedido("p1", "P1", 2, new BigDecimal("5.00")));
        pedido.recalcularTotal();

        PedidoResponse response = PedidoResponse.fromEntity(pedido);

        assertThat(response.usuarioId()).isEqualTo("user-1");
        assertThat(response.status()).isEqualTo("CRIADO");
        assertThat(response.valorTotal()).isEqualByComparingTo("10.00");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).produtoId()).isEqualTo("p1");
        assertThat(response.itens().get(0).precoUnitario()).isEqualByComparingTo("5.00");
        assertThat(response.itens().get(0).subtotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void pedidoCriadoEvent_fromEntityResumeOPedido() {
        Pedido pedido = new Pedido("user-1");
        pedido.adicionarItem(new ItemPedido("p1", "P1", 2, new BigDecimal("5.00")));
        pedido.adicionarItem(new ItemPedido("p2", "P2", 1, new BigDecimal("3.00")));
        pedido.recalcularTotal();

        PedidoCriadoEvent evento = PedidoCriadoEvent.fromEntity(pedido);

        assertThat(evento.usuarioId()).isEqualTo("user-1");
        assertThat(evento.quantidadeItens()).isEqualTo(3);
        assertThat(evento.valorTotal()).isEqualByComparingTo("13.00");
    }

    @Test
    void statusPedido_possuiOsTresEstados() {
        assertThat(StatusPedido.valueOf("CRIADO")).isEqualTo(StatusPedido.CRIADO);
        assertThat(StatusPedido.values()).containsExactly(
                StatusPedido.CRIADO, StatusPedido.CONFIRMADO, StatusPedido.CANCELADO);
    }
}
