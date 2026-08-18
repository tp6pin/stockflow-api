package com.tp6pin.stockflow.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tp6pin.stockflow.dto.request.CustomerCreateRequest;
import com.tp6pin.stockflow.dto.request.CustomerUpdateRequest;
import com.tp6pin.stockflow.dto.response.CustomerResponse;
import com.tp6pin.stockflow.dto.response.PageResponse;
import com.tp6pin.stockflow.entity.Customer;
import com.tp6pin.stockflow.exception.BusinessException;
import com.tp6pin.stockflow.exception.ErrorCode;
import com.tp6pin.stockflow.exception.ResourceNotFoundException;
import com.tp6pin.stockflow.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * 建立客戶。
     */
    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String normalizedCustomerCode =
            normalizeCustomerCode(request.getCustomerCode());

        String normalizedTaxId =
            normalizeNullableText(request.getTaxId());

        validateCustomerCodeNotDuplicated(
            normalizedCustomerCode,
            null
        );

        validateTaxIdNotDuplicated(
            normalizedTaxId,
            null
        );

        Customer customer = new Customer();

        customer.setCustomerCode(normalizedCustomerCode);
        customer.setCompanyName(
            request.getCompanyName().trim()
        );
        customer.setTaxId(normalizedTaxId);
        customer.setContactName(
            normalizeNullableText(request.getContactName())
        );
        customer.setPhone(
            normalizeNullableText(request.getPhone())
        );
        customer.setEmail(
            normalizeNullableText(request.getEmail())
        );
        customer.setAddress(
            normalizeNullableText(request.getAddress())
        );
        customer.setActive(true);

        Customer savedCustomer =
            customerRepository.save(customer);

        return CustomerResponse.from(savedCustomer);
    }

    /**
     * 使用 ID 查詢單一客戶。
     */
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        Customer customer = findCustomerById(id);

        return CustomerResponse.from(customer);
    }

    /**
     * 分頁查詢客戶。
     * keyword 為空時查詢全部，
     * 有值時依公司名稱搜尋。
     */
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> search(
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

        Page<Customer> customerPage;

        if (keyword == null || keyword.isBlank()) {
            customerPage =
                customerRepository.findAll(pageable);
        } else {
            customerPage =
                customerRepository
                    .findByCompanyNameContainingIgnoreCase(
                        keyword.trim(),
                        pageable
                    );
        }

        Page<CustomerResponse> responsePage =
            customerPage.map(CustomerResponse::from);

        return PageResponse.from(responsePage);
    }

    /**
     * 查詢目前啟用中的客戶。
     * 可供訂單表單下拉選單使用。
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> getActiveCustomers() {
        return customerRepository
            .findAllByActiveTrueOrderByCompanyNameAsc()
            .stream()
            .map(CustomerResponse::from)
            .toList();
    }

    /**
     * 更新客戶。
     */
    @Transactional
    public CustomerResponse update(
            Long id,
            CustomerUpdateRequest request
    ) {
        Customer customer = findCustomerById(id);

        String normalizedCustomerCode =
            normalizeCustomerCode(request.getCustomerCode());

        String normalizedTaxId =
            normalizeNullableText(request.getTaxId());

        validateCustomerCodeNotDuplicated(
            normalizedCustomerCode,
            id
        );

        validateTaxIdNotDuplicated(
            normalizedTaxId,
            id
        );

        customer.setCustomerCode(normalizedCustomerCode);
        customer.setCompanyName(
            request.getCompanyName().trim()
        );
        customer.setTaxId(normalizedTaxId);
        customer.setContactName(
            normalizeNullableText(request.getContactName())
        );
        customer.setPhone(
            normalizeNullableText(request.getPhone())
        );
        customer.setEmail(
            normalizeNullableText(request.getEmail())
        );
        customer.setAddress(
            normalizeNullableText(request.getAddress())
        );
        customer.setActive(request.getActive());

        return CustomerResponse.from(customer);
    }

    /**
     * 停用客戶，不直接刪除資料。
     */
    @Transactional
    public CustomerResponse deactivate(Long id) {
        Customer customer = findCustomerById(id);

        if (!customer.getActive()) {
            return CustomerResponse.from(customer);
        }

        customer.setActive(false);

        return CustomerResponse.from(customer);
    }

    /**
     * 啟用客戶。
     */
    @Transactional
    public CustomerResponse activate(Long id) {
        Customer customer = findCustomerById(id);

        if (customer.getActive()) {
            return CustomerResponse.from(customer);
        }

        customer.setActive(true);

        return CustomerResponse.from(customer);
    }

    /**
     * 查詢客戶，不存在時拋出 404 例外。
     */
    private Customer findCustomerById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "找不到 ID 為 " + id + " 的客戶"
                )
            );
    }

    /**
     * 檢查客戶編號是否被其他客戶使用。
     */
    private void validateCustomerCodeNotDuplicated(
            String customerCode,
            Long currentCustomerId
    ) {
        customerRepository
            .findByCustomerCode(customerCode)
            .filter(existingCustomer ->
                currentCustomerId == null
                || !existingCustomer.getId()
                    .equals(currentCustomerId)
            )
            .ifPresent(existingCustomer -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "客戶編號已存在"
                );
            });
    }

    /**
     * 檢查統一編號是否被其他客戶使用。
     * 統一編號沒有填寫時不需要檢查。
     */
    private void validateTaxIdNotDuplicated(
            String taxId,
            Long currentCustomerId
    ) {
        if (taxId == null) {
            return;
        }

        customerRepository
            .findByTaxId(taxId)
            .filter(existingCustomer ->
                currentCustomerId == null
                || !existingCustomer.getId()
                    .equals(currentCustomerId)
            )
            .ifPresent(existingCustomer -> {
                throw new BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "統一編號已存在"
                );
            });
    }

    /**
     * 客戶編號去除前後空白並統一轉成大寫。
     */
    private String normalizeCustomerCode(String value) {
        return value
            .trim()
            .toUpperCase(Locale.ROOT);
    }

    /**
     * 選填文字若為空白就轉為 null，
     * 否則去除前後空白。
     */
    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}