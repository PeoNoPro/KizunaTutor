package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.ExerciseQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Câu hỏi của bài làm trực tiếp (FE-07). */
@Repository
public interface ExerciseQuestionRepository extends JpaRepository<ExerciseQuestion, UUID> {

    List<ExerciseQuestion> findByExercise_ExerciseIdOrderByPositionAsc(UUID exerciseId);

    List<ExerciseQuestion> findByExercise_ExerciseIdIn(Collection<UUID> exerciseIds);

    long deleteByExercise_ExerciseId(UUID exerciseId);
}
