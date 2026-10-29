package com.kizuna.repository;

import com.kizuna.entity.KanjiPracticeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface KanjiPracticeSubmissionRepository extends JpaRepository<KanjiPracticeSubmission, UUID> {
}
