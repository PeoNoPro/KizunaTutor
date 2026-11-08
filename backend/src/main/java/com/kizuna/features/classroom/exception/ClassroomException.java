package com.kizuna.features.classroom.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Ngoại lệ nghiệp vụ của FE-06. Message luôn là tiếng Việt thân thiện, được trả thẳng cho người dùng.
 */
@Getter
public class ClassroomException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> fieldErrors;

    public ClassroomException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ClassroomException(HttpStatus status, String message, Map<String, String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }

    public static ClassroomException badRequest(String message) {
        return new ClassroomException(HttpStatus.BAD_REQUEST, message);
    }

    public static ClassroomException field(String field, String message) {
        return new ClassroomException(HttpStatus.BAD_REQUEST, message, Map.of(field, message));
    }

    public static ClassroomException unauthorized() {
        return new ClassroomException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
    }

    public static ClassroomException forbidden(String message) {
        return new ClassroomException(HttpStatus.FORBIDDEN, message);
    }

    public static ClassroomException notFound(String message) {
        return new ClassroomException(HttpStatus.NOT_FOUND, message);
    }

    public static ClassroomException conflict(String message) {
        return new ClassroomException(HttpStatus.CONFLICT, message);
    }

    /** 409 kèm lỗi trường (vd: trùng lịch học) để giao diện hiển thị đúng ô nhập, không phải đoán theo câu chữ. */
    public static ClassroomException fieldConflict(String field, String message) {
        return new ClassroomException(HttpStatus.CONFLICT, message, Map.of(field, message));
    }

    public static ClassroomException storageUnavailable() {
        return new ClassroomException(HttpStatus.SERVICE_UNAVAILABLE,
                "Dịch vụ lưu trữ tài liệu tạm thời không khả dụng. Vui lòng thử lại sau.");
    }
}
