package com.tp6pin.stockflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.InventoryTransaction;
import com.tp6pin.stockflow.enums.InventoryTransactionType;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    /**
     * 使用 ID 查詢庫存異動紀錄。
     */
    @Override
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    java.util.Optional<InventoryTransaction> findById(Long id);

    /**
     * 依商品查詢庫存異動紀錄。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    Page<InventoryTransaction> findByProduct_Id(
        Long productId,
        Pageable pageable
    );

    /**
     * 依庫存批次查詢異動紀錄。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    Page<InventoryTransaction> findByBatch_Id(
        Long batchId,
        Pageable pageable
    );

    /**
     * 依異動類型查詢。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    Page<InventoryTransaction> findByTransactionType(
        InventoryTransactionType transactionType,
        Pageable pageable
    );

    /**
     * 依來源資料查詢異動紀錄。
     *
     * 例如查詢某張訂單產生的所有庫存異動。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    Page<InventoryTransaction>
            findByReferenceTypeAndReferenceId(
                String referenceType,
                Long referenceId,
                Pageable pageable
            );

    /**
     * 庫存異動紀錄複合查詢。
     *
     * productId、batchId、transactionType 均為選填。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "batch",
            "createdBy"
        }
    )
    @Query("""
        SELECT transaction
        FROM InventoryTransaction transaction
        WHERE (
            :productId IS NULL
            OR transaction.product.id = :productId
        )
        AND (
            :batchId IS NULL
            OR transaction.batch.id = :batchId
        )
        AND (
            :transactionType IS NULL
            OR transaction.transactionType
                = :transactionType
        )
        """)
    Page<InventoryTransaction> search(
        @Param("productId") Long productId,
        @Param("batchId") Long batchId,
        @Param("transactionType")
        InventoryTransactionType transactionType,
        Pageable pageable
    );
}