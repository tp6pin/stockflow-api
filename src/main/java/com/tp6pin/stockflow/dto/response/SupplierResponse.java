package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import com.tp6pin.stockflow.entity.Supplier;

import lombok.Getter;

@Getter
public class SupplierResponse {

    private final Long id;
    private final String supplierCode;
    private final String companyName;
    private final String taxId;
    private final String contactName;
    private final String phone;
    private final String email;
    private final String address;
    private final Boolean active;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private SupplierResponse(
            Long id,
            String supplierCode,
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
        this.supplierCode = supplierCode;
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

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
            supplier.getId(),
            supplier.getSupplierCode(),
            supplier.getCompanyName(),
            supplier.getTaxId(),
            supplier.getContactName(),
            supplier.getPhone(),
            supplier.getEmail(),
            supplier.getAddress(),
            supplier.getActive(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}