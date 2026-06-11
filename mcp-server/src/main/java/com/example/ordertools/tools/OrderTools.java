package com.example.ordertools.tools;

import com.example.ordertools.domain.Order;
import com.example.ordertools.domain.OrderItem;
import com.example.ordertools.domain.OrderStatus;
import com.example.ordertools.service.OrderService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * MCP tools for managing orders. Each {@code @Tool} method becomes a callable
 * tool advertised to MCP clients; descriptions and parameter docs are sent to
 * the model, so keep them clear and accurate.
 */
@Component
public class OrderTools {

    private final OrderService orderService;

    public OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(description = "Create a new order for a customer with one or more line items. Returns the created order including its generated id and total.")
    public Order createOrder(
            @ToolParam(description = "Name or identifier of the customer placing the order") String customer,
            @ToolParam(description = "Line items to include in the order") List<ItemInput> items) {
        List<OrderItem> orderItems = items.stream()
                .map(i -> new OrderItem(i.sku(), i.name(), i.quantity(), i.unitPrice()))
                .toList();
        return orderService.create(customer, orderItems);
    }

    @Tool(description = "Look up a single order by its id. Throws if the order does not exist.")
    public Order getOrder(
            @ToolParam(description = "The order id, e.g. ORD-1001") String orderId) {
        return orderService.get(orderId);
    }

    @Tool(description = "List orders, optionally filtered by status. Pass null status to return all orders, newest first.")
    public List<Order> listOrders(
            @ToolParam(required = false, description = "Optional status filter: NEW, PAID, SHIPPED, DELIVERED, or CANCELLED") OrderStatus status) {
        return orderService.list(status);
    }

    @Tool(description = "Update the status of an existing order. Cancelled orders cannot be changed.")
    public Order updateOrderStatus(
            @ToolParam(description = "The order id to update") String orderId,
            @ToolParam(description = "New status: NEW, PAID, SHIPPED, or DELIVERED") OrderStatus status) {
        return orderService.updateStatus(orderId, status);
    }

    @Tool(description = "Cancel an existing order, setting its status to CANCELLED.")
    public Order cancelOrder(
            @ToolParam(description = "The order id to cancel") String orderId) {
        return orderService.cancel(orderId);
    }

    /**
     * Input shape for a single line item when creating an order.
     */
    public record ItemInput(
            @ToolParam(description = "Stock keeping unit / product code") String sku,
            @ToolParam(description = "Human-readable product name") String name,
            @ToolParam(description = "Quantity ordered, must be positive") int quantity,
            @ToolParam(description = "Price per unit") BigDecimal unitPrice) {
    }
}