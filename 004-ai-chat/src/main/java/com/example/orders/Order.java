package com.example.orders;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "customer_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "customer_name", nullable = false)
    private String customerName = "";

    @Column(name = "item", nullable = false)
    private String item = "";

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate = LocalDate.now();

    // store the enum name ("PENDING") in the database instead of its number
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "completed_date")
    @Nullable
    private LocalDate completedDate;

    protected Order() { // jpa requires a no-arg constructor
    }

    public Order(String customerName, String item, BigDecimal amount, LocalDate dueDate, OrderStatus status) {
        this.customerName = customerName;
        this.item = item;
        this.amount = amount;
        this.dueDate = dueDate;
        this.status = status;
    }

    public @Nullable Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getItem() {
        return item;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public @Nullable LocalDate getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(@Nullable LocalDate completedDate) {
        this.completedDate = completedDate;
    }

    // two orders are the same entity when they have the same database id
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Order other = (Order) obj;
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
