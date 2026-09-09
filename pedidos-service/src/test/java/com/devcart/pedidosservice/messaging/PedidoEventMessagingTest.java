package com.devcart.pedidosservice.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class PedidoEventMessagingTest {

    private static PedidoCriadoEvent evento() {
        return new PedidoCriadoEvent(1L, "user-1", new BigDecimal("30.00"), 3, OffsetDateTime.now());
    }

    @Test
    void publisher_enviaParaFilaPedidosCriadosNoExchangeDefault() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        PedidoEventPublisher publisher = new PedidoEventPublisher(rabbitTemplate);
        PedidoCriadoEvent ev = evento();

        publisher.publicarPedidoCriado(ev);

        verify(rabbitTemplate).convertAndSend("", RabbitConfig.FILA_PEDIDOS_CRIADOS, ev);
    }

    @Test
    void listener_delegaAoPublisherAposCommit() {
        PedidoEventPublisher publisher = mock(PedidoEventPublisher.class);
        PedidoEventListener listener = new PedidoEventListener(publisher);
        PedidoCriadoEvent ev = evento();

        listener.aoCriarPedido(ev);

        verify(publisher).publicarPedidoCriado(ev);
    }
}
