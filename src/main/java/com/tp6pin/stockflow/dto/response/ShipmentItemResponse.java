package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.OrderItemAllocation;
import com.tp6pin.stockflow.entity.ShipmentItem;
import com.tp6pin.stockflow.enums.AllocationStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentItemResponse {

    private Long id;

    private Long allocationId;

    private Long orderItemId;

    private Long productId;

    private String productSku;

    private String productName;

    private Long batchId;

    private String batchNumber;

    private Integer quantity;

    private AllocationStatus allocationStatus;

    private LocalDateTime createdAt;

    public static ShipmentItemResponse from(
            ShipmentItem shipmentItem
    ) {
        OrderItemAllocation allocation =
            shipmentItem.getAllocation();

        ShipmentItemResponse response =
            new ShipmentItemResponse();

        response.setId(shipmentItem.getId());

        response.setAllocationId(
            allocation.getId()
        );

        response.setOrderItemId(
            allocation.getOrderItem().getId()
        );

        response.setProductId(
            allocation
                .getOrderItem()
                .getProduct()
                .getId()
        );

        response.setProductSku(
            allocation
                .getOrderItem()
                .getProduct()
                .getSku()
        );

        response.setProductName(
            allocation
                .getOrderItem()
                .getProduct()
                .getName()
        );

        response.setBatchId(
            allocation.getBatch().getId()
        );

        response.setBatchNumber(
            allocation.getBatch().getBatchNumber()
        );

        response.setQuantity(
            shipmentItem.getQuantity()
        );

        response.setAllocationStatus(
            allocation.getStatus()
        );

        response.setCreatedAt(
            shipmentItem.getCreatedAt()
        );

        return response;
    }
}