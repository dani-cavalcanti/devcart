package com.devcart.carrinhoservice.dto;

import java.math.BigDecimal;

import com.devcart.carrinhoservice.model.ItemCarrinho;

public record ItemResponse(

        String produtoId,
        String nome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static ItemResponse fromEntity(ItemCarrinho item) {
        return new ItemResponse(
                item.getProdutoId(),
                item.getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
