package com.tp6pin.stockflow.entity;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.enums.AllocationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "order_item_allocations",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_allocations_order_item_batch",
            columnNames = {"order_item_id", "batch_id"}
        )
    }
)
public class OrderItemAllocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    @Column(name = "allocated_quantity", nullable = false)
    private Integer allocatedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationStatus status = AllocationStatus.ACTIVE;

    @Column(name = "allocated_at", nullable = false, updatable = false)
    private LocalDateTime allocatedAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;
}