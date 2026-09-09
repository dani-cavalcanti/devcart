package com.devcart.carrinhoservice.service;

import org.springframework.stereotype.Service;

import com.devcart.carrinhoservice.dto.AdicionarItemRequest;
import com.devcart.carrinhoservice.dto.CarrinhoResponse;
import com.devcart.carrinhoservice.exception.RegraNegocioException;
import com.devcart.carrinhoservice.exception.UsuarioNaoInformadoException;
import com.devcart.carrinhoservice.model.Carrinho;
import com.devcart.carrinhoservice.model.ItemCarrinho;
import com.devcart.carrinhoservice.repository.CarrinhoRepository;

/**
 * Regras de negocio do carrinho. O carrinho e sempre resolvido pela chave
 * do usuario (X-User-Id). Injecao por construtor.
 */
@Service
public class CarrinhoService {

    /** Limite de unidades por produto no carrinho. */
    private static final int QUANTIDADE_MAXIMA_POR_ITEM = 100;

    private final CarrinhoRepository carrinhoRepository;

    public CarrinhoService(CarrinhoRepository carrinhoRepository) {
        this.carrinhoRepository = carrinhoRepository;
    }

    public CarrinhoResponse adicionarItem(String usuarioId, AdicionarItemRequest request) {
        String chave = validarUsuario(usuarioId);
        Carrinho carrinho = carrinhoRepository.findById(chave).orElseGet(() -> new Carrinho(chave));

        ItemCarrinho existente = carrinho.getItens().stream()
                .filter(item -> item.getProdutoId().equals(request.produtoId()))
                .findFirst()
                .orElse(null);

        int novaQuantidade = (existente == null ? 0 : existente.getQuantidade()) + request.quantidade();
        if (novaQuantidade > QUANTIDADE_MAXIMA_POR_ITEM) {
            throw new RegraNegocioException(
                    "Quantidade do produto " + request.produtoId() + " excede o limite de " + QUANTIDADE_MAXIMA_POR_ITEM);
        }

        if (existente == null) {
            carrinho.getItens().add(new ItemCarrinho(
                    request.produtoId(),
                    request.nome(),
                    request.quantidade(),
                    request.precoUnitario()));
        } else {
            existente.setQuantidade(novaQuantidade);
            existente.setNome(request.nome());
            existente.setPrecoUnitario(request.precoUnitario());
        }

        return CarrinhoResponse.fromEntity(carrinhoRepository.save(carrinho));
    }

    public CarrinhoResponse listarCarrinho(String usuarioId) {
        String chave = validarUsuario(usuarioId);
        return carrinhoRepository.findById(chave)
                .map(CarrinhoResponse::fromEntity)
                .orElseGet(() -> CarrinhoResponse.fromEntity(new Carrinho(chave)));
    }

    public void limparCarrinho(String usuarioId) {
        String chave = validarUsuario(usuarioId);
        carrinhoRepository.deleteById(chave);
    }

    private String validarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new UsuarioNaoInformadoException();
        }
        return usuarioId.trim();
    }
}
