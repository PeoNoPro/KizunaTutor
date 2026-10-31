package com.kizuna.features.tutor_search.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Xây dựng Specification truy vấn lọc động danh sách gia sư theo JPA Criteria.
 * Tuân thủ quy tắc nghiệp vụ GB-06 (Chỉ hiển thị gia sư đã được phê duyệt hồ sơ).
 */
public class TutorSearchSpecification {

    public static Specification<Tutor> build(TutorSearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Quy tắc cốt lõi GB-06: Hồ sơ phải được Ban học vụ phê duyệt (APPROVED)
            predicates.add(cb.equal(root.get("verificationStatus"), TutorVerificationStatus.APPROVED));

            // 2. Tài khoản User liên kết phải đang hoạt động (ACTIVE) và chưa bị xóa mềm
            Join<Tutor, User> userJoin = root.join("user");
            predicates.add(cb.equal(userJoin.get("accountStatus"), UserAccountStatus.ACTIVE));
            predicates.add(cb.isNull(userJoin.get("deletedAt")));

            // 3. Từ khóa tìm kiếm (Tên gia sư, Headline hoặc Cấp độ JLPT)
            if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                String keywordPattern = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), keywordPattern);
                Predicate headlineMatch = cb.like(cb.lower(root.get("headline")), keywordPattern);
                Predicate jlptMatch = cb.like(cb.lower(root.get("highestJlptLevel")), keywordPattern);
                predicates.add(cb.or(nameMatch, headlineMatch, jlptMatch));
            }

            // 4. Lọc theo trình độ JLPT
            if (criteria.getJlptLevel() != null && !criteria.getJlptLevel().trim().isEmpty() && !"all".equalsIgnoreCase(criteria.getJlptLevel())) {
                predicates.add(cb.equal(cb.upper(root.get("highestJlptLevel")), criteria.getJlptLevel().trim().toUpperCase()));
            }

            // 5. Lọc theo khoảng học phí
            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("hourlyRate"), criteria.getMinPrice()));
            }
            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("hourlyRate"), criteria.getMaxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

