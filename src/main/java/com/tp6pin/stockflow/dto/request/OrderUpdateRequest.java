package com.tp6pin.stockflow.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderUpdateRequest {

    /**
     * 預計交貨日期。
     *
     * 如果有填，不可早於今天。
     */
    @FutureOrPresent(message = "預計交貨日期不可早於今天")
    private LocalDate expectedDeliveryDate;

    /**
     * 訂單備註。
     */
    @Size(max = 500, message = "訂單備註不可超過 500 個字元")
    private String note;
}