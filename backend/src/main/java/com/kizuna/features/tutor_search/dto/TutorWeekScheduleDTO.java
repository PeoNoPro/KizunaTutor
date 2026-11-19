package com.kizuna.features.tutor_search.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/** Lịch còn trống của gia sư trong một tuần cụ thể (chỉ gồm khung giờ học viên có thể chọn). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorWeekScheduleDTO {

    /** Thứ Hai của tuần đang xem. */
    private LocalDate weekStart;
    /** Chủ nhật của tuần đang xem. */
    private LocalDate weekEnd;
    /** Tuần sớm nhất được xem (tuần hiện tại). */
    private LocalDate minWeekStart;
    /** Tuần muộn nhất được xem. */
    private LocalDate maxWeekStart;

    private List<Day> days;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Day {
        private LocalDate date;
        /** 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy */
        private Integer dayOfWeek;
        private String dayName;
        private List<TimeRange> freeRanges;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeRange {
        /** "HH:mm" */
        private String startTime;
        /** "HH:mm" (có thể là "24:00") */
        private String endTime;
    }
}
