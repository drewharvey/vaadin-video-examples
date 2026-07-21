package com.example.orders;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

/**
 * Seeds the database with ~100 demo orders on first startup, so there is
 * interesting data to ask the AI assistant about.
 */
@Component
class OrderDemoDataGenerator implements CommandLineRunner {

    private static final String[] CUSTOMERS = { "Alice Johnson", "Bob Smith", "Carol White", "David Brown",
            "Emma Davis", "Frank Miller", "Grace Wilson", "Henry Moore", "Isabella Taylor", "Jack Anderson",
            "Karen Thomas", "Liam Jackson", "Mia Martin", "Noah Lee", "Olivia Perez", "Peter Clark",
            "Quinn Rodriguez", "Rachel Lewis", "Samuel Walker", "Tina Hall" };

    private static final String[] ITEMS = { "Wireless Mouse", "Mechanical Keyboard", "USB-C Hub", "Laptop Stand",
            "Webcam", "Noise-Cancelling Headphones", "Monitor Arm", "Desk Lamp", "External SSD", "Phone Charger",
            "Bluetooth Speaker", "Ergonomic Chair Cushion", "Cable Organizer", "Screen Cleaner Kit", "Notebook Set" };

    private final OrderRepository orderRepository;

    OrderDemoDataGenerator(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) {
        // only seed an empty database
        if (orderRepository.count() > 0) {
            return;
        }

        var random = new Random(42);
        var today = LocalDate.now();
        var orders = new ArrayList<Order>();

        // a well-known order to cancel in the demo
        orders.add(new Order("John Snow", "Winter Coat", new BigDecimal("129.99"), today.plusDays(3),
                OrderStatus.PENDING));

        for (int i = 0; i < 99; i++) {
            var customer = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
            var item = ITEMS[random.nextInt(ITEMS.length)];
            var amount = BigDecimal.valueOf(5 + random.nextDouble() * 195).setScale(2, RoundingMode.HALF_UP);

            // spread due dates from 20 days ago to 20 days ahead, with a few due today
            var dueDate = i % 12 == 0 ? today : today.plusDays(random.nextInt(41) - 20L);

            var status = switch (random.nextInt(10)) {
                case 0, 1, 2 -> OrderStatus.COMPLETED;
                case 3 -> OrderStatus.CANCELLED;
                default -> OrderStatus.PENDING;
            };

            var order = new Order(customer, item, amount, dueDate, status);
            if (status == OrderStatus.COMPLETED) {
                // completed 0-15 days ago, so some are older than a week
                order.setCompletedDate(today.minusDays(random.nextInt(16)));
            }
            orders.add(order);
        }

        orderRepository.saveAll(orders);
    }
}
