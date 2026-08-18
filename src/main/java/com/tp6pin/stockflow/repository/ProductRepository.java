package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.tp6pin.stockflow.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findAllByActiveTrueOrderByNameAsc();

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
        String name,
        String sku,
        Pageable pageable
    );

    Page<Product> findByCategory_Id(Long categoryId, Pageable pageable);
}