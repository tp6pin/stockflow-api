package com.tp6pin.stockflow.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "商品編號不可為空")
    @Size(max = 50, message = "商品編號不可超過 50 個字元")
    private String sku;

    @NotBlank(message = "商品名稱不可為空")
    @Size(max = 100, message = "商品名稱不可超過 100 個字元")
    private String name;

    @Size(max = 500, message = "商品說明不可超過 500 個字元")
    private String description;

    @NotNull(message = "商品分類不可為空")
    @Positive(message = "商品分類 ID 必須大於 0")
    private Long categoryId;

    @NotBlank(message = "商品單位不可為空")
    @Size(max = 20, message = "商品單位不可超過 20 個字元")
    private String unit;

    @NotNull(message = "商品成本不可為空")
    @DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "商品成本不可小於 0"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "商品成本最多為 10 位整數及 2 位小數"
    )
    private BigDecimal cost;

    @NotNull(message = "商品售價不可為空")
    @DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "商品售價不可小於 0"
    )
    @Digits(
        integer = 10,
        fraction = 2,
        message = "商品售價最多為 10 位整數及 2 位小數"
    )
    private BigDecimal price;

    @NotNull(message = "安全庫存不可為空")
    @PositiveOrZero(message = "安全庫存不可小於 0")
    private Integer safetyStock;
}