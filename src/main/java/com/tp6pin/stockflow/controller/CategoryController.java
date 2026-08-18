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

import com.tp6pin.stockflow.dto.request.CategoryCreateRequest;
import com.tp6pin.stockflow.dto.request.CategoryUpdateRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.CategoryResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.service.CategoryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 建立商品分類。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        CategoryResponse response =
            categoryService.create(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "商品分類建立成功",
                    response
                )
            );
    }

    /**
     * 分頁查詢商品分類。
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<CategoryResponse>>
        > search(
            @RequestParam(
                name = "keyword",
                required = false
            )
            String keyword,

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
        PageResponse<CategoryResponse> response =
            categoryService.search(
                keyword,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品分類查詢成功",
                response
            )
        );
    }

    /**
     * 查詢目前啟用中的商品分類。
     */
    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<List<CategoryResponse>>
        > getActiveCategories() {
        List<CategoryResponse> response =
            categoryService.getActiveCategories();

        return ResponseEntity.ok(
            ApiResponse.success(
                "啟用中的商品分類查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一商品分類。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @PathVariable("id") Long id
    ) {
        CategoryResponse response =
            categoryService.getById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品分類查詢成功",
                response
            )
        );
    }

    /**
     * 更新商品分類。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        CategoryResponse response =
            categoryService.update(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品分類更新成功",
                response
            )
        );
    }

    /**
     * 停用商品分類。
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivate(
            @PathVariable("id") Long id
    ) {
        CategoryResponse response =
            categoryService.deactivate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品分類停用成功",
                response
            )
        );
    }

    /**
     * 啟用商品分類。
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CategoryResponse>> activate(
            @PathVariable("id") Long id
    ) {
        CategoryResponse response =
            categoryService.activate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "商品分類啟用成功",
                response
            )
        );
    }
}