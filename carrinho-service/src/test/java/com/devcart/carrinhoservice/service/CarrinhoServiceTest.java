package com.devcart.carrinhoservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devcart.carrinhoservice.dto.AdicionarItemRequest;
import com.devcart.carrinhoservice.dto.CarrinhoResponse;
import com.devcart.carrinhoservice.exception.RegraNegocioException;
import com.devcart.carrinhoservice.exception.UsuarioNaoInformadoException;
import com.devcart.carrinhoservice.model.Carrinho;
import com.devcart.carrinhoservice.model.ItemCarrinho;
import com.devcart.carrinhoservice.repository.CarrinhoRepository;

@ExtendWith(MockitoExtension.class)
class CarrinhoServiceTest {

    @Mock
    private CarrinhoRepository carrinhoRepository;

    @InjectMocks
    private CarrinhoService carrinhoService;

    private static AdicionarItemRequest req(String produtoId, int qtd) {
        return new AdicionarItemRequest(produtoId, "Produto " + produtoId, qtd, new BigDecimal("10.00"));
    }

    // ---------- adicionarItem ----------

    @Test
    void adicionarItem_criaCarrinhoNovoQuandoNaoExiste() {
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.empty());
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> inv.getArgument(0));

        CarrinhoResponse resposta = carrinhoService.adicionarItem("user-1", req("p1", 2));

        ArgumentCaptor<Carrinho> captor = ArgumentCaptor.forClass(Carrinho.class);
        verify(carrinhoRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuarioId()).isEqualTo("user-1");
        assertThat(captor.getValue().getItens()).hasSize(1);
        assertThat(captor.getValue().getItens().get(0).getQuantidade()).isEqualTo(2);

        assertThat(resposta.usuarioId()).isEqualTo("user-1");
        assertThat(resposta.quantidadeItens()).isEqualTo(2);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void adicionarItem_incrementaQuantidadeDeItemJaExistente() {
        Carrinho existente = new Carrinho("user-1");
        existente.getItens().add(new ItemCarrinho("p1", "antigo", 3, new BigDecimal("10.00")));
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.of(existente));
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> inv.getArgument(0));

        carrinhoService.adicionarItem("user-1",
                new AdicionarItemRequest("p1", "novo nome", 4, new BigDecimal("12.00")));

        ItemCarrinho item = existente.getItens().get(0);
        assertThat(existente.getItens()).hasSize(1);
        assertThat(item.getQuantidade()).isEqualTo(7);
        assertThat(item.getNome()).isEqualTo("novo nome");
        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("12.00");
    }

    @Test
    void adicionarItem_acrescentaSegundoProdutoDistinto() {
        Carrinho existente = new Carrinho("user-1");
        existente.getItens().add(new ItemCarrinho("p1", "P1", 1, new BigDecimal("10.00")));
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.of(existente));
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> inv.getArgument(0));

        carrinhoService.adicionarItem("user-1", req("p2", 5));

        assertThat(existente.getItens()).extracting(ItemCarrinho::getProdutoId)
                .containsExactly("p1", "p2");
    }

    @Test
    void adicionarItem_lancaQuandoUltrapassaLimiteDe100() {
        Carrinho existente = new Carrinho("user-1");
        existente.getItens().add(new ItemCarrinho("p1", "P1", 98, new BigDecimal("10.00")));
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> carrinhoService.adicionarItem("user-1", req("p1", 3)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("excede o limite de 100");

        verify(carrinhoRepository, never()).save(any());
    }

    @Test
    void adicionarItem_permiteExatamente100Unidades() {
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.empty());
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> inv.getArgument(0));

        CarrinhoResponse resposta = carrinhoService.adicionarItem("user-1", req("p1", 100));

        assertThat(resposta.quantidadeItens()).isEqualTo(100);
    }

    @Test
    void adicionarItem_fazTrimDoUsuarioId() {
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.empty());
        when(carrinhoRepository.save(any(Carrinho.class))).thenAnswer(inv -> inv.getArgument(0));

        carrinhoService.adicionarItem("  user-1  ", req("p1", 1));

        verify(carrinhoRepository).findById("user-1");
    }

    @Test
    void adicionarItem_lancaQuandoUsuarioIdEmBranco() {
        assertThatThrownBy(() -> carrinhoService.adicionarItem("   ", req("p1", 1)))
                .isInstanceOf(UsuarioNaoInformadoException.class)
                .hasMessage("Header X-User-Id e obrigatorio");
    }

    @Test
    void adicionarItem_lancaQuandoUsuarioIdNulo() {
        assertThatThrownBy(() -> carrinhoService.adicionarItem(null, req("p1", 1)))
                .isInstanceOf(UsuarioNaoInformadoException.class);
    }

    // ---------- listarCarrinho ----------

    @Test
    void listarCarrinho_retornaCarrinhoPersistido() {
        Carrinho existente = new Carrinho("user-1");
        existente.getItens().add(new ItemCarrinho("p1", "P1", 2, new BigDecimal("10.00")));
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.of(existente));

        CarrinhoResponse resposta = carrinhoService.listarCarrinho("user-1");

        assertThat(resposta.itens()).hasSize(1);
        assertThat(resposta.valorTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void listarCarrinho_retornaCarrinhoVazioQuandoNaoExiste() {
        when(carrinhoRepository.findById("user-1")).thenReturn(Optional.empty());

        CarrinhoResponse resposta = carrinhoService.listarCarrinho("user-1");

        assertThat(resposta.usuarioId()).isEqualTo("user-1");
        assertThat(resposta.itens()).isEmpty();
        assertThat(resposta.quantidadeItens()).isZero();
        assertThat(resposta.valorTotal()).isEqualByComparingTo("0");
    }

    // ---------- limparCarrinho ----------

    @Test
    void limparCarrinho_delegaDeleteByIdComChaveNormalizada() {
        carrinhoService.limparCarrinho("  user-1 ");

        verify(carrinhoRepository).deleteById("user-1");
    }

    @Test
    void limparCarrinho_lancaQuandoUsuarioIdEmBranco() {
        assertThatThrownBy(() -> carrinhoService.limparCarrinho(""))
                .isInstanceOf(UsuarioNaoInformadoException.class);

        verify(carrinhoRepository, never()).deleteById(any());
    }
}
