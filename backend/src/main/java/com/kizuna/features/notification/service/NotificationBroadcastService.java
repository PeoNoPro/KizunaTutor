package com.kizuna.features.notification.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.dto.PageResponse;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.Dispatch;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.HistoryFilter;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.NewDispatch;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * FE-12 Story 3–4: Quản trị viên và Quản lý (bổ sung 2026-11-09) soạn thông báo, xem trước, gửi ngay/hẹn giờ theo vai trò hoặc khẩn toàn hệ thống,
 * và xem lịch sử kèm kết quả từng kênh. Gửi ngay thì chốt người nhận trong cùng transaction; email do tác vụ nền gửi sau.
 */
@Service
public class NotificationBroadcastService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final Map<String, String> ROLES;
    public static final Map<String, String> CHANNELS = Map.of("IN_APP", "Trong ứng dụng", "EMAIL", "Email");
    public static final Map<String, String> STATUSES;
    static {
        var roles = new LinkedHashMap<String, String>();
        roles.put("ROLE_LEARNER", "Học viên"); roles.put("ROLE_TUTOR", "Gia sư"); roles.put("ROLE_ACADEMIC_OFFICER", "Cán bộ học vụ");
        roles.put("ROLE_CS_STAFF", "Nhân viên hỗ trợ"); roles.put("ROLE_MANAGER", "Quản lý"); roles.put("ROLE_ADMIN", "Quản trị viên");
        ROLES = Collections.unmodifiableMap(roles);
        var statuses = new LinkedHashMap<String, String>();
        statuses.put("SCHEDULED", "Đã lên lịch"); statuses.put("QUEUED", "Đang chờ"); statuses.put("SENDING", "Đang gửi");
        statuses.put("COMPLETED", "Hoàn tất"); statuses.put("PARTIAL", "Hoàn tất một phần"); statuses.put("FAILED", "Thất bại");
        statuses.put("NO_RECIPIENTS", "Không có người nhận"); statuses.put("NEEDS_REVIEW", "Cần kiểm tra"); statuses.put("CANCELLED", "Đã hủy");
        STATUSES = Collections.unmodifiableMap(statuses);
    }

    private final NotificationDispatchRepository dispatches;
    private final OperationsActorResolver actors;
    private final Clock clock;

    public NotificationBroadcastService(NotificationDispatchRepository dispatches, OperationsActorResolver actors,
                                        @org.springframework.beans.factory.annotation.Qualifier("notificationClock") Clock clock) {
        this.dispatches = dispatches; this.actors = actors; this.clock = clock;
    }

    public record BroadcastRequest(String title, String content, String type, String targetMode, List<String> roles, List<String> channels,
                                   Boolean urgent, OffsetDateTime scheduledAt, UUID submissionKey) {}

    /** Đã kiểm tra hợp lệ và chuẩn hóa (vai trò/kênh sắp xếp cố định để so sánh gửi lặp). */
    record Validated(String title, String content, String type, String targetMode, List<String> roles, List<String> channels, boolean urgent, OffsetDateTime scheduledAt) {}

    private static BusinessException invalid(String message) { return new BusinessException(400, message); }
    private static BusinessException unavailable() { return new BusinessException(503, "Tạm thời chưa xử lý được yêu cầu gửi thông báo. Vui lòng thử lại sau."); }

    private static String text(String value, int max, String label) {
        if (value == null || value.replaceAll("[\\s\\p{Z}\\p{Cf}]", "").isEmpty()) throw invalid("Vui lòng nhập " + label + ".");
        var trimmed = value.strip();
        if (trimmed.codePointCount(0, trimmed.length()) > max) throw invalid(label.substring(0, 1).toUpperCase(new Locale("vi")) + label.substring(1) + " tối đa " + String.format(new Locale("vi"), "%,d", max).replace(',', '.') + " ký tự.");
        return trimmed;
    }

    Validated validate(BroadcastRequest r, boolean forSubmit) {
        if (r == null) throw BusinessException.invalid();
        // Tiêu đề còn làm tiêu đề email nên chỉ được một dòng (chặn xuống dòng, tab và ký tự điều khiển khác).
        if (r.title() != null && r.title().strip().codePoints().anyMatch(Character::isISOControl)) throw invalid("Tiêu đề chỉ được một dòng, không xuống dòng.");
        var title = text(r.title(), 150, "tiêu đề");
        var content = text(r.content(), 10000, "nội dung");
        if (r.type() == null || !NotificationInboxService.TYPES.containsKey(r.type())) throw invalid("Vui lòng chọn nhóm thông báo.");
        boolean urgent = Boolean.TRUE.equals(r.urgent());
        String mode = r.targetMode();
        List<String> roles;
        if ("ALL".equals(mode)) {
            if (!urgent) throw invalid("Gửi toàn hệ thống chỉ dùng cho thông báo khẩn.");
            roles = List.copyOf(ROLES.keySet());
        } else if ("ROLES".equals(mode)) {
            if (urgent) throw invalid("Thông báo khẩn phải gửi cho toàn hệ thống.");
            if (r.roles() == null || r.roles().isEmpty()) throw invalid("Vui lòng chọn ít nhất một nhóm người nhận.");
            if (!ROLES.keySet().containsAll(r.roles())) throw BusinessException.invalid();
            roles = ROLES.keySet().stream().filter(r.roles()::contains).toList();
        } else throw invalid("Vui lòng chọn người nhận.");
        var requested = new HashSet<>(r.channels() == null ? List.<String>of() : r.channels());
        if (!CHANNELS.keySet().containsAll(requested)) throw BusinessException.invalid();
        if (urgent) requested.add("IN_APP");
        if (requested.isEmpty()) throw invalid("Vui lòng chọn ít nhất một kênh gửi.");
        var channels = List.of("IN_APP", "EMAIL").stream().filter(requested::contains).toList();
        var scheduled = r.scheduledAt();
        if (scheduled != null) {
            if (urgent) throw invalid("Thông báo khẩn được gửi ngay, không hẹn giờ.");
            var now = OffsetDateTime.now(clock);
            if (forSubmit && !scheduled.isAfter(now)) throw invalid("Thời điểm hẹn phải ở tương lai.");
            if (scheduled.isAfter(now.plusDays(366))) throw invalid("Chỉ hẹn giờ trong vòng một năm.");
        }
        return new Validated(title, content, r.type(), mode, roles, channels, urgent, scheduled);
    }

    /** Chỉ Quản trị viên và Quản lý được gửi thông báo. */
    public void requireSender() { actors.requireBroadcaster(); }

    /** Xem trước: kiểm tra và đếm số người dự kiến; không lưu, không gửi. */
    public Map<String, Object> preview(BroadcastRequest request) {
        actors.requireBroadcaster();
        var v = validate(request, false);
        try {
            var result = new LinkedHashMap<String, Object>();
            result.put("title", v.title()); result.put("content", v.content());
            result.put("typeLabel", NotificationInboxService.TYPES.get(v.type()));
            result.put("targetLabel", targetLabel(v.targetMode(), v.roles()));
            result.put("channelLabels", v.channels().stream().map(CHANNELS::get).toList());
            result.put("scheduledAt", v.scheduledAt());
            result.put("estimatedRecipients", dispatches.estimate(v.roles()));
            result.put("requiresConfirmation", "ALL".equals(v.targetMode()));
            return result;
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    private static boolean sameBody(Dispatch d, Validated v) {
        return d.title().equals(v.title()) && d.content().equals(v.content()) && d.type().equals(v.type()) && d.urgent() == v.urgent()
                && d.targetMode().equals(v.targetMode()) && d.roles().equals(v.roles()) && d.channels().equals(v.channels())
                && Objects.equals(d.scheduledAt() == null ? null : d.scheduledAt().toInstant(), v.scheduledAt() == null ? null : v.scheduledAt().toInstant());
    }

    /**
     * Xác nhận gửi. Cùng mã thao tác + cùng nội dung (bấm hai lần, mạng gửi lại) trả đúng đợt cũ; cùng mã mà khác nội dung thì 409.
     * Gửi ngay: chốt người nhận và tạo bản trong ứng dụng ngay trong transaction này; email xếp hàng cho tác vụ nền.
     */
    @Transactional(timeout = 15)
    public Map<String, Object> submit(BroadcastRequest request) {
        var actor = actors.requireBroadcaster();
        var v = validate(request, true);
        if (request.submissionKey() == null) throw BusinessException.invalid();
        // Bỏ bước gõ "Xác nhận" (yêu cầu nhóm 2026-11-09): giao diện hỏi lại bằng hộp xác nhận trước khi gửi.
        try {
            var existing = dispatches.bySubmission(actor.userId(), request.submissionKey());
            if (existing.isEmpty()) {
                var id = UUID.randomUUID();
                boolean scheduled = v.scheduledAt() != null;
                dispatches.insert(new NewDispatch(id, "ADMIN", actor.userId(), request.submissionKey(), null, v.type(), v.title(), v.content(), v.urgent(),
                        v.targetMode(), v.roles(), v.channels(), v.scheduledAt(), dispatches.estimate(v.roles()), scheduled ? "SCHEDULED" : "QUEUED", null));
                existing = dispatches.bySubmission(actor.userId(), request.submissionKey());
                if (existing.isPresent() && !scheduled && existing.get().id().equals(id)) {
                    dispatches.freeze(id);
                    existing = dispatches.find(id);
                }
            }
            var dispatch = existing.orElseThrow(NotificationBroadcastService::unavailable);
            if (!sameBody(dispatch, v)) throw new BusinessException(409, "Yêu cầu gửi này đã được ghi nhận với nội dung khác. Vui lòng tải lại trang trước khi gửi.");
            var result = new LinkedHashMap<String, Object>();
            result.put("broadcastId", dispatch.id());
            result.put("status", dispatch.status());
            result.put("statusLabel", STATUSES.get(dispatch.status()));
            result.put("scheduledAt", dispatch.scheduledAt());
            result.put("recipientCount", dispatch.recipientCount());
            result.put("message", dispatch.scheduledAt() != null && dispatch.frozenAt() == null
                    ? "Đã lên lịch gửi thông báo."
                    : "Đã ghi nhận thông báo. Bản trong ứng dụng đã có cho người nhận; email (nếu có) đang được xếp hàng gửi.");
            return result;
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    private static String targetLabel(String mode, List<String> roles) {
        return "ALL".equals(mode) ? "Toàn hệ thống" : String.join(", ", roles.stream().map(ROLES::get).toList());
    }

    /**
     * Hủy thông báo đã hẹn giờ khi chưa đến giờ gửi. Người tạo được hủy đợt của mình; Quản trị viên hủy được mọi đợt.
     * Ghi ai hủy, lúc nào (hiện trong Nhật ký thao tác). Đã chốt người nhận hoặc đã hủy rồi thì báo 409.
     */
    @Transactional(timeout = 5)
    public Map<String, Object> cancel(UUID id) {
        var actor = actors.requireBroadcaster();
        try {
            var d = dispatches.find(id).filter(found -> found.creatorId() != null).orElseThrow(BusinessException::missing);
            if (!canCancel(d, actor)) {
                if (!"SCHEDULED".equals(d.status())) throw new BusinessException(409, "Thông báo này đã gửi hoặc đã hủy, không hủy được nữa.");
                throw new BusinessException(403, "Bạn chỉ hủy được thông báo do chính mình hẹn giờ.");
            }
            if (!dispatches.cancel(id, actor.userId())) throw new BusinessException(409, "Thông báo vừa đến giờ gửi nên không hủy được nữa.");
            var m = summary(dispatches.find(id).orElseThrow(BusinessException::missing), actor);
            m.put("message", "Đã hủy thông báo hẹn giờ. Người nhận sẽ không nhận được thông báo này.");
            return m;
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    static boolean canCancel(Dispatch d, com.kizuna.features.admin.dto.OperationsActor actor) {
        return "SCHEDULED".equals(d.status()) && d.frozenAt() == null
                && ("ROLE_ADMIN".equals(actor.role()) || actor.userId().equals(d.creatorId()));
    }

    private Map<String, Object> summary(Dispatch d, com.kizuna.features.admin.dto.OperationsActor actor) {
        var m = summary(d);
        m.put("canCancel", canCancel(d, actor));
        return m;
    }

    private Map<String, Object> summary(Dispatch d) {
        var m = new LinkedHashMap<String, Object>();
        m.put("id", d.id()); m.put("title", d.title());
        m.put("typeLabel", NotificationInboxService.TYPES.get(d.type()));
        m.put("urgent", d.urgent());
        m.put("targetLabel", targetLabel(d.targetMode(), d.roles()));
        m.put("channelLabels", d.channels().stream().map(CHANNELS::get).toList());
        m.put("creatorName", d.creatorName());
        m.put("createdAt", d.createdAt()); m.put("scheduledAt", d.scheduledAt()); m.put("frozenAt", d.frozenAt());
        m.put("status", d.status()); m.put("statusLabel", STATUSES.get(d.status()));
        m.put("previewCount", d.previewCount()); m.put("recipientCount", d.recipientCount());
        return m;
    }

    @Transactional(readOnly = true, timeout = 5)
    public PageResponse<Map<String, Object>> history(LocalDate fromDate, LocalDate toDate, String status, String channel, String role, String title, int page, int size) {
        var actor = actors.requireBroadcaster();
        if (page < 0 || size < 1 || size > 50) throw BusinessException.invalid();
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) throw invalid("Ngày bắt đầu phải trước ngày kết thúc.");
        if (status != null && !STATUSES.containsKey(status)) throw BusinessException.invalid();
        if (channel != null && !CHANNELS.containsKey(channel)) throw BusinessException.invalid();
        if (role != null && !ROLES.containsKey(role)) throw BusinessException.invalid();
        var filter = new HistoryFilter(fromDate == null ? null : fromDate.atStartOfDay(ZONE).toOffsetDateTime(),
                toDate == null ? null : toDate.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime(), status, channel, role,
                title == null || title.isBlank() ? null : title.strip());
        try {
            long total = dispatches.historyCount(filter);
            var items = dispatches.history(filter, page, size).stream().map(d -> summary(d, actor)).toList();
            return new PageResponse<>(items, page, size, total, (total + size - 1) / size);
        } catch (DataAccessException exception) { throw unavailable(); }
    }

    private static final List<String> STATE_ORDER = List.of("IN_APP_AVAILABLE", "EMAIL_ACCEPTED", "PENDING", "SENDING", "FAILED", "SKIPPED", "UNKNOWN");

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> detail(UUID id) {
        var actor = actors.requireBroadcaster();
        try {
            var d = dispatches.find(id).filter(found -> found.creatorId() != null).orElseThrow(BusinessException::missing);
            var m = summary(d, actor);
            m.put("content", d.content());
            var counts = dispatches.counts(id);
            var channels = new ArrayList<Map<String, Object>>();
            for (var channel : d.channels()) {
                var states = counts.getOrDefault(channel, Map.of());
                var row = new LinkedHashMap<String, Object>();
                row.put("channel", channel); row.put("channelLabel", CHANNELS.get(channel));
                row.put("success", states.getOrDefault("IN_APP_AVAILABLE", 0L) + states.getOrDefault("EMAIL_ACCEPTED", 0L));
                row.put("pending", states.getOrDefault("PENDING", 0L) + states.getOrDefault("SENDING", 0L));
                row.put("failed", states.getOrDefault("FAILED", 0L));
                row.put("skipped", states.getOrDefault("SKIPPED", 0L));
                row.put("unknown", states.getOrDefault("UNKNOWN", 0L));
                row.put("total", STATE_ORDER.stream().mapToLong(state -> states.getOrDefault(state, 0L)).sum());
                channels.add(row);
            }
            m.put("channels", channels);
            return m;
        } catch (DataAccessException exception) { throw unavailable(); }
    }
}
