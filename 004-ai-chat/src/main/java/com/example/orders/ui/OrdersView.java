package com.example.orders.ui;

import com.example.base.ui.ViewTitle;
import com.example.orders.OrderService;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import org.springframework.ai.chat.model.ChatModel;

import static com.vaadin.flow.spring.data.VaadinSpringDataHelpers.toSpringPageRequest;

// @Route maps this view to a url, @Menu adds it to the side navigation
@Route("")
@RouteAlias("orders")
@PageTitle("Orders")
@Menu(order = 1, icon = "vaadin:cart", title = "Orders")
class OrdersView extends HorizontalLayout {

    // spring injects the service and the ai model when creating the view
    OrdersView(OrderService orderService, ChatModel chatModel) {
        var dueTodayOnly = new Checkbox("Due today");

        // the grid loads rows lazily: this callback runs whenever the grid
        // needs a page of data, so it always sees the current toggle state
        var grid = new OrdersGrid();
        grid.setItems(query -> {
            var pageRequest = toSpringPageRequest(query);
            var orders = dueTodayOnly.getValue()
                    ? orderService.listDueToday(pageRequest)
                    : orderService.list(pageRequest);
            return orders.stream();
        });
        // refreshAll makes the grid re-run the callback above
        dueTodayOnly.addValueChangeListener(event -> grid.getDataProvider().refreshAll());
        // pushes the checkbox to the right edge of the title row
        dueTodayOnly.getStyle().set("margin-left", "auto");

        var viewTitle = new ViewTitle("Orders");
        var assistantBtn = new OrderAssistant(orderService, chatModel, grid);
        viewTitle.add(assistantBtn, dueTodayOnly);

        var ordersPanel = new VerticalLayout(viewTitle, grid);
        ordersPanel.setSizeFull();

        setSizeFull();
        setPadding(false);
        addAndExpand(ordersPanel);
    }
}
