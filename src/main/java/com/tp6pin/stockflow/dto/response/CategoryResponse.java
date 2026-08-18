package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.Category;

import lombok.Getter;

@Getter
public class CategoryResponse {

    private final Long id;
    private final String name;
    private final String description;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CategoryResponse(
            Long id,
            String name,
            String description,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getActive(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}