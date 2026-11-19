package com.kizuna.features.classroom.repository;

import com.kizuna.features.classroom.entity.ClassExtraSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassExtraSessionRepository extends JpaRepository<ClassExtraSession, UUID> {

    List<ClassExtraSession> findBySpace_SpaceIdIn(Collection<UUID> spaceIds);

    Optional<ClassExtraSession> findByExtraIdAndSpace_SpaceId(UUID extraId, UUID spaceId);
}
