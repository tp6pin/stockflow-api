package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryAdjustmentRequest {

    @NotNull(message = "庫存批次不可為空")
    @Positive(message = "庫存批次 ID 必須大於 0")
    private Long batchId;

    @NotNull(message = "庫存調整數量不可為空")
    private Integer quantityChange;

    @NotBlank(message = "庫存調整原因不可為空")
    @Size(
        max = 255,
        message = "庫存調整原因不可超過 255 個字元"
    )
    private String reason;
}