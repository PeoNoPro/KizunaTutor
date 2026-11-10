package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.OnlineExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Bài làm trực tiếp (FE-07, migration 017). */
@Repository
public interface OnlineExerciseRepository extends JpaRepository<OnlineExercise, UUID> {

    List<OnlineExercise> findBySpace_SpaceIdOrderByCreatedAtDesc(UUID spaceId);

    Optional<OnlineExercise> findByExerciseIdAndSpace_SpaceId(UUID exerciseId, UUID spaceId);
}
