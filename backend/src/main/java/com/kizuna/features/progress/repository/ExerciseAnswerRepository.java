package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.ExerciseAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Câu trả lời của từng lượt làm bài trực tiếp (FE-07). */
@Repository
public interface ExerciseAnswerRepository extends JpaRepository<ExerciseAnswer, UUID> {

    List<ExerciseAnswer> findByAttempt_AttemptId(UUID attemptId);

    List<ExerciseAnswer> findByAttempt_AttemptIdIn(Collection<UUID> attemptIds);

    Optional<ExerciseAnswer> findByAttempt_AttemptIdAndQuestionId(UUID attemptId, UUID questionId);
}
