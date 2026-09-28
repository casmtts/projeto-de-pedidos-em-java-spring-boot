package com.portfolio.orderflow.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateProductRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @Min(0) int availableQuantity) {}

    public record ProductResponse(UUID id, String name, BigDecimal price, int availableQuantity, Instant createdAt)
            implements java.io.Serializable {
        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                    product.getAvailableQuantity(), product.getCreatedAt());
        }
    }
}
