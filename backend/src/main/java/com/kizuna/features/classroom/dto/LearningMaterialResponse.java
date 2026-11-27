package com.kizuna.features.classroom.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Thông tin tài liệu hiển thị cho người dùng. Một tài liệu gồm tiêu đề, mô tả và 1–10 tệp.
 * fileType: loại của tệp đầu tiên (giữ cho giao diện cũ). Không chứa đường dẫn lưu trữ nội bộ.
 */
public record LearningMaterialResponse(
        UUID materialId,
        String title,
        String description,
        String fileType,
        List<MaterialFileResponse> files,
        OffsetDateTime createdAt,
        String uploadedByName) {
}
