package com.tp6pin.stockflow.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.InventoryTransaction;
import com.tp6pin.stockflow.enums.InventoryTransactionType;

public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, Long> {

    Page<InventoryTransaction> findByProduct_Id(
        Long productId,
        Pageable pageable
    );

    Page<InventoryTransaction> findByBatch_Id(
        Long batchId,
        Pageable pageable
    );

    Page<InventoryTransaction> findByTransactionType(
        InventoryTransactionType transactionType,
        Pageable pageable
    );

    Page<InventoryTransaction> findByReferenceTypeAndReferenceId(
        String referenceType,
        Long referenceId,
        Pageable pageable
    );
}