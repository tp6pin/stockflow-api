package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemCreateRequest {

    /**
     * 訂購商品 ID。
     */
    @NotNull(message = "商品不可為空")
    @Positive(message = "商品 ID 必須大於 0")
    private Long productId;

    /**
     * 訂購數量。
     */
    @NotNull(message = "訂購數量不可為空")
    @Positive(message = "訂購數量必須大於 0")
    private Integer quantity;
}