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

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 處理業務邏輯例外。
     */
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

    /**
     * 處理 Request Body DTO 驗證錯誤。
     *
     * 例如：
     * @NotBlank
     * @NotNull
     * @Size
     * @DecimalMin
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>>
            handleValidationException(
                    MethodArgumentNotValidException exception
            ) {
        Map<String, String> fieldErrors =
            new LinkedHashMap<>();

        for (
            FieldError fieldError :
                exception.getBindingResult().getFieldErrors()
        ) {
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

    /**
     * 處理 Controller 方法參數驗證錯誤。
     *
     * 例如：
     * @RequestParam @Min
     * @RequestParam @Max
     * @RequestParam @Positive
     * @PathVariable @Positive
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>>
            handleConstraintViolationException(
                    ConstraintViolationException exception
            ) {
        Map<String, String> validationErrors =
            new LinkedHashMap<>();

        for (
            ConstraintViolation<?> violation :
                exception.getConstraintViolations()
        ) {
            String parameterPath =
                violation.getPropertyPath().toString();

            validationErrors.putIfAbsent(
                parameterPath,
                violation.getMessage()
            );
        }

        ApiResponse<Map<String, String>> response =
            ApiResponse.error(
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getDefaultMessage(),
                validationErrors
            );

        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT.getHttpStatus())
            .body(response);
    }

    /**
     * 處理 JSON 格式錯誤或型別轉換失敗。
     */
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

    /**
     * 處理資料庫唯一鍵、外鍵與其他限制衝突。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>>
            handleDataIntegrityViolation(
                    DataIntegrityViolationException exception
            ) {
        log.warn(
            "Database constraint violation",
            exception
        );

        ApiResponse<Void> response = ApiResponse.error(
            ErrorCode.DATA_CONFLICT.getCode(),
            "資料重複，或仍被其他資料使用"
        );

        return ResponseEntity
            .status(ErrorCode.DATA_CONFLICT.getHttpStatus())
            .body(response);
    }

    /**
     * 處理未預期的系統錯誤。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>>
            handleUnexpectedException(
                    Exception exception
            ) {
        log.error(
            "Unexpected server error",
            exception
        );

        ApiResponse<Void> response = ApiResponse.error(
            ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
            ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}