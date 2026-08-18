package com.tp6pin.stockflow.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByTaxId(String taxId);

    boolean existsByCustomerCode(String customerCode);

    boolean existsByTaxId(String taxId);

    Page<Customer> findByCompanyNameContainingIgnoreCase(
        String companyName,
        Pageable pageable
    );
}