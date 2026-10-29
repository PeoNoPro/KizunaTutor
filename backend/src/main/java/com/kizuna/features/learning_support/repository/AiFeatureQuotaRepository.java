package com.kizuna.features.learning_support.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.entity.AiFeatureQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AiFeatureQuotaRepository extends JpaRepository<AiFeatureQuota, UUID> {
}
