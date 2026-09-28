package com.portfolio.orderflow.order;

import static com.portfolio.orderflow.order.OrderDtos.CreateOrderRequest;
import static com.portfolio.orderflow.order.OrderDtos.OrderLineRequest;
import static com.portfolio.orderflow.order.OrderDtos.OrderResponse;

import com.portfolio.orderflow.product.Product;
import com.portfolio.orderflow.product.ProductService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxEventRepository outbox;
    private final ProductService products;

    public OrderService(OrderRepository orders, OutboxEventRepository outbox, ProductService products) {
        this.orders = orders;
        this.outbox = outbox;
        this.products = products;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Map<UUID, Integer> quantities = combineDuplicateItems(request);
        Map<UUID, Product> lockedProducts = new LinkedHashMap<>();
        quantities.keySet().stream().sorted().forEach(id -> lockedProducts.put(id, products.getForUpdate(id)));

        PurchaseOrder order = new PurchaseOrder();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> line : quantities.entrySet()) {
            Product product = lockedProducts.get(line.getKey());
            int quantity = line.getValue();
            try {
                product.reserve(quantity);
            } catch (IllegalStateException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for product " + product.getId(), exception);
            }
            order.addItem(product.getId(), product.getName(), product.getPrice(), quantity);
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);

        PurchaseOrder saved = orders.save(order);
        outbox.save(new OutboxEvent(saved.getId(), "OrderCreated", saved.getId().toString()));
        products.invalidateCacheAfterCommit();
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(UUID id) {
        return OrderResponse.from(orders.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
    }

    @Transactional
    public void confirm(UUID id) {
        PurchaseOrder order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found in event: " + id));
        order.confirm();
    }

    private Map<UUID, Integer> combineDuplicateItems(CreateOrderRequest request) {
        Map<UUID, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest item : request.items()) {
            try {
                quantities.merge(item.productId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total quantity is too large", exception);
            }
        }
        return quantities;
    }
}
