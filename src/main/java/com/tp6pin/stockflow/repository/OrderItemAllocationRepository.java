package com.tp6pin.stockflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.OrderItemAllocation;
import com.tp6pin.stockflow.enums.AllocationStatus;

public interface OrderItemAllocationRepository
        extends JpaRepository<OrderItemAllocation, Long> {

    List<OrderItemAllocation> findAllByOrderItem_IdOrderByIdAsc(
        Long orderItemId
    );

    List<OrderItemAllocation> findAllByOrderItem_Order_Id(
        Long orderId
    );

    List<OrderItemAllocation> findAllByBatch_IdAndStatus(
        Long batchId,
        AllocationStatus status
    );

    List<OrderItemAllocation> findAllByOrderItem_Order_IdAndStatus(
        Long orderId,
        AllocationStatus status
    );
}