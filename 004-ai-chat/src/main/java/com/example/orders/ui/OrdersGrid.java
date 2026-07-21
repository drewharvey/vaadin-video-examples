package com.example.orders.ui;

import com.example.orders.Order;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class OrdersGrid extends Grid<Order> {

    public OrdersGrid() {
        var currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        // setSortProperty enables sorting by clicking the column header;
        // setAutoWidth + setFlexGrow(0) sizes a column to fit its content
        addColumn(Order::getId)
                .setHeader("ID")
                .setAutoWidth(true)
                .setFlexGrow(0);
        addColumn(Order::getCustomerName)
                .setHeader("Customer")
                .setSortProperty("customerName");
        addColumn(Order::getItem)
                .setHeader("Item");
        addColumn(order -> currencyFormat.format(order.getAmount()))
                .setHeader("Amount")
                .setSortProperty("amount")
                .setTextAlign(ColumnTextAlign.END)
                .setAutoWidth(true)
                .setFlexGrow(0);
        addColumn(Order::getDueDate)
                .setHeader("Due Date")
                .setSortProperty("dueDate")
                .setAutoWidth(true)
                .setFlexGrow(0);
        // a component renderer puts a real component in the cell instead of text
        addColumn(new ComponentRenderer<>(this::createStatusBadge))
                .setHeader("Status")
                .setSortProperty("status")
                .setWidth("130px")
                .setFlexGrow(0);
        addColumn(order -> Optional.ofNullable(order.getCompletedDate()).map(Object::toString).orElse(""))
                .setHeader("Completed")
                .setSortProperty("completedDate")
                .setAutoWidth(true)
                .setFlexGrow(0);
        setSizeFull();
    }

    private Badge createStatusBadge(Order order) {
        var badge = new Badge(order.getStatus().name());
        // theme variants control the badge color
        switch (order.getStatus()) {
            case COMPLETED -> badge.addThemeVariants(BadgeVariant.SUCCESS);
            case CANCELLED -> badge.addThemeVariants(BadgeVariant.ERROR);
            case PENDING -> badge.addThemeVariants(BadgeVariant.WARNING);
            case ARCHIVED -> badge.getStyle().setOpacity("0.5");
        }
        return badge;
    }
}
