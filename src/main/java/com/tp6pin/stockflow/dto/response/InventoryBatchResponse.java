package com.tp6pin.stockflow.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.InventoryBatch;

import lombok.Getter;

@Getter
public class InventoryBatchResponse {

    private final Long id;

    private final Long productId;
    private final String productSku;
    private final String productName;

    private final Long supplierId;
    private final String supplierCode;
    private final String supplierName;

    private final String batchNumber;

    private final Integer quantityOnHand;
    private final Integer quantityReserved;
    private final Integer quantityAvailable;

    private final LocalDate receivedDate;
    private final LocalDate manufactureDate;
    private final LocalDate expirationDate;

    private final Boolean expired;
    private final Long version;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private InventoryBatchResponse(
            Long id,
            Long productId,
            String productSku,
            String productName,
            Long supplierId,
            String supplierCode,
            String supplierName,
            String batchNumber,
            Integer quantityOnHand,
            Integer quantityReserved,
            Integer quantityAvailable,
            LocalDate receivedDate,
            LocalDate manufactureDate,
            LocalDate expirationDate,
            Boolean expired,
            Long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.supplierId = supplierId;
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.batchNumber = batchNumber;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
        this.receivedDate = receivedDate;
        this.manufactureDate = manufactureDate;
        this.expirationDate = expirationDate;
        this.expired = expired;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static InventoryBatchResponse from(
            InventoryBatch batch
    ) {
        Integer quantityAvailable =
            batch.getQuantityOnHand()
                - batch.getQuantityReserved();

        boolean expired =
            batch.getExpirationDate().isBefore(LocalDate.now());

        return new InventoryBatchResponse(
            batch.getId(),
            batch.getProduct().getId(),
            batch.getProduct().getSku(),
            batch.getProduct().getName(),
            batch.getSupplier().getId(),
            batch.getSupplier().getSupplierCode(),
            batch.getSupplier().getCompanyName(),
            batch.getBatchNumber(),
            batch.getQuantityOnHand(),
            batch.getQuantityReserved(),
            quantityAvailable,
            batch.getReceivedDate(),
            batch.getManufactureDate(),
            batch.getExpirationDate(),
            expired,
            batch.getVersion(),
            batch.getCreatedAt(),
            batch.getUpdatedAt()
        );
    }
}