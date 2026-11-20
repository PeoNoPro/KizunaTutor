package com.kizuna.features.payment.service;

import com.kizuna.features.notification.service.NotificationEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * FE-12: tác vụ nền cho hội viên gia sư (chạy mỗi giờ).
 * <ul>
 *   <li>Cập nhật gói đã quá ngày kết thúc sang "hết hạn" (trước đây chỉ cập nhật khi có người mở trang), kèm thông báo
 *       "Gói hội viên đã hết hạn" do {@link MembershipExpiryService}.</li>
 *   <li>Nhắc "sắp hết hạn" khi gói cuối cùng còn tối đa {@value #WARN_DAYS} ngày: một thông báo cho mỗi ngày hết hạn.
 *       Gia sư đã gia hạn (còn gói khác dài hơn) thì không bị nhắc.</li>
 * </ul>
 */
@Slf4j
@Component
public class MembershipReminderScheduler {
    static final int WARN_DAYS = 3;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final NotificationEvents events;
    private final MembershipExpiryService expiryService;

    public MembershipReminderScheduler(DataSource dataSource, NotificationEvents events, MembershipExpiryService expiryService) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(10);
        this.events = events;
        this.expiryService = expiryService;
    }

    private record Expiring(UUID tutorId, UUID userId, LocalDate endDate) { }

    @Scheduled(fixedDelay = 60 * 60 * 1000L, initialDelay = 90 * 1000L)
    public void run() {
        LocalDate today = LocalDate.now(VN);
        try {
            List<UUID> ended = jdbc.query("SELECT DISTINCT tutor_id FROM tutor_memberships WHERE status::text = 'ACTIVE' AND end_date < ?",
                    (rs, i) -> rs.getObject(1, UUID.class), today);
            for (UUID tutorId : ended) {
                try { expiryService.refresh(tutorId, today); }
                catch (RuntimeException exception) { log.warn("[NOTIFICATION] Chưa cập nhật được hết hạn hội viên: {}", exception.getClass().getSimpleName()); }
            }
        } catch (RuntimeException exception) {
            log.warn("[NOTIFICATION] Chưa tìm được gói hội viên hết hạn: {}", exception.getClass().getSimpleName());
        }
        try {
            for (var row : expiringSoon(today)) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(today, row.endDate());
                events.publish("membership:" + row.tutorId() + ":expiring:" + row.endDate(), "PAYMENT", "Gói hội viên sắp hết hạn",
                        "Gói hội viên của bạn sẽ hết hạn ngày " + row.endDate().format(MembershipLabels.DATE_VN)
                                + (days <= 0 ? " (hôm nay)" : " (còn " + days + " ngày)")
                                + ". Hãy gia hạn để không bị gián đoạn việc dạy.", "/tutor/membership", List.of(row.userId()));
            }
        } catch (RuntimeException exception) {
            log.warn("[NOTIFICATION] Chưa nhắc được gói hội viên sắp hết hạn: {}", exception.getClass().getSimpleName());
        }
    }

    private List<Expiring> expiringSoon(LocalDate today) {
        return jdbc.query("""
                SELECT m.tutor_id, t.user_id, max(m.end_date) AS end_date
                  FROM tutor_memberships m JOIN tutors t ON t.tutor_id = m.tutor_id
                 WHERE m.status::text = 'ACTIVE'
                 GROUP BY m.tutor_id, t.user_id
                HAVING max(m.end_date) >= ? AND max(m.end_date) <= ?
                   AND NOT EXISTS (SELECT 1 FROM notification_dispatches d
                                    WHERE d.event_key = 'membership:' || m.tutor_id || ':expiring:' || max(m.end_date))
                """, (rs, i) -> new Expiring(rs.getObject("tutor_id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("end_date", LocalDate.class)), today, today.plusDays(WARN_DAYS));
    }
}
