package com.kizuna.features.user_management.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.dto.PageResponse;
import com.kizuna.core.entity.TutorState;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse.LearnerProfile;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse.StatusChange;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse.StatusOption;
import com.kizuna.features.user_management.dto.ManagedUserDetailResponse.TutorProfile;
import com.kizuna.features.user_management.dto.ManagedUserResponse;
import com.kizuna.features.user_management.dto.UpdateUserStatusRequest;
import com.kizuna.features.user_management.repository.ManagedUserRepository;
import com.kizuna.features.user_management.repository.ManagedUserRepository.Account;
import com.kizuna.features.user_management.service.ManagedAccountStatus;
import com.kizuna.features.user_management.service.UserManagementService;
import com.kizuna.features.user_management.service.UserStatusMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.kizuna.features.user_management.service.ManagedAccountStatus.label;

/**
 * FE-14 (SRS UC-20, UC-21, UC-12). Chặn truy cập tức thời không cần thêm gì: JwtAuthenticationFilter đọc lại
 * trạng thái tài khoản ở mỗi request và đăng nhập từ chối tài khoản không ACTIVE (research §1).
 */
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    static final int KEYWORD_MAX = 100;
    static final int DEFAULT_SIZE = 20;
    static final int HISTORY_LIMIT = 20;

    static final String NOT_FOUND = "Không tìm thấy tài khoản gia sư hoặc học viên này.";
    static final String PENDING_BLOCKED = "Tài khoản chưa hoàn tất đăng ký nên chưa thể đổi trạng thái.";
    static final String CHANGED_ELSEWHERE = "Trạng thái tài khoản vừa được thay đổi. Vui lòng tải lại trước khi cập nhật.";

    private final ManagedUserRepository repository;
    private final UserStatusMailService mail;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManagedUserResponse> list(String category, String keyword, String status, int page, int size) {
        String cat = normalizeCategory(category);
        String st = normalizeStatusFilter(status);
        int safePage = Math.max(page, 0);
        int safeSize = ManagedAccountStatus.PAGE_SIZES.contains(size) ? size : DEFAULT_SIZE;
        var result = repository.list(ManagedAccountStatus.CATEGORY_ROLES.get(cat), st, likePattern(keyword), safePage, safeSize);
        List<ManagedUserResponse> items = result.items().stream()
                .map(row -> new ManagedUserResponse(row.userId(), row.fullName(), row.email(), cat,
                        ManagedAccountStatus.CATEGORY_LABELS.get(cat), row.status(), label(row.status()), row.createdAt()))
                .toList();
        long totalPages = result.total() == 0 ? 0 : (result.total() + safeSize - 1) / safeSize;
        return new PageResponse<>(items, safePage, safeSize, result.total(), totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public ManagedUserDetailResponse detail(UUID userId) {
        Account account = requireManagedAccount(userId);
        String category = ManagedAccountStatus.categoryOfRole(account.roleName());

        TutorProfile tutor = null;
        LearnerProfile learner = null;
        String phone = account.phone();
        if ("TUTOR".equals(category)) {
            var row = repository.tutorProfile(userId).orElse(null);
            if (row != null) {
                tutor = new TutorProfile(row.tutorCode(), row.headline(), row.experienceYears(), row.highestJlptLevel(),
                        row.avgRating(), row.totalReviews(), tutorStateLabel(row.tutorState()), row.dateOfBirth(),
                        row.address(), row.verifiedAt());
                if (isBlank(phone)) phone = row.phone();
            }
        } else {
            learner = repository.learnerProfile(userId)
                    .map(row -> new LearnerProfile(row.currentLevel(), row.targetLevel(), row.learningGoals(),
                            row.province(), row.address()))
                    .orElse(null);
        }

        boolean pending = "PENDING".equals(account.status());
        List<StatusOption> allowed = pending ? List.of() : ManagedAccountStatus.TARGET_STATUSES.stream()
                .filter(s -> !s.equals(account.status()))
                .map(s -> new StatusOption(s, label(s)))
                .toList();
        List<StatusChange> history = repository.statusHistory(userId, HISTORY_LIMIT).stream()
                .map(h -> new StatusChange(h.changedAt(), h.actorName() == null ? "Tài khoản đã bị xóa" : h.actorName(),
                        label(h.fromStatus()), label(h.toStatus()), h.reason(), h.messageToUser()))
                .toList();

        return new ManagedUserDetailResponse(account.userId(), account.fullName(), account.email(), phone,
                account.avatarUrl(), category, ManagedAccountStatus.CATEGORY_LABELS.get(category), account.status(),
                label(account.status()), account.createdAt(), account.updatedAt(), allowed,
                pending ? PENDING_BLOCKED : null, tutor, learner, history);
    }

    @Override
    @Transactional
    public ManagedUserDetailResponse updateStatus(UUID userId, UpdateUserStatusRequest request, UUID managerId) {
        String target = upper(request.status());
        if (!ManagedAccountStatus.TARGET_STATUSES.contains(target)) {
            throw new BusinessException(400, "Trạng thái mới không hợp lệ.");
        }
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (reason.isEmpty()) {
            throw new BusinessException(400, "Vui lòng nhập lý do nội bộ.");
        }
        String message = isBlank(request.message()) ? null : request.message().strip();

        Account account = requireManagedAccount(userId);
        if ("PENDING".equals(account.status())) {
            throw new BusinessException(400, PENDING_BLOCKED);
        }
        if (!account.status().equals(upper(request.expectedStatus()))) {
            throw new BusinessException(409, CHANGED_ELSEWHERE);
        }
        if (account.status().equals(target)) {
            throw new BusinessException(400, "Tài khoản đã ở trạng thái này.");
        }
        if (repository.updateStatus(userId, account.status(), target) != 1) {
            throw new BusinessException(409, CHANGED_ELSEWHERE);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fromStatus", account.status());
        details.put("toStatus", target);
        details.put("reason", reason);
        details.put("messageToUser", message);
        repository.insertAudit(managerId, userId, toJson(details));

        mail.statusChangedAfterCommit(account.email(), account.fullName(), target, message);
        return detail(userId);
    }

    private Account requireManagedAccount(UUID userId) {
        return repository.findAccount(userId)
                .filter(a -> ManagedAccountStatus.categoryOfRole(a.roleName()) != null)
                .orElseThrow(() -> new BusinessException(404, NOT_FOUND));
    }

    static String normalizeCategory(String category) {
        String value = isBlank(category) ? "TUTOR" : upper(category);
        if (!ManagedAccountStatus.CATEGORY_ROLES.containsKey(value)) {
            throw new BusinessException(400, "Nhóm người dùng không hợp lệ.");
        }
        return value;
    }

    static String normalizeStatusFilter(String status) {
        if (isBlank(status)) return null;
        String value = upper(status);
        if (!ManagedAccountStatus.STATUS_LABELS.containsKey(value)) {
            throw new BusinessException(400, "Trạng thái lọc không hợp lệ.");
        }
        return value;
    }

    /** Từ khóa → mẫu ILIKE: tìm đúng ký tự %, _ và \ người dùng gõ. Trả null khi không có từ khóa. */
    static String likePattern(String keyword) {
        if (isBlank(keyword)) return null;
        String value = keyword.strip();
        if (value.length() > KEYWORD_MAX) value = value.substring(0, KEYWORD_MAX);
        return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    static String tutorStateLabel(String state) {
        if (isBlank(state)) return null;
        try {
            return TutorState.valueOf(state).getDisplayName();
        } catch (IllegalArgumentException e) {
            return "Cần kiểm tra";
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được nhật ký đổi trạng thái", e);
        }
    }

    private static String upper(String value) {
        return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
