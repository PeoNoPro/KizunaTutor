package com.kizuna.features.progress.repository;

import com.kizuna.features.progress.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findBySpace_SpaceIdOrderByCreatedAtDesc(UUID spaceId);

    Optional<Assignment> findByAssignmentIdAndSpace_SpaceId(UUID assignmentId, UUID spaceId);
}
