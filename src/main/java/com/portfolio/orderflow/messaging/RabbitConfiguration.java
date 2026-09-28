package com.portfolio.orderflow.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitConfiguration {
    public static final String ORDER_EXCHANGE = "order.events";
    public static final String ORDER_ROUTING_KEY = "order.created";
    public static final String ORDER_QUEUE = "order.created.queue";
    public static final String DEAD_LETTER_EXCHANGE = "order.dead-letter";
    public static final String DEAD_LETTER_ROUTING_KEY = "order.created.dead";
    public static final String DEAD_LETTER_QUEUE = "order.created.dlq";

    @Bean
    DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding orderBinding(@Qualifier("orderQueue") Queue orderQueue,
                         @Qualifier("orderExchange") DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                              @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }
}
