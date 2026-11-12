package com.kizuna.features.notification.service;

import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.NewDispatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import java.util.*;

/**
 * FE-12 giai đoạn 3 (FR-015): điểm để các module nghiệp vụ tạo thông báo tự động khi có sự kiện thật đã ghi nhận.
 *
 * <p>Cách dùng (gọi ngay sau khi lưu xong thay đổi, trong cùng @Transactional của nghiệp vụ):
 * <pre>
 * notificationEvents.publish("payment:" + transactionId + ":success", "PAYMENT",
 *         "Thanh toán thành công", "Gói hội viên 3 tháng đã được kích hoạt.", "/tutor/membership", List.of(tutorUserId));
 * </pre>
 * <ul>
 *   <li>{@code eventKey} cố định cho đúng một lần chuyển trạng thái (không dùng số ngẫu nhiên): xử lý lại cùng sự kiện
 *       không tạo thêm thông báo.</li>
 *   <li>{@code type}: SYSTEM, TRIAL, ASSIGNMENT, PAYMENT hoặc TICKET (người nhận bật/tắt theo nhóm này).</li>
 *   <li>Nội dung chỉ là chữ thường, không chứa ghi chú nội bộ, mật khẩu, mã OTP hay dữ liệu cổng thanh toán.</li>
 *   <li>{@code actionPath}: đường dẫn nội bộ bắt đầu bằng "/" để mở đúng trang liên quan; trang đó tự kiểm quyền.</li>
 *   <li>Người nhận do module nguồn chọn; tài khoản không còn hoạt động bị bỏ qua. Luôn có bản trong ứng dụng (nếu người
 *       nhận không tắt), email chỉ khi người nhận đã bật email ở nhóm đó.</li>
 * </ul>
 * Lỗi khi tạo thông báo (ví dụ DB chưa chạy SQL FE-12) chỉ ghi cảnh báo vào log, không làm hỏng thao tác của nghiệp vụ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEvents {
    private final Writer writer;

    /**
     * Đường dẫn nội bộ an toàn: bắt đầu bằng đúng một "/", chỉ chữ, số và / _ . ~ % = & ? # + -.
     * Chặn "//trang-ngoai", "/\\trang-ngoai" (trình duyệt hiểu "\\" là "/") và ký tự xuống dòng. Dùng chung với giao diện và ràng buộc DB.
     */
    public static final java.util.regex.Pattern SAFE_PATH = java.util.regex.Pattern.compile("^/([A-Za-z0-9_.~%=&?#+-][A-Za-z0-9/_.~%=&?#+-]*)?$");

    public static boolean safePath(String path) { return path == null || (path.length() <= 300 && SAFE_PATH.matcher(path).matches()); }

    /** Tiêu đề một dòng: đổi ký tự điều khiển (xuống dòng, tab...) thành dấu cách. */
    static String oneLine(String value) { return value == null ? null : value.replaceAll("\\p{Cntrl}+", " ").strip(); }

    public void publish(String eventKey, String type, String title, String content, String actionPath, Collection<UUID> recipients) {
        var targets = recipients == null ? List.<UUID>of() : recipients.stream().filter(Objects::nonNull).distinct().toList();
        if (targets.isEmpty() || eventKey == null || eventKey.isBlank()) return;
        if (!NotificationInboxService.TYPES.containsKey(type)) throw new IllegalArgumentException("Nhóm thông báo không hợp lệ: " + type);
        if (!safePath(actionPath)) throw new IllegalArgumentException("Đường dẫn thông báo phải là đường dẫn nội bộ.");
        try {
            writer.write(eventKey.length() > 255 ? eventKey.substring(0, 255) : eventKey, type, limit(oneLine(title), 150), limit(content, 10000), actionPath, targets);
        } catch (RuntimeException exception) {
            log.warn("[NOTIFICATION] Chưa tạo được thông báo cho sự kiện {}: {}", eventKey, exception.getClass().getSimpleName());
        }
    }

    /** Gửi cho mọi tài khoản đang hoạt động của một vai trò (ví dụ mọi Quản trị viên khi có sự cố mới). */
    public void publishToRole(String eventKey, String type, String title, String content, String actionPath, String role) {
        try { publish(eventKey, type, title, content, actionPath, writer.dispatches.activeUsers(role)); }
        catch (RuntimeException exception) { log.warn("[NOTIFICATION] Chưa tìm được người nhận cho sự kiện {}: {}", eventKey, exception.getClass().getSimpleName()); }
    }

    /** Vai trò hiện tại của một tài khoản (ROLE_...), rỗng nếu không tìm thấy hoặc lỗi. */
    public Optional<String> roleOf(UUID user) {
        try { return writer.dispatches.roleOf(user); } catch (RuntimeException exception) { return Optional.empty(); }
    }

    private static String limit(String value, int max) {
        var text = value == null || value.isBlank() ? "Thông báo" : value.strip();
        return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max - 1)) + "…";
    }

    /**
     * Ghi trong một savepoint JDBC của transaction nghiệp vụ: thành công thì lưu cùng lúc với nghiệp vụ,
     * lỗi thì chỉ phần thông báo bị hủy, nghiệp vụ vẫn tiếp tục. Không có transaction thì tự mở một transaction riêng.
     * <p>Không dùng {@code Propagation.NESTED}: JpaTransactionManager + HibernateJpaDialect không hỗ trợ savepoint,
     * mọi lần gọi đều lỗi NestedTransactionNotSupportedException nên chưa thông báo tự động nào được tạo.
     */
    @Service
    public static class Writer {
        final NotificationDispatchRepository dispatches;
        private final DataSource dataSource;
        private final TransactionTemplate standalone;

        public Writer(NotificationDispatchRepository dispatches, DataSource dataSource, PlatformTransactionManager transactionManager) {
            this.dispatches = dispatches; this.dataSource = dataSource;
            this.standalone = new TransactionTemplate(transactionManager);
            this.standalone.setTimeout(10);
        }

        public void write(String eventKey, String type, String title, String content, String actionPath, List<UUID> recipients) {
            if (!TransactionSynchronizationManager.isActualTransactionActive()) {
                standalone.executeWithoutResult(status -> insert(eventKey, type, title, content, actionPath, recipients));
                return;
            }
            // Cùng kết nối mà JpaTransactionManager đã gắn cho transaction nghiệp vụ (JdbcTemplate cũng dùng kết nối này).
            Connection connection = DataSourceUtils.getConnection(dataSource);
            Savepoint savepoint;
            try { savepoint = connection.setSavepoint(); }
            catch (SQLException exception) { throw new CannotCreateTransactionException("Không tạo được savepoint cho thông báo", exception); }
            try {
                insert(eventKey, type, title, content, actionPath, recipients);
                try { connection.releaseSavepoint(savepoint); } catch (SQLException ignored) { /* savepoint tự giải phóng khi commit */ }
            } catch (RuntimeException exception) {
                try { connection.rollback(savepoint); } catch (SQLException ignored) { /* giữ lỗi gốc */ }
                throw exception;
            }
        }

        private void insert(String eventKey, String type, String title, String content, String actionPath, List<UUID> recipients) {
            var id = UUID.randomUUID();
            // Trùng mã sự kiện (xử lý lại) thì không thêm; đã có đợt cũ thì giữ nguyên.
            if (!dispatches.insert(new NewDispatch(id, "EVENT", null, null, eventKey, type, title, content, false, "EXPLICIT",
                    List.of(), List.of("IN_APP", "EMAIL"), null, recipients.size(), "QUEUED", actionPath))) return;
            dispatches.freeze(id, recipients);
        }
    }
}
