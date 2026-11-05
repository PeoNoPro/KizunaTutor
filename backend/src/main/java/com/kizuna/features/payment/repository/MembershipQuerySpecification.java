package com.kizuna.features.payment.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Bộ lọc danh sách hội viên (UC-44) và danh sách giao dịch (UC-98).
 */
public final class MembershipQuerySpecification {

    private MembershipQuerySpecification() {
    }

    /** Trạng thái hiển thị của hội viên, tính theo ngày (không ghi hàng loạt khi liệt kê). */
    public enum DisplayStatus { ACTIVE, EXPIRED }

    /**
     * Mỗi gia sư một dòng: bản ghi hội viên có ngày hết hạn xa nhất.
     */
    public static Specification<TutorMembership> latestMembershipPerTutor(String keyword, UUID planId,
                                                                          DisplayStatus status, LocalDate today) {
        return (root, query, cb) -> {
            boolean countQuery = isCountQuery(query.getResultType());
            Join<TutorMembership, Tutor> tutor;
            Join<Tutor, User> user;
            if (countQuery) {
                tutor = root.join("tutor");
                user = tutor.join("user");
            } else {
                tutor = fetchJoin(root, "tutor");
                user = fetchJoin(tutor, "user");
                fetchJoin(root, "plan");
            }

            Subquery<LocalDate> latest = query.subquery(LocalDate.class);
            Root<TutorMembership> other = latest.from(TutorMembership.class);
            latest.select(cb.greatest(other.<LocalDate>get("endDate")))
                    .where(cb.equal(other.get("tutor"), root.get("tutor")));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("endDate"), latest));
            addKeyword(cb, predicates, user, keyword);
            if (planId != null) {
                predicates.add(cb.equal(root.get("plan").get("planId"), planId));
            }
            if (status == DisplayStatus.ACTIVE) {
                predicates.add(cb.equal(root.get("status"), TutorMembershipState.ACTIVE));
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));
            } else if (status == DisplayStatus.EXPIRED) {
                predicates.add(cb.or(
                        cb.lessThan(root.get("endDate"), today),
                        cb.notEqual(root.get("status"), TutorMembershipState.ACTIVE)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Giao dịch gói hội viên cho quản trị viên: trạng thái, khoảng thời gian, từ khóa (mã giao dịch hoặc tên/email).
     */
    public static Specification<PaymentTransaction> membershipTransactions(PaymentTransactionStatus status,
                                                                           OffsetDateTime from, OffsetDateTime toExclusive,
                                                                           String keyword) {
        return (root, query, cb) -> {
            boolean countQuery = isCountQuery(query.getResultType());
            Join<PaymentTransaction, User> user;
            if (countQuery) {
                user = root.join("user");
                root.join("plan");
            } else {
                user = fetchJoin(root, "user");
                root.fetch("plan", JoinType.INNER);
            }
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("plan")));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionTime"), from));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(root.get("transactionTime"), toExclusive));
            }
            String trimmed = keyword == null ? "" : keyword.trim();
            if (!trimmed.isEmpty()) {
                UUID id = parseTransactionKeyword(trimmed);
                if (id != null) {
                    predicates.add(cb.equal(root.get("transactionId"), id));
                } else {
                    addKeyword(cb, predicates, user, trimmed);
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Nhận UUID có gạch nối hoặc mã đơn VNPay 32 ký tự hex; trả null nếu không phải mã giao dịch. */
    public static UUID parseTransactionKeyword(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() == 36) {
                return UUID.fromString(text);
            }
            if (text.length() == 32 && text.matches("[0-9a-fA-F]{32}")) {
                return UUID.fromString(text.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})", "$1-$2-$3-$4-$5"));
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        return null;
    }

    private static void addKeyword(CriteriaBuilder cb, List<Predicate> predicates, Join<?, User> user, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        String pattern = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        Expression<String> name = cb.lower(user.get("fullName"));
        Expression<String> email = cb.lower(user.get("email"));
        predicates.add(cb.or(cb.like(name, pattern, '\\'), cb.like(email, pattern, '\\')));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean isCountQuery(Class<?> resultType) {
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }

    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> fetchJoin(From<?, X> from, String attribute) {
        return (Join<X, Y>) from.<X, Y>fetch(attribute, JoinType.INNER);
    }
}
