package com.devcart.pedidosservice.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica eventos de pedido no RabbitMQ. Usa o exchange default ("") com
 * routing key igual ao nome da fila {@code pedidos.criados}.
 */
@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarPedidoCriado(PedidoCriadoEvent evento) {
        rabbitTemplate.convertAndSend("", RabbitConfig.FILA_PEDIDOS_CRIADOS, evento);
        log.info("Evento publicado em '{}': pedido {}", RabbitConfig.FILA_PEDIDOS_CRIADOS, evento.pedidoId());
    }
}
