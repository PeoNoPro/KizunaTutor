package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.LearningMaterialFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningMaterialFileRepository extends JpaRepository<LearningMaterialFile, UUID> {

    List<LearningMaterialFile> findByMaterialIdOrderByCreatedAtAscFileNameAsc(UUID materialId);

    List<LearningMaterialFile> findByMaterialIdInOrderByCreatedAtAscFileNameAsc(Collection<UUID> materialIds);

    Optional<LearningMaterialFile> findByFileIdAndMaterialId(UUID fileId, UUID materialId);

    /** Ghi tệp kèm nội dung nhị phân (cột file_data không ánh xạ vào entity). */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO learning_material_files (file_id, material_id, file_name, file_size, file_data)
            VALUES (:fileId, :materialId, :fileName, :fileSize, :data)
            """, nativeQuery = true)
    int insertFile(@Param("fileId") UUID fileId, @Param("materialId") UUID materialId,
                   @Param("fileName") String fileName, @Param("fileSize") long fileSize, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM learning_material_files WHERE file_id = :fileId", nativeQuery = true)
    byte[] findFileData(@Param("fileId") UUID fileId);
}
