package com.kizuna.features.verification.repository;

import com.kizuna.features.verification.entity.TutorCredential;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorCredentialRepository extends JpaRepository<TutorCredential, UUID> {

    List<TutorCredential> findByTutor_TutorIdOrderByUploadedAtAsc(UUID tutorId);

    Optional<TutorCredential> findByCredentialIdAndTutor_TutorId(UUID credentialId, UUID tutorId);

    @Modifying
    @Query("UPDATE TutorCredential c SET c.verifiedStatus = :newStatus "
            + "WHERE c.tutor.tutorId = :tutorId AND c.verifiedStatus = :currentStatus")
    int updateStatusForTutor(@Param("tutorId") UUID tutorId,
                             @Param("currentStatus") TutorCredentialStatus currentStatus,
                             @Param("newStatus") TutorCredentialStatus newStatus);
}
