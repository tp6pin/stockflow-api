package com.tp6pin.stockflow.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.tp6pin.stockflow.entity.Order;
import com.tp6pin.stockflow.enums.OrderStatus;

import lombok.Getter;

@Getter
public class OrderResponse {

    private final Long id;
    private final String orderNumber;

    private final Long customerId;
    private final String customerCode;
    private final String customerName;

    private final OrderStatus status;
    private final LocalDateTime orderDate;
    private final LocalDate expectedDeliveryDate;

    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal totalAmount;

    private final String note;

    private final LocalDateTime confirmedAt;
    private final LocalDateTime processingAt;
    private final LocalDateTime shippedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime cancelledAt;

    private final Long createdById;
    private final String createdByName;

    private final Long version;
    private final List<OrderItemResponse> items;

    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private OrderResponse(
            Long id,
            String orderNumber,
            Long customerId,
            String customerCode,
            String customerName,
            OrderStatus status,
            LocalDateTime orderDate,
            LocalDate expectedDeliveryDate,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            BigDecimal totalAmount,
            String note,
            LocalDateTime confirmedAt,
            LocalDateTime processingAt,
            LocalDateTime shippedAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            Long createdById,
            String createdByName,
            Long version,
            List<OrderItemResponse> items,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.status = status;
        this.orderDate = orderDate;
        this.expectedDeliveryDate = expectedDeliveryDate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
        this.note = note;
        this.confirmedAt = confirmedAt;
        this.processingAt = processingAt;
        this.shippedAt = shippedAt;
        this.completedAt = completedAt;
        this.cancelledAt = cancelledAt;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.version = version;
        this.items = items;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses =
            order.getItems()
                .stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getCustomer().getId(),
            order.getCustomer().getCustomerCode(),
            order.getCustomer().getCompanyName(),
            order.getStatus(),
            order.getOrderDate(),
            order.getExpectedDeliveryDate(),
            order.getSubtotal(),
            order.getTaxAmount(),
            order.getTotalAmount(),
            order.getNote(),
            order.getConfirmedAt(),
            order.getProcessingAt(),
            order.getShippedAt(),
            order.getCompletedAt(),
            order.getCancelledAt(),
            order.getCreatedBy().getId(),
            order.getCreatedBy().getName(),
            order.getVersion(),
            itemResponses,
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}