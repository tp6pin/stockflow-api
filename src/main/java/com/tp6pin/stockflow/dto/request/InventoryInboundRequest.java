package com.tp6pin.stockflow.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InventoryInboundRequest {

    @NotNull(message = "商品不可為空")
    @Positive(message = "商品 ID 必須大於 0")
    private Long productId;

    @NotNull(message = "供應商不可為空")
    @Positive(message = "供應商 ID 必須大於 0")
    private Long supplierId;

    @NotBlank(message = "批次編號不可為空")
    @Size(max = 50, message = "批次編號不可超過 50 個字元")
    private String batchNumber;

    @NotNull(message = "入庫數量不可為空")
    @Positive(message = "入庫數量必須大於 0")
    private Integer quantity;

    @NotNull(message = "收貨日期不可為空")
    @PastOrPresent(message = "收貨日期不可晚於今天")
    private LocalDate receivedDate;

    @PastOrPresent(message = "製造日期不可晚於今天")
    private LocalDate manufactureDate;

    @NotNull(message = "有效期限不可為空")
    private LocalDate expirationDate;

    @Size(max = 255, message = "備註不可超過 255 個字元")
    private String note;
}