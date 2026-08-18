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
public class InventoryReservationRequest {

    /**
     * 要預留庫存的商品 ID。
     */
    @NotNull(message = "商品不可為空")
    @Positive(message = "商品 ID 必須大於 0")
    private Long productId;

    /**
     * 要預留的商品數量。
     */
    @NotNull(message = "預留數量不可為空")
    @Positive(message = "預留數量必須大於 0")
    private Integer quantity;

    /**
     * 預留來源類型。
     *
     * 現階段 Postman 測試可傳 ORDER_ITEM，
     * 未來串接訂單時代表此預留來自訂單明細。
     */
    @NotBlank(message = "參考來源類型不可為空")
    @Size(
        max = 30,
        message = "參考來源類型不可超過 30 個字元"
    )
    private String referenceType;

    /**
     * 來源資料 ID。
     *
     * 未來可以存放 orderItemId。
     */
    @NotNull(message = "參考來源 ID 不可為空")
    @Positive(message = "參考來源 ID 必須大於 0")
    private Long referenceId;

    /**
     * 預留備註。
     */
    @Size(
        max = 255,
        message = "備註不可超過 255 個字元"
    )
    private String note;
}