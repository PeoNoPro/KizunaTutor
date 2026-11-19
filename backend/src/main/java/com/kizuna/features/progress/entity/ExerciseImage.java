package com.kizuna.features.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ảnh minh họa của câu hỏi bài làm trực tiếp (bảng exercise_image, migration 034).
 * Nội dung ảnh: trên Cloudflare R2 (khóa "exercise-images/&lt;image_id&gt;") khi bật R2, hoặc cột image_data (BYTEA).
 * Cột image_data cố ý KHÔNG ánh xạ để không tải dữ liệu nhị phân; ghi/đọc qua truy vấn native trong ExerciseImageRepository.
 * exercise_id NULL = gia sư vừa tải lên chưa lưu bài, hoặc câu hỏi đã bỏ ảnh / bài đã bị xóa (sẽ được dọn).
 */
@Entity
@Table(name = "exercise_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseImage {

    @Id
    @Column(name = "image_id", updatable = false, nullable = false)
    private UUID imageId;

    @Column(name = "space_id", nullable = false, updatable = false)
    private UUID spaceId;

    @Column(name = "exercise_id")
    private UUID exerciseId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
