package com.kizuna.core.repository;

import com.kizuna.core.entity.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerRepository extends JpaRepository<Learner, UUID> {
    Optional<Learner> findByUser_UserId(UUID userId);
    Optional<Learner> findByUser_Email(String email);
}
