package com.devcart.pedidosservice.client;

import java.math.BigDecimal;

/** Projecao do produto retornada pelo catalogo-service. */
public record ProdutoDto(

        String id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque
) {
}
