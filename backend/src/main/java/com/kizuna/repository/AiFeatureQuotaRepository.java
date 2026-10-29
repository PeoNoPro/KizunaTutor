package com.kizuna.repository;

import com.kizuna.entity.AiFeatureQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AiFeatureQuotaRepository extends JpaRepository<AiFeatureQuota, UUID> {
}
