package com.tp6pin.stockflow.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

    INVALID_INPUT(
        HttpStatus.BAD_REQUEST,
        "INVALID_INPUT",
        "輸入資料不正確"
    ),

    MALFORMED_JSON(
        HttpStatus.BAD_REQUEST,
        "MALFORMED_JSON",
        "JSON 格式不正確"
    ),

    UNAUTHORIZED(
        HttpStatus.UNAUTHORIZED,
        "UNAUTHORIZED",
        "尚未登入或登入憑證已失效"
    ),

    FORBIDDEN(
        HttpStatus.FORBIDDEN,
        "FORBIDDEN",
        "沒有執行此操作的權限"
    ),

    RESOURCE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "RESOURCE_NOT_FOUND",
        "找不到指定資料"
    ),

    DUPLICATE_RESOURCE(
        HttpStatus.CONFLICT,
        "DUPLICATE_RESOURCE",
        "資料已存在"
    ),

    INVALID_STATUS_TRANSITION(
        HttpStatus.CONFLICT,
        "INVALID_STATUS_TRANSITION",
        "不允許進行此狀態變更"
    ),

    INSUFFICIENT_STOCK(
        HttpStatus.CONFLICT,
        "INSUFFICIENT_STOCK",
        "可用庫存不足"
    ),

    DATA_CONFLICT(
        HttpStatus.CONFLICT,
        "DATA_CONFLICT",
        "資料狀態發生衝突"
    ),

    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "系統發生未預期錯誤"
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    ErrorCode(
            HttpStatus httpStatus,
            String code,
            String defaultMessage
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }
}