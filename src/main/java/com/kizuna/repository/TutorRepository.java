package com.kizuna.repository;

import com.kizuna.entity.Tutor;
import com.kizuna.entity.TutorVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {
    Optional<Tutor> findByUser_UserId(UUID userId);
    Optional<Tutor> findByUser_Email(String email);
    long countByVerificationStatus(TutorVerificationStatus verificationStatus);
}
