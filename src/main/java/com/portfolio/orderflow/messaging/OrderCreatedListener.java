package com.portfolio.orderflow.messaging;

import com.portfolio.orderflow.order.OrderService;
import java.util.UUID;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedListener {
    private final OrderService orders;

    public OrderCreatedListener(OrderService orders) {
        this.orders = orders;
    }

    @RabbitListener(queues = RabbitConfiguration.ORDER_QUEUE)
    public void handle(String orderId) {
        orders.confirm(UUID.fromString(orderId));
    }
}
