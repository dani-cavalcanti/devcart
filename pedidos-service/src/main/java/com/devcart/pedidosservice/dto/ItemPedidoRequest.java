package com.devcart.pedidosservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Item solicitado no pedido. O preco NAO vem do cliente:
 * e obtido do catalogo-service via Feign no momento da criacao.
 */
public record ItemPedidoRequest(

        @NotBlank(message = "produtoId e obrigatorio")
        String produtoId,

        @NotNull(message = "quantidade e obrigatoria")
        @Positive(message = "quantidade deve ser maior que zero")
        Integer quantidade
) {
}
