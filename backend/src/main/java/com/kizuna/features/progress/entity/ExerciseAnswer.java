package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Câu trả lời một câu trong một lượt (bảng exercise_answers). response = JSON câu trả lời của học viên.
 * Điểm của câu = tutorScore nếu gia sư đã chấm / sửa, ngược lại autoScore.
 */
@Entity
@Table(name = "exercise_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "answer_id", updatable = false, nullable = false)
    private UUID answerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false, updatable = false)
    private ExerciseAttempt attempt;

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    /** null = câu tự luận chờ gia sư chấm. */
    @Column(name = "correct")
    private Boolean correct;

    @Column(name = "auto_score", precision = 6, scale = 2)
    private BigDecimal autoScore;

    @Column(name = "tutor_score", precision = 6, scale = 2)
    private BigDecimal tutorScore;

    @Column(name = "tutor_comment", columnDefinition = "TEXT")
    private String tutorComment;
}
