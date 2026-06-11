package com.example.ordertools.service;

import com.example.ordertools.domain.Order;
import com.example.ordertools.domain.OrderItem;
import com.example.ordertools.domain.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceTest {

    private final OrderService service = new OrderService();

    private List<OrderItem> sampleItems() {
        return List.of(
                new OrderItem("SKU-1", "Widget", 2, new BigDecimal("9.99")),
                new OrderItem("SKU-2", "Gadget", 1, new BigDecimal("19.50")));
    }

    @Test
    void createsOrderWithTotalAndNewStatus() {
        Order order = service.create("alice", sampleItems());

        assertThat(order.id()).startsWith("ORD-");
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.total()).isEqualByComparingTo(new BigDecimal("39.48"));
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> service.create("alice", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updatesStatusAndFiltersByStatus() {
        Order order = service.create("bob", sampleItems());
        service.updateStatus(order.id(), OrderStatus.PAID);

        assertThat(service.list(OrderStatus.PAID))
                .extracting(Order::id)
                .containsExactly(order.id());
        assertThat(service.list(OrderStatus.NEW)).isEmpty();
    }

    @Test
    void cancelledOrderCannotChange() {
        Order order = service.create("carol", sampleItems());
        service.cancel(order.id());

        assertThatThrownBy(() -> service.updateStatus(order.id(), OrderStatus.PAID))
                .isInstanceOf(IllegalStateException.class);
    }
}