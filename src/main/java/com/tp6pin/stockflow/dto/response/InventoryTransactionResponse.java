package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.InventoryTransaction;
import com.tp6pin.stockflow.enums.InventoryTransactionType;

import lombok.Getter;

@Getter
public class InventoryTransactionResponse {

    private final Long id;

    private final Long productId;
    private final String productSku;
    private final String productName;

    private final Long batchId;
    private final String batchNumber;

    private final InventoryTransactionType transactionType;

    private final Integer onHandChange;
    private final Integer reservedChange;
    private final Integer onHandAfter;
    private final Integer reservedAfter;

    private final String referenceType;
    private final Long referenceId;
    private final String note;

    private final Long createdById;
    private final String createdByName;
    private final LocalDateTime createdAt;

    private InventoryTransactionResponse(
            Long id,
            Long productId,
            String productSku,
            String productName,
            Long batchId,
            String batchNumber,
            InventoryTransactionType transactionType,
            Integer onHandChange,
            Integer reservedChange,
            Integer onHandAfter,
            Integer reservedAfter,
            String referenceType,
            Long referenceId,
            String note,
            Long createdById,
            String createdByName,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.batchId = batchId;
        this.batchNumber = batchNumber;
        this.transactionType = transactionType;
        this.onHandChange = onHandChange;
        this.reservedChange = reservedChange;
        this.onHandAfter = onHandAfter;
        this.reservedAfter = reservedAfter;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.note = note;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }

    public static InventoryTransactionResponse from(
            InventoryTransaction transaction
    ) {
        Long createdById = null;
        String createdByName = null;

        if (transaction.getCreatedBy() != null) {
            createdById =
                transaction.getCreatedBy().getId();

            createdByName =
                transaction.getCreatedBy().getName();
        }

        return new InventoryTransactionResponse(
            transaction.getId(),
            transaction.getProduct().getId(),
            transaction.getProduct().getSku(),
            transaction.getProduct().getName(),
            transaction.getBatch().getId(),
            transaction.getBatch().getBatchNumber(),
            transaction.getTransactionType(),
            transaction.getOnHandChange(),
            transaction.getReservedChange(),
            transaction.getOnHandAfter(),
            transaction.getReservedAfter(),
            transaction.getReferenceType(),
            transaction.getReferenceId(),
            transaction.getNote(),
            createdById,
            createdByName,
            transaction.getCreatedAt()
        );
    }
}