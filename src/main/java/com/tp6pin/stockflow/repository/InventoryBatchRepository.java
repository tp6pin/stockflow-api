package com.tp6pin.stockflow.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.InventoryBatch;

import jakarta.persistence.LockModeType;

public interface InventoryBatchRepository
        extends JpaRepository<InventoryBatch, Long> {

    /**
     * 使用 ID 查詢批次時，一併載入商品及供應商。
     */
    @Override
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    Optional<InventoryBatch> findById(Long id);

    /**
     * 依商品及批次編號查詢。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    Optional<InventoryBatch> findByProduct_IdAndBatchNumber(
        Long productId,
        String batchNumber
    );

    /**
     * 檢查同一商品是否已存在相同批次編號。
     */
    boolean existsByProduct_IdAndBatchNumber(
        Long productId,
        String batchNumber
    );

    /**
     * 入庫時查詢既有批次並加上寫入鎖。
     *
     * 避免兩個入庫請求同時讀到相同庫存數量，
     * 造成其中一筆數量被覆蓋。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE batch.product.id = :productId
          AND LOWER(batch.batchNumber)
              = LOWER(:batchNumber)
        """)
    Optional<InventoryBatch> findByProductAndBatchNumberForUpdate(
        @Param("productId") Long productId,
        @Param("batchNumber") String batchNumber
    );

    /**
     * 依商品查詢所有批次，優先顯示最早到期批次。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    List<InventoryBatch>
            findAllByProduct_IdOrderByExpirationDateAsc(
                Long productId
            );

    /**
     * 分頁與條件查詢庫存批次。
     *
     * keyword 可搜尋：
     * 1. 批次編號
     * 2. 商品 SKU
     * 3. 商品名稱
     *
     * productId、supplierId 均為選填。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE (
            :keyword IS NULL
            OR LOWER(batch.batchNumber) LIKE LOWER(
                CONCAT('%', :keyword, '%')
            )
            OR LOWER(batch.product.sku) LIKE LOWER(
                CONCAT('%', :keyword, '%')
            )
            OR LOWER(batch.product.name) LIKE LOWER(
                CONCAT('%', :keyword, '%')
            )
        )
        AND (
            :productId IS NULL
            OR batch.product.id = :productId
        )
        AND (
            :supplierId IS NULL
            OR batch.supplier.id = :supplierId
        )
        """)
    Page<InventoryBatch> search(
        @Param("keyword") String keyword,
        @Param("productId") Long productId,
        @Param("supplierId") Long supplierId,
        Pageable pageable
    );

    /**
     * 鎖定商品目前可以使用的批次。
     *
     * 未來 FEFO 配置訂單庫存時使用。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE batch.product.id = :productId
          AND batch.expirationDate >= :today
          AND batch.quantityOnHand
              > batch.quantityReserved
        ORDER BY
            batch.expirationDate ASC,
            batch.receivedDate ASC,
            batch.id ASC
        """)
    List<InventoryBatch> findAvailableBatchesForUpdate(
        @Param("productId") Long productId,
        @Param("today") LocalDate today
    );

    /**
     * 查詢指定日期範圍內即將到期的批次。
     */
    @EntityGraph(
        attributePaths = {
            "product",
            "supplier"
        }
    )
    @Query("""
        SELECT batch
        FROM InventoryBatch batch
        WHERE batch.expirationDate
            BETWEEN :startDate AND :endDate
        ORDER BY
            batch.expirationDate ASC,
            batch.id ASC
        """)
    List<InventoryBatch> findExpiringBatches(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * 新增依批次 ID 悲觀鎖定的方法
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"product", "supplier"})
    @Query("""
            SELECT batch
            FROM InventoryBatch batch
            WHERE batch.id = :batchId
            """)
    Optional<InventoryBatch> findByIdForUpdate(
            @Param("batchId") Long batchId
    );
}