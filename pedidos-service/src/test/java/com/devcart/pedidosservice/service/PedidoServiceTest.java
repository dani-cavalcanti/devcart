package com.devcart.pedidosservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import com.devcart.pedidosservice.repository.PedidoRepository;

/**
 * Testes unitarios de {@link PedidoService}, com foco em {@code criarPedido}.
 *
 * <p>Colaboradores mockados:
 * <ul>
 *   <li><b>Feign / catalogo-service</b>: {@link CatalogoClientService} - fachada resiliente
 *       (@CircuitBreaker/@TimeLimiter) sobre o {@code CatalogoFeignClient}. E com ela que o
 *       {@code PedidoService} conversa, entao e o seam correto para mockar a chamada sincrona.</li>
 *   <li><b>RabbitMQ</b>: {@link ApplicationEventPublisher} - o {@code PedidoService} apenas emite
 *       um evento de dominio; a publicacao no broker (apos o commit) e feita pelo
 *       {@code PedidoEventListener}/{@code PedidoEventPublisher}. Verificar esse publish e o
 *       equivalente unitario a "mockar o RabbitMQ" nesta camada.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService")
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private CatalogoClientService catalogoClientService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PedidoService pedidoService;

    // ------------------------------------------------------------------ helpers

    private static ProdutoDto produto(String id, String nome, String preco, Integer estoque) {
        return new ProdutoDto(id, nome, "descricao de " + nome, new BigDecimal(preco), estoque);
    }

    private static CriarPedidoRequest requestCom(ItemPedidoRequest... itens) {
        return new CriarPedidoRequest(List.of(itens));
    }

    /** Identidade/carimbo de tempo que o JPA atribuiria ao pedido apos o insert. */
    private static final long ID_PERSISTIDO = 4242L;
    private static final OffsetDateTime CRIADO_EM_PERSISTIDO = OffsetDateTime.parse("2026-08-30T12:00:00Z");

    private void dadoQueOcatalogoRetorna(ProdutoDto produtoDto) {
        when(catalogoClientService.buscarProduto(produtoDto.id()))
                .thenReturn(CompletableFuture.completedFuture(produtoDto));
    }

    /**
     * Simula fielmente o {@code save()} do Spring Data JPA: devolve a MESMA entidade,
     * porem agora com {@code id} e {@code criadoEm} preenchidos. Assim os testes conseguem
     * provar que a resposta e o evento sao montados a partir da entidade <b>persistida</b>.
     */
    private void dadoQueOrepositorioPersisteAtribuindoIdEData() {
        when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocacao -> comIdEData(invocacao.getArgument(0), ID_PERSISTIDO, CRIADO_EM_PERSISTIDO));
    }

    private static Pedido comIdEData(Pedido pedido, Long id, OffsetDateTime criadoEm) {
        escreverCampo(pedido, "id", id);
        escreverCampo(pedido, "criadoEm", criadoEm);
        return pedido;
    }

    private static void escreverCampo(Object alvo, String nomeCampo, Object valor) {
        try {
            Field campo = Pedido.class.getDeclaredField(nomeCampo);
            campo.setAccessible(true);
            campo.set(alvo, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("nao foi possivel preparar o Pedido de teste", e);
        }
    }

    // ======================================================== CAMINHO FELIZ ===

    @Nested
    @DisplayName("criarPedido - caminho feliz")
    class CaminhoFeliz {

        @Test
        @DisplayName("deveCriarPedidoComUmItem_quandoProdutoExisteEHaEstoqueSuficiente")
        void deveCriarPedidoComUmItem_quandoProdutoExisteEHaEstoqueSuficiente() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "120.00", 5));
            dadoQueOrepositorioPersisteAtribuindoIdEData();

            PedidoResponse resposta = pedidoService.criarPedido(
                    "  user-123 ", requestCom(new ItemPedidoRequest("p1", 2)));

            assertThat(resposta.id()).isEqualTo(ID_PERSISTIDO);                 // veio da entidade salva
            assertThat(resposta.criadoEm()).isEqualTo(CRIADO_EM_PERSISTIDO);   // veio da entidade salva
            assertThat(resposta.usuarioId()).isEqualTo("user-123"); // trim aplicado
            assertThat(resposta.status()).isEqualTo("CRIADO");
            assertThat(resposta.valorTotal()).isEqualByComparingTo("240.00");
            assertThat(resposta.itens()).singleElement().satisfies(item -> {
                assertThat(item.produtoId()).isEqualTo("p1");
                assertThat(item.nomeProduto()).isEqualTo("Mousepad");
                assertThat(item.quantidade()).isEqualTo(2);
                assertThat(item.precoUnitario()).isEqualByComparingTo("120.00");
                assertThat(item.subtotal()).isEqualByComparingTo("240.00");
            });
        }

        @Test
        @DisplayName("devePersistirPedidoComPrecoDoCatalogo_quandoCriadoComSucesso")
        void devePersistirPedidoComPrecoDoCatalogo_quandoCriadoComSucesso() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "120.00", 5));
            dadoQueOrepositorioPersisteAtribuindoIdEData();

            pedidoService.criarPedido("user-123", requestCom(new ItemPedidoRequest("p1", 2)));

            ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
            verify(pedidoRepository).save(pedidoCaptor.capture());

            Pedido persistido = pedidoCaptor.getValue();
            assertThat(persistido.getUsuarioId()).isEqualTo("user-123");
            assertThat(persistido.getValorTotal()).isEqualByComparingTo("240.00");
            assertThat(persistido.getItens()).singleElement().satisfies(item -> {
                assertThat(item.getProdutoId()).isEqualTo("p1");
                assertThat(item.getPrecoUnitario()).isEqualByComparingTo("120.00"); // veio do catalogo
                assertThat(item.getPedido()).isSameAs(persistido);
            });
        }

        @Test
        @DisplayName("devePublicarEventoPedidoCriado_quandoPedidoPersistido")
        void devePublicarEventoPedidoCriado_quandoPedidoPersistido() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "120.00", 5));
            dadoQueOrepositorioPersisteAtribuindoIdEData();

            pedidoService.criarPedido("user-123", requestCom(new ItemPedidoRequest("p1", 2)));

            ArgumentCaptor<PedidoCriadoEvent> eventoCaptor = ArgumentCaptor.forClass(PedidoCriadoEvent.class);
            verify(applicationEventPublisher).publishEvent(eventoCaptor.capture());

            PedidoCriadoEvent evento = eventoCaptor.getValue();
            assertThat(evento.pedidoId()).isEqualTo(ID_PERSISTIDO);            // id da entidade persistida
            assertThat(evento.criadoEm()).isEqualTo(CRIADO_EM_PERSISTIDO);
            assertThat(evento.usuarioId()).isEqualTo("user-123");
            assertThat(evento.valorTotal()).isEqualByComparingTo("240.00");
            assertThat(evento.quantidadeItens()).isEqualTo(2);
        }

        @Test
        @DisplayName("deveMontarRespostaEEventoAPartirDaEntidadePersistida_quandoSaveRetornaOutraReferencia")
        void deveMontarRespostaEEventoAPartirDaEntidadePersistida_quandoSaveRetornaOutraReferencia() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "120.00", 5));

            // save() devolve uma REFERENCIA DIFERENTE (entidade gerenciada), com id/data proprios.
            Pedido gerenciado = comIdEData(new Pedido("user-123"), 999L,
                    OffsetDateTime.parse("2026-01-15T09:30:00Z"));
            when(pedidoRepository.save(any(Pedido.class))).thenReturn(gerenciado);

            PedidoResponse resposta = pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 2)));

            ArgumentCaptor<PedidoCriadoEvent> eventoCaptor = ArgumentCaptor.forClass(PedidoCriadoEvent.class);
            verify(applicationEventPublisher).publishEvent(eventoCaptor.capture());

            // resposta e evento devem refletir a entidade retornada por save(), nao a de entrada
            assertThat(resposta.id()).isEqualTo(999L);
            assertThat(resposta.criadoEm()).isEqualTo(OffsetDateTime.parse("2026-01-15T09:30:00Z"));
            assertThat(eventoCaptor.getValue().pedidoId()).isEqualTo(999L);
            assertThat(eventoCaptor.getValue().criadoEm()).isEqualTo(OffsetDateTime.parse("2026-01-15T09:30:00Z"));
        }

        @Test
        @DisplayName("deveSomarSubtotaisDeTodosOsItens_quandoPedidoTemVariosProdutos")
        void deveSomarSubtotaisDeTodosOsItens_quandoPedidoTemVariosProdutos() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "120.00", 5));
            dadoQueOcatalogoRetorna(produto("p2", "Caneca", "49.90", 10));
            dadoQueOrepositorioPersisteAtribuindoIdEData();

            PedidoResponse resposta = pedidoService.criarPedido("user-123",
                    requestCom(new ItemPedidoRequest("p1", 2), new ItemPedidoRequest("p2", 3)));

            // (2 x 120.00) + (3 x 49.90) = 240.00 + 149.70
            assertThat(resposta.itens()).hasSize(2);
            assertThat(resposta.valorTotal()).isEqualByComparingTo("389.70");
        }

        @Test
        @DisplayName("deveAceitarPedido_quandoEstoqueEhExatamenteIgualAquantidade")
        void deveAceitarPedido_quandoEstoqueEhExatamenteIgualAquantidade() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "10.00", 2));
            dadoQueOrepositorioPersisteAtribuindoIdEData();

            PedidoResponse resposta = pedidoService.criarPedido("user-123",
                    requestCom(new ItemPedidoRequest("p1", 2)));

            assertThat(resposta.itens()).hasSize(1);
            assertThat(resposta.valorTotal()).isEqualByComparingTo("20.00");
        }
    }

    // =================================================== REGRAS / FALHAS ======

    @Nested
    @DisplayName("criarPedido - regras de negocio e falhas de integracao")
    class RegrasEfalhas {

        @Test
        @DisplayName("deveLancarRegraNegocio_quandoUsuarioIdEhNulo")
        void deveLancarRegraNegocio_quandoUsuarioIdEhNulo() {
            assertThatThrownBy(() -> pedidoService.criarPedido(
                    null, requestCom(new ItemPedidoRequest("p1", 1))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Header X-User-Id e obrigatorio");

            verifyNoInteractions(catalogoClientService, pedidoRepository, applicationEventPublisher);
        }

        @Test
        @DisplayName("deveLancarRegraNegocio_quandoUsuarioIdEhEmBranco")
        void deveLancarRegraNegocio_quandoUsuarioIdEhEmBranco() {
            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "   ", requestCom(new ItemPedidoRequest("p1", 1))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Header X-User-Id e obrigatorio");

            verifyNoInteractions(catalogoClientService, pedidoRepository, applicationEventPublisher);
        }

        @Test
        @DisplayName("deveLancarRegraNegocio_quandoEstoqueEhInsuficiente")
        void deveLancarRegraNegocio_quandoEstoqueEhInsuficiente() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "10.00", 1));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 2))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Estoque insuficiente para o produto p1"); // mensagem inclui o produtoId

            verify(pedidoRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any(PedidoCriadoEvent.class));
        }

        @Test
        @DisplayName("deveLancarRegraNegocio_quandoEstoqueEhMenorPorExatamenteUmaUnidade")
        void deveLancarRegraNegocio_quandoEstoqueEhMenorPorExatamenteUmaUnidade() {
            // fixa o limite inferior do operador '<': estoque == quantidade - 1 ainda barra.
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "10.00", 4));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 5))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Estoque insuficiente para o produto p1");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("deveLancarRegraNegocio_quandoEstoqueDoProdutoEhNulo")
        void deveLancarRegraNegocio_quandoEstoqueDoProdutoEhNulo() {
            dadoQueOcatalogoRetorna(produto("p1", "Mousepad", "10.00", null));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 1))))
                    .isInstanceOf(RegraNegocioException.class)
                    .hasMessage("Estoque insuficiente para o produto p1");

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("devePropagarMesmaExcecao_quandoCatalogoFalhaComCatalogoIndisponivel")
        void devePropagarMesmaExcecao_quandoCatalogoFalhaComCatalogoIndisponivel() {
            CatalogoIndisponivelException original =
                    new CatalogoIndisponivelException("p1", new RuntimeException("timeout de 2s"));
            when(catalogoClientService.buscarProduto("p1"))
                    .thenReturn(CompletableFuture.failedFuture(original));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 1))))
                    .isSameAs(original);

            verify(pedidoRepository, never()).save(any());
        }

        @Test
        @DisplayName("devePropagarMesmaExcecao_quandoCatalogoFalhaComRegraNegocio")
        void devePropagarMesmaExcecao_quandoCatalogoFalhaComRegraNegocio() {
            RegraNegocioException original = new RegraNegocioException("produto invalido no catalogo");
            when(catalogoClientService.buscarProduto("p1"))
                    .thenReturn(CompletableFuture.failedFuture(original));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 1))))
                    .isSameAs(original);
        }

        @Test
        @DisplayName("deveEnvolverEmCatalogoIndisponivel_quandoCatalogoFalhaComErroInesperado")
        void deveEnvolverEmCatalogoIndisponivel_quandoCatalogoFalhaComErroInesperado() {
            IllegalStateException causaRaiz = new IllegalStateException("conexao recusada");
            when(catalogoClientService.buscarProduto("p1"))
                    .thenReturn(CompletableFuture.failedFuture(causaRaiz));

            assertThatThrownBy(() -> pedidoService.criarPedido(
                    "user-123", requestCom(new ItemPedidoRequest("p1", 1))))
                    .isInstanceOf(CatalogoIndisponivelException.class)
                    .hasCause(causaRaiz);
        }
    }

    // ============================ OPERACOES DE LEITURA (cobertura da classe) ===

    @Nested
    @DisplayName("operacoes de leitura")
    class Leitura {

        @Test
        @DisplayName("deveRetornarPedido_quandoIdExiste")
        void deveRetornarPedido_quandoIdExiste() {
            when(pedidoRepository.findById(1L)).thenReturn(Optional.of(new Pedido("user-123")));

            PedidoResponse resposta = pedidoService.buscarPorId(1L);

            assertThat(resposta.usuarioId()).isEqualTo("user-123");
            assertThat(resposta.status()).isEqualTo("CRIADO");
        }

        @Test
        @DisplayName("deveLancarPedidoNaoEncontrado_quandoIdNaoExiste")
        void deveLancarPedidoNaoEncontrado_quandoIdNaoExiste() {
            when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> pedidoService.buscarPorId(99L))
                    .isInstanceOf(PedidoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("deveListarPedidosDoUsuario_quandoUsuarioIdEhValido")
        void deveListarPedidosDoUsuario_quandoUsuarioIdEhValido() {
            when(pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc("user-123"))
                    .thenReturn(List.of(new Pedido("user-123"), new Pedido("user-123")));

            List<PedidoResponse> resposta = pedidoService.listarPorUsuario("  user-123  ");

            assertThat(resposta).hasSize(2)
                    .allSatisfy(pedido -> assertThat(pedido.usuarioId()).isEqualTo("user-123"));
        }
    }
}
