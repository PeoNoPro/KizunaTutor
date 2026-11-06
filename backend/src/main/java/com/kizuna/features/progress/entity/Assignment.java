package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.BaseEntity;
import com.kizuna.features.classroom.entity.LearningSpace;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Bài Tập (FE-07) giao trong một Learning Space (lớp gồm một gia sư và một học viên).
 * dueDate null = bài tập không có hạn nộp.
 * Tệp đề bài (tùy chọn): attachmentUrl lưu tên tệp gốc đã làm sạch; nội dung ở cột file_data (BYTEA),
 * cố ý KHÔNG ánh xạ để danh sách không tải dữ liệu nhị phân.
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    private UUID assignmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false)
    private LearningSpace space;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "total_points", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal totalPoints = new BigDecimal("100.00");

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;
}
