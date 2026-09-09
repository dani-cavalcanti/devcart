package com.devcart.pedidosservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.devcart.pedidosservice.model.Pedido;

public record PedidoResponse(

        Long id,
        String usuarioId,
        String status,
        OffsetDateTime criadoEm,
        BigDecimal valorTotal,
        List<ItemPedidoResponse> itens
) {

    public static PedidoResponse fromEntity(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getStatus().name(),
                pedido.getCriadoEm(),
                pedido.getValorTotal(),
                pedido.getItens().stream().map(ItemPedidoResponse::fromEntity).toList()
        );
    }
}
