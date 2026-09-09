package com.devcart.carrinhoservice.model;

import java.math.BigDecimal;

/**
 * Item persistido dentro do hash Redis do carrinho.
 * POJO mutavel (exigencia do mapeamento de objetos aninhados do Spring Data Redis).
 * Nao e um DTO - nao trafega pela controller.
 */
public class ItemCarrinho {

    private String produtoId;
    private String nome;
    private int quantidade;
    private BigDecimal precoUnitario;

    public ItemCarrinho() {
    }

    public ItemCarrinho(String produtoId, String nome, int quantidade, BigDecimal precoUnitario) {
        this.produtoId = produtoId;
        this.nome = nome;
        this.quantidade = quantidade;
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public String getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(String produtoId) {
        this.produtoId = produtoId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }
}
