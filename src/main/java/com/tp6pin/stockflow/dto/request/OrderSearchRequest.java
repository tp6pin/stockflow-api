package com.tp6pin.stockflow.dto.request;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tp6pin.stockflow.enums.OrderStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderSearchRequest {

    /**
     * 搜尋訂單編號、客戶編號或客戶公司名稱。
     */
    @Size(
        max = 100,
        message = "搜尋關鍵字不可超過 100 個字"
    )
    private String keyword;

    /**
     * 指定客戶。
     */
    @Positive(message = "客戶 ID 必須大於 0")
    private Long customerId;

    /**
     * 指定訂單狀態。
     */
    private OrderStatus status;

    /**
     * 訂單日期起始時間。
     */
    @DateTimeFormat(
        iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime startDate;

    /**
     * 訂單日期結束時間。
     */
    @DateTimeFormat(
        iso = DateTimeFormat.ISO.DATE_TIME
    )
    private LocalDateTime endDate;

    /**
     * 分頁從第 0 頁開始。
     */
    @Min(value = 0, message = "頁碼不可小於 0")
    private int page = 0;

    /**
     * 每頁預設 20 筆，最多 100 筆。
     */
    @Min(value = 1, message = "每頁筆數不可小於 1")
    @Max(value = 100, message = "每頁筆數不可超過 100")
    private int size = 20;

    /**
     * 起始時間不可晚於結束時間。
     */
    @AssertTrue(message = "開始時間不可晚於結束時間")
    public boolean isDateRangeValid() {
        return startDate == null
            || endDate == null
            || !startDate.isAfter(endDate);
    }
}