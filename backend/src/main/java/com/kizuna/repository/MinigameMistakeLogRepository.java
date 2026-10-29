package com.kizuna.repository;

import com.kizuna.entity.MinigameMistakeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MinigameMistakeLogRepository extends JpaRepository<MinigameMistakeLog, UUID> {

    List<MinigameMistakeLog> findBySessionSessionIdOrderByMistakeCountDesc(UUID sessionId);
}
