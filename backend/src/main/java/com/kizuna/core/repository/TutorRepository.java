package com.kizuna.core.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {
    Optional<Tutor> findByUser_UserId(UUID userId);
    Optional<Tutor> findByUser_Email(String email);
    long countByVerificationStatus(TutorVerificationStatus verificationStatus);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Tutor t LEFT JOIN FETCH t.user u")
    java.util.List<Tutor> findAllWithUser();
}
