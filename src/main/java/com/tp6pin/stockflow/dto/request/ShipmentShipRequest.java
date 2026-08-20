package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShipmentShipRequest {

    /**
     * 物流追蹤編號。
     *
     * 執行實際出貨時寫入 Shipment，
     * 同一個追蹤編號不可重複使用。
     */
    @NotBlank(message = "物流追蹤編號不可為空")
    @Size(
        max = 100,
        message = "物流追蹤編號不可超過 100 個字元"
    )
    private String trackingNumber;
}