package com.kizuna.features.booking.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Xếp các buổi học cụ thể cho yêu cầu kết nối và kiểm tra trùng lịch TỪNG buổi (không truy cập DB).
 * Quy ước thứ: 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy (giống tutor_availabilities / connection_request_slots).
 */
public final class ConnectionSchedulePlanner {

    private ConnectionSchedulePlanner() {
    }

    /** Ca hằng tuần học viên chọn. */
    public record WeeklySlot(int dayOfWeek, LocalTime start, LocalTime end) {
    }

    /** Một buổi học cụ thể. */
    public record PlannedSession(LocalDate date, int dayOfWeek, LocalTime start, LocalTime end) {
    }

    /** Vì sao một khung giờ bị kín. */
    public enum BusyKind {
        TUTOR_CLASS("trùng lịch dạy lớp chính thức của gia sư", "trùng lịch dạy lớp chính thức của bạn"),
        LEARNER_CLASS("trùng lịch học lớp chính thức của bạn", "trùng lịch học lớp chính thức của học viên"),
        TUTOR_COURSE("gia sư đã có buổi học với học viên khác", "bạn đã có buổi học với học viên khác"),
        PENDING_COURSE("đang có học viên khác chờ gia sư duyệt giờ này", "đang có yêu cầu kết nối khác chờ bạn duyệt giờ này"),
        TRIAL("gia sư đã có lịch học thử", "bạn đã có lịch học thử với học viên khác");

        private final String learnerText;
        private final String tutorText;

        BusyKind(String learnerText, String tutorText) {
            this.learnerText = learnerText;
            this.tutorText = tutorText;
        }

        public String learnerText() {
            return learnerText;
        }

        public String tutorText() {
            return tutorText;
        }
    }

    /** Lịch bận lặp lại mọi tuần (lịch lớp chính thức, ca đã khóa kiểu cũ). */
    public record WeeklyBusy(int dayOfWeek, LocalTime start, LocalTime end, BusyKind kind) {
    }

    /** Lịch bận vào một ngày cụ thể (buổi học thử, buổi của khóa học khác). */
    public record DatedBusy(LocalDate date, LocalTime start, LocalTime end, BusyKind kind) {
    }

    /** Toàn bộ lịch bận của gia sư (và học viên) trong một khoảng ngày. */
    public record BusyIndex(List<WeeklyBusy> weekly, List<DatedBusy> dated) {
        public static final BusyIndex EMPTY = new BusyIndex(List.of(), List.of());

        public Optional<BusyKind> conflictOf(PlannedSession s) {
            for (WeeklyBusy w : weekly) {
                if (w.dayOfWeek() == s.dayOfWeek() && overlaps(s.start(), s.end(), w.start(), w.end())) {
                    return Optional.of(w.kind());
                }
            }
            for (DatedBusy d : dated) {
                if (d.date().equals(s.date()) && overlaps(s.start(), s.end(), d.start(), d.end())) {
                    return Optional.of(d.kind());
                }
            }
            return Optional.empty();
        }

        public boolean allFree(List<PlannedSession> sessions) {
            return sessions.stream().allMatch(s -> conflictOf(s).isEmpty());
        }
    }

    /** 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy. */
    public static int dayOfWeekOf(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7 + 1;
    }

    public static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }

    /**
     * Xếp {@code total} buổi theo thứ tự thời gian, bắt đầu từ {@code startDate}, bỏ qua buổi đã qua thời điểm {@code now}.
     * Mỗi tuần có ít nhất một ca nên tối đa {@code total} tuần là đủ.
     */
    public static List<PlannedSession> plan(List<WeeklySlot> slots, LocalDate startDate, int total, LocalDateTime now) {
        List<PlannedSession> result = new ArrayList<>();
        if (slots == null || slots.isEmpty() || total <= 0) return result;
        List<WeeklySlot> sorted = slots.stream()
                .sorted(Comparator.comparing(WeeklySlot::start))
                .toList();
        LocalDate date = startDate;
        LocalDate limit = startDate.plusWeeks(total + 1L);
        while (result.size() < total && !date.isAfter(limit)) {
            int dow = dayOfWeekOf(date);
            for (WeeklySlot s : sorted) {
                if (s.dayOfWeek() != dow) continue;
                if (!date.atTime(s.start()).isAfter(now)) continue;
                result.add(new PlannedSession(date, dow, s.start(), s.end()));
                if (result.size() == total) break;
            }
            date = date.plusDays(1);
        }
        return result;
    }

    /**
     * Tuần bắt đầu sớm nhất SAU tuần của {@code requestedStart} (không muộn hơn {@code lastWeekStart}) mà tất cả
     * các buổi đều trống. Trả về Thứ Hai của tuần đó.
     */
    public static Optional<LocalDate> suggestStartWeek(List<WeeklySlot> slots, LocalDate requestedStart, int total,
                                                       LocalDateTime now, LocalDate lastWeekStart, BusyIndex busy) {
        LocalDate today = now.toLocalDate();
        for (LocalDate monday = mondayOf(requestedStart).plusWeeks(1);
             !monday.isAfter(lastWeekStart);
             monday = monday.plusWeeks(1)) {
            LocalDate from = monday.isBefore(today) ? today : monday;
            List<PlannedSession> sessions = plan(slots, from, total, now);
            if (sessions.size() == total && busy.allFree(sessions)) {
                return Optional.of(monday);
            }
        }
        return Optional.empty();
    }
}
