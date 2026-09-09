package com.devcart.catalogoservice.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.devcart.catalogoservice.dto.ProdutoResponse;

class ProdutoTest {

    @Test
    void construtorPreencheTodosOsCampos() {
        Produto p = new Produto("id-1", "Teclado", "Mecanico", new BigDecimal("199.90"), 10);

        assertThat(p.getId()).isEqualTo("id-1");
        assertThat(p.getNome()).isEqualTo("Teclado");
        assertThat(p.getDescricao()).isEqualTo("Mecanico");
        assertThat(p.getPreco()).isEqualByComparingTo("199.90");
        assertThat(p.getEstoque()).isEqualTo(10);
    }

    @Test
    void settersAtualizamOsCampos() {
        Produto p = new Produto();
        p.setId("id-2");
        p.setNome("Mouse");
        p.setDescricao("Sem fio");
        p.setPreco(new BigDecimal("89.90"));
        p.setEstoque(3);

        assertThat(p.getId()).isEqualTo("id-2");
        assertThat(p.getNome()).isEqualTo("Mouse");
        assertThat(p.getDescricao()).isEqualTo("Sem fio");
        assertThat(p.getPreco()).isEqualByComparingTo("89.90");
        assertThat(p.getEstoque()).isEqualTo(3);
    }

    @Test
    void fromEntity_copiaCamposParaOResponse() {
        Produto p = new Produto("id-3", "Monitor", "27 pol", new BigDecimal("1200.00"), 7);

        ProdutoResponse response = ProdutoResponse.fromEntity(p);

        assertThat(response.id()).isEqualTo("id-3");
        assertThat(response.nome()).isEqualTo("Monitor");
        assertThat(response.descricao()).isEqualTo("27 pol");
        assertThat(response.preco()).isEqualByComparingTo("1200.00");
        assertThat(response.estoque()).isEqualTo(7);
    }
}
