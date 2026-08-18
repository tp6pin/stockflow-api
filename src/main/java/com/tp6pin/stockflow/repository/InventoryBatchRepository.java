package com.tp6pin.stockflow.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.InventoryBatch;

import jakarta.persistence.LockModeType;

public interface InventoryBatchRepository
        extends JpaRepository<InventoryBatch, Long> {

    Optional<InventoryBatch> findByProduct_IdAndBatchNumber(
        Long productId,
        String batchNumber
    );

    boolean existsByProduct_IdAndBatchNumber(
        Long productId,
        String batchNumber
    );

    List<InventoryBatch> findAllByProduct_IdOrderByExpirationDateAsc(
        Long productId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE batch.product.id = :productId
          AND batch.expirationDate >= :today
          AND batch.quantityOnHand > batch.quantityReserved
        ORDER BY
            batch.expirationDate ASC,
            batch.receivedDate ASC,
            batch.id ASC
        """)
    List<InventoryBatch> findAvailableBatchesForUpdate(
        @Param("productId") Long productId,
        @Param("today") LocalDate today
    );

    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE batch.expirationDate BETWEEN :startDate AND :endDate
        ORDER BY batch.expirationDate ASC
        """)
    List<InventoryBatch> findExpiringBatches(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}