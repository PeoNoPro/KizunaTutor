package com.kizuna.core.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, UUID> {
    Optional<Tutor> findByUser(User user);
    Optional<Tutor> findByUser_UserId(UUID userId);
}
