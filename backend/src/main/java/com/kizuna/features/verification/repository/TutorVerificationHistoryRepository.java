package com.kizuna.features.verification.repository;

import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorVerificationHistoryRepository extends JpaRepository<TutorVerificationHistory, UUID> {

    @Query("SELECT h FROM TutorVerificationHistory h LEFT JOIN FETCH h.officer "
            + "WHERE h.tutor.tutorId = :tutorId ORDER BY h.createdAt ASC")
    List<TutorVerificationHistory> findTimeline(@Param("tutorId") UUID tutorId);

    long countByActionAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            TutorVerifAction action, OffsetDateTime from, OffsetDateTime to);

    @Query("SELECT h FROM TutorVerificationHistory h "
            + "JOIN FETCH h.tutor t JOIN FETCH t.user LEFT JOIN FETCH h.officer "
            + "WHERE h.action IN :actions ORDER BY h.createdAt DESC")
    List<TutorVerificationHistory> findRecent(@Param("actions") Collection<TutorVerifAction> actions,
                                              Pageable pageable);

    /**
     * Lịch sử quyết định thẩm định trên toàn hệ thống (trang "Lịch sử xét duyệt").
     * keywordPattern rỗng = không lọc.
     */
    @Query(value = "SELECT h FROM TutorVerificationHistory h "
            + "JOIN FETCH h.tutor t JOIN FETCH t.user u LEFT JOIN FETCH h.officer o "
            + "WHERE h.action IN :actions "
            + "AND (:keywordPattern = '' OR LOWER(u.fullName) LIKE :keywordPattern OR LOWER(u.email) LIKE :keywordPattern) "
            + "ORDER BY h.createdAt DESC",
            countQuery = "SELECT COUNT(h) FROM TutorVerificationHistory h "
            + "JOIN h.tutor t JOIN t.user u LEFT JOIN h.officer o "
            + "WHERE h.action IN :actions "
            + "AND (:keywordPattern = '' OR LOWER(u.fullName) LIKE :keywordPattern OR LOWER(u.email) LIKE :keywordPattern)")
    Page<TutorVerificationHistory> searchDecisions(@Param("actions") Collection<TutorVerifAction> actions,
                                                   @Param("keywordPattern") String keywordPattern,
                                                   Pageable pageable);

    Optional<TutorVerificationHistory> findFirstByTutor_TutorIdAndActionInOrderByCreatedAtDesc(
            UUID tutorId, Collection<TutorVerifAction> actions);
}
