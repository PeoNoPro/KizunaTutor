package com.kizuna.features.progress.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.progress.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
}
