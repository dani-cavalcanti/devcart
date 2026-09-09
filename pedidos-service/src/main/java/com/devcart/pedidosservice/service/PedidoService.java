package com.devcart.pedidosservice.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devcart.pedidosservice.client.CatalogoClientService;
import com.devcart.pedidosservice.client.ProdutoDto;
import com.devcart.pedidosservice.dto.CriarPedidoRequest;
import com.devcart.pedidosservice.dto.ItemPedidoRequest;
import com.devcart.pedidosservice.dto.PedidoResponse;
import com.devcart.pedidosservice.exception.CatalogoIndisponivelException;
import com.devcart.pedidosservice.exception.PedidoNaoEncontradoException;
import com.devcart.pedidosservice.exception.RegraNegocioException;
import com.devcart.pedidosservice.messaging.PedidoCriadoEvent;
import com.devcart.pedidosservice.model.ItemPedido;
import com.devcart.pedidosservice.model.Pedido;
import com.devcart.pedidosservice.repository.PedidoRepository;

/**
 * Regras de negocio de pedidos. Injecao 100% por construtor.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CatalogoClientService catalogoClientService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PedidoService(PedidoRepository pedidoRepository,
                         CatalogoClientService catalogoClientService,
                         ApplicationEventPublisher applicationEventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.catalogoClientService = catalogoClientService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public PedidoResponse criarPedido(String usuarioId, CriarPedidoRequest request) {
        String dono = validarUsuario(usuarioId);

        // O mesmo produto pode aparecer em varias linhas: consolida a quantidade
        // total pedida por produto ANTES de validar o estoque.
        Map<String, Integer> quantidadePorProduto = new LinkedHashMap<>();
        for (ItemPedidoRequest itemReq : request.itens()) {
            quantidadePorProduto.merge(itemReq.produtoId(), itemReq.quantidade(), Integer::sum);
        }

        Pedido pedido = new Pedido(dono);
        for (Map.Entry<String, Integer> linha : quantidadePorProduto.entrySet()) {
            int quantidadeTotal = linha.getValue();
            ProdutoDto produto = consultarProduto(linha.getKey());

            if (produto.estoque() == null || produto.estoque() < quantidadeTotal) {
                throw new RegraNegocioException(
                        "Estoque insuficiente para o produto " + linha.getKey());
            }

            pedido.adicionarItem(new ItemPedido(
                    produto.id(),
                    produto.nome(),
                    quantidadeTotal,
                    produto.preco()));
        }
        pedido.recalcularTotal();

        Pedido salvo = pedidoRepository.save(pedido);

        // Evento de dominio -> publicado no RabbitMQ apos o commit (ver PedidoEventListener).
        applicationEventPublisher.publishEvent(PedidoCriadoEvent.fromEntity(salvo));

        return PedidoResponse.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .map(PedidoResponse::fromEntity)
                .orElseThrow(() -> new PedidoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorUsuario(String usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(validarUsuario(usuarioId)).stream()
                .map(PedidoResponse::fromEntity)
                .toList();
    }

    /** Executa a chamada Feign resiliente e normaliza as falhas assincronas. */
    private ProdutoDto consultarProduto(String produtoId) {
        try {
            return catalogoClientService.buscarProduto(produtoId).join();
        } catch (CompletionException ex) {
            // join() sempre embrulha a falha com a causa real preenchida.
            Throwable causa = ex.getCause();
            if (causa instanceof CatalogoIndisponivelException cie) {
                throw cie;
            }
            if (causa instanceof RegraNegocioException rne) {
                throw rne;
            }
            throw new CatalogoIndisponivelException(produtoId, causa);
        }
    }

    private String validarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new RegraNegocioException("Header X-User-Id e obrigatorio");
        }
        return usuarioId.trim();
    }
}
