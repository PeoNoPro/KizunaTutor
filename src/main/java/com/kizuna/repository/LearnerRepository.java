package com.kizuna.repository;

import com.kizuna.entity.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface LearnerRepository extends JpaRepository<Learner, UUID> {
}
