package com.kizuna.features.payment.repository;

import com.kizuna.core.entity.Tutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Truy cập hồ sơ gia sư phục vụ FE-09 (không dùng repository của feature khác).
 * Khóa dòng tutors khi kích hoạt hoặc hết hạn gói để các giao dịch của cùng một gia sư được xử lý tuần tự (cộng nối đúng).
 * Thứ tự khóa cố định: payment_transactions trước, tutors sau.
 */
@Repository
public interface MembershipTutorRepository extends JpaRepository<Tutor, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tutor t JOIN FETCH t.user u WHERE u.userId = :userId")
    Optional<Tutor> findLockedByUserId(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tutor t JOIN FETCH t.user WHERE t.tutorId = :tutorId")
    Optional<Tutor> findLockedByTutorId(@Param("tutorId") UUID tutorId);

    @Query("SELECT t FROM Tutor t JOIN FETCH t.user u WHERE u.userId = :userId")
    Optional<Tutor> findWithUserByUserId(@Param("userId") UUID userId);

    @Query("SELECT t FROM Tutor t JOIN FETCH t.user WHERE t.tutorId = :tutorId")
    Optional<Tutor> findWithUserByTutorId(@Param("tutorId") UUID tutorId);
}
