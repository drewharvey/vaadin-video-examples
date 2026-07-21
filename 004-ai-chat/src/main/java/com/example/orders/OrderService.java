package com.example.orders;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<Order> list(Pageable pageable) {
        return orderRepository.findAllBy(pageable).toList();
    }

    @Transactional(readOnly = true)
    public List<Order> listDueToday(Pageable pageable) {
        return orderRepository.findAllByDueDate(LocalDate.now(), pageable).toList();
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public void save(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public void save(List<Order> orders) {
        orderRepository.saveAll(orders);
    }
}
