package com.example.orders;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

// spring data generates the database queries from these method names,
// no sql needed; slice fetches one page of rows at a time
interface OrderRepository extends JpaRepository<Order, Long> {

    Slice<Order> findAllBy(Pageable pageable);

    Slice<Order> findAllByDueDate(LocalDate dueDate, Pageable pageable);
}
