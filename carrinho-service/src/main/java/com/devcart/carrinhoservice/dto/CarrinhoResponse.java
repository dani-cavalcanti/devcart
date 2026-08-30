package com.devcart.carrinhoservice.dto;

import java.math.BigDecimal;
import java.util.List;

import com.devcart.carrinhoservice.model.Carrinho;

public record CarrinhoResponse(

        String usuarioId,
        List<ItemResponse> itens,
        int quantidadeItens,
        BigDecimal valorTotal
) {

    public static CarrinhoResponse fromEntity(Carrinho carrinho) {
        List<ItemResponse> itens = carrinho.getItens().stream()
                .map(ItemResponse::fromEntity)
                .toList();

        int quantidadeItens = itens.stream()
                .mapToInt(ItemResponse::quantidade)
                .sum();

        BigDecimal valorTotal = itens.stream()
                .map(ItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CarrinhoResponse(carrinho.getUsuarioId(), itens, quantidadeItens, valorTotal);
    }
}
