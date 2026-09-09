package com.devcart.carrinhoservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devcart.carrinhoservice.dto.CarrinhoResponse;

class CarrinhoModelTest {

    @Test
    void itemCarrinho_subtotalEhPrecoVezesQuantidade() {
        ItemCarrinho item = new ItemCarrinho("p1", "Produto", 3, new BigDecimal("9.90"));

        assertThat(item.getSubtotal()).isEqualByComparingTo("29.70");
    }

    @Test
    void itemCarrinho_settersAtualizamEstado() {
        ItemCarrinho item = new ItemCarrinho();
        item.setProdutoId("p2");
        item.setNome("Outro");
        item.setQuantidade(5);
        item.setPrecoUnitario(new BigDecimal("2.00"));

        assertThat(item.getProdutoId()).isEqualTo("p2");
        assertThat(item.getNome()).isEqualTo("Outro");
        assertThat(item.getQuantidade()).isEqualTo(5);
        assertThat(item.getSubtotal()).isEqualByComparingTo("10.00");
    }

    @Test
    void carrinho_settersEGettersDeUsuarioEItens() {
        Carrinho carrinho = new Carrinho();
        carrinho.setUsuarioId("user-9");
        carrinho.setItens(List.of(new ItemCarrinho("p1", "P1", 1, new BigDecimal("1.00"))));

        assertThat(carrinho.getUsuarioId()).isEqualTo("user-9");
        assertThat(carrinho.getItens()).hasSize(1);
    }

    @Test
    void carrinhoResponse_fromEntityAgregaQuantidadeEValorTotal() {
        Carrinho carrinho = new Carrinho("user-1");
        carrinho.getItens().add(new ItemCarrinho("p1", "P1", 2, new BigDecimal("10.00")));
        carrinho.getItens().add(new ItemCarrinho("p2", "P2", 1, new BigDecimal("5.50")));

        CarrinhoResponse response = CarrinhoResponse.fromEntity(carrinho);

        assertThat(response.usuarioId()).isEqualTo("user-1");
        assertThat(response.itens()).hasSize(2);
        assertThat(response.quantidadeItens()).isEqualTo(3);
        assertThat(response.valorTotal()).isEqualByComparingTo("25.50");
    }

    @Test
    void carrinhoResponse_fromEntitySemItens() {
        CarrinhoResponse response = CarrinhoResponse.fromEntity(new Carrinho("user-2"));

        assertThat(response.itens()).isEmpty();
        assertThat(response.quantidadeItens()).isZero();
        assertThat(response.valorTotal()).isEqualByComparingTo("0");
    }
}
