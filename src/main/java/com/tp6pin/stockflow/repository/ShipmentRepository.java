package com.tp6pin.stockflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.Shipment;
import com.tp6pin.stockflow.enums.ShipmentStatus;

import jakarta.persistence.LockModeType;

public interface ShipmentRepository
        extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByShipmentNumber(
        String shipmentNumber
    );

    Optional<Shipment> findByOrder_Id(
        Long orderId
    );

    Optional<Shipment> findByTrackingNumber(
        String trackingNumber
    );

    boolean existsByShipmentNumber(
        String shipmentNumber
    );

    boolean existsByOrder_Id(
        Long orderId
    );

    boolean existsByTrackingNumber(
        String trackingNumber
    );

    Page<Shipment> findByStatus(
        ShipmentStatus status,
        Pageable pageable
    );

    /**
     * 取得出貨單所屬的訂單 ID。
     *
     * 此查詢本身不加鎖，目的是讓 Service 可以依序執行：
     * 1. 先取得 orderId
     * 2. 鎖定 Order
     * 3. 再鎖定 Shipment
     *
     * 讓出貨、配送完成與取消流程
     * 使用相同的資料鎖定順序。
     */
    @Query("""
        SELECT s.order.id
        FROM Shipment s
        WHERE s.id = :shipmentId
        """)
    Optional<Long> findOrderIdByShipmentId(
        @Param("shipmentId") Long shipmentId
    );

    /**
     * 使用 ID 悲觀鎖定出貨單。
     *
     * 呼叫此方法前應先鎖定所屬 Order，
     * 避免出貨與取消同時執行時產生死鎖。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Shipment s
        WHERE s.id = :shipmentId
        """)
    Optional<Shipment> findByIdForUpdate(
        @Param("shipmentId") Long shipmentId
    );

    /**
     * 依訂單 ID 悲觀鎖定出貨單。
     *
     * 取消 PROCESSING 訂單時使用：
     * 1. OrderService 已先鎖定 Order
     * 2. 再透過此方法鎖定 Shipment
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Shipment s
        WHERE s.order.id = :orderId
        """)
    Optional<Shipment> findByOrderIdForUpdate(
        @Param("orderId") Long orderId
    );
}