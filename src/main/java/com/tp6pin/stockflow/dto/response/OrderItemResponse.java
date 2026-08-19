package com.tp6pin.stockflow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.OrderItem;

import lombok.Getter;

@Getter
public class OrderItemResponse {

    private final Long id;
    private final Long productId;
    private final String productSku;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal lineAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private OrderItemResponse(
            Long id,
            Long productId,
            String productSku,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal lineAmount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productSku = productSku;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = lineAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderItemResponse from(
            OrderItem orderItem
    ) {
        return new OrderItemResponse(
            orderItem.getId(),
            orderItem.getProduct().getId(),
            orderItem.getProduct().getSku(),
            orderItem.getProduct().getName(),
            orderItem.getQuantity(),
            orderItem.getUnitPrice(),
            orderItem.getLineAmount(),
            orderItem.getCreatedAt(),
            orderItem.getUpdatedAt()
        );
    }
}