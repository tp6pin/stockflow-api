package com.tp6pin.stockflow.dto.response;

import java.time.LocalDate;

import com.tp6pin.stockflow.entity.InventoryBatch;

import lombok.Getter;

@Getter
public class InventoryReservationBatchResponse {

    private final Long batchId;
    private final String batchNumber;
    private final LocalDate expirationDate;

    /**
     * 本次從此批次預留的數量。
     */
    private final Integer reservedQuantity;

    /**
     * 此批次完成預留後的總預留數量。
     */
    private final Integer quantityReservedAfter;

    /**
     * 此批次完成預留後仍可使用的數量。
     */
    private final Integer quantityAvailableAfter;

    private InventoryReservationBatchResponse(
            Long batchId,
            String batchNumber,
            LocalDate expirationDate,
            Integer reservedQuantity,
            Integer quantityReservedAfter,
            Integer quantityAvailableAfter
    ) {
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.expirationDate = expirationDate;
        this.reservedQuantity = reservedQuantity;
        this.quantityReservedAfter = quantityReservedAfter;
        this.quantityAvailableAfter = quantityAvailableAfter;
    }

    public static InventoryReservationBatchResponse from(
            InventoryBatch batch,
            Integer reservedQuantity
    ) {
        int quantityAvailableAfter =
            batch.getQuantityOnHand()
                - batch.getQuantityReserved();

        return new InventoryReservationBatchResponse(
            batch.getId(),
            batch.getBatchNumber(),
            batch.getExpirationDate(),
            reservedQuantity,
            batch.getQuantityReserved(),
            quantityAvailableAfter
        );
    }
}