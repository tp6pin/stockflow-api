package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.ShipmentItem;

public interface ShipmentItemRepository
        extends JpaRepository<ShipmentItem, Long> {

    /**
     * 查詢指定出貨單的所有出貨明細。
     */
    List<ShipmentItem> findAllByShipment_IdOrderByIdAsc(
        Long shipmentId
    );

    /**
     * 使用庫存配置 ID 查詢出貨明細。
     */
    Optional<ShipmentItem> findByAllocation_Id(
        Long allocationId
    );

    /**
     * 確認庫存配置是否已加入其他出貨明細。
     */
    boolean existsByAllocation_Id(
        Long allocationId
    );

    /**
     * 計算指定出貨單的明細數量。
     */
    long countByShipment_Id(
        Long shipmentId
    );
}