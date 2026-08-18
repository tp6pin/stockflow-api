package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.Customer;

import lombok.Getter;

@Getter
public class CustomerResponse {

    private final Long id;
    private final String customerCode;
    private final String companyName;
    private final String taxId;
    private final String contactName;
    private final String phone;
    private final String email;
    private final String address;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CustomerResponse(
            Long id,
            String customerCode,
            String companyName,
            String taxId,
            String contactName,
            String phone,
            String email,
            String address,
            Boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.customerCode = customerCode;
        this.companyName = companyName;
        this.taxId = taxId;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getCustomerCode(),
            customer.getCompanyName(),
            customer.getTaxId(),
            customer.getContactName(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getAddress(),
            customer.getActive(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}