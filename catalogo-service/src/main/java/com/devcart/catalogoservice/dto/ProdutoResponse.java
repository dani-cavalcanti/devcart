package com.devcart.catalogoservice.dto;

import java.math.BigDecimal;

import com.devcart.catalogoservice.model.Produto;

/**
 * Representacao de saida do produto exposta pela API.
 * A entidade {@link Produto} nunca trafega pela controller.
 */
public record ProdutoResponse(

        String id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer estoque
) {

    public static ProdutoResponse fromEntity(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getEstoque()
        );
    }
}
