package com.tp6pin.stockflow.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.Order;
import com.tp6pin.stockflow.enums.OrderStatus;

import jakarta.persistence.LockModeType;

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
    
    /**
     * 鎖定訂單進行狀態變更。
     *
     * 避免同一張訂單被重複確認、
     * 重複預留或同時取消。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT o
        FROM Order o
        WHERE o.id = :orderId
        """)
    Optional<Order> findByIdForUpdate(
        @Param("orderId") Long orderId
    );
}