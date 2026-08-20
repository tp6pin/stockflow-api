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
     * 鎖定出貨單進行狀態變更。
     *
     * 避免同一張出貨單被重複出貨，
     * 或同時執行出貨與取消。
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
}