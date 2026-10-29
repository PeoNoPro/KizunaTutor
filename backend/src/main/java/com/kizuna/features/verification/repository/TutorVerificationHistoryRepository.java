package com.kizuna.features.verification.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TutorVerificationHistoryRepository extends JpaRepository<TutorVerificationHistory, UUID> {
}
