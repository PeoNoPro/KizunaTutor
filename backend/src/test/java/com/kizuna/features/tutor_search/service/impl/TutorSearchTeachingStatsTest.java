package com.kizuna.features.tutor_search.service.impl;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TutorSearchTeachingStatsTest {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void countOccurredClassLessons_countsOnlySlotsStartedAfterCreationAndEndedBeforeCutoff() {
        ZonedDateTime createdMondayMorning = ZonedDateTime.of(2026, 11, 2, 10, 0, 0, 0, VN_ZONE);
        ZonedDateTime thursdayMorningCutoff = ZonedDateTime.of(2026, 11, 12, 2, 55, 0, 0, VN_ZONE);

        // Thứ Hai (1) 18:00-19:30 -> 2/11 và 9/11 đã kết thúc => 2 buổi
        assertEquals(2, TutorSearchServiceImpl.countOccurredClassLessons(
                1, LocalTime.of(18, 0), LocalTime.of(19, 30),
                createdMondayMorning, thursdayMorningCutoff));

        // Thứ Năm (4) 06:00-07:00 -> 5/11 đã kết thúc, 12/11 chưa tới giờ => 1 buổi
        assertEquals(1, TutorSearchServiceImpl.countOccurredClassLessons(
                4, LocalTime.of(6, 0), LocalTime.of(7, 0),
                createdMondayMorning, thursdayMorningCutoff));

        // Sau 07:05 ngày 12/11 -> cả 5/11 và 12/11 đã kết thúc => 2 buổi
        ZonedDateTime thursdayAfterLesson = ZonedDateTime.of(2026, 11, 12, 7, 5, 0, 0, VN_ZONE);
        assertEquals(2, TutorSearchServiceImpl.countOccurredClassLessons(
                4, LocalTime.of(6, 0), LocalTime.of(7, 0),
                createdMondayMorning, thursdayAfterLesson));
    }

    @Test
    void countOccurredClassLessons_ignoresSlotOnSameDayThatAlreadyStartedBeforeCreation() {
        ZonedDateTime createdMondayNight = ZonedDateTime.of(2026, 11, 9, 21, 0, 0, 0, VN_ZONE);
        ZonedDateTime thursdayCutoff = ZonedDateTime.of(2026, 11, 12, 2, 55, 0, 0, VN_ZONE);

        // Slot 03:30-05:35 vào Thứ Hai đã qua trước thời điểm tạo lớp 21:00 => 0 buổi
        assertEquals(0, TutorSearchServiceImpl.countOccurredClassLessons(
                1, LocalTime.of(3, 30), LocalTime.of(5, 35),
                createdMondayNight, thursdayCutoff));
    }

    @Test
    void countOccurredClassLessons_handlesInvalidOrEmptyRange() {
        ZonedDateTime start = ZonedDateTime.of(2026, 11, 10, 10, 0, 0, 0, VN_ZONE);
        ZonedDateTime earlier = start.minusHours(1);

        assertEquals(0, TutorSearchServiceImpl.countOccurredClassLessons(
                0, LocalTime.of(8, 0), LocalTime.of(9, 0), start, start.plusDays(7)));
        assertEquals(0, TutorSearchServiceImpl.countOccurredClassLessons(
                2, LocalTime.of(9, 0), LocalTime.of(8, 0), start, start.plusDays(7)));
        assertEquals(0, TutorSearchServiceImpl.countOccurredClassLessons(
                2, LocalTime.of(8, 0), LocalTime.of(9, 0), start, earlier));
    }
}
