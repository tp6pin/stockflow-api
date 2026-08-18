package com.tp6pin.stockflow.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tp6pin.stockflow.dto.response.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        ApiResponse<Void> response = ApiResponse.error(
            errorCode.getCode(),
            exception.getMessage()
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>>
            handleValidationException(
                    MethodArgumentNotValidException exception
            ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(
                fieldError.getField(),
                fieldError.getDefaultMessage()
            );
        }

        ApiResponse<Map<String, String>> response =
            ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getDefaultMessage(),
                fieldErrors
            );

        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT.getHttpStatus())
            .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleUnreadableMessage(
                    HttpMessageNotReadableException exception
            ) {
        ApiResponse<Void> response = ApiResponse.error(
            ErrorCode.MALFORMED_JSON.getCode(),
            ErrorCode.MALFORMED_JSON.getDefaultMessage()
        );

        return ResponseEntity
            .status(ErrorCode.MALFORMED_JSON.getHttpStatus())
            .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleDataIntegrityViolation(
                    DataIntegrityViolationException exception
            ) {
        log.warn("Database constraint violation", exception);

        ApiResponse<Void> response = ApiResponse.error(
            ErrorCode.DATA_CONFLICT.getCode(),
            "資料重複，或仍被其他資料使用"
        );

        return ResponseEntity
            .status(ErrorCode.DATA_CONFLICT.getHttpStatus())
            .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected server error", exception);

        ApiResponse<Void> response = ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}