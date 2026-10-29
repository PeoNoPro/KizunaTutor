package com.kizuna.repository;

import com.kizuna.entity.LearningProgressRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LearningProgressRecordRepository extends JpaRepository<LearningProgressRecord, UUID> {
}
