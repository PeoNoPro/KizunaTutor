package com.kizuna.features.progress.service;

import com.kizuna.features.notification.service.NotificationEvents;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * FE-12: nhắc học viên về bài tập chưa nộp (chạy mỗi 10 phút).
 * <ul>
 *   <li>Sắp đến hạn: bài đã mở, hạn nộp trong 24 giờ tới, học viên chưa nộp lần nào → một thông báo.</li>
 *   <li>Quá hạn: hạn nộp vừa qua trong 24 giờ, học viên vẫn chưa nộp → một thông báo.</li>
 * </ul>
 * Chỉ xét lớp đang hoạt động. Mã sự kiện cố định theo bài tập ({@code assignment:<id>:due-soon|overdue}) nên mỗi bài chỉ báo
 * một lần mỗi loại; đổi hạn nộp sang lúc khác thì bài tập đã có thông báo "Bài tập được cập nhật" riêng.
 */
@Slf4j
@Component
public class AssignmentReminderScheduler {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String BASE = """
            SELECT a.assignment_id, a.title, a.due_date, l.user_id AS learner_user
              FROM assignments a
              JOIN learning_spaces s ON s.space_id = a.space_id AND s.status::text = 'ACTIVE'
              JOIN learners l ON l.learner_id = s.learner_id
             WHERE a.due_date IS NOT NULL
               AND (a.opens_at IS NULL OR a.opens_at <= now())
               AND NOT EXISTS (SELECT 1 FROM assignment_submissions x
                                WHERE x.assignment_id = a.assignment_id AND x.learner_id = s.learner_id)
               AND NOT EXISTS (SELECT 1 FROM notification_dispatches d
                                WHERE d.event_key = 'assignment:' || a.assignment_id || ':%s')
            """;
    static final String DUE_SOON_SQL = BASE.formatted("due-soon")
            + " AND a.due_date > now() AND a.due_date <= now() + interval '24 hours'";
    static final String OVERDUE_SQL = BASE.formatted("overdue")
            + " AND a.due_date <= now() AND a.due_date > now() - interval '24 hours'";

    private record Row(UUID assignmentId, String title, OffsetDateTime dueDate, UUID learnerUser) { }

    private final JdbcTemplate jdbc;
    private final NotificationEvents events;

    public AssignmentReminderScheduler(DataSource dataSource, NotificationEvents events) {
        this.jdbc = new JdbcTemplate(dataSource);
        this.jdbc.setQueryTimeout(10);
        this.events = events;
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void remind() {
        try {
            for (var row : load(DUE_SOON_SQL)) {
                events.publish("assignment:" + row.assignmentId() + ":due-soon", "ASSIGNMENT", "Bài tập sắp đến hạn nộp",
                        "Bài \"" + row.title() + "\" sẽ hết hạn nộp lúc " + row.dueDate().atZoneSameInstant(VN).format(WHEN)
                                + ". Bạn chưa nộp bài này.", "/learner/my-courses", List.of(row.learnerUser()));
            }
            for (var row : load(OVERDUE_SQL)) {
                events.publish("assignment:" + row.assignmentId() + ":overdue", "ASSIGNMENT", "Bài tập đã quá hạn nộp",
                        "Bài \"" + row.title() + "\" đã hết hạn nộp lúc " + row.dueDate().atZoneSameInstant(VN).format(WHEN)
                                + " và bạn chưa nộp. Hãy liên hệ gia sư nếu cần nộp muộn.", "/learner/my-courses", List.of(row.learnerUser()));
            }
        } catch (RuntimeException exception) {
            log.warn("[NOTIFICATION] Chưa nhắc được hạn nộp bài tập: {}", exception.getClass().getSimpleName());
        }
    }

    private List<Row> load(String sql) {
        return jdbc.query(sql, (rs, i) -> new Row(rs.getObject("assignment_id", UUID.class), rs.getString("title"),
                rs.getObject("due_date", OffsetDateTime.class), rs.getObject("learner_user", UUID.class)));
    }
}
