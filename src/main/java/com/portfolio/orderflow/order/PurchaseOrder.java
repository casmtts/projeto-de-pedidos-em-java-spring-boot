package com.portfolio.orderflow.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customer_orders")
public class PurchaseOrder {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public PurchaseOrder() {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.PROCESSING;
        this.createdAt = Instant.now();
    }

    public void addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        items.add(new OrderItem(this, productId, productName, unitPrice, quantity));
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public void confirm() {
        if (status == OrderStatus.PROCESSING) {
            status = OrderStatus.CONFIRMED;
        }
    }

    public UUID getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
