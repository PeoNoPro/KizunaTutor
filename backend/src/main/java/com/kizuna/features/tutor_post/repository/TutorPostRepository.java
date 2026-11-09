package com.kizuna.features.tutor_post.repository;

import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TutorPostRepository extends JpaRepository<TutorPost, UUID> {

    /** Lần gửi gần nhất của gia sư (quyết định trạng thái hiển thị cho gia sư). */
    Optional<TutorPost> findFirstByTutorTutorIdOrderByCreatedAtDesc(UUID tutorId);

    /** Bản đang hiển thị cho học viên: dòng PUBLISHED được duyệt gần nhất. */
    Optional<TutorPost> findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(UUID tutorId, TutorPostStatus status);

    Optional<TutorPost> findFirstByTutorTutorIdAndStatusOrderByCreatedAtDesc(UUID tutorId, TutorPostStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TutorPost p WHERE p.postId = :postId")
    Optional<TutorPost> findLockedById(@Param("postId") UUID postId);

    /** Danh sách cho cán bộ học vụ; keywordPattern rỗng = không lọc. */
    @Query(value = "SELECT p FROM TutorPost p JOIN FETCH p.tutor t JOIN FETCH t.user u "
            + "WHERE p.status IN :statuses "
            + "AND (:keywordPattern = '' OR LOWER(u.fullName) LIKE :keywordPattern OR LOWER(u.email) LIKE :keywordPattern "
            + "     OR LOWER(p.title) LIKE :keywordPattern) "
            + "ORDER BY p.updatedAt ASC",
            countQuery = "SELECT COUNT(p) FROM TutorPost p JOIN p.tutor t JOIN t.user u "
            + "WHERE p.status IN :statuses "
            + "AND (:keywordPattern = '' OR LOWER(u.fullName) LIKE :keywordPattern OR LOWER(u.email) LIKE :keywordPattern "
            + "     OR LOWER(p.title) LIKE :keywordPattern)")
    Page<TutorPost> search(@Param("statuses") Collection<TutorPostStatus> statuses,
                           @Param("keywordPattern") String keywordPattern,
                           Pageable pageable);

    long countByStatus(TutorPostStatus status);
}
