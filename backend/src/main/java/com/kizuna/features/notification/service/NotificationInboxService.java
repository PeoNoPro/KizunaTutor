package com.kizuna.features.notification.service;

import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.notification.repository.NotificationInboxRepository;
import com.kizuna.features.notification.repository.NotificationInboxRepository.Item;
import com.kizuna.features.notification.repository.NotificationInboxRepository.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * FE-12 Story 1–2: hộp thông báo cá nhân và tùy chọn nhận thông báo của người đang đăng nhập (sáu vai trò).
 * Bản của người khác hoặc đã xóa trả 404 như không tồn tại.
 */
@Service
@RequiredArgsConstructor
public class NotificationInboxService {
    /** Năm nhóm thông báo theo đúng thứ tự hiển thị. */
    public static final Map<String, String> TYPES;
    static {
        var types = new LinkedHashMap<String, String>();
        types.put("SYSTEM", "Hệ thống");
        types.put("TRIAL", "Học thử");
        types.put("ASSIGNMENT", "Bài tập");
        types.put("PAYMENT", "Thanh toán");
        types.put("TICKET", "Hỗ trợ");
        TYPES = Collections.unmodifiableMap(types);
    }
    /** Chưa thiết lập: bật trong ứng dụng, tắt email (FR-006). */
    public static final boolean DEFAULT_IN_APP = true, DEFAULT_EMAIL = false;

    private final NotificationInboxRepository inbox;

    public record PreferenceGroup(String type, Boolean inAppEnabled, Boolean emailEnabled) {}
    public record PreferenceUpdate(Long expectedVersion, List<PreferenceGroup> groups) {}

    /** Tài khoản đang đăng nhập còn hoạt động, kèm vai trò và số chưa đọc (một truy vấn). */
    NotificationInboxRepository.Account currentAccount() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new BusinessException(401, "Vui lòng đăng nhập để tiếp tục.");
        try {
            return inbox.activeAccount(auth.getName(), auth.getPrincipal() instanceof User u ? u.getUserId() : null)
                    .orElseThrow(() -> new BusinessException(403, "Tài khoản hiện không có quyền sử dụng chức năng này."));
        } catch (DataAccessException exception) {
            throw unavailable();
        }
    }

    UUID currentUser() { return currentAccount().userId(); }

    private static BusinessException unavailable() { return new BusinessException(503, "Tạm thời chưa tải được thông báo. Vui lòng thử lại sau."); }
    private static BusinessException missing() { return new BusinessException(404, "Không tìm thấy thông báo hoặc thông báo đã bị xóa."); }

    private static Map<String, Object> view(Item item, boolean withContent) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", item.id());
        m.put("title", item.title());
        if (withContent) m.put("content", item.content());
        else m.put("preview", item.content().length() > 160 ? item.content().substring(0, 160) + "…" : item.content());
        m.put("type", item.type());
        m.put("typeLabel", TYPES.getOrDefault(item.type(), "Khác"));
        m.put("receivedAt", item.receivedAt());
        m.put("isRead", item.read());
        m.put("readAt", item.readAt());
        m.put("actionPath", item.actionPath());
        return m;
    }

    // Chỉ đọc bằng một truy vấn: không mở transaction để bớt lượt đi về tới DB ở xa.
    public Map<String, Object> list(String read, String type, java.time.LocalDate fromDate, java.time.LocalDate toDate, int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw BusinessException.invalid();
        if (read != null && !Set.of("all", "unread").contains(read)) throw BusinessException.invalid();
        if (type != null && !TYPES.containsKey(type)) throw BusinessException.invalid();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw new BusinessException(400, "Ngày bắt đầu phải trước ngày kết thúc.");
        var zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        var from = fromDate == null ? null : fromDate.atStartOfDay(zone).toOffsetDateTime();
        var to = toDate == null ? null : toDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        var user = currentUser();
        try {
            var data = inbox.pageWithTotals(user, "unread".equals(read), type, from, to, page, size);
            var result = new LinkedHashMap<String, Object>();
            result.put("items", data.items().stream().map(item -> view(item, false)).toList());
            result.put("page", page);
            result.put("size", size);
            result.put("totalElements", data.total());
            result.put("totalPages", (data.total() + size - 1) / size);
            result.put("unreadCount", data.unread());
            return result;
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    private static final Map<String, String> VIEW_ROLES = Map.of("ROLE_LEARNER", "learner", "ROLE_TUTOR", "tutor", "ROLE_ACADEMIC_OFFICER", "academic",
            "ROLE_CS_STAFF", "cs", "ROLE_MANAGER", "manager", "ROLE_ADMIN", "admin");

    /** Vai trò hiển thị (learner/tutor/academic/cs/manager/admin) và tên, cùng số chưa đọc cho chuông. */
    // Chỉ đọc bằng một truy vấn: không mở transaction để bớt lượt đi về tới DB ở xa.
    public Map<String, Object> me() {
        var account = currentAccount();
        var role = VIEW_ROLES.get(account.role());
        if (role == null) throw new BusinessException(403, "Tài khoản hiện không có quyền sử dụng chức năng này.");
        return Map.of("viewRole", role, "displayName", account.fullName() == null ? "" : account.fullName(), "unreadCount", account.unread());
    }

    // Chỉ đọc bằng một truy vấn: không mở transaction để bớt lượt đi về tới DB ở xa.
    public Map<String, Object> unreadCount() {
        return Map.of("unreadCount", currentAccount().unread());
    }

    /** Chỉ đọc; giao diện tự gọi markRead sau khi tải chi tiết thành công. */
    // Chỉ đọc bằng một truy vấn: không mở transaction để bớt lượt đi về tới DB ở xa.
    public Map<String, Object> detail(UUID id) {
        var user = currentUser();
        try { return view(inbox.find(user, id).orElseThrow(NotificationInboxService::missing), true); }
        catch (DataAccessException exception) { throw unavailable(); }
    }

    @Transactional(timeout = 5)
    public Map<String, Object> markRead(UUID id) {
        var user = currentUser();
        try {
            if (!inbox.markRead(user, id)) throw missing();
            var item = inbox.find(user, id).orElseThrow(NotificationInboxService::missing);
            var result = new LinkedHashMap<String, Object>();
            result.put("isRead", true);
            result.put("readAt", item.readAt());
            result.put("unreadCount", inbox.unread(user));
            return result;
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    @Transactional(timeout = 5)
    public Map<String, Object> delete(UUID id) {
        var user = currentUser();
        try {
            if (!inbox.softDelete(user, id)) throw missing();
            return Map.of("deleted", true, "unreadCount", inbox.unread(user));
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    private Map<String, Object> preferenceView(UUID user) {
        var saved = inbox.preferences(user);
        var groups = new ArrayList<Map<String, Object>>();
        TYPES.forEach((type, label) -> {
            var p = saved.get(type);
            var m = new LinkedHashMap<String, Object>();
            m.put("type", type);
            m.put("typeLabel", label);
            m.put("inAppEnabled", p == null ? DEFAULT_IN_APP : p.inApp());
            m.put("emailEnabled", p == null ? DEFAULT_EMAIL : p.email());
            groups.add(m);
        });
        return Map.of("version", inbox.preferenceVersion(user), "groups", groups);
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> preferences() {
        var user = currentUser();
        try { return preferenceView(user); } catch (DataAccessException exception) { throw unavailable(); }
    }

    /** Lưu cả bảng năm nhóm trong một transaction; phiên bản cũ thì 409, không ghi đè âm thầm. */
    @Transactional(timeout = 5)
    public Map<String, Object> savePreferences(PreferenceUpdate request) {
        if (request == null || request.expectedVersion() == null || request.groups() == null || request.groups().size() != TYPES.size())
            throw BusinessException.invalid();
        var groups = new LinkedHashMap<String, Preference>();
        for (var group : request.groups()) {
            if (group == null || !TYPES.containsKey(group.type()) || group.inAppEnabled() == null || group.emailEnabled() == null || groups.containsKey(group.type()))
                throw BusinessException.invalid();
            groups.put(group.type(), new Preference(group.type(), group.inAppEnabled(), group.emailEnabled()));
        }
        var user = currentUser();
        try {
            if (inbox.lockPreferenceVersion(user) != request.expectedVersion())
                throw new BusinessException(409, "Tùy chọn đã được thay đổi ở nơi khác. Vui lòng tải lại trước khi lưu.");
            inbox.savePreferences(user, groups.values());
            return preferenceView(user);
        } catch (DataAccessException exception) { throw unavailable(); }
    }
}
