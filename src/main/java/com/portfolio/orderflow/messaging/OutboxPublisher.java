package com.portfolio.orderflow.messaging;

import com.portfolio.orderflow.order.OutboxEvent;
import com.portfolio.orderflow.order.OutboxEventRepository;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository events;
    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(OutboxEventRepository events, RabbitTemplate rabbitTemplate) {
        this.events = events;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 2_000)
    @Transactional
    public void publishPendingEvents() {
        for (OutboxEvent event : events.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc()) {
            try {
                CorrelationData correlation = new CorrelationData(event.getId().toString());
                rabbitTemplate.convertAndSend(
                        RabbitConfiguration.ORDER_EXCHANGE,
                        RabbitConfiguration.ORDER_ROUTING_KEY,
                        event.getPayload(),
                        message -> {
                            message.getMessageProperties().setMessageId(event.getId().toString());
                            return message;
                        },
                        correlation);

                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.ack() || correlation.getReturned() != null) {
                    throw new IllegalStateException("RabbitMQ did not route event " + event.getId());
                }
                event.markPublished();
            } catch (Exception exception) {
                log.warn("Could not publish outbox event {}. It will be retried.", event.getId(), exception);
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
