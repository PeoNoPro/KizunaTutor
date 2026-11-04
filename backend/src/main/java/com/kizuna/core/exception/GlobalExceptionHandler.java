package com.kizuna.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý ngoại lệ toàn cục cho toàn bộ hệ thống KizunaTutor.
 * Tuân thủ BACKEND_RULES.md:
 * - Trả về định dạng JSON thân thiện.
 * - Không lộ mã lỗi kỹ thuật hoặc stack trace ra bên ngoài.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "BAD_REQUEST",
                "message", ex.getMessage() != null ? ex.getMessage() : "Yêu cầu không hợp lệ"
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "NOT_FOUND",
                "message", ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy dữ liệu yêu cầu"
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "success", false,
                "error", "FORBIDDEN",
                "message", "Bạn không có quyền truy cập chức năng này"
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        String firstMessage = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Dữ liệu gửi lên không hợp lệ"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "success", false,
                "error", "VALIDATION_FAILED",
                "message", firstMessage,
                "details", errors
        ));
    }

    /**
     * Trang hoặc tệp tĩnh không tồn tại: trả 404 thay vì báo lỗi hệ thống 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        log.debug("Không tìm thấy tài nguyên: {}", ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "success", false,
                "error", "NOT_FOUND",
                "message", "Không tìm thấy trang bạn yêu cầu"
        ));
    }

    /**
     * FE-09: xung đột trạng thái nghiệp vụ (409).
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "success", false,
                "error", "CONFLICT",
                "message", ex.getMessage() != null ? ex.getMessage() : "Thao tác không phù hợp với trạng thái hiện tại"
        ));
    }

    /**
     * FE-09: dịch vụ bên ngoài (cổng thanh toán) chưa sẵn sàng (503).
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(ServiceUnavailableException ex) {
        log.warn("Dịch vụ bên ngoài không khả dụng: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "error", "SERVICE_UNAVAILABLE",
                "message", ex.getMessage() != null ? ex.getMessage() : "Dịch vụ đang tạm thời không khả dụng. Vui lòng thử lại sau."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Internal Server Error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "error", "INTERNAL_SERVER_ERROR",
                "message", "Hệ thống đang bận, vui lòng thử lại sau ít phút"
        ));
    }
}
