package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.admin.repository.ManagedUserRepository;
import com.kizuna.features.admin.repository.ManagedUserRepository.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * FE-11 (2026-11-13): Quản lý người dùng của Quản lý vận hành.
 * <ul>
 *   <li>Xem, tìm kiếm, lọc tài khoản Học viên và Gia sư; xem tóm tắt hoạt động (chỉ số đếm).</li>
 *   <li>Tạm khóa / mở lại tài khoản, bắt buộc lý do, ghi nhật ký. Tài khoản bị khóa bị từ chối ngay ở mọi request
 *       (JwtAuthenticationFilter chỉ nhận tài khoản ACTIVE).</li>
 *   <li>Không sửa hồ sơ, không xóa, không đổi vai trò; tài khoản nhân viên vẫn do Admin quản lý.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ManagedUserService {
    static final Map<String, String> ROLE_LABELS = Map.of("ROLE_LEARNER", "Học viên", "ROLE_TUTOR", "Gia sư");
    static final Map<String, String> STATUS_LABELS = Map.of("ACTIVE", "Đang hoạt động", "SUSPENDED", "Tạm khóa", "PENDING", "Chờ kích hoạt", "BANNED", "Đã bị khóa");
    static final Map<String, String> VERIFICATION_LABELS = Map.of("UNVERIFIED", "Chưa nộp hồ sơ", "PENDING", "Chờ duyệt", "APPROVED", "Đã duyệt",
            "REJECTED", "Đã từ chối", "ADDITIONAL_INFO_REQUIRED", "Cần bổ sung");
    static final Map<String, String> MEMBERSHIP_LABELS = Map.of("FREE", "Miễn phí", "STANDARD", "Tiêu chuẩn", "PREMIUM", "Cao cấp", "EXPIRED", "Hết hạn");
    public static final Map<String, String> ACTION_LABELS = Map.of("SUSPEND", "Tạm khóa tài khoản", "REACTIVATE", "Mở lại tài khoản");
    private static final int MAX_REASON = 500;

    private final ManagedUserRepository users;
    private final OperationsActorResolver actors;
    private final OperationsCommandExecutor locks;

    /** FE-12: báo người dùng khi tài khoản được mở lại. Không bắt buộc để test dựng service bằng tay vẫn chạy. */
    @Autowired(required = false)
    private com.kizuna.features.notification.service.NotificationEvents notificationEvents;

    public record StatusChange(String status, String reason, String expectedVersion) { }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> list(String q, String role, String status, int page, int size) {
        actors.requireManager();
        int offset = OperationsValidation.page(page, size);
        var filter = new Filter(blankToNull(q, 200), role(role), status(status));
        var result = new LinkedHashMap<String, Object>();
        result.put("items", users.list(filter, offset, size).stream().map(this::row).toList());
        result.put("page", page); result.put("size", size); result.put("totalElements", users.count(filter));
        result.put("summary", users.summary());
        return result;
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> detail(UUID id) {
        actors.requireManager();
        return detailView(users.find(id, false).orElseThrow(BusinessException::missing));
    }

    @Transactional(timeout = 5)
    public Map<String, Object> changeStatus(UUID id, StatusChange request) {
        var actor = actors.requireManager();
        locks.lockManager(actor.userId());
        if (request == null) throw BusinessException.invalid();
        String target = OperationsValidation.member(request.status(), Set.of("ACTIVE", "SUSPENDED"));
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (reason.isEmpty()) throw new BusinessException(400, "Vui lòng nhập lý do.");
        if (reason.codePointCount(0, reason.length()) > MAX_REASON) throw new BusinessException(400, "Lý do tối đa 500 ký tự.");
        if (!users.historyReady()) throw new BusinessException(503, "Chưa bật được chức năng khóa tài khoản. Vui lòng báo quản trị viên chạy cập nhật cơ sở dữ liệu.");

        var user = users.find(id, true).orElseThrow(BusinessException::missing);
        if (request.expectedVersion() == null || !request.expectedVersion().equals(user.get("version"))) throw BusinessException.conflict();
        String current = String.valueOf(user.get("status"));
        if (target.equals("SUSPENDED") && !current.equals("ACTIVE"))
            throw new BusinessException(409, "Chỉ tạm khóa được tài khoản đang hoạt động.");
        if (target.equals("ACTIVE") && !current.equals("SUSPENDED"))
            throw new BusinessException(409, "Chỉ mở lại được tài khoản đang tạm khóa.");

        users.changeStatus(id, target);
        users.record(actor.userId(), id, target.equals("ACTIVE") ? "REACTIVATE" : "SUSPEND", reason);
        if (target.equals("ACTIVE") && notificationEvents != null) {
            notificationEvents.publish("account:" + id + ":reactivated:" + user.get("version"), "SYSTEM", "Tài khoản đã được mở lại",
                    "Tài khoản của bạn đã được mở lại và có thể sử dụng bình thường.", null, List.of(id));
        }
        return detailView(users.find(id, false).orElseThrow(BusinessException::missing));
    }

    private Map<String, Object> row(Map<String, Object> r) {
        var out = new LinkedHashMap<String, Object>();
        String role = (String) r.get("role_name"), status = (String) r.get("status"), verification = (String) r.get("verification_status");
        out.put("userId", r.get("user_id"));
        out.put("fullName", r.get("full_name"));
        out.put("email", r.get("email"));
        out.put("phone", r.get("phone"));
        out.put("role", role.substring("ROLE_".length()));
        out.put("roleLabel", ROLE_LABELS.get(role));
        out.put("status", status);
        out.put("statusLabel", STATUS_LABELS.getOrDefault(status, "Cần kiểm tra trạng thái"));
        out.put("createdAt", r.get("created_at"));
        out.put("version", r.get("version"));
        if ("ROLE_TUTOR".equals(role)) out.put("verificationLabel", VERIFICATION_LABELS.getOrDefault(verification, "Chưa nộp hồ sơ"));
        return out;
    }

    private Map<String, Object> detailView(Map<String, Object> r) {
        var out = row(r);
        var learnerId = (UUID) r.get("learner_id");
        var tutorId = (UUID) r.get("tutor_id");
        boolean tutor = "TUTOR".equals(out.get("role"));
        if (tutor) {
            out.put("membershipLabel", MEMBERSHIP_LABELS.getOrDefault((String) r.get("membership_status"), "Miễn phí"));
            out.put("avgRating", r.get("avg_rating"));
            out.put("totalReviews", r.get("total_reviews"));
        } else {
            out.put("currentLevel", r.get("current_level"));
            out.put("targetLevel", r.get("target_level"));
        }
        // Tài khoản chưa có hồ sơ học viên/gia sư thì không có hoạt động để đếm.
        out.put("activity", tutor ? (tutorId == null ? null : users.activity(null, tutorId)) : (learnerId == null ? null : users.activity(learnerId, null)));
        out.put("ticketCount", users.ticketCount((UUID) r.get("user_id")));
        boolean ready = users.historyReady();
        out.put("historyReady", ready);
        out.put("history", ready ? users.history((UUID) r.get("user_id"), 10).stream().map(h -> {
            var item = new LinkedHashMap<String, Object>();
            item.put("action", h.get("action"));
            item.put("actionLabel", ACTION_LABELS.get(h.get("action")));
            item.put("reason", h.get("reason"));
            item.put("occurredAt", h.get("occurred_at"));
            item.put("actorName", h.get("actor_name"));
            return item;
        }).toList() : List.of());
        return out;
    }

    private static String role(String value) {
        if (value == null || value.isBlank()) return null;
        return OperationsValidation.member("ROLE_" + value.strip().toUpperCase(Locale.ROOT), ManagedUserRepository.ROLES);
    }

    private static String status(String value) {
        if (value == null || value.isBlank()) return null;
        return OperationsValidation.member(value.strip().toUpperCase(Locale.ROOT), STATUS_LABELS.keySet());
    }

    private static String blankToNull(String value, int max) {
        if (value == null || value.isBlank()) return null;
        var text = value.strip();
        if (text.codePointCount(0, text.length()) > max) throw BusinessException.invalid();
        return text;
    }
}
