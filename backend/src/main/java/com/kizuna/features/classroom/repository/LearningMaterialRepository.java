package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.LearningMaterial;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
