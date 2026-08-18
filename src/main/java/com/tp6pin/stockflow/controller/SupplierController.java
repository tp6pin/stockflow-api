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

import com.tp6pin.stockflow.dto.request.SupplierCreateRequest;
import com.tp6pin.stockflow.dto.request.SupplierUpdateRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.dto.response.SupplierResponse;
import com.tp6pin.stockflow.service.SupplierService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * 建立供應商。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponse>> create(
            @Valid @RequestBody SupplierCreateRequest request
    ) {
        SupplierResponse response =
            supplierService.create(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "供應商建立成功",
                    response
                )
            );
    }

    /**
     * 分頁查詢供應商。
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<SupplierResponse>>
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
        PageResponse<SupplierResponse> response =
            supplierService.search(
                keyword,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "供應商查詢成功",
                response
            )
        );
    }

    /**
     * 查詢目前啟用中的供應商。
     */
    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<List<SupplierResponse>>
        > getActiveSuppliers() {
        List<SupplierResponse> response =
            supplierService.getActiveSuppliers();

        return ResponseEntity.ok(
            ApiResponse.success(
                "啟用中的供應商查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一供應商。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> getById(
            @PathVariable("id") Long id
    ) {
        SupplierResponse response =
            supplierService.getById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "供應商查詢成功",
                response
            )
        );
    }

    /**
     * 更新供應商。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody SupplierUpdateRequest request
    ) {
        SupplierResponse response =
            supplierService.update(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "供應商更新成功",
                response
            )
        );
    }

    /**
     * 停用供應商。
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<SupplierResponse>> deactivate(
            @PathVariable("id") Long id
    ) {
        SupplierResponse response =
            supplierService.deactivate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "供應商停用成功",
                response
            )
        );
    }

    /**
     * 啟用供應商。
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SupplierResponse>> activate(
            @PathVariable("id") Long id
    ) {
        SupplierResponse response =
            supplierService.activate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "供應商啟用成功",
                response
            )
        );
    }
}