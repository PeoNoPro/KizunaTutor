package com.kizuna.repository;

import com.kizuna.entity.TutorMembership;
import com.kizuna.entity.TutorMembershipState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorMembershipRepository extends JpaRepository<TutorMembership, UUID> {
    List<TutorMembership> findByTutor_TutorIdOrderByCreatedAtDesc(UUID tutorId);
    Optional<TutorMembership> findFirstByTutor_User_EmailAndStatusOrderByEndDateDesc(String email, TutorMembershipState status);
    Optional<TutorMembership> findByTransactionRef(String transactionRef);
}
