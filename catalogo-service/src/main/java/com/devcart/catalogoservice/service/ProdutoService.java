package com.devcart.catalogoservice.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.devcart.catalogoservice.dto.ProdutoRequest;
import com.devcart.catalogoservice.dto.ProdutoResponse;
import com.devcart.catalogoservice.exception.ProdutoJaExisteException;
import com.devcart.catalogoservice.exception.ProdutoNaoEncontradoException;
import com.devcart.catalogoservice.model.Produto;
import com.devcart.catalogoservice.repository.ProdutoRepository;

/**
 * Regras de negocio do catalogo. Opera sobre a entidade internamente e
 * entrega/aceita apenas DTOs, garantindo que {@link Produto} nao vaze
 * para a camada de apresentacao.
 */
@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    // Injecao obrigatoriamente por construtor (sem @Autowired em campo).
    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    public ProdutoResponse criar(ProdutoRequest request) {
        if (produtoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new ProdutoJaExisteException(request.nome());
        }
        Produto salvo = produtoRepository.save(toEntity(null, request));
        return ProdutoResponse.fromEntity(salvo);
    }

    public List<ProdutoResponse> listar() {
        return produtoRepository.findAll().stream()
                .map(ProdutoResponse::fromEntity)
                .toList();
    }

    public ProdutoResponse buscarPorId(String id) {
        return ProdutoResponse.fromEntity(obterEntidade(id));
    }

    public ProdutoResponse atualizar(String id, ProdutoRequest request) {
        Produto existente = obterEntidade(id);

        if (!existente.getNome().equalsIgnoreCase(request.nome())
                && produtoRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new ProdutoJaExisteException(request.nome());
        }

        existente.setNome(request.nome());
        existente.setDescricao(request.descricao());
        existente.setPreco(request.preco());
        existente.setEstoque(request.estoque());

        return ProdutoResponse.fromEntity(produtoRepository.save(existente));
    }

    public void remover(String id) {
        Produto existente = obterEntidade(id);
        produtoRepository.delete(existente);
    }

    private Produto obterEntidade(String id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private Produto toEntity(String id, ProdutoRequest request) {
        return new Produto(
                id,
                request.nome(),
                request.descricao(),
                request.preco(),
                request.estoque()
        );
    }
}
