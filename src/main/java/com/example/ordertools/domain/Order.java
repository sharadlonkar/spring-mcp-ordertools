package com.example.ordertools.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An immutable snapshot of a customer order.
 */
public record Order(
        String id,
        String customer,
        List<OrderItem> items,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Order withStatus(OrderStatus newStatus, Instant when) {
        return new Order(id, customer, items, newStatus, createdAt, when);
    }
}
