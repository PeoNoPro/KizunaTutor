package com.kizuna.repository;

import com.kizuna.entity.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, UUID> {
}
