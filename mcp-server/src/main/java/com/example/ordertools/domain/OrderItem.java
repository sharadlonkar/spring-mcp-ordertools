package com.example.ordertools.domain;

import java.math.BigDecimal;

/**
 * A single line item within an order.
 */
public record OrderItem(
        String sku,
        String name,
        int quantity,
        BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
