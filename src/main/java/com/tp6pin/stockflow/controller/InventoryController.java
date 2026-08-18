package com.tp6pin.stockflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tp6pin.stockflow.dto.request.InventoryAdjustmentRequest;
import com.tp6pin.stockflow.dto.request.InventoryInboundRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.InventoryBatchResponse;
import com.tp6pin.stockflow.dto.response.InventoryTransactionResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.service.InventoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * 商品入庫。
     */
    @PostMapping("/inbound")
    public ResponseEntity<
            ApiResponse<InventoryBatchResponse>
        > inbound(
            @Valid
            @RequestBody
            InventoryInboundRequest request
    ) {
        InventoryBatchResponse response =
            inventoryService.inbound(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "商品入庫成功",
                    response
                )
            );
    }

    /**
     * 手動調整庫存。
     */
    @PostMapping("/adjustments")
    public ResponseEntity<
            ApiResponse<InventoryBatchResponse>
        > adjustInventory(
            @Valid
            @RequestBody
            InventoryAdjustmentRequest request
    ) {
        InventoryBatchResponse response =
            inventoryService.adjustInventory(request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "庫存調整成功",
                response
            )
        );
    }

    /**
     * 分頁與條件查詢庫存批次。
     */
    @GetMapping("/batches")
    public ResponseEntity<
            ApiResponse<
                PageResponse<InventoryBatchResponse>
            >
        > searchBatches(
            @RequestParam(
                name = "keyword",
                required = false
            )
            String keyword,

            @RequestParam(
                name = "productId",
                required = false
            )
            @Positive(message = "商品 ID 必須大於 0")
            Long productId,

            @RequestParam(
                name = "supplierId",
                required = false
            )
            @Positive(message = "供應商 ID 必須大於 0")
            Long supplierId,

            @RequestParam(
                name = "page",
                defaultValue = "0"
            )
            @Min(value = 0, message = "頁碼不可小於 0")
            int page,

            @RequestParam(
                name = "size",
                defaultValue = "20"
            )
            @Min(value = 1, message = "每頁筆數不可小於 1")
            @Max(value = 100, message = "每頁筆數不可超過 100")
            int size
    ) {
        PageResponse<InventoryBatchResponse> response =
            inventoryService.searchBatches(
                keyword,
                productId,
                supplierId,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "庫存批次查詢成功",
                response
            )
        );
    }

    /**
     * 查詢指定天數內即將到期的批次。
     */
    @GetMapping("/batches/expiring")
    public ResponseEntity<
            ApiResponse<List<InventoryBatchResponse>>
        > getExpiringBatches(
            @RequestParam(
                name = "days",
                defaultValue = "30"
            )
            @Min(value = 0, message = "查詢天數不可小於 0")
            @Max(
                value = 365,
                message = "查詢天數不可超過 365"
            )
            int days
    ) {
        List<InventoryBatchResponse> response =
            inventoryService.getExpiringBatches(days);

        return ResponseEntity.ok(
            ApiResponse.success(
                "即將到期批次查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一庫存批次。
     */
    @GetMapping("/batches/{id}")
    public ResponseEntity<
            ApiResponse<InventoryBatchResponse>
        > getBatchById(
            @PathVariable("id")
            @Positive(message = "庫存批次 ID 必須大於 0")
            Long id
    ) {
        InventoryBatchResponse response =
            inventoryService.getBatchById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "庫存批次查詢成功",
                response
            )
        );
    }

    /**
     * 分頁與條件查詢庫存異動紀錄。
     */
    @GetMapping("/transactions")
    public ResponseEntity<
            ApiResponse<
                PageResponse<InventoryTransactionResponse>
            >
        > searchTransactions(
            @RequestParam(
                name = "productId",
                required = false
            )
            @Positive(message = "商品 ID 必須大於 0")
            Long productId,

            @RequestParam(
                name = "batchId",
                required = false
            )
            @Positive(message = "庫存批次 ID 必須大於 0")
            Long batchId,

            @RequestParam(
                name = "transactionType",
                required = false
            )
            String transactionType,

            @RequestParam(
                name = "page",
                defaultValue = "0"
            )
            @Min(value = 0, message = "頁碼不可小於 0")
            int page,

            @RequestParam(
                name = "size",
                defaultValue = "20"
            )
            @Min(value = 1, message = "每頁筆數不可小於 1")
            @Max(value = 100, message = "每頁筆數不可超過 100")
            int size
    ) {
        PageResponse<InventoryTransactionResponse> response =
            inventoryService.searchTransactions(
                productId,
                batchId,
                transactionType,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "庫存異動紀錄查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一庫存異動紀錄。
     */
    @GetMapping("/transactions/{id}")
    public ResponseEntity<
            ApiResponse<InventoryTransactionResponse>
        > getTransactionById(
            @PathVariable("id")
            @Positive(
                message = "庫存異動紀錄 ID 必須大於 0"
            )
            Long id
    ) {
        InventoryTransactionResponse response =
            inventoryService.getTransactionById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "庫存異動紀錄查詢成功",
                response
            )
        );
    }
}