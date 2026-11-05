package com.kizuna.features.verification.controller;

import com.kizuna.features.verification.dto.response.ApiErrorResponse;
import com.kizuna.features.verification.service.VerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ánh xạ lỗi của các API FE-03 sang JSON tiếng Việt theo contracts/verification-api.md.
 * Chỉ áp dụng cho controller của feature verification; ưu tiên hơn GlobalExceptionHandler.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.kizuna.features.verification.controller")
public class VerificationExceptionHandler {

    @ExceptionHandler(VerificationException.class)
    public ResponseEntity<ApiErrorResponse> handleVerification(VerificationException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponse.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .fields(ex.getFields())
                .missingDocuments(ex.getMissingDocuments())
                .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message(VerificationException.MSG_VALIDATION)
                .fields(fields)
                .build());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiErrorResponse> handleUnreadable(Exception ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message(VerificationException.MSG_VALIDATION)
                .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiErrorResponse.builder()
                .code("INVALID_FILE")
                .message(VerificationException.MSG_INVALID_FILE)
                .build());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiErrorResponse> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                .code("INVALID_FILE")
                .message(VerificationException.MSG_INVALID_FILE)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong muốn tại API xác minh gia sư", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("Hệ thống đang bận, bạn vui lòng thử lại sau.")
                .build());
    }
}
