package com.devcart.pedidosservice.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void filaPedidosCriados_ehDuravelComONomeEsperado() {
        Queue fila = config.filaPedidosCriados();

        assertThat(fila.getName()).isEqualTo("pedidos.criados");
        assertThat(fila.getName()).isEqualTo(RabbitConfig.FILA_PEDIDOS_CRIADOS);
        assertThat(fila.isDurable()).isTrue();
    }

    @Test
    void jacksonMessageConverter_ehJson() {
        MessageConverter converter = config.jacksonMessageConverter();

        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }

    @Test
    void rabbitTemplate_usaOConversorInjetado() {
        MessageConverter converter = new Jackson2JsonMessageConverter();

        RabbitTemplate template = config.rabbitTemplate(mock(ConnectionFactory.class), converter);

        assertThat(template.getMessageConverter()).isSameAs(converter);
    }
}
