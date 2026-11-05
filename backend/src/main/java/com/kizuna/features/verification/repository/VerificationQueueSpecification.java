package com.kizuna.features.verification.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
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

    @SuppressWarnings("unchecked")
    public static Specification<Tutor> of(Collection<TutorVerificationStatus> statuses, String keyword, String jlptLevel,
                                          LocalDate submittedFrom, LocalDate submittedTo, boolean newestFirst) {
        return (root, query, cb) -> {
            boolean countQuery = Long.class == query.getResultType() || long.class == query.getResultType();
            Join<Tutor, User> user = countQuery
                    ? root.join("user", JoinType.INNER)
                    : (Join<Tutor, User>) root.<Tutor, User>fetch("user", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("verificationStatus").in(statuses));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("fullName")), pattern, '\\'),
                        cb.like(cb.lower(user.get("email")), pattern, '\\')));
            }
            if (jlptLevel != null && !jlptLevel.isBlank()) {
                predicates.add(cb.equal(root.get("highestJlptLevel"), jlptLevel.trim().toUpperCase()));
            }
            Expression<OffsetDateTime> submittedAt = root.get("submittedAt");
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

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
