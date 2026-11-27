package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.BaseEntity;
import com.kizuna.features.classroom.entity.LearningSpace;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bài làm trực tiếp trên web (FE-07, bảng online_exercises, migration 017).
 * Độc lập với bài nộp tệp (Assignment): đề do gia sư soạn từng câu, học viên làm trên web, điểm tính riêng.
 */
@Entity
@Table(name = "online_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineExercise extends BaseEntity {

    public enum GradingMode { AUTO, TUTOR }

    public enum Status { DRAFT, PUBLISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "exercise_id", updatable = false, nullable = false)
    private UUID exerciseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id", nullable = false, updatable = false)
    private LearningSpace space;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    /** null = mở ngay khi giao. */
    @Column(name = "opens_at")
    private OffsetDateTime opensAt;

    /** null = không đóng đề. */
    @Column(name = "closes_at")
    private OffsetDateTime closesAt;

    /** null = không giới hạn thời gian làm bài. */
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_mode", length = 10, nullable = false)
    @Builder.Default
    private GradingMode gradingMode = GradingMode.AUTO;

    /** Cho học viên xem đáp án đúng sau khi nộp (gia sư bật / tắt bất cứ lúc nào). */
    @Column(name = "show_answers", nullable = false)
    @Builder.Default
    private boolean showAnswers = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
}
