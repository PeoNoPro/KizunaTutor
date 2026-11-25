package com.kizuna.features.tutor_search.repository;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.features.tutor_search.dto.TutorSearchCriteriaDTO;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.service.TutorPostService;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Xây dựng Specification truy vấn lọc động danh sách gia sư theo JPA Criteria.
 * Tuân thủ quy tắc nghiệp vụ GB-06 (Chỉ hiển thị gia sư đã được phê duyệt hồ
 * sơ).
 */
public class TutorSearchSpecification {

    public static Specification<Tutor> build(TutorSearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Quy tắc cốt lõi GB-06: Hồ sơ phải được Ban học vụ phê duyệt (APPROVED)
            predicates.add(cb.equal(root.get("verificationStatus"), TutorVerificationStatus.APPROVED));

            // 1b. Chỉ hiển thị gia sư có bài đăng đã được cán bộ học vụ duyệt và gói hội viên còn hạn
            predicates.add(hasPublishedPostAndActiveMembership(root, query, cb));

            // 2. Tài khoản User liên kết phải đang hoạt động (ACTIVE) và chưa bị xóa mềm
            Join<Tutor, User> userJoin = root.join("user");
            predicates.add(cb.equal(userJoin.get("accountStatus"), UserAccountStatus.ACTIVE));
            predicates.add(cb.isNull(userJoin.get("deletedAt")));

            // 3. Biểu thức địa chỉ đồng bộ fallback
            Expression<String> addressExpr = cb.coalesce(root.get("address"), "Hà Nội, Việt Nam");

            // 3.1. Từ khóa tìm kiếm (Tên gia sư, Headline, Cấp độ JLPT hoặc Địa chỉ)
            if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                String keywordPattern = "%" + criteria.getKeyword().trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(userJoin.get("fullName")), keywordPattern);
                Predicate headlineMatch = cb.like(cb.lower(root.get("headline")), keywordPattern);
                Predicate jlptMatch = cb.like(cb.lower(root.get("highestJlptLevel")), keywordPattern);
                Predicate addressMatch = cb.like(cb.lower(addressExpr), keywordPattern);
                predicates.add(cb.or(nameMatch, headlineMatch, jlptMatch, addressMatch));
            }

            // 3.2. Lọc theo địa chỉ / khu vực (FE-04)
            if (criteria.getLocation() != null && !criteria.getLocation().trim().isEmpty() && !"all".equalsIgnoreCase(criteria.getLocation())) {
                String locationPattern = "%" + criteria.getLocation().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(addressExpr), locationPattern));
            }

            // 4. Trình độ của gia sư: "từ mức này trở lên" (gia sư N1 dạy được mọi mức), không so khớp đúng.
            //    Giao diện mới lọc trình độ qua "Mục tiêu học" (goal = jlpt_n3...); tham số jlptLevel giữ cho link cũ.
            List<String> minLevels = levelsAtLeast(criteria.getJlptLevel());
            GoalFilter goalFilter = goalFilterOf(criteria.getGoal());
            if (goalFilter != null && goalFilter.tutorLevels() != null) {
                minLevels = minLevels == null ? goalFilter.tutorLevels()
                        : minLevels.stream().filter(goalFilter.tutorLevels()::contains).toList();
            }
            if (minLevels != null) {
                predicates.add(cb.upper(root.get("highestJlptLevel")).in(minLevels.isEmpty() ? List.of("-") : minLevels));
            }

            // 5. Hình thức học, môn học, học phí: lọc theo BÀI ĐĂNG ĐANG HIỂN THỊ (bài PUBLISHED được duyệt gần nhất),
            //    vì học viên nhìn thấy hình thức dạy / chuyên môn / học phí lấy từ bài đăng đó.
            Predicate postFilters = displayedPostMatches(root, query, cb, criteria);
            if (postFilters != null) {
                predicates.add(postFilters);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Bộ lọc "Mục tiêu học".
     *
     * @param priceLevels mức trong bảng học phí của bài đăng
     * (tutor_posts.price_items, enum PriceLevel) cho biết gia sư CÓ DẠY mục
     * tiêu này; khớp một trong các mức là đạt
     * @param keywords dự phòng cho bài đăng cũ CHƯA có bảng học phí: đoán theo
     * chuyên môn, tiêu đề, nội dung
     * @param tutorLevels trình độ gia sư cần có (null = không yêu cầu)
     */
    private record GoalFilter(List<String> priceLevels, List<String> keywords, List<String> tutorLevels) {

    }

    /**
     * Thứ tự từ cao xuống thấp.
     */
    private static final List<String> JLPT_LEVELS = List.of("N1", "N2", "N3", "N4", "N5");

    // Văn bản bài đăng được chuẩn hóa: chữ thường, dấu câu thành khoảng trắng, có khoảng trắng hai đầu
    // → " n3 " khớp "Luyện thi N3," nhưng không khớp nhầm giữa một từ khác.
    private static final List<String> JLPT_KEYWORDS
            = List.of("jlpt", "luyện thi", "luyện đề", "ôn thi", " n1 ", " n2 ", " n3 ", " n4 ", " n5 ");

    private static final List<String> ALL_JLPT_PRICE_LEVELS = List.of("JLPT_N5", "JLPT_N4", "JLPT_N3", "JLPT_N2", "JLPT_N1");

    // Bộ lọc "Tiếng Nhật IT & BrSE" đã bỏ (bảng học phí không có mức IT). Link cũ ?goal=it bị bỏ qua như giá trị lạ.
    private static final Map<String, GoalFilter> GOALS = Map.of(
            "kaiwa", new GoalFilter(List.of("KAIWA"), List.of("kaiwa", "giao tiếp", "hội thoại"), null),
            "business", new GoalFilter(List.of("BUSINESS"), List.of("thương mại", "doanh nghiệp", "business", "công sở"), null),
            "eju", new GoalFilter(List.of("EJU"), List.of("eju"), null),
            // Luyện thi JLPT: bảng học phí có đúng cấp thi; gia sư phải có trình độ cao hơn hoặc bằng mức thi
            "jlpt", new GoalFilter(ALL_JLPT_PRICE_LEVELS, JLPT_KEYWORDS, null),
            "jlpt_n5", new GoalFilter(List.of("JLPT_N5"), JLPT_KEYWORDS, List.of("N1", "N2", "N3")),
            "jlpt_n4", new GoalFilter(List.of("JLPT_N4"), JLPT_KEYWORDS, List.of("N1", "N2", "N3")),
            // Link cũ "Luyện thi N5 – N4" (trước khi tách riêng N5 / N4)
            "jlpt_n5n4", new GoalFilter(List.of("JLPT_N5", "JLPT_N4"), JLPT_KEYWORDS, List.of("N1", "N2", "N3")),
            "jlpt_n3", new GoalFilter(List.of("JLPT_N3"), JLPT_KEYWORDS, List.of("N1", "N2")),
            "jlpt_n2", new GoalFilter(List.of("JLPT_N2"), JLPT_KEYWORDS, List.of("N1", "N2")),
            "jlpt_n1", new GoalFilter(List.of("JLPT_N1"), JLPT_KEYWORDS, List.of("N1")));

    private static GoalFilter goalFilterOf(String goal) {
        String g = normalize(goal);
        return g == null ? null : GOALS.get(g);
    }

    /**
     * "N3" → [N1, N2, N3]; null nếu không lọc hoặc giá trị không hợp lệ.
     */
    static List<String> levelsAtLeast(String level) {
        String l = normalize(level);
        if (l == null) {
            return null;
        }
        int idx = JLPT_LEVELS.indexOf(l.toUpperCase());
        return idx < 0 ? null : JLPT_LEVELS.subList(0, idx + 1);
    }

    /**
     * Gia sư có dạy mục tiêu này: - bài đăng có bảng học phí → bảng có một
     * trong các mức của mục tiêu (bảng học phí là nguồn chính xác); - bài đăng
     * cũ chưa có bảng học phí → đoán theo từ khóa trong chuyên môn, tiêu đề,
     * nội dung.
     */
    private static Predicate teachesGoal(CriteriaBuilder cb, Root<TutorPost> p, GoalFilter goal) {
        // jsonb::text của PostgreSQL có dạng chuẩn [{"level": "KAIWA", "hourlyRate": 200000}, ...]
        Expression<String> priceText = cb.function("text", String.class, p.get("priceItems"));
        Predicate hasPriceTable = cb.and(cb.isNotNull(p.get("priceItems")), cb.notEqual(priceText, "[]"));
        Predicate priceMatch = cb.or(goal.priceLevels().stream()
                .map(level -> cb.like(priceText, "%\"level\": \"" + level + "\"%"))
                .toArray(Predicate[]::new));
        return cb.or(cb.and(hasPriceTable, priceMatch), cb.and(cb.not(hasPriceTable), teachesGoalByKeywords(cb, p, goal)));
    }

    /**
     * Dự phòng cho bài đăng chưa có bảng học phí: bài đăng đang hiển thị chứa
     * một trong các từ khóa.
     */
    private static Predicate teachesGoalByKeywords(CriteriaBuilder cb, Root<TutorPost> p, GoalFilter goal) {
        // " " + chuyên môn + tiêu đề + nội dung + " ", bỏ dấu câu thành khoảng trắng để so khớp được cả từ ngắn như "IT"
        Expression<String> text = cb.concat(cb.concat(cb.concat(cb.concat(cb.concat(
                cb.literal(" "), cb.coalesce(p.get("specialties"), "")), cb.literal(" ")),
                cb.coalesce(p.get("title"), "")), cb.literal(" ")), cb.coalesce(p.get("content"), ""));
        Expression<String> normalizedText = cb.function("translate", String.class,
                cb.lower(cb.concat(text, cb.literal(" "))), cb.literal(",.;:/()-•|"), cb.literal("          "));
        return cb.or(goal.keywords().stream()
                .map(k -> cb.like(normalizedText, "%" + k + "%"))
                .toArray(Predicate[]::new));
    }

    /**
     * Hình thức dạy trong bài đăng phù hợp với từng lựa chọn trên bộ lọc.
     */
    private static final Map<String, List<String>> FORMAT_VALUES = Map.of(
            "online", List.of("ONLINE", "BOTH"),
            "offline", List.of("OFFLINE", "BOTH"),
            "hybrid", List.of("BOTH"));

    /**
     * EXISTS bài đăng đang hiển thị của gia sư (PUBLISHED, không có bài
     * PUBLISHED nào được duyệt muộn hơn) thỏa các bộ lọc hình thức học / môn
     * học / học phí. Trả về null khi không có bộ lọc nào trong nhóm này.
     */
    private static Predicate displayedPostMatches(Root<Tutor> root, CriteriaQuery<?> query, CriteriaBuilder cb,
            TutorSearchCriteriaDTO criteria) {
        String format = normalize(criteria.getFormat());
        List<String> formatValues = format == null ? null : FORMAT_VALUES.get(format);
        GoalFilter goal = goalFilterOf(criteria.getGoal());
        if (formatValues == null && goal == null
                && criteria.getMinPrice() == null && criteria.getMaxPrice() == null) {
            return null;
        }

        Subquery<UUID> post = query.subquery(UUID.class);
        Root<TutorPost> p = post.from(TutorPost.class);

        // Không có bài PUBLISHED nào của cùng gia sư được duyệt muộn hơn → đây là bài đang hiển thị
        Subquery<UUID> newer = query.subquery(UUID.class);
        Root<TutorPost> p2 = newer.from(TutorPost.class);
        newer.select(p2.get("postId")).where(
                cb.equal(p2.get("tutor"), p.get("tutor")),
                cb.equal(p2.get("status"), TutorPostStatus.PUBLISHED),
                cb.greaterThan(p2.<OffsetDateTime>get("reviewedAt"), p.<OffsetDateTime>get("reviewedAt")));

        List<Predicate> conditions = new ArrayList<>();
        conditions.add(cb.equal(p.get("tutor"), root));
        conditions.add(cb.equal(p.get("status"), TutorPostStatus.PUBLISHED));
        conditions.add(cb.not(cb.exists(newer)));

        if (formatValues != null) {
            conditions.add(cb.upper(p.get("teachingFormat")).in(formatValues));
        }

        if (goal != null) {
            conditions.add(teachesGoal(cb, p, goal));
        }

        // Học phí hiển thị = học phí của bài đăng (nếu bài đăng không ghi thì dùng học phí trong hồ sơ)
        Expression<BigDecimal> price = cb.coalesce(p.<BigDecimal>get("hourlyRate"), root.<BigDecimal>get("hourlyRate"));
        if (criteria.getMinPrice() != null) {
            conditions.add(cb.greaterThanOrEqualTo(price, criteria.getMinPrice()));
        }
        if (criteria.getMaxPrice() != null) {
            conditions.add(cb.lessThanOrEqualTo(price, criteria.getMaxPrice()));
        }

        post.select(p.get("postId")).where(conditions.toArray(new Predicate[0]));
        return cb.exists(post);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim().toLowerCase();
        return v.isEmpty() || "all".equals(v) ? null : v;
    }

    /**
     * EXISTS bài đăng đã duyệt AND EXISTS gói hội viên ACTIVE chưa hết hạn.
     */
    private static Predicate hasPublishedPostAndActiveMembership(Root<Tutor> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<UUID> post = query.subquery(UUID.class);
        Root<TutorPost> p = post.from(TutorPost.class);
        post.select(p.get("postId")).where(
                cb.equal(p.get("tutor"), root),
                cb.equal(p.get("status"), TutorPostStatus.PUBLISHED));

        Subquery<UUID> membership = query.subquery(UUID.class);
        Root<TutorMembership> m = membership.from(TutorMembership.class);
        membership.select(m.get("membershipId")).where(
                cb.equal(m.get("tutor"), root),
                cb.equal(m.get("status"), TutorMembershipState.ACTIVE),
                cb.greaterThanOrEqualTo(m.<LocalDate>get("endDate"), LocalDate.now(TutorPostService.ZONE)));

        return cb.and(cb.exists(post), cb.exists(membership));
    }

}
