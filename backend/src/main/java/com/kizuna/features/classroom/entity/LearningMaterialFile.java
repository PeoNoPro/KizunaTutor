package com.kizuna.features.classroom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một tệp của tài liệu học tập (bảng learning_material_files, migration 014).
 * Nội dung tệp (cột file_data BYTEA) cố ý KHÔNG ánh xạ; ghi/đọc qua truy vấn native trong LearningMaterialFileRepository.
 */
@Entity
@Table(name = "learning_material_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningMaterialFile {

    @Id
    @Column(name = "file_id", updatable = false, nullable = false)
    private UUID fileId;

    @Column(name = "material_id", nullable = false, updatable = false)
    private UUID materialId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
