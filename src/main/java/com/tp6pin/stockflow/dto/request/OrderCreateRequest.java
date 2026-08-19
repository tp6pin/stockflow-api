package com.tp6pin.stockflow.dto.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderCreateRequest {

    /**
     * 下訂單的客戶 ID。
     */
    @NotNull(message = "客戶不可為空")
    @Positive(message = "客戶 ID 必須大於 0")
    private Long customerId;

    /**
     * 預計交貨日期。
     *
     * 可以不填；如果有填，不可早於今天。
     */
    @FutureOrPresent(message = "預計交貨日期不可早於今天")
    private LocalDate expectedDeliveryDate;

    /**
     * 訂單備註。
     */
    @Size(max = 500, message = "訂單備註不可超過 500 個字元")
    private String note;

    /**
     * 訂單商品明細。
     *
     * 建立訂單時至少需要一項商品。
     */
    @Valid
    @NotEmpty(message = "訂單至少需要一項商品")
    private List<OrderItemCreateRequest> items =
        new ArrayList<>();
}