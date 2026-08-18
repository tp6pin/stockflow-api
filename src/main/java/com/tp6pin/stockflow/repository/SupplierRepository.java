package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.Supplier;

public interface SupplierRepository
        extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    Optional<Supplier> findByTaxId(String taxId);

    boolean existsBySupplierCode(String supplierCode);

    boolean existsByTaxId(String taxId);

    Page<Supplier> findByCompanyNameContainingIgnoreCase(
        String companyName,
        Pageable pageable
    );

    List<Supplier> findAllByActiveTrueOrderByCompanyNameAsc();
}