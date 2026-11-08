package com.kizuna.features.progress.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một tệp bài làm của bài nộp (bảng submission_files, migration 013).
 * Nội dung tệp (cột file_data BYTEA) cố ý KHÔNG ánh xạ; ghi/đọc qua truy vấn native trong {@code SubmissionFileRepository}.
 */
@Entity
@Table(name = "submission_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionFile {

    @Id
    @Column(name = "file_id", updatable = false, nullable = false)
    private UUID fileId;

    @Column(name = "submission_id", nullable = false, updatable = false)
    private UUID submissionId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
