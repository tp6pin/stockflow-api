package com.tp6pin.stockflow.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tp6pin.stockflow.entity.Product;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    /**
     * 查詢單一商品時一併載入分類。
     */
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(Long id);

    /**
     * 查詢所有啟用商品，提供下拉選單使用。
     */
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByActiveTrueOrderByNameAsc();

    /**
     * 分頁查詢商品。
     *
     * keyword 與 categoryId 都可以不提供：
     * 1. 都不提供：查詢全部商品
     * 2. 只提供 keyword：依名稱或 SKU 查詢
     * 3. 只提供 categoryId：依分類查詢
     * 4. 兩者都提供：同時套用兩個條件
     */
    @EntityGraph(attributePaths = "category")
    @Query("""
        SELECT p
        FROM Product p
        WHERE (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(
                CONCAT('%', :keyword, '%')
            )
            OR LOWER(p.sku) LIKE LOWER(
                CONCAT('%', :keyword, '%')
            )
        )
        AND (
            :categoryId IS NULL
            OR p.category.id = :categoryId
        )
        """)
    Page<Product> search(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );
}