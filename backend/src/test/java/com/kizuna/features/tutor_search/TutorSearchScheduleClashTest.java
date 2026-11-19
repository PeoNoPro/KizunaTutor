package com.kizuna.features.tutor_search;

import com.kizuna.features.tutor_search.service.impl.TutorSearchServiceImpl;
import com.kizuna.features.tutor_search.service.impl.TutorSearchServiceImpl.TimeInterval;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TutorSearchScheduleClashTest {

    @Test
    @DisplayName("Trùng giờ ở đầu ca rảnh: 18:00 - 20:00 trừ lịch lớp 18:00 - 19:00 còn lại 19:00 - 20:00")
    void testSubtractIntervalStartClash() {
        List<TimeInterval> avail = List.of(new TimeInterval(18 * 60, 20 * 60)); // 1080 -> 1200
        List<TimeInterval> busyClass = List.of(new TimeInterval(18 * 60, 19 * 60)); // 1080 -> 1140

        List<TimeInterval> remaining = TutorSearchServiceImpl.subtractIntervals(avail, busyClass, 30);

        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).startMinutes()).isEqualTo(19 * 60);
        assertThat(remaining.get(0).endMinutes()).isEqualTo(20 * 60);
    }

    @Test
    @DisplayName("Trùng giờ ở giữa ca rảnh: 18:00 - 21:00 trừ lịch lớp 19:00 - 20:00 còn lại hai ca 18:00 - 19:00 và 20:00 - 21:00")
    void testSubtractIntervalMiddleClash() {
        List<TimeInterval> avail = List.of(new TimeInterval(18 * 60, 21 * 60));
        List<TimeInterval> busyClass = List.of(new TimeInterval(19 * 60, 20 * 60));

        List<TimeInterval> remaining = TutorSearchServiceImpl.subtractIntervals(avail, busyClass, 30);

        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).startMinutes()).isEqualTo(18 * 60);
        assertThat(remaining.get(0).endMinutes()).isEqualTo(19 * 60);
        assertThat(remaining.get(1).startMinutes()).isEqualTo(20 * 60);
        assertThat(remaining.get(1).endMinutes()).isEqualTo(21 * 60);
    }

    @Test
    @DisplayName("Trùng toàn bộ ca rảnh: 18:00 - 19:00 trừ lịch lớp 18:00 - 19:00 thì không còn giờ rảnh")
    void testSubtractIntervalFullClash() {
        List<TimeInterval> avail = List.of(new TimeInterval(18 * 60, 19 * 60));
        List<TimeInterval> busyClass = List.of(new TimeInterval(18 * 60, 19 * 60));

        List<TimeInterval> remaining = TutorSearchServiceImpl.subtractIntervals(avail, busyClass, 30);

        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Khoảng thời gian còn lại nhỏ hơn 30 phút bị loại bỏ (không đủ tối thiểu 1 buổi học thử)")
    void testSubtractIntervalPruneSliver() {
        List<TimeInterval> avail = List.of(new TimeInterval(18 * 60, 19 * 60)); // 60 phút
        List<TimeInterval> busyClass = List.of(new TimeInterval(18 * 60 + 20, 19 * 60)); // 18:20 -> 19:00
        // Phần trước là 18:00 - 18:20 (20 phút < 30 phút) -> Bị loại bỏ

        List<TimeInterval> remaining = TutorSearchServiceImpl.subtractIntervals(avail, busyClass, 30);

        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Không trùng giờ: giữ nguyên ca rảnh ban đầu")
    void testSubtractIntervalNoClash() {
        List<TimeInterval> avail = List.of(new TimeInterval(18 * 60, 20 * 60));
        List<TimeInterval> busyClass = List.of(new TimeInterval(8 * 60, 10 * 60));

        List<TimeInterval> remaining = TutorSearchServiceImpl.subtractIntervals(avail, busyClass, 30);

        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).startMinutes()).isEqualTo(18 * 60);
        assertThat(remaining.get(0).endMinutes()).isEqualTo(20 * 60);
    }
}

