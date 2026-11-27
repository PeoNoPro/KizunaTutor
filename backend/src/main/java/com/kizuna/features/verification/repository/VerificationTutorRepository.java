package com.kizuna.features.verification.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập hồ sơ gia sư phục vụ FE-03.
 * Mọi thao tác thay đổi hồ sơ xác minh PHẢI dùng các phương thức findLocked* để khóa dòng
 * (SELECT ... FOR UPDATE), bảo đảm GB-01 và "một hồ sơ đang chờ" khi có thao tác đồng thời.
 */
@Repository
public interface VerificationTutorRepository extends JpaRepository<Tutor, UUID>, JpaSpecificationExecutor<Tutor> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tutor t JOIN FETCH t.user WHERE t.tutorId = :tutorId")
    Optional<Tutor> findLockedByTutorId(@Param("tutorId") UUID tutorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tutor t JOIN FETCH t.user u WHERE u.userId = :userId")
    Optional<Tutor> findLockedByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM Tutor t JOIN FETCH t.user WHERE t.tutorId = :tutorId")
    Optional<Tutor> findWithUserByTutorId(@Param("tutorId") UUID tutorId);

    @Query("SELECT t FROM Tutor t JOIN FETCH t.user u WHERE u.userId = :userId")
    Optional<Tutor> findWithUserByUserId(@Param("userId") UUID userId);

    long countByVerificationStatus(TutorVerificationStatus status);

    long countByVerificationStatusAndSubmittedAtGreaterThanEqual(TutorVerificationStatus status, OffsetDateTime from);

    long countByVerificationStatusAndSubmittedAtLessThan(TutorVerificationStatus status, OffsetDateTime before);
}
