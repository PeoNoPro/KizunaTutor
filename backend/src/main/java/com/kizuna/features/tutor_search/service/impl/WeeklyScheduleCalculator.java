package com.kizuna.features.tutor_search.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tính khung giờ còn trống của gia sư cho từng ngày trong một tuần cụ thể (không truy cập DB).
 *
 * Quy ước thứ: 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy (giống tutor_availabilities).
 * Một khoảng giờ của ngày D bị loại khi:
 *  - trùng lịch lớp chính thức / ca hằng tuần đã có học viên (lặp lại mọi tuần);
 *  - trùng lịch bận đúng ngày D: buổi học thử, buổi của khóa kết nối (đã xác nhận hoặc đang chờ duyệt);
 *  - đã qua thời điểm hiện tại.
 * Chỉ giữ các khoảng đủ dài cho ca ngắn nhất ({@code minDuration} phút).
 */
public final class WeeklyScheduleCalculator {

    public static final int DAY_MINUTES = 24 * 60;
    private static final int STEP = 30;

    private WeeklyScheduleCalculator() {
    }

    /** Khoảng giờ hằng tuần (phút tính từ 00:00). */
    public record WeeklyRange(int dayOfWeek, int start, int end) {
    }

    /** Lịch bận vào một ngày cụ thể (buổi học thử, buổi của khóa kết nối) (phút tính từ 00:00, giờ Việt Nam). */
    public record DatedRange(LocalDate date, int start, int end) {
    }

    public record Range(int start, int end) {
    }

    public record DaySlots(LocalDate date, int dayOfWeek, List<Range> freeRanges) {
    }

    /** 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy. */
    public static int dayOfWeekOf(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7 + 1;
    }

    public static List<DaySlots> compute(LocalDate weekStart,
                                         List<WeeklyRange> availabilities,
                                         List<WeeklyRange> weeklyBusy,
                                         List<DatedRange> trials,
                                         LocalDateTime now,
                                         int minDuration) {
        List<DaySlots> days = new ArrayList<>(7);
        LocalDate today = now.toLocalDate();
        int nowCut = ((now.getHour() * 60 + now.getMinute() + STEP - 1) / STEP) * STEP;
        if (now.getSecond() > 0 && (now.getHour() * 60 + now.getMinute()) % STEP == 0) {
            nowCut += STEP; // đúng mốc 18:00:20 thì 18:00 đã qua
        }

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            int dow = dayOfWeekOf(date);
            if (date.isBefore(today)) {
                days.add(new DaySlots(date, dow, List.of()));
                continue;
            }

            List<Range> free = new ArrayList<>();
            for (WeeklyRange a : availabilities) {
                if (a.dayOfWeek() == dow) free.add(new Range(a.start(), Math.min(a.end(), DAY_MINUTES)));
            }
            free = merge(free);

            List<Range> busy = new ArrayList<>();
            for (WeeklyRange b : weeklyBusy) {
                if (b.dayOfWeek() == dow) busy.add(new Range(b.start(), b.end()));
            }
            for (DatedRange t : trials) {
                if (t.date().equals(date)) busy.add(new Range(t.start(), t.end()));
            }
            if (date.equals(today)) busy.add(new Range(0, nowCut));

            for (Range b : busy) free = subtract(free, b);
            List<Range> result = free.stream().filter(r -> r.end() - r.start() >= minDuration).toList();
            days.add(new DaySlots(date, dow, result));
        }
        return days;
    }

    static List<Range> merge(List<Range> ranges) {
        List<Range> sorted = ranges.stream()
                .filter(r -> r.end() > r.start())
                .sorted(Comparator.comparingInt(Range::start))
                .toList();
        List<Range> out = new ArrayList<>();
        for (Range r : sorted) {
            if (!out.isEmpty() && r.start() <= out.get(out.size() - 1).end()) {
                Range last = out.remove(out.size() - 1);
                out.add(new Range(last.start(), Math.max(last.end(), r.end())));
            } else {
                out.add(r);
            }
        }
        return out;
    }

    static List<Range> subtract(List<Range> free, Range busy) {
        if (busy.end() <= busy.start()) return free;
        List<Range> out = new ArrayList<>();
        for (Range f : free) {
            if (busy.end() <= f.start() || busy.start() >= f.end()) {
                out.add(f);
                continue;
            }
            if (busy.start() > f.start()) out.add(new Range(f.start(), busy.start()));
            if (busy.end() < f.end()) out.add(new Range(busy.end(), f.end()));
        }
        return out;
    }
}
