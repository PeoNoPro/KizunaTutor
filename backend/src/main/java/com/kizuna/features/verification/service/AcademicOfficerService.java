package com.kizuna.features.verification.service;

import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.verification.dto.request.UpdateOfficerProfileRequest;
import com.kizuna.features.verification.dto.response.OfficerProfileResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.ReviewRecordResponse;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationHistory;
import com.kizuna.features.verification.repository.TutorVerificationHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Trang "Lịch sử xét duyệt" và "Hồ sơ cá nhân" của cán bộ học vụ.
 */
@Service
@RequiredArgsConstructor
public class AcademicOfficerService {

    public static final String MSG_PROFILE_UPDATED = "Đã cập nhật hồ sơ cá nhân.";

    private static final Set<TutorVerifAction> DECISIONS =
            EnumSet.of(TutorVerifAction.APPROVE, TutorVerifAction.REJECT, TutorVerifAction.REQUEST_INFO);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_KEYWORD = 100;

    private final TutorVerificationHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public PageResponse<ReviewRecordResponse> getReviewHistory(String action, String keyword, Integer page, Integer size) {
        UUID me = currentUserProvider.requireCurrentUser().getUserId();
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.length() > MAX_KEYWORD) {
            throw VerificationException.validation(null, Map.of("keyword", "Từ khóa tìm kiếm tối đa 100 ký tự."));
        }
        String pattern = kw.isEmpty() ? "" : "%" + kw.toLowerCase(Locale.ROOT) + "%";
        int p = page == null || page < 0 ? 0 : page;
        int s = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);

        return PageResponse.of(
                historyRepository.searchDecisions(parseActions(action), pattern, PageRequest.of(p, s)),
                h -> toRecord(h, me));
    }

    @Transactional(readOnly = true)
    public OfficerProfileResponse getProfile() {
        User user = loadCurrentUser();
        return toProfile(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(UpdateOfficerProfileRequest request) {
        User user = loadCurrentUser();
        String fullName = request == null || request.getFullName() == null ? "" : request.getFullName().trim();
        String phone = request == null || request.getPhone() == null ? "" : request.getPhone().trim();

        Map<String, String> errors = new LinkedHashMap<>();
        if (fullName.isEmpty()) {
            errors.put("fullName", "Vui lòng nhập họ và tên.");
        } else if (fullName.length() > 100) {
            errors.put("fullName", "Họ và tên tối đa 100 ký tự.");
        }
        if (!phone.isEmpty() && !phone.matches("^(\\+84|0)\\d{9,10}$")) {
            errors.put("phone", "Số điện thoại không hợp lệ (ví dụ 0912345678).");
        }
        if (!errors.isEmpty()) {
            throw VerificationException.validation(null, errors);
        }

        user.setFullName(fullName);
        user.setPhone(phone.isEmpty() ? null : phone);
        userRepository.save(user);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", MSG_PROFILE_UPDATED);
        result.put("profile", toProfile(user));
        return result;
    }

    private User loadCurrentUser() {
        UUID id = currentUserProvider.requireCurrentUser().getUserId();
        return userRepository.findById(id).orElseThrow(VerificationException::unauthenticated);
    }

    private OfficerProfileResponse toProfile(User user) {
        return OfficerProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .roleLabel("Cán bộ học vụ")
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private ReviewRecordResponse toRecord(TutorVerificationHistory h, UUID me) {
        User tutorUser = h.getTutor().getUser();
        User officer = h.getOfficer();
        return ReviewRecordResponse.builder()
                .historyId(h.getHistoryId())
                .tutorId(h.getTutor().getTutorId())
                .tutorName(tutorUser.getFullName())
                .tutorEmail(tutorUser.getEmail())
                .tutorJlptLevel(h.getTutor().getHighestJlptLevel())
                .action(h.getAction().name())
                .actionLabel(VerificationLabels.historyAction(h))
                .feedback(h.getFeedback())
                .reasonLabel(VerificationLabels.rejectionReason(h.getReasonCode()))
                .internalNote(h.getInternalNote())
                .officerName(officer != null ? officer.getFullName() : null)
                .mine(officer != null && me.equals(officer.getUserId()))
                .createdAt(h.getCreatedAt())
                .build();
    }

    private static Set<TutorVerifAction> parseActions(String action) {
        if (action == null || action.isBlank() || "ALL".equalsIgnoreCase(action.trim())) {
            return DECISIONS;
        }
        try {
            TutorVerifAction parsed = TutorVerifAction.valueOf(action.trim().toUpperCase(Locale.ROOT));
            if (!DECISIONS.contains(parsed)) {
                throw new IllegalArgumentException();
            }
            return EnumSet.of(parsed);
        } catch (IllegalArgumentException e) {
            throw VerificationException.validation(null, Map.of("action", "Loại quyết định không hợp lệ."));
        }
    }
}
