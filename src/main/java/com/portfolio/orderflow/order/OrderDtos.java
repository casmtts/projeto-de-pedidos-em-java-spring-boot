package com.portfolio.orderflow.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {}

    public record CreateOrderRequest(@NotEmpty List<@Valid OrderLineRequest> items) {}

    public record OrderLineRequest(@NotNull UUID productId, @Positive int quantity) {}

    public record OrderItemResponse(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity());
        }
    }

    public record OrderResponse(UUID id, OrderStatus status, BigDecimal total, Instant createdAt,
                                List<OrderItemResponse> items) {
        static OrderResponse from(PurchaseOrder order) {
            return new OrderResponse(order.getId(), order.getStatus(), order.getTotal(), order.getCreatedAt(),
                    order.getItems().stream().map(OrderItemResponse::from).toList());
        }
    }
}
