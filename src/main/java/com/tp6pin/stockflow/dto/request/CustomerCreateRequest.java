package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerCreateRequest {

    @NotBlank(message = "客戶編號不可為空")
    @Size(max = 30, message = "客戶編號不可超過 30 個字元")
    private String customerCode;

    @NotBlank(message = "公司名稱不可為空")
    @Size(max = 100, message = "公司名稱不可超過 100 個字元")
    private String companyName;

    @Size(max = 20, message = "統一編號不可超過 20 個字元")
    private String taxId;

    @Size(max = 50, message = "聯絡人姓名不可超過 50 個字元")
    private String contactName;

    @Size(max = 30, message = "電話不可超過 30 個字元")
    private String phone;

    @Email(message = "Email 格式不正確")
    @Size(max = 100, message = "Email 不可超過 100 個字元")
    private String email;

    @Size(max = 255, message = "地址不可超過 255 個字元")
    private String address;
}