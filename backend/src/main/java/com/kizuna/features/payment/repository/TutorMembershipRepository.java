package com.kizuna.features.payment.repository;


import com.kizuna.core.entity.*;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
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
