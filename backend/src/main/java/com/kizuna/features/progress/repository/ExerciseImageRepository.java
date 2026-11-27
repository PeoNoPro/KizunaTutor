package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.ExerciseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExerciseImageRepository extends JpaRepository<ExerciseImage, UUID> {

    Optional<ExerciseImage> findByImageIdAndSpaceId(UUID imageId, UUID spaceId);

    List<ExerciseImage> findByImageIdInAndSpaceId(Collection<UUID> imageIds, UUID spaceId);

    /** Ảnh chưa gắn / không còn gắn vào bài nào của lớp, tải lên trước thời điểm cho trước (để dọn). */
    @Query("SELECT i FROM ExerciseImage i WHERE i.spaceId = :spaceId AND i.exerciseId IS NULL AND i.createdAt < :before")
    List<ExerciseImage> findUnlinkedBefore(@Param("spaceId") UUID spaceId, @Param("before") OffsetDateTime before);

    /** Ghi ảnh kèm nội dung (null khi nội dung đã lên R2). Cột image_data không ánh xạ vào entity. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO exercise_image (image_id, space_id, file_name, file_size, content_type, image_data)
            VALUES (:imageId, :spaceId, :fileName, :fileSize, :contentType, :data)
            """, nativeQuery = true)
    int insertImage(@Param("imageId") UUID imageId, @Param("spaceId") UUID spaceId, @Param("fileName") String fileName,
                    @Param("fileSize") long fileSize, @Param("contentType") String contentType, @Param("data") byte[] data);

    @Query(value = "SELECT image_data FROM exercise_image WHERE image_id = :imageId", nativeQuery = true)
    byte[] findImageData(@Param("imageId") UUID imageId);
}
