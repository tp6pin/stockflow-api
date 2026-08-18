package com.tp6pin.stockflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.Shipment;
import com.tp6pin.stockflow.enums.ShipmentStatus;

public interface ShipmentRepository
        extends JpaRepository<Shipment, Long> {

    Optional<Shipment> findByShipmentNumber(String shipmentNumber);

    Optional<Shipment> findByOrder_Id(Long orderId);

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    boolean existsByShipmentNumber(String shipmentNumber);

    boolean existsByOrder_Id(Long orderId);

    Page<Shipment> findByStatus(
        ShipmentStatus status,
        Pageable pageable
    );
}