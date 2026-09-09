package com.devcart.pedidosservice.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CriarPedidoRequest(

        @NotEmpty(message = "o pedido deve conter ao menos um item")
        @Valid
        List<ItemPedidoRequest> itens
) {
}
