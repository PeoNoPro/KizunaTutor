package com.kizuna.features.booking.service;

import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyIndex;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.BusyKind;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.DatedBusy;
import com.kizuna.features.booking.service.ConnectionSchedulePlanner.WeeklyBusy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Nạp lịch bận của gia sư (và học viên) trong một khoảng ngày bằng MỘT truy vấn (DB ở xa, mỗi query ~110 ms).
 *
 * Chỉ tính lịch ĐÃ ĐƯỢC GIA SƯ CHẤP NHẬN (học thử đã nhận / đã dời, kết nối đã đồng ý, lịch lớp).
 * Yêu cầu đang chờ không giữ chỗ: nhiều học viên xin cùng một giờ được, gia sư chọn nhận ai.
 */
@Component
@RequiredArgsConstructor
public class ConnectionBusyLoader {

    private static final UUID NONE = new UUID(0L, 0L);

    private final JdbcTemplate jdbcTemplate;

    public BusyIndex load(UUID tutorId, UUID learnerId, UUID excludeRequestId,
                          LocalDate from, LocalDate to) {
        UUID learner = learnerId == null ? NONE : learnerId;
        UUID exclude = excludeRequestId == null ? NONE : excludeRequestId;

        String sessionStatus = "cr.status = 'ACCEPTED'";
        // Đã chốt: gia sư đã xác nhận, hoặc gia sư tự dời lịch (RESCHEDULED cũng là đã chốt)
        String trialStatus = "tb.booking_status IN ('CONFIRMED', 'RESCHEDULED')";

        String sql = ""
                // Lịch lớp chính thức (class_schedules: 1 = Thứ Hai ... 7 = Chủ nhật → quy đổi sang 1 = Chủ nhật)
                + "SELECT CASE WHEN ls.tutor_id = ? THEN 'CT' ELSE 'CL' END AS kind, (c.day_of_week % 7) + 1 AS dow, "
                + "       NULL::date AS d, c.start_time::text AS s, c.end_time::text AS e "
                + "FROM class_schedules c JOIN learning_spaces ls ON ls.space_id = c.space_id "
                + "WHERE ls.status IN ('ACTIVE', 'PAUSED') AND (ls.tutor_id = ? OR ls.learner_id = ?) "
                + "UNION ALL "
                // Ca hằng tuần kiểu cũ (yêu cầu đã đồng ý nhưng chưa có buổi cụ thể): khóa mọi tuần
                + "SELECT 'LK', sl.day_of_week, NULL, sl.start_time::text, sl.end_time::text "
                + "FROM connection_request_slots sl JOIN connection_requests cr ON cr.request_id = sl.request_id "
                + "WHERE cr.tutor_id = ? AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED' AND cr.request_id <> ? "
                + "  AND NOT EXISTS (SELECT 1 FROM connection_request_sessions x WHERE x.request_id = cr.request_id) "
                + "  AND (EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id "
                + "               AND x.status IN ('ACTIVE', 'PAUSED')) "
                + "       OR NOT EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id)) "
                + "UNION ALL "
                // Buổi học cụ thể của các yêu cầu kết nối khác
                + "SELECT CASE WHEN cr.status = 'ACCEPTED' THEN 'SA' ELSE 'SP' END, NULL, s.session_date, "
                + "       s.start_time::text, s.end_time::text "
                + "FROM connection_request_sessions s JOIN connection_requests cr ON cr.request_id = s.request_id "
                + "WHERE cr.tutor_id = ? AND cr.request_type = 'DIRECT' AND cr.request_id <> ? "
                + "  AND s.session_date BETWEEN ? AND ? AND " + sessionStatus + " "
                + "UNION ALL "
                // Buổi học thử của học viên khác (giờ Việt Nam)
                + "SELECT 'TR', NULL, CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date), "
                + "       to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'), "
                + "       CASE WHEN CAST(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) "
                + "                 > CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) THEN '23:59' "
                + "            ELSE to_char(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI') END "
                + "FROM trial_bookings tb "
                + "WHERE tb.tutor_id = ? AND tb.learner_id <> ? AND tb.scheduled_end_time > CURRENT_TIMESTAMP "
                + "  AND CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) BETWEEN ? AND ? "
                + "  AND " + trialStatus + " "
                // ---- Lịch ĐÃ XÁC NHẬN của chính học viên với GIA SƯ KHÁC (cả hai chế độ).
                //      Yêu cầu đang chờ của học viên với gia sư khác KHÔNG tính: học viên được hỏi song song nhiều gia sư;
                //      gia sư nào đồng ý sau sẽ bị chặn ở bước đồng ý (vì lúc đó lịch kia đã thành "đã xác nhận").
                + "UNION ALL "
                // Buổi học cụ thể của khóa học đã được gia sư khác đồng ý
                + "SELECT 'LA', NULL, s.session_date, s.start_time::text, s.end_time::text "
                + "FROM connection_request_sessions s JOIN connection_requests cr ON cr.request_id = s.request_id "
                + "WHERE cr.learner_id = ? AND cr.tutor_id <> ? AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED' "
                + "  AND s.session_date BETWEEN ? AND ? "
                + "UNION ALL "
                // Ca hằng tuần kiểu cũ của khóa học đã được gia sư khác đồng ý (chưa có buổi cụ thể)
                + "SELECT 'LW', sl.day_of_week, NULL, sl.start_time::text, sl.end_time::text "
                + "FROM connection_request_slots sl JOIN connection_requests cr ON cr.request_id = sl.request_id "
                + "WHERE cr.learner_id = ? AND cr.tutor_id <> ? AND cr.request_type = 'DIRECT' AND cr.status = 'ACCEPTED' "
                + "  AND NOT EXISTS (SELECT 1 FROM connection_request_sessions x WHERE x.request_id = cr.request_id) "
                + "  AND (EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id "
                + "               AND x.status IN ('ACTIVE', 'PAUSED')) "
                + "       OR NOT EXISTS (SELECT 1 FROM learning_spaces x WHERE x.tutor_id = cr.tutor_id AND x.learner_id = cr.learner_id)) "
                + "UNION ALL "
                // Buổi học thử đã được gia sư khác xác nhận (kể cả gia sư đã dời lịch)
                + "SELECT 'LT', NULL, CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date), "
                + "       to_char(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI'), "
                + "       CASE WHEN CAST(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) "
                + "                 > CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) THEN '23:59' "
                + "            ELSE to_char(tb.scheduled_end_time AT TIME ZONE 'Asia/Ho_Chi_Minh', 'HH24:MI') END "
                + "FROM trial_bookings tb "
                + "WHERE tb.learner_id = ? AND tb.tutor_id <> ? AND tb.scheduled_end_time > CURRENT_TIMESTAMP "
                + "  AND tb.booking_status IN ('CONFIRMED', 'RESCHEDULED') "
                + "  AND CAST(tb.scheduled_start_time AT TIME ZONE 'Asia/Ho_Chi_Minh' AS date) BETWEEN ? AND ?";

        List<Object> args = new ArrayList<>(List.of(tutorId, tutorId, learner, tutorId, exclude,
                tutorId, exclude, Date.valueOf(from), Date.valueOf(to)));
        args.addAll(List.of(tutorId, learner, Date.valueOf(from), Date.valueOf(to)));
        // Lịch đã xác nhận của học viên với gia sư khác (learner = NONE thì không có dòng nào)
        args.addAll(List.of(learner, tutorId, Date.valueOf(from), Date.valueOf(to)));
        args.addAll(List.of(learner, tutorId));
        args.addAll(List.of(learner, tutorId, Date.valueOf(from), Date.valueOf(to)));

        List<WeeklyBusy> weekly = new ArrayList<>();
        List<DatedBusy> dated = new ArrayList<>();
        jdbcTemplate.query(sql, rs -> {
            String kind = rs.getString("kind");
            LocalTime start = LocalTime.parse(hhmm(rs.getString("s")));
            LocalTime end = LocalTime.parse(hhmm(rs.getString("e")));
            if (!end.isAfter(start)) end = LocalTime.MAX;
            switch (kind) {
                case "CT" -> weekly.add(new WeeklyBusy(rs.getInt("dow"), start, end, BusyKind.TUTOR_CLASS));
                case "CL" -> weekly.add(new WeeklyBusy(rs.getInt("dow"), start, end, BusyKind.LEARNER_CLASS));
                case "LK" -> weekly.add(new WeeklyBusy(rs.getInt("dow"), start, end, BusyKind.TUTOR_COURSE));
                case "SA" -> dated.add(new DatedBusy(rs.getDate("d").toLocalDate(), start, end, BusyKind.TUTOR_COURSE));
                case "SP" -> dated.add(new DatedBusy(rs.getDate("d").toLocalDate(), start, end, BusyKind.PENDING_COURSE));
                case "TR" -> dated.add(new DatedBusy(rs.getDate("d").toLocalDate(), start, end, BusyKind.TRIAL));
                case "LA" -> dated.add(new DatedBusy(rs.getDate("d").toLocalDate(), start, end, BusyKind.LEARNER_COURSE));
                case "LW" -> weekly.add(new WeeklyBusy(rs.getInt("dow"), start, end, BusyKind.LEARNER_COURSE));
                case "LT" -> dated.add(new DatedBusy(rs.getDate("d").toLocalDate(), start, end, BusyKind.LEARNER_TRIAL));
                default -> { }
            }
        }, args.toArray());
        return new BusyIndex(weekly, dated);
    }

    private static String hhmm(String time) {
        return time != null && time.length() >= 5 ? time.substring(0, 5) : time;
    }
}
