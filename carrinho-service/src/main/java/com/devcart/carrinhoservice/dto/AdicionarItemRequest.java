package com.devcart.carrinhoservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload de entrada para adicionar um item ao carrinho.
 * O dono do carrinho NAO vem no corpo - vem do header X-User-Id.
 */
public record AdicionarItemRequest(

        @NotBlank(message = "produtoId e obrigatorio")
        String produtoId,

        @NotBlank(message = "nome e obrigatorio")
        String nome,

        @NotNull(message = "quantidade e obrigatoria")
        @Positive(message = "quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "precoUnitario e obrigatorio")
        @Positive(message = "precoUnitario deve ser maior que zero")
        BigDecimal precoUnitario
) {
}
