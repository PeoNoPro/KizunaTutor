package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.LearningMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<LearningMaterial> findBySpace_SpaceIdOrderByCreatedAtDesc(UUID spaceId);

    Optional<LearningMaterial> findByMaterialIdAndSpace_SpaceId(UUID materialId, UUID spaceId);

    long countBySpace_SpaceId(UUID spaceId);

    /*
     * Nội dung tệp (cột file_data BYTEA) KHÔNG ánh xạ vào entity để danh sách tài liệu
     * không phải tải dữ liệu nhị phân; chỉ đọc/ghi riêng qua hai truy vấn dưới đây.
     */

    @Modifying(flushAutomatically = true)
    @Query(value = "UPDATE learning_materials SET file_data = :data WHERE material_id = :materialId", nativeQuery = true)
    int updateFileData(@Param("materialId") UUID materialId, @Param("data") byte[] data);

    @Query(value = "SELECT file_data FROM learning_materials WHERE material_id = :materialId", nativeQuery = true)
    byte[] findFileData(@Param("materialId") UUID materialId);
}
