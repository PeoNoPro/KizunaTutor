package com.kizuna.features.payment.repository;

import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Bản ghi hội viên của gia sư (UC-97, UC-44, UC-45).
 */
@Repository
public interface TutorMembershipRepository extends JpaRepository<TutorMembership, UUID>,
        JpaSpecificationExecutor<TutorMembership> {

    @EntityGraph(attributePaths = {"plan"})
    List<TutorMembership> findByTutorTutorIdOrderByStartDateDesc(UUID tutorId);

    @EntityGraph(attributePaths = {"plan"})
    Optional<TutorMembership> findFirstByTransactionRef(String transactionRef);

    boolean existsByTutorTutorId(UUID tutorId);

    @Query("SELECT MAX(m.endDate) FROM TutorMembership m WHERE m.tutor.tutorId = :tutorId AND m.status = :status")
    Optional<LocalDate> findMaxEndDate(@Param("tutorId") UUID tutorId, @Param("status") TutorMembershipState status);

    @Query("SELECT COUNT(m) FROM TutorMembership m WHERE m.tutor.tutorId = :tutorId AND m.status = :status AND m.endDate < :today")
    long countEndedBefore(@Param("tutorId") UUID tutorId, @Param("status") TutorMembershipState status,
                          @Param("today") LocalDate today);

    @Query("SELECT COUNT(m) FROM TutorMembership m WHERE m.tutor.tutorId = :tutorId AND m.status = :status AND m.endDate >= :today")
    long countValidOn(@Param("tutorId") UUID tutorId, @Param("status") TutorMembershipState status,
                      @Param("today") LocalDate today);

    @Query("SELECT m FROM TutorMembership m WHERE m.tutor.tutorId = :tutorId AND m.status = :status AND m.endDate < :today")
    List<TutorMembership> findEndedBefore(@Param("tutorId") UUID tutorId, @Param("status") TutorMembershipState status,
                                          @Param("today") LocalDate today);
}
