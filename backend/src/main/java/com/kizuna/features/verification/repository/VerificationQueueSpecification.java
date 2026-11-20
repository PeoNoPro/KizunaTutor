package com.kizuna.features.verification.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Điều kiện lọc và sắp xếp hàng đợi hồ sơ xác minh (UC-25, FR-011, FR-012).
 * Sắp xếp được đặt trong Specification để đẩy hồ sơ chưa có ngày nộp xuống cuối ở cả hai chiều.
 */
public final class VerificationQueueSpecification {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private VerificationQueueSpecification() {
    }

    public static Specification<Tutor> of(Collection<TutorVerificationStatus> statuses, String keyword, String jlptLevel,
                                          LocalDate submittedFrom, LocalDate submittedTo, boolean newestFirst) {
        return of(statuses, null, keyword, jlptLevel, submittedFrom, submittedTo, newestFirst);
    }

    /**
     * Như trên; updateMarkerPattern khác null thì hàng đợi lấy thêm gia sư đã phê duyệt đang có yêu cầu cập nhật hồ sơ chờ duyệt
     * (dòng lịch sử mới nhất là SUBMIT có notes khớp mẫu, cùng điều kiện với TutorVerificationHistoryRepository.PENDING_UPDATE).
     * Với các dòng đó, ngày dùng để lọc và sắp xếp là ngày gửi yêu cầu cập nhật.
     */
    @SuppressWarnings("unchecked")
    public static Specification<Tutor> of(Collection<TutorVerificationStatus> statuses, String updateMarkerPattern,
                                          String keyword, String jlptLevel,
                                          LocalDate submittedFrom, LocalDate submittedTo, boolean newestFirst) {
        return (root, query, cb) -> {
            boolean countQuery = Long.class == query.getResultType() || long.class == query.getResultType();
            Join<Tutor, User> user = countQuery
                    ? root.join("user", JoinType.INNER)
                    : (Join<Tutor, User>) root.<Tutor, User>fetch("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            Expression<OffsetDateTime> submittedAt = root.get("submittedAt");
            Predicate statusMatch = root.get("verificationStatus").in(statuses);
            if (updateMarkerPattern != null) {
                Subquery<Integer> update = query.subquery(Integer.class);
                Root<TutorVerificationHistory> h = update.from(TutorVerificationHistory.class);
                update.select(cb.literal(1)).where(
                        cb.equal(h.get("tutor"), root),
                        cb.equal(h.get("action"), TutorVerifAction.SUBMIT),
                        cb.like(h.get("internalNote"), updateMarkerPattern),
                        cb.equal(h.get("createdAt"), latestHistoryAt(root, query, cb)));
                Predicate approved = cb.equal(root.get("verificationStatus"), TutorVerificationStatus.APPROVED);
                statusMatch = cb.or(statusMatch, cb.and(approved, cb.exists(update)));
                submittedAt = cb.<OffsetDateTime>selectCase()
                        .when(approved, latestHistoryAt(root, query, cb))
                        .otherwise(root.<OffsetDateTime>get("submittedAt"));
            }
            predicates.add(statusMatch);

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(user.get("email")), pattern, '\\')));
            }
            if (jlptLevel != null && !jlptLevel.isBlank()) {
                predicates.add(cb.equal(root.get("highestJlptLevel"), jlptLevel.trim().toUpperCase()));
            }
            if (submittedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(submittedAt, submittedFrom.atStartOfDay(ZONE).toOffsetDateTime()));
            }
            if (submittedTo != null) {
                predicates.add(cb.lessThan(submittedAt, submittedTo.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime()));
            }

            if (!countQuery) {
                Expression<Integer> nullsLast = cb.<Integer>selectCase().when(cb.isNull(submittedAt), 1).otherwise(0);
                query.orderBy(cb.asc(nullsLast), newestFirst ? cb.desc(submittedAt) : cb.asc(submittedAt));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Thời điểm dòng lịch sử mới nhất của gia sư (subquery tương quan). */
    private static Subquery<OffsetDateTime> latestHistoryAt(Root<Tutor> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<OffsetDateTime> latest = query.subquery(OffsetDateTime.class);
        Root<TutorVerificationHistory> last = latest.from(TutorVerificationHistory.class);
        latest.select(cb.greatest(last.<OffsetDateTime>get("createdAt"))).where(cb.equal(last.get("tutor"), root));
        return latest;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
