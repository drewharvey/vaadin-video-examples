package com.example.orders.ui;

import com.example.orders.Order;
import com.example.orders.OrderAiTools;
import com.example.orders.OrderService;
import com.vaadin.flow.component.ai.orchestrator.AIOrchestrator;
import com.vaadin.flow.component.ai.provider.SpringAILLMProvider;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import org.springframework.ai.chat.model.ChatModel;

/**
 * A button that opens an AI chat for querying and modifying orders in natural
 * language. Refreshes the given grid whenever the AI modifies order data.
 */
public class OrderAssistant extends Button {

    private static final String SYSTEM_PROMPT = """
            You are an assistant that helps the user manage customer orders shown in a data grid.
            Look up the current order data before answering questions about it.
            When modifying orders:
            - "cancel" an order = set its status to CANCELLED
            - "archive" an order = set its status to ARCHIVED
            - "complete" an order = set its status to COMPLETED
            Amounts are in US dollars. Keep your answers short and to the point.
            After modifying data, briefly confirm what changed.
            """;

    public OrderAssistant(OrderService orderService, ChatModel chatModel, Grid<Order> grid) {
        super(new SvgIcon("icons/sparkles.svg"));

        var messageList = new MessageList();
        messageList.setSizeFull();

        var messageInput = new MessageInput();
        messageInput.setWidthFull();

        // tool calls run on a background thread, so ui changes like the grid
        // refresh must go through ui.access()
        var tools = new OrderAiTools(orderService,
                () -> getUI().ifPresent(ui -> ui.access(() -> grid.getDataProvider().refreshAll())));

        // connects the chat components to the llm and handles streaming, chat
        // memory and tool calls; it is not a component and is not added to any layout
        AIOrchestrator.builder(new SpringAILLMProvider(chatModel), SYSTEM_PROMPT)
                .withMessageList(messageList)
                .withInput(messageInput)
                .withTools(tools)
                .build();

        var chatPanel = new VerticalLayout(new H2("AI Assistant"), messageList, messageInput);
        chatPanel.setWidth("400px");
        chatPanel.setHeight("600px");
        chatPanel.setFlexGrow(1, messageList);

        // shows the chat panel when this button is clicked
        var popover = new Popover(chatPanel);
        popover.setTarget(this);
        popover.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                messageInput.focus();
            }
        });
    }
}
