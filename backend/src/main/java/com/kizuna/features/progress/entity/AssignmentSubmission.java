package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.Learner;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity Bài Nộp (FE-07). Mỗi học viên có một bài nộp cho mỗi bài tập; sửa bài nộp (khi còn hạn, chưa chấm) cập nhật
 * nội dung, tệp (bảng submission_files) và submittedAt. Dữ liệu cũ có thể còn nhiều dòng (attemptNumber) — dùng dòng mới nhất.
 * Nội dung tệp nằm ở cột file_data (BYTEA) và cố ý KHÔNG ánh xạ để danh sách không tải dữ liệu nhị phân;
 * attachmentUrl lưu tên tệp gốc đã làm sạch. Bảng không có created_at/updated_at nên không kế thừa BaseEntity.
 */
@Entity
@Table(name = "assignment_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "submission_id", updatable = false, nullable = false)
    private UUID submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false)
    private Learner learner;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Short attemptNumber = 1;

    @Column(name = "submission_content", columnDefinition = "TEXT")
    private String submissionContent;

    @Column(name = "attachment_url", length = 500)
    private String attachmentUrl;

    @Column(name = "grade", precision = 5, scale = 2)
    private BigDecimal grade;

    @Column(name = "tutor_feedback", columnDefinition = "TEXT")
    private String tutorFeedback;

    @Column(name = "is_finalized", nullable = false)
    @Builder.Default
    private boolean finalized = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "assignment_submission_status_enum", nullable = false)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Builder.Default
    private AssignmentSubmissionStatus status = AssignmentSubmissionStatus.SUBMITTED;

    /** Gán bởi service từ đồng hồ máy chủ để so sánh hạn nộp nhất quán. */
    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "graded_at")
    private OffsetDateTime gradedAt;
}
