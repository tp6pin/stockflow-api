package com.tp6pin.stockflow.dto.response;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(
            boolean success,
            String code,
            String message,
            T data
    ) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            true,
            "SUCCESS",
            "操作成功",
            data
        );
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return new ApiResponse<>(
            true,
            "SUCCESS",
            message,
            data
        );
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(
            true,
            "SUCCESS",
            message,
            null
        );
    }

    public static <T> ApiResponse<T> error(
            String code,
            String message
    ) {
        return new ApiResponse<>(
            false,
            code,
            message,
            null
        );
    }

    public static <T> ApiResponse<T> error(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
            false,
            code,
            message,
            data
        );
    }
}