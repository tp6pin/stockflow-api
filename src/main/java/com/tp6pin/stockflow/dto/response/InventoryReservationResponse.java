package com.tp6pin.stockflow.dto.response;

import java.util.List;

import com.tp6pin.stockflow.entity.Product;

import lombok.Getter;

@Getter
public class InventoryReservationResponse {

    private final Long productId;
    private final String productSku;
    private final String productName;

    /**
     * 使用者要求預留的總數量。
     */
    private final Integer requestedQuantity;

    /**
     * 實際完成預留的總數量。
     */
    private final Integer reservedQuantity;

    private final String referenceType;
    private final Long referenceId;

    /**
     * FEFO 分配到的各批次明細。
     */
    private final List<InventoryReservationBatchResponse> batches;

    private InventoryReservationResponse(
            Long productId,
            String productSku,
            String productName,
            Integer requestedQuantity,
            Integer reservedQuantity,
            String referenceType,
            Long referenceId,
            List<InventoryReservationBatchResponse> batches
    ) {
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.requestedQuantity = requestedQuantity;
        this.reservedQuantity = reservedQuantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.batches = List.copyOf(batches);
    }

    public static InventoryReservationResponse of(
            Product product,
            Integer requestedQuantity,
            Integer reservedQuantity,
            String referenceType,
            Long referenceId,
            List<InventoryReservationBatchResponse> batches
    ) {
        return new InventoryReservationResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            requestedQuantity,
            reservedQuantity,
            referenceType,
            referenceId,
            batches
        );
    }
}