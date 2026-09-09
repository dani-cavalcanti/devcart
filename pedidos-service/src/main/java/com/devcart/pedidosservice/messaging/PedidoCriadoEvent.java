package com.devcart.pedidosservice.messaging;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.devcart.pedidosservice.model.Pedido;

/** Evento publicado na fila {@code pedidos.criados} apos a persistencia do pedido. */
public record PedidoCriadoEvent(

        Long pedidoId,
        String usuarioId,
        BigDecimal valorTotal,
        int quantidadeItens,
        OffsetDateTime criadoEm
) {

    public static PedidoCriadoEvent fromEntity(Pedido pedido) {
        return new PedidoCriadoEvent(
                pedido.getId(),
                pedido.getUsuarioId(),
                pedido.getValorTotal(),
                pedido.getQuantidadeTotalItens(),
                pedido.getCriadoEm()
        );
    }
}
