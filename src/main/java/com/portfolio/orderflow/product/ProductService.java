package com.portfolio.orderflow.product;

import static com.portfolio.orderflow.product.ProductDtos.CreateProductRequest;
import static com.portfolio.orderflow.product.ProductDtos.ProductResponse;

import java.util.List;
import java.util.UUID;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository products;
    private final CacheManager cacheManager;

    public ProductService(ProductRepository products, CacheManager cacheManager) {
        this.products = products;
        this.cacheManager = cacheManager;
    }

    @Transactional
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse create(CreateProductRequest request) {
        return ProductResponse.from(products.save(
                new Product(request.name().trim(), request.price(), request.availableQuantity())));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return products.findAll().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products", key = "#id")
    public ProductResponse get(UUID id) {
        return ProductResponse.from(products.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }

    public Product getForUpdate(UUID id) {
        return products.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    public void invalidateCacheAfterCommit() {
        Runnable eviction = () -> {
            if (cacheManager.getCache("products") != null) {
                cacheManager.getCache("products").clear();
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
