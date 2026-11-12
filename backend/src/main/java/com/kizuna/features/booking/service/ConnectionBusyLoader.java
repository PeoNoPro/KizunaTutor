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
 * Hai chế độ:
 *  - {@link Mode#LEARNER_REQUEST}: học viên xem trước / gửi yêu cầu. Yêu cầu kết nối đang chờ (còn hạn) và buổi
 *    học thử đang chờ đều GIỮ CHỖ.
 *  - {@link Mode#TUTOR_ACCEPT}: gia sư bấm đồng ý. Chỉ tính những gì gia sư đã xác nhận; yêu cầu đang chờ khác
 *    không chặn (gia sư tự chọn nhận ai).
 */
@Component
@RequiredArgsConstructor
public class ConnectionBusyLoader {

    public enum Mode { LEARNER_REQUEST, TUTOR_ACCEPT }

    private static final UUID NONE = new UUID(0L, 0L);

    private final JdbcTemplate jdbcTemplate;

    public BusyIndex load(UUID tutorId, UUID learnerId, UUID excludeRequestId,
                          LocalDate from, LocalDate to, Mode mode) {
        UUID learner = learnerId == null ? NONE : learnerId;
        UUID exclude = excludeRequestId == null ? NONE : excludeRequestId;
        boolean learnerMode = mode == Mode.LEARNER_REQUEST;

        String sessionStatus = learnerMode
                ? "(cr.status = 'ACCEPTED' OR (cr.status = 'PENDING' AND cr.created_at > CURRENT_TIMESTAMP - (? * INTERVAL '1 hour')))"
                : "cr.status = 'ACCEPTED'";
        String trialStatus = learnerMode
                ? "tb.booking_status IN ('PENDING', 'CONFIRMED', 'RESCHEDULED') "
                  + "AND NOT (tb.booking_status = 'PENDING' AND tb.updated_at < CURRENT_TIMESTAMP - (? * INTERVAL '1 hour'))"
                : "tb.booking_status = 'CONFIRMED'";

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
                + "  AND " + trialStatus;

        List<Object> args = new ArrayList<>(List.of(tutorId, tutorId, learner, tutorId, exclude,
                tutorId, exclude, Date.valueOf(from), Date.valueOf(to)));
        if (learnerMode) args.add(BookingLimits.DIRECT_RESPONSE_HOURS);
        args.addAll(List.of(tutorId, learner, Date.valueOf(from), Date.valueOf(to)));
        if (learnerMode) args.add(BookingLimits.TRIAL_RESPONSE_HOURS);

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
                default -> { }
            }
        }, args.toArray());
        return new BusyIndex(weekly, dated);
    }

    private static String hhmm(String time) {
        return time != null && time.length() >= 5 ? time.substring(0, 5) : time;
    }
}
