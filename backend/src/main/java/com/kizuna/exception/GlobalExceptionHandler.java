package com.kizuna.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ toàn cục cho các REST API:
 * Chuyển đổi các Exception thành định dạng JSON chuẩn { success: false, error: ..., message: ... }
 * Tuân thủ quy chuẩn UI Copywriting: Không để lộ lỗi kỹ thuật ra bên ngoài, 100% tiếng Việt thân thiện.
 */
@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "BAD_REQUEST",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "error", "UNAUTHORIZED",
                "message", "Email hoặc mật khẩu không chính xác"
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "NOT_FOUND",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "error", "FORBIDDEN",
                "message", ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Bạn không có quyền truy cập chức năng này"
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        String firstMessage = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Dữ liệu không hợp lệ"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "VALIDATION_FAILED",
                "message", firstMessage,
                "details", errors
        ));
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "INTERNAL_SERVER_ERROR",
                "message", "Đã có lỗi xảy ra trong quá trình xử lý"
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Internal Server Error xảy ra tại: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "INTERNAL_SERVER_ERROR",
                "message", "Hệ thống đang bận, vui lòng thử lại sau ít phút"
        ));
    }
}
