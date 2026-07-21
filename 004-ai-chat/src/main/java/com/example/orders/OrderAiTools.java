package com.example.orders;

import org.springframework.ai.tool.annotation.Tool;

import java.util.List;

/**
 * Tools that the LLM can call during a chat conversation to query and modify
 * orders. Registered on the AIOrchestrator with {@code withTools()}.
 */
public class OrderAiTools {

    private final OrderService orderService;
    private final Runnable onDataChanged;

    public OrderAiTools(OrderService orderService, Runnable onDataChanged) {
        this.orderService = orderService;
        this.onDataChanged = onDataChanged;
    }

    // the llm reads the @Tool descriptions to decide when to call each method;
    // spring ai converts the orders to and from json automatically
    @Tool(description = "Returns all orders")
    public List<Order> getAllOrders() {
        return orderService.findAll();
    }

    @Tool(description = "Updates orders in database")
    public void updateOrders(List<Order> orders) {
        orderService.save(orders);
        // tells the ui to refresh
        onDataChanged.run();
    }
}
