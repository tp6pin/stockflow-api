package com.tp6pin.stockflow.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.Order;
import com.tp6pin.stockflow.enums.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    Page<Order> findByStatus(
        OrderStatus status,
        Pageable pageable
    );

    Page<Order> findByCustomer_Id(
        Long customerId,
        Pageable pageable
    );

    Page<Order> findByOrderDateBetween(
        LocalDateTime startDate,
        LocalDateTime endDate,
        Pageable pageable
    );

    Page<Order> findByCustomer_IdAndStatus(
        Long customerId,
        OrderStatus status,
        Pageable pageable
    );
}