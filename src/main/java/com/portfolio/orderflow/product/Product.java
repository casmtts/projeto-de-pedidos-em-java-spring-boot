package com.portfolio.orderflow.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Product() {
    }

    public Product(String name, BigDecimal price, int availableQuantity) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.price = price;
        this.availableQuantity = availableQuantity;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getAvailableQuantity() { return availableQuantity; }
    public Instant getCreatedAt() { return createdAt; }

    public void reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock for product " + id);
        }
        availableQuantity -= quantity;
    }
}
