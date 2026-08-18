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
public class InventoryReleaseRequest {

    /**
     * 要釋放預留庫存的批次 ID。
     */
    @NotNull(message = "庫存批次不可為空")
    @Positive(message = "庫存批次 ID 必須大於 0")
    private Long batchId;

    /**
     * 要釋放的預留數量。
     */
    @NotNull(message = "釋放數量不可為空")
    @Positive(message = "釋放數量必須大於 0")
    private Integer quantity;

    /**
     * 原始預留來源類型。
     *
     * 必須與預留時使用的 referenceType 相同。
     */
    @NotBlank(message = "參考來源類型不可為空")
    @Size(
        max = 30,
        message = "參考來源類型不可超過 30 個字元"
    )
    private String referenceType;

    /**
     * 原始預留來源 ID。
     *
     * 必須與預留時使用的 referenceId 相同。
     */
    @NotNull(message = "參考來源 ID 不可為空")
    @Positive(message = "參考來源 ID 必須大於 0")
    private Long referenceId;

    /**
     * 釋放原因或備註。
     */
    @Size(
        max = 255,
        message = "備註不可超過 255 個字元"
    )
    private String note;
}