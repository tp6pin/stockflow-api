package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.ShipmentItem;

public interface ShipmentItemRepository
        extends JpaRepository<ShipmentItem, Long> {

    List<ShipmentItem> findAllByShipment_IdOrderByIdAsc(
        Long shipmentId
    );

    Optional<ShipmentItem> findByAllocation_Id(
        Long allocationId
    );

    boolean existsByAllocation_Id(Long allocationId);
}