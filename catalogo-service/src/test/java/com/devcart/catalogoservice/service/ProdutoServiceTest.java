package com.devcart.catalogoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devcart.catalogoservice.dto.ProdutoRequest;
import com.devcart.catalogoservice.dto.ProdutoResponse;
import com.devcart.catalogoservice.exception.ProdutoJaExisteException;
import com.devcart.catalogoservice.exception.ProdutoNaoEncontradoException;
import com.devcart.catalogoservice.model.Produto;
import com.devcart.catalogoservice.repository.ProdutoRepository;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @InjectMocks
    private ProdutoService produtoService;

    private static ProdutoRequest umRequest() {
        return new ProdutoRequest("Teclado", "Teclado mecanico", new BigDecimal("199.90"), 10);
    }

    private static Produto umProduto(String id) {
        return new Produto(id, "Teclado", "Teclado mecanico", new BigDecimal("199.90"), 10);
    }

    // ---------- criar ----------

    @Test
    void criar_persisteEntidadeSemIdERetornaResponse() {
        when(produtoRepository.existsByNomeIgnoreCase("Teclado")).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenReturn(umProduto("abc-123"));

        ProdutoResponse response = produtoService.criar(umRequest());

        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getNome()).isEqualTo("Teclado");
        assertThat(captor.getValue().getPreco()).isEqualByComparingTo("199.90");
        assertThat(captor.getValue().getEstoque()).isEqualTo(10);

        assertThat(response.id()).isEqualTo("abc-123");
        assertThat(response.nome()).isEqualTo("Teclado");
        assertThat(response.descricao()).isEqualTo("Teclado mecanico");
        assertThat(response.preco()).isEqualByComparingTo("199.90");
        assertThat(response.estoque()).isEqualTo(10);
    }

    @Test
    void criar_lancaQuandoNomeJaExiste() {
        when(produtoRepository.existsByNomeIgnoreCase("Teclado")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.criar(umRequest()))
                .isInstanceOf(ProdutoJaExisteException.class)
                .hasMessage("Ja existe um produto com o nome: Teclado");

        verify(produtoRepository, never()).save(any());
    }

    // ---------- listar ----------

    @Test
    void listar_mapeiaTodasAsEntidades() {
        when(produtoRepository.findAll()).thenReturn(List.of(umProduto("id-1"), umProduto("id-2")));

        List<ProdutoResponse> resultado = produtoService.listar();

        assertThat(resultado).hasSize(2)
                .extracting(ProdutoResponse::id)
                .containsExactly("id-1", "id-2");
    }

    @Test
    void listar_retornaListaVaziaQuandoNaoHaProdutos() {
        when(produtoRepository.findAll()).thenReturn(List.of());

        assertThat(produtoService.listar()).isEmpty();
    }

    // ---------- buscarPorId ----------

    @Test
    void buscarPorId_retornaResponseQuandoExiste() {
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(umProduto("id-1")));

        assertThat(produtoService.buscarPorId("id-1").id()).isEqualTo("id-1");
    }

    @Test
    void buscarPorId_lancaQuandoNaoExiste() {
        when(produtoRepository.findById("sumiu")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId("sumiu"))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessage("Produto nao encontrado: sumiu");
    }

    // ---------- atualizar ----------

    @Test
    void atualizar_alteraCamposEPersiste() {
        Produto existente = umProduto("id-1");
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(existente));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProdutoRequest req = new ProdutoRequest("Teclado", "Nova descricao", new BigDecimal("149.00"), 5);
        ProdutoResponse response = produtoService.atualizar("id-1", req);

        assertThat(response.descricao()).isEqualTo("Nova descricao");
        assertThat(response.preco()).isEqualByComparingTo("149.00");
        assertThat(response.estoque()).isEqualTo(5);
        assertThat(existente.getDescricao()).isEqualTo("Nova descricao");
    }

    @Test
    void atualizar_permiteManterMesmoNomeSemChecarDuplicidade() {
        Produto existente = umProduto("id-1");
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(existente));
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        // mesmo nome (case-insensitive) -> nao deve consultar existsByNomeIgnoreCase
        produtoService.atualizar("id-1", new ProdutoRequest("TECLADO", "d", new BigDecimal("1.00"), 0));

        verify(produtoRepository, never()).existsByNomeIgnoreCase(any());
    }

    @Test
    void atualizar_aceitaNovoNomeQuandoNaoPertenceAOutroProduto() {
        Produto existente = umProduto("id-1");
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(existente));
        when(produtoRepository.existsByNomeIgnoreCase("Mouse")).thenReturn(false);
        when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProdutoResponse response = produtoService.atualizar("id-1",
                new ProdutoRequest("Mouse", "d", new BigDecimal("1.00"), 0));

        assertThat(response.nome()).isEqualTo("Mouse");
        verify(produtoRepository).save(existente);
    }

    @Test
    void atualizar_lancaQuandoNovoNomeJaPertenceAOutroProduto() {
        Produto existente = umProduto("id-1");
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(existente));
        when(produtoRepository.existsByNomeIgnoreCase("Mouse")).thenReturn(true);

        assertThatThrownBy(() -> produtoService.atualizar("id-1",
                new ProdutoRequest("Mouse", "d", new BigDecimal("1.00"), 0)))
                .isInstanceOf(ProdutoJaExisteException.class);

        verify(produtoRepository, never()).save(any());
    }

    @Test
    void atualizar_lancaQuandoIdNaoExiste() {
        when(produtoRepository.findById("sumiu")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar("sumiu", umRequest()))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    // ---------- remover ----------

    @Test
    void remover_deletaEntidadeExistente() {
        Produto existente = umProduto("id-1");
        when(produtoRepository.findById("id-1")).thenReturn(java.util.Optional.of(existente));

        produtoService.remover("id-1");

        verify(produtoRepository).delete(existente);
    }

    @Test
    void remover_lancaQuandoIdNaoExiste() {
        when(produtoRepository.findById("sumiu")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> produtoService.remover("sumiu"))
                .isInstanceOf(ProdutoNaoEncontradoException.class);

        verify(produtoRepository, never()).delete(any());
    }
}
