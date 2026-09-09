package com.devcart.pedidosservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.devcart.pedidosservice.client.CatalogoClientService;
import com.devcart.pedidosservice.client.ProdutoDto;
import com.devcart.pedidosservice.dto.CriarPedidoRequest;
import com.devcart.pedidosservice.dto.ItemPedidoRequest;
import com.devcart.pedidosservice.dto.PedidoResponse;
import com.devcart.pedidosservice.exception.CatalogoIndisponivelException;
import com.devcart.pedidosservice.exception.PedidoNaoEncontradoException;
import com.devcart.pedidosservice.exception.RegraNegocioException;
import com.devcart.pedidosservice.messaging.PedidoCriadoEvent;
import com.devcart.pedidosservice.model.Pedido;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private com.devcart.pedidosservice.repository.PedidoRepository pedidoRepository;

    @Mock
    private CatalogoClientService catalogoClientService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PedidoService pedidoService;

    private static ProdutoDto produto(String id, BigDecimal preco, Integer estoque) {
        return new ProdutoDto(id, "Produto " + id, "desc", preco, estoque);
    }

    private static CriarPedidoRequest pedidoCom(ItemPedidoRequest... itens) {
        return new CriarPedidoRequest(List.of(itens));
    }

    // ---------- criarPedido: caminho feliz ----------

    @Test
    void criarPedido_calculaTotalPersisteEPublicaEvento() {
        when(catalogoClientService.buscarProduto("p1"))
                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), 50)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse response = pedidoService.criarPedido("  user-1 ",
                pedidoCom(new ItemPedidoRequest("p1", 3)));

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(captor.capture());
        Pedido salvo = captor.getValue();
        assertThat(salvo.getUsuarioId()).isEqualTo("user-1");
        assertThat(salvo.getItens()).hasSize(1);
        assertThat(salvo.getValorTotal()).isEqualByComparingTo("30.00");

        assertThat(response.usuarioId()).isEqualTo("user-1");
        assertThat(response.status()).isEqualTo("CRIADO");
        assertThat(response.valorTotal()).isEqualByComparingTo("30.00");
        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).subtotal()).isEqualByComparingTo("30.00");

        verify(applicationEventPublisher).publishEvent(any(PedidoCriadoEvent.class));
    }

    @Test
    void criarPedido_somaSubtotaisDeMultiplosItens() {
        when(catalogoClientService.buscarProduto("p1"))
                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), 50)));
        when(catalogoClientService.buscarProduto("p2"))
                .thenReturn(CompletableFuture.completedFuture(produto("p2", new BigDecimal("2.50"), 50)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse response = pedidoService.criarPedido("user-1",
                pedidoCom(new ItemPedidoRequest("p1", 2), new ItemPedidoRequest("p2", 4)));

        assertThat(response.valorTotal()).isEqualByComparingTo("30.00");
        assertThat(response.itens()).hasSize(2);
    }

    // ---------- criarPedido: validacao de usuario ----------

    @Test
    void criarPedido_lancaQuandoUsuarioNulo() {
        assertThatThrownBy(() -> pedidoService.criarPedido(null, pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Header X-User-Id e obrigatorio");

        verifyNoInteractions(catalogoClientService, pedidoRepository, applicationEventPublisher);
    }

    @Test
    void criarPedido_lancaQuandoUsuarioEmBranco() {
        assertThatThrownBy(() -> pedidoService.criarPedido("   ", pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isInstanceOf(RegraNegocioException.class);
    }

    // ---------- criarPedido: regras de estoque ----------

    @Test
    void criarPedido_lancaQuandoEstoqueInsuficiente() {
        when(catalogoClientService.buscarProduto("p1"))
                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), 2)));

        assertThatThrownBy(() -> pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 5))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Estoque insuficiente para o produto p1");

        verify(pedidoRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void criarPedido_lancaQuandoEstoqueNulo() {
        when(catalogoClientService.buscarProduto("p1"))
                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), null)));

        assertThatThrownBy(() -> pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
    }

    @Test
    void criarPedido_aceitaQuandoEstoqueIgualAQuantidade() {
        when(catalogoClientService.buscarProduto("p1"))
                .thenReturn(CompletableFuture.completedFuture(produto("p1", new BigDecimal("10.00"), 4)));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        PedidoResponse response = pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 4)));

        assertThat(response.valorTotal()).isEqualByComparingTo("40.00");
    }

    // ---------- criarPedido: falhas assincronas do catalogo ----------

    @Test
    void criarPedido_propagaCatalogoIndisponivelSemEmbrulharDeNovo() {
        CatalogoIndisponivelException original = new CatalogoIndisponivelException("p1", new RuntimeException("timeout"));
        when(catalogoClientService.buscarProduto("p1")).thenReturn(CompletableFuture.failedFuture(original));

        assertThatThrownBy(() -> pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isSameAs(original);
    }

    @Test
    void criarPedido_propagaRegraNegocioVindaDoCatalogo() {
        RegraNegocioException original = new RegraNegocioException("produto invalido no catalogo");
        when(catalogoClientService.buscarProduto("p1")).thenReturn(CompletableFuture.failedFuture(original));

        assertThatThrownBy(() -> pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isSameAs(original);
    }

    @Test
    void criarPedido_embrulhaFalhaGenericaEmCatalogoIndisponivel() {
        IllegalStateException causaRaiz = new IllegalStateException("conexao recusada");
        when(catalogoClientService.buscarProduto("p1")).thenReturn(CompletableFuture.failedFuture(causaRaiz));

        assertThatThrownBy(() -> pedidoService.criarPedido("user-1", pedidoCom(new ItemPedidoRequest("p1", 1))))
                .isInstanceOf(CatalogoIndisponivelException.class)
                .hasMessage("Catalogo indisponivel ao consultar o produto p1")
                .hasCause(causaRaiz);
    }

    // ---------- buscarPorId ----------

    @Test
    void buscarPorId_retornaResponseQuandoExiste() {
        Pedido pedido = new Pedido("user-1");
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponse response = pedidoService.buscarPorId(1L);

        assertThat(response.usuarioId()).isEqualTo("user-1");
        assertThat(response.status()).isEqualTo("CRIADO");
    }

    @Test
    void buscarPorId_lancaQuandoNaoExiste() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.buscarPorId(99L))
                .isInstanceOf(PedidoNaoEncontradoException.class)
                .hasMessage("Pedido nao encontrado: 99");
    }

    // ---------- listarPorUsuario ----------

    @Test
    void listarPorUsuario_normalizaUsuarioEMapeiaResultados() {
        when(pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc("user-1"))
                .thenReturn(List.of(new Pedido("user-1"), new Pedido("user-1")));

        List<PedidoResponse> resultado = pedidoService.listarPorUsuario("  user-1  ");

        assertThat(resultado).hasSize(2);
        verify(pedidoRepository).findByUsuarioIdOrderByCriadoEmDesc("user-1");
    }

    @Test
    void listarPorUsuario_retornaVazioQuandoNaoHaPedidos() {
        when(pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(anyString())).thenReturn(List.of());

        assertThat(pedidoService.listarPorUsuario("user-1")).isEmpty();
    }

    @Test
    void listarPorUsuario_lancaQuandoUsuarioEmBranco() {
        assertThatThrownBy(() -> pedidoService.listarPorUsuario(""))
                .isInstanceOf(RegraNegocioException.class);
    }
}
