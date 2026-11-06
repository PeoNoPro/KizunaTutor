package com.kizuna.core.exception;

import java.io.Serial;

/**
 * Dịch vụ bên ngoài (ví dụ cổng thanh toán) chưa cấu hình hoặc không phản hồi (HTTP 503).
 * Thông điệp phải là tiếng Việt thân thiện, hiển thị được cho người dùng.
 */
public class ServiceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
