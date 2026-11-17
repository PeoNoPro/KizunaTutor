package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.BaseEntity;
import com.kizuna.core.entity.Learner;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Một lượt làm bài trực tiếp của học viên (bảng exercise_attempts).
 * Lượt được tính từ lúc bấm "Bắt đầu làm bài"; tắt trang không mở lượt mới, vào lại thì làm tiếp lượt đó.
 * deadlineAt = startedAt + thời gian làm bài (null = không giới hạn). Hết giờ / đóng đề thì tự nộp.
 */
@Entity
@Table(name = "exercise_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAttempt extends BaseEntity {

    public enum Status { IN_PROGRESS, SUBMITTED, GRADED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attempt_id", updatable = false, nullable = false)
    private UUID attemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false, updatable = false)
    private OnlineExercise exercise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learner_id", nullable = false, updatable = false)
    private Learner learner;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "deadline_at")
    private OffsetDateTime deadlineAt;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    /** MANUAL = học viên bấm nộp, TIMEOUT = hết giờ, CLOSED = đóng đề. */
    @Column(name = "submit_reason", length = 10)
    private String submitReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 12, nullable = false)
    @Builder.Default
    private Status status = Status.IN_PROGRESS;

    @Column(name = "late", nullable = false)
    private boolean late;

    @Column(name = "auto_score", precision = 8, scale = 2)
    private BigDecimal autoScore;

    /** Điểm cuối của lượt; null khi còn câu tự luận chờ gia sư chấm. */
    @Column(name = "final_score", precision = 8, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "max_score", precision = 8, scale = 2, nullable = false)
    private BigDecimal maxScore;
}
