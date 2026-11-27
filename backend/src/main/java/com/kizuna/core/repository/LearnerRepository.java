package com.kizuna.core.repository;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearnerRepository extends JpaRepository<Learner, UUID> {
    Optional<Learner> findByUser(User user);
    Optional<Learner> findByUser_UserId(UUID userId);
}
