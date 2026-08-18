package com.tp6pin.stockflow.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.SupplierCreateRequest;
import com.tp6pin.stockflow.dto.request.SupplierUpdateRequest;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.dto.response.SupplierResponse;
import com.tp6pin.stockflow.entity.Supplier;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    /**
     * 建立供應商。
     */
    @Transactional
    public SupplierResponse create(
            SupplierCreateRequest request
    ) {
        String normalizedSupplierCode =
            normalizeSupplierCode(request.getSupplierCode());

        String normalizedTaxId =
            normalizeNullableText(request.getTaxId());

        validateSupplierCodeNotDuplicated(
            normalizedSupplierCode,
            null
        );

        validateTaxIdNotDuplicated(
            normalizedTaxId,
            null
        );

        Supplier supplier = new Supplier();

        supplier.setSupplierCode(normalizedSupplierCode);
        supplier.setCompanyName(
            request.getCompanyName().trim()
        );
        supplier.setTaxId(normalizedTaxId);
        supplier.setContactName(
            normalizeNullableText(request.getContactName())
        );
        supplier.setPhone(
            normalizeNullableText(request.getPhone())
        );
        supplier.setEmail(
            normalizeNullableText(request.getEmail())
        );
        supplier.setAddress(
            normalizeNullableText(request.getAddress())
        );
        supplier.setActive(true);

        Supplier savedSupplier =
            supplierRepository.save(supplier);

        return SupplierResponse.from(savedSupplier);
    }

    /**
     * 使用 ID 查詢單一供應商。
     */
    @Transactional(readOnly = true)
    public SupplierResponse getById(Long id) {
        Supplier supplier = findSupplierById(id);

        return SupplierResponse.from(supplier);
    }

    /**
     * 分頁查詢供應商。
     * keyword 為空時查詢全部，
     * 有值時依公司名稱搜尋。
     */
    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> search(
            String keyword,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("active"),
                Sort.Order.asc("companyName")
            )
        );

        Page<Supplier> supplierPage;

        if (keyword == null || keyword.isBlank()) {
            supplierPage =
                supplierRepository.findAll(pageable);
        } else {
            supplierPage =
                supplierRepository
                    .findByCompanyNameContainingIgnoreCase(
                        keyword.trim(),
                        pageable
                    );
        }

        Page<SupplierResponse> responsePage =
            supplierPage.map(SupplierResponse::from);

        return PageResponse.from(responsePage);
    }

    /**
     * 查詢目前啟用中的供應商。
     */
    @Transactional(readOnly = true)
    public List<SupplierResponse> getActiveSuppliers() {
        return supplierRepository
            .findAllByActiveTrueOrderByCompanyNameAsc()
            .stream()
            .map(SupplierResponse::from)
            .toList();
    }

    /**
     * 更新供應商。
     */
    @Transactional
    public SupplierResponse update(
            Long id,
            SupplierUpdateRequest request
    ) {
        Supplier supplier = findSupplierById(id);

        String normalizedSupplierCode =
            normalizeSupplierCode(request.getSupplierCode());

        String normalizedTaxId =
            normalizeNullableText(request.getTaxId());

        validateSupplierCodeNotDuplicated(
            normalizedSupplierCode,
            id
        );

        validateTaxIdNotDuplicated(
            normalizedTaxId,
            id
        );

        supplier.setSupplierCode(normalizedSupplierCode);
        supplier.setCompanyName(
            request.getCompanyName().trim()
        );
        supplier.setTaxId(normalizedTaxId);
        supplier.setContactName(
            normalizeNullableText(request.getContactName())
        );
        supplier.setPhone(
            normalizeNullableText(request.getPhone())
        );
        supplier.setEmail(
            normalizeNullableText(request.getEmail())
        );
        supplier.setAddress(
            normalizeNullableText(request.getAddress())
        );
        supplier.setActive(request.getActive());

        return SupplierResponse.from(supplier);
    }

    /**
     * 停用供應商，不直接刪除資料。
     */
    @Transactional
    public SupplierResponse deactivate(Long id) {
        Supplier supplier = findSupplierById(id);

        if (!supplier.getActive()) {
            return SupplierResponse.from(supplier);
        }

        supplier.setActive(false);

        return SupplierResponse.from(supplier);
    }

    /**
     * 啟用供應商。
     */
    @Transactional
    public SupplierResponse activate(Long id) {
        Supplier supplier = findSupplierById(id);

        if (supplier.getActive()) {
            return SupplierResponse.from(supplier);
        }

        supplier.setActive(true);

        return SupplierResponse.from(supplier);
    }

    /**
     * 查詢供應商，不存在時拋出 404 例外。
     */
    private Supplier findSupplierById(Long id) {
        return supplierRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 " + id + " 的供應商"
                )
            );
    }

    /**
     * 檢查供應商編號是否被其他供應商使用。
     */
    private void validateSupplierCodeNotDuplicated(
            String supplierCode,
            Long currentSupplierId
    ) {
        supplierRepository
            .findBySupplierCode(supplierCode)
            .filter(existingSupplier ->
                currentSupplierId == null
                || !existingSupplier.getId()
                    .equals(currentSupplierId)
            )
            .ifPresent(existingSupplier -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "供應商編號已存在"
                );
            });
    }

    /**
     * 檢查統一編號是否被其他供應商使用。
     */
    private void validateTaxIdNotDuplicated(
            String taxId,
            Long currentSupplierId
    ) {
        if (taxId == null) {
            return;
        }

        supplierRepository
            .findByTaxId(taxId)
            .filter(existingSupplier ->
                currentSupplierId == null
                || !existingSupplier.getId()
                    .equals(currentSupplierId)
            )
            .ifPresent(existingSupplier -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "統一編號已存在"
                );
            });
    }

    /**
     * 供應商編號去除前後空白並統一轉成大寫。
     */
    private String normalizeSupplierCode(String value) {
        return value
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    /**
     * 選填文字若為空白就轉為 null。
     */
    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}