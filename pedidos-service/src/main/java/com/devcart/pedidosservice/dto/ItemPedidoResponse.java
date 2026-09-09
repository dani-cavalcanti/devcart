package com.devcart.pedidosservice.dto;

import java.math.BigDecimal;

import com.devcart.pedidosservice.model.ItemPedido;

public record ItemPedidoResponse(

        String produtoId,
        String nomeProduto,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static ItemPedidoResponse fromEntity(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getProdutoId(),
                item.getNomeProduto(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal()
        );
    }
}
