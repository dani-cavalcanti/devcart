package com.devcart.pedidosservice.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Garante que o evento so chegue ao RabbitMQ APOS o commit da transacao
 * que persistiu o pedido (requisito: publicar apos persistir com sucesso).
 */
@Component
public class PedidoEventListener {

    private final PedidoEventPublisher pedidoEventPublisher;

    public PedidoEventListener(PedidoEventPublisher pedidoEventPublisher) {
        this.pedidoEventPublisher = pedidoEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoCriarPedido(PedidoCriadoEvent evento) {
        pedidoEventPublisher.publicarPedidoCriado(evento);
    }
}
