package com.kizuna.features.progress.entity;

import com.kizuna.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Một câu hỏi của bài làm trực tiếp (bảng exercise_questions). data = JSON theo dạng câu ({@link ExerciseQuestionType}). */
@Entity
@Table(name = "exercise_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "question_id", updatable = false, nullable = false)
    private UUID questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false, updatable = false)
    private OnlineExercise exercise;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", length = 20, nullable = false)
    private ExerciseQuestionType type;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "data", columnDefinition = "TEXT", nullable = false)
    private String data;

    @Column(name = "points", precision = 6, scale = 2, nullable = false)
    private BigDecimal points;
}
