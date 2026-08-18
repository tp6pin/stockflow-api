package com.tp6pin.stockflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tp6pin.stockflow.dto.request.ProductCreateRequest;
import com.tp6pin.stockflow.dto.request.ProductUpdateRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.dto.response.ProductResponse;
import com.tp6pin.stockflow.service.ProductService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 建立商品。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        ProductResponse response =
            productService.create(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "商品建立成功",
                    response
                )
            );
    }

    /**
     * 分頁查詢商品。
     *
     * keyword 可搜尋商品名稱或 SKU。
     * categoryId 可依商品分類篩選。
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<ProductResponse>>
        > search(
            @RequestParam(
                name = "keyword",
                required = false
            )
            String keyword,

            @RequestParam(
                name = "categoryId",
                required = false
            )
            @Positive(message = "商品分類 ID 必須大於 0")
            Long categoryId,

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
        PageResponse<ProductResponse> response =
            productService.search(
                keyword,
                categoryId,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品查詢成功",
                response
            )
        );
    }

    /**
     * 查詢所有啟用中的商品。
     */
    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<List<ProductResponse>>
        > getActiveProducts() {
        List<ProductResponse> response =
            productService.getActiveProducts();

        return ResponseEntity.ok(
            ApiResponse.success(
                "啟用中的商品查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一商品。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable("id")
            @Positive(message = "商品 ID 必須大於 0")
            Long id
    ) {
        ProductResponse response =
            productService.getById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品查詢成功",
                response
            )
        );
    }

    /**
     * 更新商品。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable("id")
            @Positive(message = "商品 ID 必須大於 0")
            Long id,

            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse response =
            productService.update(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品更新成功",
                response
            )
        );
    }

    /**
     * 停用商品。
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(
            @PathVariable("id")
            @Positive(message = "商品 ID 必須大於 0")
            Long id
    ) {
        ProductResponse response =
            productService.deactivate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品停用成功",
                response
            )
        );
    }

    /**
     * 啟用商品。
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResponse>> activate(
            @PathVariable("id")
            @Positive(message = "商品 ID 必須大於 0")
            Long id
    ) {
        ProductResponse response =
            productService.activate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品啟用成功",
                response
            )
        );
    }
}