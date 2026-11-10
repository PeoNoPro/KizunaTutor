package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lượt làm bài trực tiếp (FE-07). Lớp 1-1 nên mỗi bài chỉ có lượt của một học viên. */
@Repository
public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, UUID> {

    List<ExerciseAttempt> findByExercise_ExerciseIdOrderByAttemptNumberAsc(UUID exerciseId);

    List<ExerciseAttempt> findByExercise_ExerciseIdInOrderByAttemptNumberAsc(Collection<UUID> exerciseIds);

    Optional<ExerciseAttempt> findByAttemptIdAndExercise_ExerciseId(UUID attemptId, UUID exerciseId);

    boolean existsByExercise_ExerciseId(UUID exerciseId);
}
