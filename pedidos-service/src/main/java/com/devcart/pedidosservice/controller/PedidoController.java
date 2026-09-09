package com.devcart.pedidosservice.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.devcart.pedidosservice.dto.CriarPedidoRequest;
import com.devcart.pedidosservice.dto.PedidoResponse;
import com.devcart.pedidosservice.service.PedidoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private static final String HEADER_USUARIO = "X-User-Id";

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestHeader(HEADER_USUARIO) String usuarioId,
                                                @Valid @RequestBody CriarPedidoRequest request,
                                                UriComponentsBuilder uriBuilder) {
        PedidoResponse criado = pedidoService.criarPedido(usuarioId, request);
        URI location = uriBuilder.path("/pedidos/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestHeader(HEADER_USUARIO) String usuarioId) {
        return pedidoService.listarPorUsuario(usuarioId);
    }
}
