package com.kizuna.features.classroom;

import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** FR-6.1.2 / FR-6.2.2 / FR-6.5.2: quy tắc một buổi học và danh sách buổi học hằng tuần (giá trị biên). */
class ClassScheduleRulesTest {

    private static ClassScheduleRequest slot(int day, String start, String end) {
        return new ClassScheduleRequest(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static void assertScheduleError(Runnable call, String field, String messagePart) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ClassroomException.class, e ->
                assertThat(e.getFieldErrors()).hasEntrySatisfying(field, m -> assertThat(m).contains(messagePart)));
    }

    @ParameterizedTest(name = "buổi dài {0} phút hợp lệ")
    @ValueSource(ints = {60, 90, 120})
    void sessionBetween60And120MinutesIsAccepted(int minutes) {
        LocalTime start = LocalTime.of(18, 0);
        assertThatCode(() -> ClassScheduleRules.validateSlot(new ClassScheduleRequest(3, start, start.plusMinutes(minutes))))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "buổi dài {0} phút bị từ chối")
    @ValueSource(ints = {30, 59, 121, 240})
    void sessionShorterThan60OrLongerThan120MinutesIsRejected(int minutes) {
        LocalTime start = LocalTime.of(8, 0);
        assertScheduleError(() -> ClassScheduleRules.validateSlot(new ClassScheduleRequest(3, start, start.plusMinutes(minutes))),
                ClassScheduleRules.FIELD, "60 phút đến 120 phút");
    }

    @Test
    void dayMustBeMondayToSundayAndEndAfterStart() {
        assertScheduleError(() -> ClassScheduleRules.validateSlot(slot(0, "18:00", "19:00")), ClassScheduleRules.FIELD, "thứ");
        assertScheduleError(() -> ClassScheduleRules.validateSlot(slot(8, "18:00", "19:00")), ClassScheduleRules.FIELD, "thứ");
        assertScheduleError(() -> ClassScheduleRules.validateSlot(slot(2, "19:00", "18:00")), ClassScheduleRules.FIELD, "sau giờ bắt đầu");
        assertScheduleError(() -> ClassScheduleRules.validateSlot(new ClassScheduleRequest(2, null, LocalTime.NOON)),
                ClassScheduleRules.FIELD, "giờ bắt đầu");
        assertThatCode(() -> ClassScheduleRules.validateSlot(slot(1, "07:00", "08:00"))).doesNotThrowAnyException();
        assertThatCode(() -> ClassScheduleRules.validateSlot(slot(7, "21:00", "22:30"))).doesNotThrowAnyException();
    }

    @Test
    void newClassNeedsOneToFourteenSlotsWithoutOverlap() {
        assertScheduleError(() -> ClassScheduleRules.validateNewList(List.of()), ClassScheduleRules.FIELD, "ít nhất một buổi");
        assertScheduleError(() -> ClassScheduleRules.validateNewList(null), ClassScheduleRules.FIELD, "ít nhất một buổi");

        List<ClassScheduleRequest> fourteen = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            fourteen.add(slot(day, "08:00", "09:00"));
            fourteen.add(slot(day, "19:00", "20:00"));
        }
        assertThatCode(() -> ClassScheduleRules.validateNewList(fourteen)).doesNotThrowAnyException();
        List<ClassScheduleRequest> fifteen = new ArrayList<>(fourteen);
        fifteen.add(slot(1, "13:00", "14:00"));
        assertScheduleError(() -> ClassScheduleRules.validateNewList(fifteen), ClassScheduleRules.FIELD, "tối đa 14");

        // Trùng giờ trong cùng một ngày bị từ chối; nối tiếp nhau (19:00 kết thúc – 19:00 bắt đầu) thì được
        assertScheduleError(() -> ClassScheduleRules.validateNewList(List.of(slot(2, "18:00", "19:30"), slot(2, "19:00", "20:00"))),
                ClassScheduleRules.FIELD, "trùng giờ");
        assertThatCode(() -> ClassScheduleRules.validateNewList(List.of(slot(2, "18:00", "19:00"), slot(2, "19:00", "20:00"))))
                .doesNotThrowAnyException();
    }

    @Test
    void newTimesMustBeOnThe30MinuteStepButKeptOldTimesAreAccepted() {
        assertThatCode(() -> ClassScheduleRules.requireStep(ClassScheduleRules.FIELD, LocalTime.of(18, 0), LocalTime.of(19, 30)))
                .doesNotThrowAnyException();
        assertScheduleError(() -> ClassScheduleRules.requireStep(ClassScheduleRules.FIELD, LocalTime.of(18, 15), LocalTime.of(19, 30)),
                ClassScheduleRules.FIELD, "nấc 30 phút");
        assertScheduleError(() -> ClassScheduleRules.requireStep(ClassScheduleRules.SESSION_FIELD, LocalTime.of(18, 0), LocalTime.of(19, 5)),
                ClassScheduleRules.SESSION_FIELD, "19:05");

        // Lớp cũ có giờ lẻ 08:03–10:03: giữ nguyên giờ thì vẫn sửa được, đổi sang giờ lẻ khác thì bị chặn
        LocalTime oldStart = LocalTime.of(8, 3), oldEnd = LocalTime.of(10, 3);
        assertThatCode(() -> ClassScheduleRules.requireStep(ClassScheduleRules.FIELD, oldStart, oldEnd, oldStart, oldEnd))
                .doesNotThrowAnyException();
        assertThatCode(() -> ClassScheduleRules.requireStep(ClassScheduleRules.FIELD, oldStart, LocalTime.of(9, 30), oldStart, oldEnd))
                .doesNotThrowAnyException();
        assertScheduleError(() -> ClassScheduleRules.requireStep(ClassScheduleRules.FIELD, LocalTime.of(8, 7), oldEnd, oldStart, oldEnd),
                ClassScheduleRules.FIELD, "08:07");
    }

    @Test
    void newClassWithOffStepTimeIsRejected() {
        assertScheduleError(() -> ClassScheduleRules.validateNewList(List.of(slot(2, "18:00", "19:00"), slot(4, "18:10", "19:10"))),
                ClassScheduleRules.FIELD, "nấc 30 phút");
    }

    @Test
    void movedOrExtraSessionFollowsTheSameDurationRuleButReportsOnTheSessionField() {
        LocalDate day = LocalDate.of(2026, 11, 24);
        assertThatCode(() -> ClassScheduleRules.validateSession(day, LocalTime.of(18, 0), LocalTime.of(20, 0)))
                .doesNotThrowAnyException();
        assertScheduleError(() -> ClassScheduleRules.validateSession(day, LocalTime.of(18, 0), LocalTime.of(18, 45)),
                ClassScheduleRules.SESSION_FIELD, "60 phút đến 120 phút");
        assertScheduleError(() -> ClassScheduleRules.validateSession(null, LocalTime.of(18, 0), LocalTime.of(19, 0)),
                ClassScheduleRules.SESSION_FIELD, "chọn ngày");
    }
}
