package com.kizuna.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.Serial;

/**
 * Xung đột trạng thái nghiệp vụ (HTTP 409, BACKEND_RULES.md mục 6.4).
 * Thông điệp phải là tiếng Việt thân thiện, hiển thị được cho người dùng.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
