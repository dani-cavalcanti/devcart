package com.devcart.catalogoservice.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Payload de entrada para criacao/atualizacao de produto.
 * Validado com Bean Validation via {@code @Valid} na controller.
 */
public record ProdutoRequest(

        @NotBlank(message = "nome e obrigatorio")
        String nome,

        @NotBlank(message = "descricao e obrigatoria")
        String descricao,

        @NotNull(message = "preco e obrigatorio")
        @Positive(message = "preco deve ser maior que zero")
        BigDecimal preco,

        // estoque zero e valido (produto cadastrado sem unidades disponiveis)
        @NotNull(message = "estoque e obrigatorio")
        @PositiveOrZero(message = "estoque nao pode ser negativo")
        Integer estoque
) {
}
