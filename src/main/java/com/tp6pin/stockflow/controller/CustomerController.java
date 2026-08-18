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

import com.tp6pin.stockflow.dto.request.CustomerCreateRequest;
import com.tp6pin.stockflow.dto.request.CustomerUpdateRequest;
import com.tp6pin.stockflow.dto.response.ApiResponse;
import com.tp6pin.stockflow.dto.response.CustomerResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.service.CustomerService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 建立客戶。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CustomerCreateRequest request
    ) {
        CustomerResponse response =
            customerService.create(request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                ApiResponse.success(
                    "客戶建立成功",
                    response
                )
            );
    }

    /**
     * 分頁查詢客戶。
     */
    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<CustomerResponse>>
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
        PageResponse<CustomerResponse> response =
            customerService.search(
                keyword,
                page,
                size
            );

        return ResponseEntity.ok(
            ApiResponse.success(
                "客戶查詢成功",
                response
            )
        );
    }

    /**
     * 查詢目前啟用中的客戶。
     */
    @GetMapping("/active")
    public ResponseEntity<
            ApiResponse<List<CustomerResponse>>
        > getActiveCustomers() {
        List<CustomerResponse> response =
            customerService.getActiveCustomers();

        return ResponseEntity.ok(
            ApiResponse.success(
                "啟用中的客戶查詢成功",
                response
            )
        );
    }

    /**
     * 使用 ID 查詢單一客戶。
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(
            @PathVariable("id") Long id
    ) {
        CustomerResponse response =
            customerService.getById(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "客戶查詢成功",
                response
            )
        );
    }

    /**
     * 更新客戶。
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody CustomerUpdateRequest request
    ) {
        CustomerResponse response =
            customerService.update(id, request);

        return ResponseEntity.ok(
            ApiResponse.success(
                "客戶更新成功",
                response
            )
        );
    }

    /**
     * 停用客戶。
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CustomerResponse>> deactivate(
            @PathVariable("id") Long id
    ) {
        CustomerResponse response =
            customerService.deactivate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "客戶停用成功",
                response
            )
        );
    }

    /**
     * 啟用客戶。
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CustomerResponse>> activate(
            @PathVariable("id") Long id
    ) {
        CustomerResponse response =
            customerService.activate(id);

        return ResponseEntity.ok(
            ApiResponse.success(
                "客戶啟用成功",
                response
            )
        );
    }
}