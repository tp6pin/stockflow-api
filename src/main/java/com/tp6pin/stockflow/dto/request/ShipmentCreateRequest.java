package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentCreateRequest {

    @Size(
        max = 50,
        message = "物流商不可超過 50 個字元"
    )
    private String carrier;

    @NotBlank(message = "收件人姓名不可為空")
    @Size(
        max = 100,
        message = "收件人姓名不可超過 100 個字元"
    )
    private String recipientName;

    @Size(
        max = 30,
        message = "收件人電話不可超過 30 個字元"
    )
    private String recipientPhone;

    @NotBlank(message = "配送地址不可為空")
    @Size(
        max = 255,
        message = "配送地址不可超過 255 個字元"
    )
    private String shippingAddress;

    @Size(
        max = 500,
        message = "備註不可超過 500 個字元"
    )
    private String note;
}