package com.devcart.carrinhoservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devcart.carrinhoservice.dto.AdicionarItemRequest;
import com.devcart.carrinhoservice.dto.CarrinhoResponse;
import com.devcart.carrinhoservice.service.CarrinhoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private static final String HEADER_USUARIO = "X-User-Id";

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @PostMapping("/itens")
    @ResponseStatus(HttpStatus.CREATED)
    public CarrinhoResponse adicionarItem(@RequestHeader(HEADER_USUARIO) String usuarioId,
                                          @Valid @RequestBody AdicionarItemRequest request) {
        return carrinhoService.adicionarItem(usuarioId, request);
    }

    @GetMapping
    public CarrinhoResponse listar(@RequestHeader(HEADER_USUARIO) String usuarioId) {
        return carrinhoService.listarCarrinho(usuarioId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void limpar(@RequestHeader(HEADER_USUARIO) String usuarioId) {
        carrinhoService.limparCarrinho(usuarioId);
    }
}
