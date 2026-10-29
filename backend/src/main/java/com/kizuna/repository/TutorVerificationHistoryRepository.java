package com.kizuna.repository;

import com.kizuna.entity.TutorVerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TutorVerificationHistoryRepository extends JpaRepository<TutorVerificationHistory, UUID> {
}
