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

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(
        String orderNumber
    );

    boolean existsByOrderNumber(
        String orderNumber
    );

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
     * 訂單分頁及條件查詢。
     *
     * 所有查詢條件皆為選填：
     *
     * keyword：
     * 1. 訂單編號
     * 2. 客戶編號
     * 3. 客戶公司名稱
     *
     * 其他條件：
     * 1. 客戶 ID
     * 2. 訂單狀態
     * 3. 訂單日期起始時間
     * 4. 訂單日期結束時間
     */
    @Query("""
        SELECT o
        FROM Order o
        JOIN o.customer c
        WHERE (
            :keyword IS NULL
            OR LOWER(o.orderNumber)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.customerCode)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.companyName)
                LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (
            :customerId IS NULL
            OR c.id = :customerId
        )
        AND (
            :status IS NULL
            OR o.status = :status
        )
        AND (
            :startDate IS NULL
            OR o.orderDate >= :startDate
        )
        AND (
            :endDate IS NULL
            OR o.orderDate <= :endDate
        )
        """)
    Page<Order> searchOrders(
        @Param("keyword")
        String keyword,

        @Param("customerId")
        Long customerId,

        @Param("status")
        OrderStatus status,

        @Param("startDate")
        LocalDateTime startDate,

        @Param("endDate")
        LocalDateTime endDate,

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
        @Param("orderId")
        Long orderId
    );
}