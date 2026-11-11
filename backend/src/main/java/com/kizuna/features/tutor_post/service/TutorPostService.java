package com.kizuna.features.tutor_post.service;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.tutor_post.dto.TutorPostDtos.*;
import com.kizuna.features.tutor_post.entity.PostPriceItem;
import com.kizuna.features.tutor_post.entity.PriceLevel;
import com.kizuna.features.tutor_post.entity.TutorPost;
import com.kizuna.features.tutor_post.entity.TutorPostStatus;
import com.kizuna.features.tutor_post.repository.TutorPostRepository;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Bài đăng gia sư: gia sư có gói hội viên còn hạn gửi bài -> cán bộ học vụ duyệt -> bài hiển thị ở trang Tìm gia sư.
 * Mỗi lần gửi là một dòng tutor_posts; bản hiển thị là dòng PUBLISHED mới nhất; tối đa một dòng PENDING_REVIEW mỗi gia sư.
 */
@Service
@RequiredArgsConstructor
public class TutorPostService {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final String MSG_SUBMITTED = "Đã gửi bài đăng. Cán bộ học vụ sẽ duyệt trước khi hiển thị cho học viên.";
    public static final String MSG_APPROVED = "Đã duyệt bài đăng. Bài đã hiển thị trên trang Tìm gia sư.";
    public static final String MSG_REJECTED = "Đã từ chối bài đăng và gửi lý do cho gia sư.";
    public static final String MSG_NEED_MEMBERSHIP = "Bạn cần có gói hội viên còn hạn để đăng bài.";
    public static final String MSG_NEED_APPROVED = "Hồ sơ gia sư của bạn cần được phê duyệt trước khi đăng bài.";
    public static final String MSG_ALREADY_REVIEWED = "Bài đăng này không còn ở trạng thái chờ duyệt.";
    public static final String MSG_INFO_REQUESTED = "Đã gửi yêu cầu bổ sung cho gia sư.";
    public static final String MSG_NOT_EDITABLE = "Bài đăng đang chờ duyệt hoặc đã bị từ chối nên chưa thể cập nhật.";
    public static final String MSG_UPDATE_SUBMITTED =
            "Đã gửi bản cập nhật. Học viên vẫn thấy bài đang hiển thị cho tới khi cán bộ học vụ duyệt bản mới.";

    private static final Set<String> FORMATS = Set.of("ONLINE", "OFFLINE", "BOTH");
    private static final BigDecimal MIN_RATE = new BigDecimal("10000");
    private static final BigDecimal MAX_RATE = new BigDecimal("10000000");
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final TutorPostRepository postRepository;
    // Luật 3 (AGENTS.md): gói hội viên chỉ hỏi qua Service công khai của feature payment; hồ sơ gia sư qua core.
    private final TutorRepository tutorRepository;
    private final TutorMembershipService membershipService;
    private final UserRepository userRepository;

    // =====================================================================
    // Gia sư
    // =====================================================================

    @Transactional(readOnly = true)
    public MyPostView getMine(User user) {
        Tutor tutor = requireTutor(user);
        String blocked = blockedReason(tutor);
        TutorPost latest = postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId()).orElse(null);
        boolean editable = blocked == null && isEditable(latest, tutor.getTutorId());
        return new MyPostView(blocked == null, blocked, editable, membershipExpiresOn(tutor.getTutorId()),
                postView(tutor.getTutorId()), PRICE_LEVEL_OPTIONS);
    }

    /** Danh sách môn học / cấp độ cho ô chọn trên giao diện (theo thứ tự khai báo của PriceLevel). */
    public static final List<PriceLevelOption> PRICE_LEVEL_OPTIONS = java.util.Arrays.stream(PriceLevel.values())
            .map(l -> new PriceLevelOption(l.name(), l.label()))
            .toList();

    /**
     * Gia sư gửi được khi: chưa có bài; được yêu cầu bổ sung; bài đã duyệt (gửi bản cập nhật);
     * hoặc bản cập nhật bị từ chối nhưng vẫn còn bản đã duyệt đang hiển thị.
     * Bài đang chờ duyệt, hoặc bài đầu tiên bị từ chối (quyết định cuối cùng) thì không gửi được.
     */
    private boolean isEditable(TutorPost latest, UUID tutorId) {
        if (latest == null) {
            return true;
        }
        return switch (latest.getStatus()) {
            case NEEDS_REVISION, PUBLISHED -> true;
            case REJECTED -> publishedPost(tutorId).isPresent();
            case PENDING_REVIEW -> false;
        };
    }

    /** Lời nhắn sau khi gửi: đã có bản đang hiển thị thì đây là bản cập nhật. */
    public static String submittedMessage(PostView post) {
        return post != null && post.published() != null ? MSG_UPDATE_SUBMITTED : MSG_SUBMITTED;
    }

    @Transactional
    public PostView submit(User user, SubmitPostRequest request) {
        Tutor tutor = requireTutor(user);
        String blocked = blockedReason(tutor);
        if (blocked != null) {
            throw new ConflictException(blocked);
        }
        TutorPost latest = postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutor.getTutorId()).orElse(null);
        if (!isEditable(latest, tutor.getTutorId())) {
            throw new ConflictException(MSG_NOT_EDITABLE);
        }
        SubmitPostRequest r = validate(request);
        List<PostPriceItem> prices = r.prices().stream()
                .map(p -> new PostPriceItem(p.level(), p.hourlyRate()))
                .toList();

        // Bổ sung theo yêu cầu thì sửa đúng bài đó; chưa có bài hoặc cập nhật bài đã duyệt thì tạo bản mới.
        // Bản đã duyệt giữ nguyên PUBLISHED nên học viên vẫn thấy cho tới khi bản mới được duyệt.
        boolean reuse = latest != null && latest.getStatus() == TutorPostStatus.NEEDS_REVISION;
        TutorPost post = reuse ? latest : TutorPost.builder().tutor(tutor).build();
        post.setStatus(TutorPostStatus.PENDING_REVIEW);
        post.setRejectionReason(null);
        post.setReviewedBy(null);
        post.setReviewedAt(null);
        post.setTitle(r.title());
        post.setContent(r.description());
        post.setSpecialties(r.specialties());
        post.setTeachingFormat(r.teachingFormat());
        post.setPriceItems(prices);
        // Giá thấp nhất: trang Tìm gia sư lọc / sắp xếp và hiển thị "Từ ..." theo cột hourly_rate
        post.setHourlyRate(prices.stream().map(PostPriceItem::hourlyRate).min(BigDecimal::compareTo).orElseThrow());
        post.setUpdatedAt(OffsetDateTime.now(ZONE));
        postRepository.saveAndFlush(post);
        return postView(tutor.getTutorId());
    }

    // =====================================================================
    // Cán bộ học vụ
    // =====================================================================

    @Transactional(readOnly = true)
    public ReviewSummaryView summary() {
        return new ReviewSummaryView(
                postRepository.countByStatus(TutorPostStatus.PENDING_REVIEW),
                postRepository.countByStatus(TutorPostStatus.NEEDS_REVISION),
                postRepository.countByStatus(TutorPostStatus.PUBLISHED),
                postRepository.countByStatus(TutorPostStatus.REJECTED));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewItemView> list(String status, String keyword, Integer page, Integer size) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.length() > 100) {
            throw new BadRequestException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        String pattern = kw.isEmpty() ? "" : "%" + kw.toLowerCase(Locale.ROOT) + "%";
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        LocalDate today = LocalDate.now(ZONE);
        return PageResponse.of(postRepository.search(parseStatuses(status), pattern, PageRequest.of(p, s)),
                post -> toReviewItem(post, today));
    }

    @Transactional
    public ReviewItemView approve(UUID postId, User officer) {
        TutorPost post = lockPending(postId);
        markReviewed(post, officer, TutorPostStatus.PUBLISHED, null);

        // Đồng bộ các cột trang Tìm gia sư dùng để lọc theo từ khóa / học phí
        Tutor tutor = post.getTutor();
        tutor.setHourlyRate(post.getHourlyRate());
        if (post.getSpecialties() != null && !post.getSpecialties().isBlank()) {
            tutor.setHeadline(post.getSpecialties().length() > 200 ? post.getSpecialties().substring(0, 200) : post.getSpecialties());
        }
        tutorRepository.save(tutor);
        return toReviewItem(postRepository.save(post), LocalDate.now(ZONE));
    }

    /** Từ chối: quyết định cuối cùng, gia sư không gửi lại bài này được. */
    @Transactional
    public ReviewItemView reject(UUID postId, User officer, RejectPostRequest request) {
        String reason = requireText(request, "Vui lòng nhập lý do từ chối.", "Lý do từ chối tối đa 2.000 ký tự.");
        TutorPost post = lockPending(postId);
        markReviewed(post, officer, TutorPostStatus.REJECTED, reason);
        return toReviewItem(postRepository.save(post), LocalDate.now(ZONE));
    }

    /** Yêu cầu bổ sung: gia sư sửa bài theo nội dung yêu cầu rồi gửi lại. */
    @Transactional
    public ReviewItemView requestInfo(UUID postId, User officer, RejectPostRequest request) {
        String message = requireText(request, "Vui lòng nêu rõ thông tin gia sư cần bổ sung.", "Nội dung yêu cầu tối đa 2.000 ký tự.");
        TutorPost post = lockPending(postId);
        markReviewed(post, officer, TutorPostStatus.NEEDS_REVISION, message);
        return toReviewItem(postRepository.save(post), LocalDate.now(ZONE));
    }

    private static String requireText(RejectPostRequest request, String emptyMessage, String tooLongMessage) {
        String text = request == null || request.reason() == null ? "" : request.reason().trim();
        if (text.isEmpty()) {
            throw new BadRequestException(emptyMessage);
        }
        if (text.length() > 2000) {
            throw new BadRequestException(tooLongMessage);
        }
        return text;
    }

    // =====================================================================
    // Dùng chung (cả trang Tìm gia sư)
    // =====================================================================

    /** Gói hội viên còn hạn (ACTIVE và chưa hết ngày). */
    public boolean hasActiveMembership(UUID tutorId, LocalDate today) {
        return membershipService.hasActiveMembership(tutorId, today);
    }

    /** Bản bài đăng đang hiển thị cho học viên. */
    public Optional<TutorPost> publishedPost(UUID tutorId) {
        return postRepository.findFirstByTutorTutorIdAndStatusOrderByReviewedAtDesc(tutorId, TutorPostStatus.PUBLISHED);
    }

    public static String formatLabel(String format) {
        if (format == null) {
            return null;
        }
        return switch (format) {
            case "ONLINE" -> "Trực tuyến";
            case "OFFLINE" -> "Trực tiếp";
            case "BOTH" -> "Trực tuyến & trực tiếp";
            default -> format;
        };
    }

    // =====================================================================
    // Nội bộ
    // =====================================================================

    private String blockedReason(Tutor tutor) {
        if (tutor.getVerificationStatus() != TutorVerificationStatus.APPROVED) {
            return MSG_NEED_APPROVED;
        }
        if (!hasActiveMembership(tutor.getTutorId(), LocalDate.now(ZONE))) {
            return MSG_NEED_MEMBERSHIP;
        }
        return null;
    }

    private LocalDate membershipExpiresOn(UUID tutorId) {
        return membershipService.activeMembershipEndDate(tutorId, LocalDate.now(ZONE)).orElse(null);
    }

    private Tutor requireTutor(User user) {
        return tutorRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ gia sư của tài khoản này."));
    }

    private TutorPost lockPending(UUID postId) {
        TutorPost post = postRepository.findLockedById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài đăng."));
        if (post.getStatus() != TutorPostStatus.PENDING_REVIEW) {
            throw new ConflictException(MSG_ALREADY_REVIEWED);
        }
        return post;
    }

    private void markReviewed(TutorPost post, User officer, TutorPostStatus status, String reason) {
        post.setStatus(status);
        post.setRejectionReason(reason);
        post.setReviewedBy(userRepository.getReferenceById(officer.getUserId()));
        post.setReviewedAt(OffsetDateTime.now(ZONE));
    }

    private static SubmitPostRequest validate(SubmitPostRequest r) {
        if (r == null) {
            throw new BadRequestException("Vui lòng nhập nội dung bài đăng.");
        }
        List<String> errors = new ArrayList<>();
        String title = trim(r.title());
        String description = trim(r.description());
        String specialties = trim(r.specialties());
        String format = r.teachingFormat() == null ? "" : r.teachingFormat().trim().toUpperCase(Locale.ROOT);

        if (title.length() < 10 || title.length() > 150) {
            errors.add("Tiêu đề từ 10 đến 150 ký tự");
        }
        if (description.length() < 50 || description.length() > 3000) {
            errors.add("Nội dung giới thiệu từ 50 đến 3.000 ký tự");
        }
        if (specialties.length() > 255) {
            errors.add("Chuyên môn tối đa 255 ký tự");
        }
        if (!FORMATS.contains(format)) {
            errors.add("Vui lòng chọn hình thức dạy");
        }
        List<PriceItemRequest> prices = validatePrices(r.prices(), errors);
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join("; ", errors) + ".");
        }
        return new SubmitPostRequest(title, description, specialties.isEmpty() ? null : specialties, format, prices);
    }

    /**
     * Bảng học phí: ít nhất một mục; mỗi môn học / cấp độ thuộc danh sách cố định và chỉ xuất hiện một lần;
     * mỗi mức giá từ 10.000 đ đến 10.000.000 đ. Trả về danh sách đã chuẩn hóa, sắp theo thứ tự PriceLevel.
     */
    private static List<PriceItemRequest> validatePrices(List<PriceItemRequest> input, List<String> errors) {
        if (input == null || input.isEmpty()) {
            errors.add("Vui lòng thêm ít nhất một mức học phí theo môn học hoặc cấp độ");
            return List.of();
        }
        java.util.EnumMap<PriceLevel, BigDecimal> byLevel = new java.util.EnumMap<>(PriceLevel.class);
        boolean badLevel = false;
        boolean duplicate = false;
        boolean badRate = false;
        for (PriceItemRequest item : input) {
            PriceLevel level = item == null ? null : PriceLevel.fromCode(item.level());
            if (level == null) {
                badLevel = true;
                continue;
            }
            if (byLevel.containsKey(level)) {
                duplicate = true;
                continue;
            }
            BigDecimal rate = item.hourlyRate();
            if (rate == null || rate.compareTo(MIN_RATE) < 0 || rate.compareTo(MAX_RATE) > 0) {
                badRate = true;
            }
            byLevel.put(level, rate);
        }
        if (badLevel) {
            errors.add("Vui lòng chọn môn học hoặc cấp độ cho từng mức học phí");
        }
        if (duplicate) {
            errors.add("Mỗi môn học hoặc cấp độ chỉ được đặt một mức học phí");
        }
        if (badRate) {
            errors.add("Học phí mỗi giờ từ 10.000 đ đến 10.000.000 đ");
        }
        return byLevel.entrySet().stream()
                .map(e -> new PriceItemRequest(e.getKey().name(), e.getValue()))
                .toList();
    }

    /** Bảng giá để hiển thị (kèm nhãn tiếng Việt); bài cũ chưa có bảng giá trả về danh sách rỗng. */
    public static List<PriceItemView> priceViews(TutorPost p) {
        if (p == null || p.getPriceItems() == null) {
            return List.of();
        }
        return p.getPriceItems().stream()
                .map(i -> {
                    PriceLevel level = PriceLevel.fromCode(i.level());
                    return new PriceItemView(i.level(), level != null ? level.label() : i.level(), i.hourlyRate());
                })
                .toList();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static Set<TutorPostStatus> parseStatuses(String status) {
        if (status == null || status.isBlank()) {
            return EnumSet.of(TutorPostStatus.PENDING_REVIEW);
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ALL" -> EnumSet.allOf(TutorPostStatus.class);
            case "PENDING", "PENDING_REVIEW" -> EnumSet.of(TutorPostStatus.PENDING_REVIEW);
            case "NEEDS_INFO", "NEEDS_REVISION" -> EnumSet.of(TutorPostStatus.NEEDS_REVISION);
            case "APPROVED", "PUBLISHED" -> EnumSet.of(TutorPostStatus.PUBLISHED);
            case "REJECTED" -> EnumSet.of(TutorPostStatus.REJECTED);
            default -> throw new BadRequestException("Trạng thái lọc không hợp lệ.");
        };
    }

    private static PostContent contentOf(TutorPost p) {
        if (p == null) {
            return null;
        }
        return new PostContent(p.getTitle(), p.getContent(), p.getSpecialties(),
                p.getTeachingFormat(), formatLabel(p.getTeachingFormat()), p.getHourlyRate(), priceViews(p));
    }

    /** Trạng thái theo lần gửi gần nhất + bản đang hiển thị; null nếu chưa từng gửi. */
    private PostView postView(UUID tutorId) {
        TutorPost latest = postRepository.findFirstByTutorTutorIdOrderByCreatedAtDesc(tutorId).orElse(null);
        if (latest == null) {
            return null;
        }
        TutorPost published = publishedPost(tutorId).orElse(null);
        return new PostView(latest.getPostId(), latest.getStatus().code(), latest.getStatus().label(),
                latest.getRejectionReason(), latest.getUpdatedAt(), latest.getReviewedAt(),
                published != null ? published.getReviewedAt() : null, contentOf(latest), contentOf(published));
    }

    private ReviewItemView toReviewItem(TutorPost p, LocalDate today) {
        Tutor t = p.getTutor();
        // Với bài chờ duyệt, kèm bản đang hiển thị để cán bộ học vụ so sánh
        TutorPost published = p.getStatus() == TutorPostStatus.PENDING_REVIEW ? publishedPost(t.getTutorId()).orElse(null) : null;
        return new ReviewItemView(p.getPostId(), t.getTutorId(), t.getUser().getFullName(), t.getUser().getEmail(),
                t.getHighestJlptLevel(), p.getStatus().code(), p.getStatus().label(), p.getRejectionReason(),
                p.getUpdatedAt(), p.getReviewedAt(), p.getReviewedBy() != null ? p.getReviewedBy().getFullName() : null,
                hasActiveMembership(t.getTutorId(), today), contentOf(p), contentOf(published));
    }
}
