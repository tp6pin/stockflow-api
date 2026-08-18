package com.tp6pin.stockflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryCreateRequest {

    @NotBlank(message = "分類名稱不可為空")
    @Size(max = 50, message = "分類名稱不可超過 50 個字元")
    private String name;

    @Size(max = 255, message = "分類說明不可超過 255 個字元")
    private String description;
}