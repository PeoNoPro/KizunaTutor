package com.kizuna.repository;

import com.kizuna.entity.TutorMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TutorMembershipRepository extends JpaRepository<TutorMembership, UUID> {
}
