package com.kizuna.features.classroom.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Thông tin tài liệu hiển thị cho người dùng. Không chứa đường dẫn lưu trữ nội bộ. */
public record LearningMaterialResponse(
        UUID materialId,
        String title,
        String description,
        String fileType,
        OffsetDateTime createdAt,
        String uploadedByName) {
}
