package com.kizuna.features.learning_support.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.learning_support.entity.MinigameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MinigameSessionRepository extends JpaRepository<MinigameSession, UUID> {

    List<MinigameSession> findByUserUserIdOrderByPlayedAtDesc(UUID userId);

    List<MinigameSession> findTop20ByOrderByPlayedAtDesc();
}
