package com.example.ordertools.service;

import com.example.ordertools.domain.Order;
import com.example.ordertools.domain.OrderItem;
import com.example.ordertools.domain.OrderStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory order store. Replace with a JPA repository for persistence.
 */
@Service
public class OrderService {

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
}