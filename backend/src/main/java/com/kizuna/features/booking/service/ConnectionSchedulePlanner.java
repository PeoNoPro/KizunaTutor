package com.kizuna.features.booking.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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
    public record PlannedSession(LocalDate date, int dayOfWeek, LocalTime start, LocalTime end) {
    }

    /** Vì sao một khung giờ bị kín. */
    public enum BusyKind {
        TUTOR_CLASS("trùng lịch dạy lớp chính thức của gia sư", "trùng lịch dạy lớp chính thức của bạn"),
        LEARNER_CLASS("trùng lịch học lớp chính thức của bạn", "trùng lịch học lớp chính thức của học viên"),
        TUTOR_COURSE("gia sư đã có buổi học với học viên khác", "bạn đã có buổi học với học viên khác"),
        PENDING_COURSE("đang có học viên khác chờ gia sư duyệt giờ này", "đang có yêu cầu kết nối khác chờ bạn duyệt giờ này"),
        TRIAL("gia sư đã có lịch học thử", "bạn đã có lịch học thử với học viên khác"),
        // Lịch ĐÃ XÁC NHẬN của chính học viên với gia sư khác (yêu cầu đang chờ với gia sư khác không tính)
        LEARNER_COURSE("bạn đã có buổi học với gia sư khác vào giờ này", "học viên đã có buổi học với gia sư khác vào giờ này"),
        LEARNER_TRIAL("bạn đã có buổi học thử với gia sư khác vào giờ này", "học viên đã có buổi học thử với gia sư khác vào giờ này");

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

    /** Hai khoảng giờ trong cùng một ngày có chồng lên nhau không (dùng ngoài package). */
    public static boolean overlapsTimes(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return overlaps(aStart, aEnd, bStart, bEnd);
    }

    static boolean overlaps(LocalTime aStart, LocalTime aEnd, LocalTime bStart, LocalTime bEnd) {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart);
    }
}
