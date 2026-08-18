package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.OrderItem;

public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findAllByOrder_IdOrderByIdAsc(Long orderId);

    Optional<OrderItem> findByOrder_IdAndProduct_Id(
        Long orderId,
        Long productId
    );

    boolean existsByOrder_IdAndProduct_Id(
        Long orderId,
        Long productId
    );
}