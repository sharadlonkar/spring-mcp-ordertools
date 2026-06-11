package com.example.ordertools.service;

import com.example.ordertools.domain.Order;
import com.example.ordertools.domain.OrderItem;
import com.example.ordertools.domain.OrderStatus;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory order store. Replace with a JPA repository for persistence.
 *
 * <p>On startup it is seeded with 10 sample orders (IDs {@code ORD-1001}
 * through {@code ORD-1010}) covering every {@link OrderStatus}, so the tools
 * return useful data out of the box. New orders created via {@link #create}
 * continue from {@code ORD-1011}.
 */
@Service
public class OrderService {

    private static final Instant SEED_BASE = Instant.parse("2026-01-01T09:00:00Z");

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    public Order create(String customer, List<OrderItem> items) {
        if (customer == null || customer.isBlank()) {
            throw new IllegalArgumentException("customer must not be blank");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("an order requires at least one item");
        }
        Instant now = Instant.now();
        String id = "ORD-" + sequence.incrementAndGet();
        Order order = new Order(id, customer, List.copyOf(items), OrderStatus.NEW, now, now);
        orders.put(id, order);
        return order;
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Order get(String id) {
        return find(id).orElseThrow(
                () -> new NoSuchElementException("no order with id " + id));
    }

    public List<Order> list(OrderStatus status) {
        return orders.values().stream()
                .filter(o -> status == null || o.status() == status)
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .toList();
    }

    public Order updateStatus(String id, OrderStatus status) {
        Order existing = get(id);
        if (existing.status() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("order " + id + " is cancelled and cannot change status");
        }
        Order updated = existing.withStatus(status, Instant.now());
        orders.put(id, updated);
        return updated;
    }

    public Order cancel(String id) {
        return updateStatus(id, OrderStatus.CANCELLED);
    }

    /**
     * Seeds 10 sample orders, two per status, so the store is never empty.
     */
    @PostConstruct
    void seed() {
        seedOrder("alice", OrderStatus.NEW, item("SKU-1", "Widget", 2, "9.99"));
        seedOrder("bob", OrderStatus.NEW, item("SKU-2", "Gadget", 1, "19.50"));
        seedOrder("carol", OrderStatus.PAID, item("SKU-3", "Sprocket", 4, "3.25"));
        seedOrder("dave", OrderStatus.PAID,
                item("SKU-1", "Widget", 1, "9.99"), item("SKU-4", "Bolt", 10, "0.40"));
        seedOrder("erin", OrderStatus.SHIPPED, item("SKU-5", "Cog", 3, "5.00"));
        seedOrder("frank", OrderStatus.SHIPPED, item("SKU-2", "Gadget", 2, "19.50"));
        seedOrder("grace", OrderStatus.DELIVERED, item("SKU-6", "Gizmo", 1, "42.00"));
        seedOrder("heidi", OrderStatus.DELIVERED,
                item("SKU-3", "Sprocket", 2, "3.25"), item("SKU-5", "Cog", 1, "5.00"));
        seedOrder("ivan", OrderStatus.CANCELLED, item("SKU-7", "Lever", 1, "12.75"));
        seedOrder("judy", OrderStatus.CANCELLED, item("SKU-4", "Bolt", 25, "0.40"));
    }

    private void seedOrder(String customer, OrderStatus status, OrderItem... items) {
        long n = sequence.incrementAndGet();
        // Deterministic, staggered timestamps so listings sort sensibly.
        Instant createdAt = SEED_BASE.plus(Duration.ofHours(n - 1001));
        Instant updatedAt = status == OrderStatus.NEW ? createdAt : createdAt.plus(Duration.ofMinutes(30));
        String id = "ORD-" + n;
        orders.put(id, new Order(id, customer, List.of(items), status, createdAt, updatedAt));
    }

    private static OrderItem item(String sku, String name, int quantity, String unitPrice) {
        return new OrderItem(sku, name, quantity, new BigDecimal(unitPrice));
    }
}