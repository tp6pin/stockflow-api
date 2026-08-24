package com.tp6pin.stockflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.tp6pin.stockflow.dto.request.ShipmentCreateRequest;
import com.tp6pin.stockflow.dto.request.ShipmentSearchRequest;
import com.tp6pin.stockflow.dto.request.ShipmentShipRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.dto.response.ShipmentResponse;
import com.tp6pin.stockflow.service.ShipmentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final ShipmentService shipmentService;

    /**
     * 為已確認訂單建立出貨單。
     *
     * 建立後：
     * 1. Shipment 狀態為 PREPARING
     * 2. Order 狀態為 PROCESSING
     * 3. 自動建立 shipment_items
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShipmentResponse>>
            createShipment(
                @RequestParam(name = "orderId")
                @Positive(message = "訂單 ID 必須大於 0")
                Long orderId,

                /*
                 * 開發階段暫時從 Query Parameter 取得。
                 * JWT 完成後改由登入使用者取得。
                 */
                @RequestParam(name = "createdById")
                @Positive(message = "建立人 ID 必須大於 0")
                Long createdById,

                @Valid
                @RequestBody
                ShipmentCreateRequest request
            ) {
        ShipmentResponse response =
            shipmentService.createShipment(
                orderId,
                request,
                createdById
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "出貨單建立成功，訂單已進入處理中",
                    response
                )
            );
    }
    
    /**
     * 出貨單分頁及條件查詢。
     *
     * 支援 Query Parameters：
     * 1. keyword：出貨單號、訂單編號或物流追蹤編號
     * 2. orderId：訂單 ID
     * 3. status：出貨狀態
     * 4. startDate：出貨單建立時間起點
     * 5. endDate：出貨單建立時間終點
     * 6. page：頁碼，從 0 開始
     * 7. size：每頁筆數
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<ShipmentResponse>>
        > searchShipments(
            @Valid
            @ModelAttribute
            ShipmentSearchRequest request
    ) {
        PageResponse<ShipmentResponse> response =
            shipmentService.searchShipments(request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "出貨單查詢成功",
                response
            )
        );
    }
    
    /**
     * 執行實際出貨。
     *
     * 執行成功後：
     * 1. 扣除實際庫存與預留庫存
     * 2. Allocation：ACTIVE → SHIPPED
     * 3. Shipment：PREPARING → SHIPPED
     * 4. Order：PROCESSING → SHIPPED
     */
    @PostMapping("/{shipmentId}/ship")
    public ResponseEntity<ApiResponse<ShipmentResponse>> shipShipment(
            @PathVariable("shipmentId")
            @Positive(message = "出貨單 ID 必須大於 0")
            Long shipmentId,

            @Valid
            @RequestBody
            ShipmentShipRequest request
    ) {
        ShipmentResponse response =
            shipmentService.shipShipment(
                shipmentId,
                request
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "出貨成功",
                response
            )
        );
    }
    
    /**
     * 完成配送。
     *
     * 執行成功後：
     * 1. Shipment：SHIPPED → DELIVERED
     * 2. Order：SHIPPED → COMPLETED
     * 3. 寫入 deliveredAt 與 completedAt
     *
     * 庫存已在實際出貨時扣除，
     * 因此完成配送不會再次修改庫存。
     */
    @PostMapping("/{shipmentId}/complete")
    public ResponseEntity<ApiResponse<ShipmentResponse>>
            completeDelivery(
                @PathVariable("shipmentId")
                @Positive(message = "出貨單 ID 必須大於 0")
                Long shipmentId
            ) {

        ShipmentResponse response =
            shipmentService.completeDelivery(
                shipmentId
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "配送完成，訂單已完成",
                response
            )
        );
    }
}