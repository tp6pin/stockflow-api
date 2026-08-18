package com.tp6pin.stockflow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.Product;

import lombok.Getter;

@Getter
public class ProductResponse {

    private final Long id;
    private final String sku;
    private final String name;
    private final String description;
    private final Long categoryId;
    private final String categoryName;
    private final String unit;
    private final BigDecimal cost;
    private final BigDecimal price;
    private final Integer safetyStock;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ProductResponse(
            Long id,
            String sku,
            String name,
            String description,
            Long categoryId,
            String categoryName,
            String unit,
            BigDecimal cost,
            BigDecimal price,
            Integer safetyStock,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unit = unit;
        this.cost = cost;
        this.price = price;
        this.safetyStock = safetyStock;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getUnit(),
            product.getCost(),
            product.getPrice(),
            product.getSafetyStock(),
            product.getActive(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}