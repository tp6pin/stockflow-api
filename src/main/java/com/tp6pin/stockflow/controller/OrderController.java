package com.tp6pin.stockflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tp6pin.stockflow.dto.request.OrderCreateRequest;
import com.tp6pin.stockflow.dto.request.OrderItemCreateRequest;
import com.tp6pin.stockflow.dto.request.OrderItemUpdateRequest;
import com.tp6pin.stockflow.dto.request.OrderSearchRequest;
import com.tp6pin.stockflow.dto.request.OrderUpdateRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.OrderResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 建立草稿訂單。
     *
     * createdById 為開發階段暫時使用。
     * JWT 完成後改為從登入使用者取得。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @RequestParam(name = "createdById")
            @Positive(message = "建立人 ID 必須大於 0")
            Long createdById,

            @Valid
            @RequestBody
            OrderCreateRequest request
    ) {
        OrderResponse response =
            orderService.createOrder(
                request,
                createdById
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "訂單建立成功",
                    response
                )
            );
    }

    /**
     * 訂單分頁及條件查詢。
     *
     * 支援 Query Parameters：
     * 1. keyword：訂單編號、客戶編號或客戶公司名稱
     * 2. customerId：客戶 ID
     * 3. status：訂單狀態
     * 4. startDate：訂單日期起始時間
     * 5. endDate：訂單日期結束時間
     * 6. page：頁碼，從 0 開始
     * 7. size：每頁筆數
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<OrderResponse>>
        > searchOrders(
            @Valid
            @ModelAttribute
            OrderSearchRequest request
    ) {
        PageResponse<OrderResponse> response =
            orderService.searchOrders(request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單查詢成功",
                response
            )
        );
    }
    
    /**
     * 使用 ID 查詢單筆訂單。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable("id")
            @Positive(message = "訂單 ID 必須大於 0")
            Long id
    ) {
        OrderResponse response =
            orderService.getOrderById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單查詢成功",
                response
            )
        );
    }

    /**
     * 修改草稿訂單的基本資料。
     */
    @PutMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId,

            @Valid
            @RequestBody
            OrderUpdateRequest request
    ) {
        OrderResponse response =
            orderService.updateOrder(
                orderId,
                request
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單更新成功",
                response
            )
        );
    }

    /**
     * 在草稿訂單中新增商品。
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<ApiResponse<OrderResponse>> addOrderItem(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId,

            @Valid
            @RequestBody
            OrderItemCreateRequest request
    ) {
        OrderResponse response =
            orderService.addOrderItem(
                orderId,
                request
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "訂單商品新增成功",
                    response
                )
            );
    }

    /**
     * 修改草稿訂單中的商品數量。
     */
    @PutMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderItem(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId,

            @PathVariable("itemId")
            @Positive(message = "訂單明細 ID 必須大於 0")
            Long itemId,

            @Valid
            @RequestBody
            OrderItemUpdateRequest request
    ) {
        OrderResponse response =
            orderService.updateOrderItem(
                orderId,
                itemId,
                request
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單商品數量更新成功",
                response
            )
        );
    }

    /**
     * 刪除草稿訂單中的商品。
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderResponse>> removeOrderItem(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId,

            @PathVariable("itemId")
            @Positive(message = "訂單明細 ID 必須大於 0")
            Long itemId
    ) {
        OrderResponse response =
            orderService.removeOrderItem(
                orderId,
                itemId
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單商品刪除成功",
                response
            )
        );
    }
    
    /**
     * 確認草稿訂單。
     *
     * 確認後會：
     * 1. 執行 FEFO 庫存預留
     * 2. 建立 RESERVE 異動紀錄
     * 3. 建立訂單批次配置
     * 4. 將訂單狀態改為 CONFIRMED
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId
    ) {
        OrderResponse response =
            orderService.confirmOrder(orderId);

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單確認成功",
                response
            )
        );
    }
    
    /**
     * 取消訂單。
     *
     * 可取消狀態：
     * 1. DRAFT：直接取消
     * 2. CONFIRMED：釋放預留庫存後取消
     * 3. PROCESSING：取消備貨中的出貨單，
     *    並釋放預留庫存
     *
     * SHIPPED、COMPLETED 與 CANCELLED
     * 不允許執行取消。
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable("orderId")
            @Positive(message = "訂單 ID 必須大於 0")
            Long orderId
    ) {
        OrderResponse response =
            orderService.cancelOrder(orderId);

        return ResponseEntity.ok(
            ApiResponse.success(
                "訂單取消成功",
                response
            )
        );
    }
}