package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.ExerciseAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseAudioRepository extends JpaRepository<ExerciseAudio, UUID> {

    Optional<ExerciseAudio> findByAudioIdAndSpaceId(UUID audioId, UUID spaceId);

    List<ExerciseAudio> findByAudioIdInAndSpaceId(Collection<UUID> audioIds, UUID spaceId);

    /** File chưa gắn / không còn gắn vào bài nào của lớp, tải lên trước thời điểm cho trước (để dọn). */
    @Query("SELECT a FROM ExerciseAudio a WHERE a.spaceId = :spaceId AND a.exerciseId IS NULL AND a.createdAt < :before")
    List<ExerciseAudio> findUnlinkedBefore(@Param("spaceId") UUID spaceId, @Param("before") OffsetDateTime before);
}
