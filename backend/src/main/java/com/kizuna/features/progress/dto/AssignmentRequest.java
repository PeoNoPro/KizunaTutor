package com.kizuna.features.progress.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Tạo/sửa bài tập (UC-66, UC-95). dueDate null = không có hạn; totalPoints null = 100.
 * opensAt (migration 017): thời gian mở bài; null = mở ngay. Trước giờ mở học viên không thấy bài.
 */
public record AssignmentRequest(String title, String description, OffsetDateTime dueDate, BigDecimal totalPoints,
                                OffsetDateTime opensAt) {

    public AssignmentRequest(String title, String description, OffsetDateTime dueDate, BigDecimal totalPoints) {
        this(title, description, dueDate, totalPoints, null);
    }
}
