package com.kizuna.features.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một tệp đề bài của bài tập (bảng assignment_files, migration 013).
 * Nội dung tệp (cột file_data BYTEA) cố ý KHÔNG ánh xạ để danh sách không tải dữ liệu nhị phân;
 * ghi/đọc qua truy vấn native trong {@code AssignmentFileRepository}.
 */
@Entity
@Table(name = "assignment_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentFile {

    @Id
    @Column(name = "file_id", updatable = false, nullable = false)
    private UUID fileId;

    @Column(name = "assignment_id", nullable = false, updatable = false)
    private UUID assignmentId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
