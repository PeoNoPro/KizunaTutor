package com.kizuna.features.classroom;

import com.kizuna.features.classroom.entity.ClassExtraSession;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.ClassSessionChange;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.classroom.util.ClassSessionCalendar.Session;
import com.kizuna.features.classroom.util.ClassSessionCalendar.SpaceData;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Sinh lịch buổi học cụ thể của khóa (hàm thuần, không cần DB). */
class ClassSessionCalendarTest {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate START = LocalDate.of(2026, 11, 4); // Thứ Tư
    private static final LocalDate FAR = LocalDate.of(2030, 1, 1);

    private LearningSpace space;
    private ClassSchedule monday;
    private ClassSchedule thursday;
    private final List<ClassSessionChange> changes = new ArrayList<>();
    private final List<ClassExtraSession> extras = new ArrayList<>();

    @BeforeEach
    void setUp() {
        space = space(tutor(TutorMembershipStatus.PREMIUM), learner(), LearningSpaceStatus.ACTIVE);
        space.setCourseStartDate(START);
        space.setTotalSessions(5);
        OffsetDateTime from = START.atStartOfDay(VN).toOffsetDateTime();
        monday = slot(1, 19, from);
        thursday = slot(4, 6, from);
    }

    private ClassSchedule slot(int day, int hour, OffsetDateTime activeFrom) {
        return ClassSchedule.builder().scheduleId(UUID.randomUUID()).space(space).dayOfWeek(day)
                .startTime(LocalTime.of(hour, 0)).endTime(LocalTime.of(hour + 1, 0)).activeFrom(activeFrom).build();
    }

    private List<Session> generate() {
        return ClassSessionCalendar.generate(new SpaceData(space, List.of(monday, thursday), changes, extras), FAR);
    }

    private static List<LocalDate> dates(List<Session> list) {
        return list.stream().map(Session::date).toList();
    }

    @Test
    void stopsExactlyAtTotalSessionsAndSkipsDaysBeforeStartDate() {
        // Bắt đầu Thứ Tư 04/11: Thứ Hai 02/11 không tính
        assertThat(dates(generate())).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 19));
        assertThat(generate()).extracting(Session::number).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void oldClassWithoutTotalRepeatsUntilRequestedDate() {
        space.setTotalSessions(null);
        List<Session> list = ClassSessionCalendar.generate(
                new SpaceData(space, List.of(monday, thursday), changes, extras), LocalDate.of(2026, 11, 30));
        // Sinh tới hết tuần chứa 30/11 (Thứ Hai) → đến Thứ Năm 03/12
        assertThat(list).hasSize(9);
        assertThat(list.get(list.size() - 1).date()).isEqualTo(LocalDate.of(2026, 12, 3));
    }

    @Test
    void cancelledSessionIsNotCountedSoCourseIsExtended() {
        changes.add(ClassSessionChange.builder().schedule(thursday).originalDate(LocalDate.of(2026, 11, 12)).cancelled(true).build());
        assertThat(dates(generate())).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 23));
    }

    @Test
    void extraSessionCountsSoCourseEndsEarlier() {
        extras.add(ClassExtraSession.builder().extraId(UUID.randomUUID()).space(space)
                .sessionDate(LocalDate.of(2026, 11, 7)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0)).build());
        List<Session> list = generate();
        assertThat(dates(list)).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 9),
                LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 16));
        assertThat(list.get(1).extra()).isTrue();
    }

    @Test
    void movedSessionUsesNewDateAndKeepsOrderInsideWeek() {
        changes.add(ClassSessionChange.builder().schedule(thursday).originalDate(LocalDate.of(2026, 11, 12))
                .newDate(LocalDate.of(2026, 11, 14)).startTime(LocalTime.of(15, 0)).endTime(LocalTime.of(16, 0)).build());
        Session moved = generate().get(2);
        assertThat(moved.moved()).isTrue();
        assertThat(moved.date()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(moved.originalDate()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void removedWeeklySlotKeepsPastSessionsAndCourseIsExtendedWithRemainingSlot() {
        // Xóa buổi Thứ Hai từ 10/11 trở đi: Thứ Hai 09/11 đã học vẫn được tính
        monday.setActiveUntil(LocalDate.of(2026, 11, 10).atStartOfDay(VN).toOffsetDateTime());
        assertThat(dates(generate())).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 12),
                LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 26));
    }

    @Test
    void progressCountsStartedSessionsAndEndDate() {
        SpaceData data = new SpaceData(space, List.of(monday, thursday), changes, extras);
        ClassSessionCalendar.Progress p = ClassSessionCalendar.progress(data, LocalDateTime.of(2026, 11, 12, 8, 0));
        assertThat(p.startedSessions()).isEqualTo(3);
        assertThat(p.totalSessions()).isEqualTo(5);
        assertThat(p.endDate()).isEqualTo(LocalDate.of(2026, 11, 19));
    }
}
