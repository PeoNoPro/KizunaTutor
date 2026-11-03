package com.kizuna.core.dto;
import java.time.OffsetDateTime;
public record ApiResponse<T>(boolean success, String message, T data, OffsetDateTime timestamp) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "Đã tải dữ liệu.", data, OffsetDateTime.now());
    }
}
