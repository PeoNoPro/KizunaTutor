package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassScheduleResponse;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.ClassSessionChangeRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.service.impl.ClassScheduleServiceImpl;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ClassScheduleServiceImplTest {

    private ClassroomService classroomService;
    private ClassScheduleRepository schedules;
    private LearningSpaceRepository spaces;
    private ClassSessionChangeRepository changes;
    private ClassScheduleServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private ClassSchedule monday;

    @BeforeEach
    void setUp() {
        classroomService = mock(ClassroomService.class);
        schedules = mock(ClassScheduleRepository.class);
        spaces = mock(LearningSpaceRepository.class);
        changes = mock(ClassSessionChangeRepository.class);
        service = new ClassScheduleServiceImpl(classroomService, schedules,
                new ClassScheduleRules(schedules, calendar(spaces, schedules)), spaces, changes);
        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        space = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        monday = slot(space, 1, 19, 0, 20, 30);
        when(classroomService.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(schedules.findByScheduleIdAndSpace_SpaceId(monday.getScheduleId(), space.getSpaceId())).thenReturn(Optional.of(monday));
        when(schedules.save(any(ClassSchedule.class))).thenAnswer(inv -> {
            ClassSchedule s = inv.getArgument(0);
            if (s.getScheduleId() == null) s.setScheduleId(UUID.randomUUID());
            return s;
        });
    }

    private static ClassSchedule slot(LearningSpace space, int day, int sh, int sm, int eh, int em) {
        // Buổi hằng tuần đã có hiệu lực từ 4 tuần trước (đã có buổi diễn ra)
        return ClassSchedule.builder().scheduleId(UUID.randomUUID()).space(space).dayOfWeek(day)
                .startTime(LocalTime.of(sh, sm)).endTime(LocalTime.of(eh, em))
                .activeFrom(java.time.OffsetDateTime.now().minusWeeks(4)).build();
    }

    private static ClassScheduleRequest req(int day, int sh, int sm, int eh, int em) {
        return new ClassScheduleRequest(day, LocalTime.of(sh, sm), LocalTime.of(eh, em));
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void learnerAndTutorCanViewSchedule() {
        when(schedules.findBySpace_SpaceIdOrderByDayOfWeekAscStartTimeAsc(space.getSpaceId())).thenReturn(List.of(monday));
        List<ClassScheduleResponse> r = service.list(asLearner(learner), space.getSpaceId());
        assertThat(r).extracting(ClassScheduleResponse::dayOfWeek).containsExactly(1);
    }

    @Test
    void tutorUpdatesSlotTimeAndDay() {
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday));

        ClassScheduleResponse r = service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(3, 18, 0, 19, 30));

        assertThat(r.dayOfWeek()).isEqualTo(3);
        assertThat(r.startTime()).isEqualTo(LocalTime.of(18, 0));
        // Lịch cũ đã có buổi diễn ra: giữ nguyên cho các buổi đã qua, kết thúc hiệu lực từ bây giờ
        assertThat(monday.getStartTime()).isEqualTo(LocalTime.of(19, 0));
        assertThat(monday.getActiveUntil()).isNotNull();
        verify(changes).deleteBySchedule_ScheduleIdAndOriginalDateGreaterThanEqual(eq(monday.getScheduleId()), any());
    }

    @Test
    void slotNotStartedYetIsUpdatedInPlace() {
        monday.setActiveFrom(java.time.OffsetDateTime.now().plusDays(3));
        ClassScheduleResponse r = service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(3, 18, 0, 19, 30));
        assertThat(r.scheduleId()).isEqualTo(monday.getScheduleId());
        assertThat(monday.getDayOfWeek()).isEqualTo(3);
        assertThat(monday.getActiveUntil()).isNull();
    }

    @Test
    void updatingSlotIgnoresItselfWhenCheckingOverlap() {
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday));
        // Lùi 30 phút trong cùng ngày: trùng với chính nó nhưng không được tính là trùng lịch.
        assertThat(service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(1, 19, 30, 21, 0))
                .startTime()).isEqualTo(LocalTime.of(19, 30));
    }

    @Test
    void updateRejectsOverlapWithAnotherSlotOfSameClass() {
        ClassSchedule wednesday = slot(space, 3, 19, 0, 20, 0);
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday, wednesday));
        assertStatus(() -> service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(3, 19, 30, 20, 30)),
                HttpStatus.CONFLICT);
    }

    @Test
    void learnerOverlapWithOtherTutorDoesNotLeakClassName() {
        LearningSpace otherTutorsClass = space(tutor(TutorMembershipStatus.PREMIUM), learner, LearningSpaceStatus.ACTIVE);
        otherTutorsClass.setTitle("Lớp bí mật");
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any()))
                .thenReturn(List.of(monday, slot(otherTutorsClass, 5, 8, 0, 9, 0)));

        assertThatThrownBy(() -> service.add(asTutor(tutor), space.getSpaceId(), req(5, 8, 30, 9, 30)))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("Học viên đã có lớp học khác")
                .hasMessageNotContaining("Lớp bí mật");
        verify(schedules, never()).save(any());
    }

    @Test
    void conflictIsReportedAsScheduleFieldErrorAndClassIsLockedFirst() {
        ClassSchedule wednesday = slot(space, 3, 19, 0, 20, 0);
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday, wednesday));

        assertThatThrownBy(() -> service.add(asTutor(tutor), space.getSpaceId(), req(3, 19, 30, 20, 30)))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getFieldErrors()).containsKey(ClassScheduleRules.FIELD);
                });
        var order = inOrder(spaces, schedules);
        order.verify(spaces).lockById(space.getSpaceId());
        order.verify(schedules).findOpenSchedulesOfParticipants(any(), any(), any());
    }

    @Test
    void slotOverlappingConfirmedTrialIsRejected() {
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday));
        // Buổi học thử của chính gia sư: Thứ Bảy 09:00–09:50
        when(schedules.findUpcomingTrialSlots(tutor.getTutorId(), learner.getLearnerId())).thenReturn(List.<Object[]>of(
                new Object[]{tutor.getTutorId().toString(), 6, "09:00", "09:50", "14/11/2026"}));

        assertThatThrownBy(() -> service.add(asTutor(tutor), space.getSpaceId(), req(6, 9, 30, 10, 30)))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("buổi học thử của bạn").contains("14/11/2026");
                    assertThat(e.getFieldErrors()).containsKey(ClassScheduleRules.FIELD);
                });
        // Sát giờ (bắt đầu đúng lúc học thử kết thúc) thì không tính là trùng
        assertThat(service.add(asTutor(tutor), space.getSpaceId(), req(6, 9, 50, 10, 50)).dayOfWeek()).isEqualTo(6);
    }

    @Test
    void timetableShowsLearnerNameForTutorAndTutorNameForLearner() {
        when(schedules.findTimetable(eq(tutor.getTutorId()), isNull(), any())).thenReturn(List.of(monday));
        when(schedules.findTimetable(isNull(), eq(learner.getLearnerId()), any())).thenReturn(List.of(monday));

        var forTutor = service.timetable(asTutor(tutor));
        var forLearner = service.timetable(asLearner(learner));

        assertThat(forTutor).singleElement().satisfies(e -> {
            assertThat(e.partnerName()).isEqualTo("Học viên B");
            assertThat(e.classTitle()).isEqualTo("Lớp N4");
            assertThat(e.dayOfWeek()).isEqualTo(1);
        });
        assertThat(forLearner).singleElement().satisfies(e -> assertThat(e.partnerName()).isEqualTo("Gia sư A"));
    }

    @Test
    void tutorDeletesSlotWhenClassKeepsAtLeastOne() {
        when(schedules.countBySpace_SpaceId(space.getSpaceId())).thenReturn(2L);
        service.delete(asTutor(tutor), space.getSpaceId(), monday.getScheduleId());
        // Xóa ở mọi tuần từ bây giờ; các buổi đã diễn ra vẫn được giữ (lịch hết hiệu lực chứ không xóa hẳn)
        assertThat(monday.getActiveUntil()).isNotNull();
        verify(schedules, never()).delete(monday);
        verify(changes).deleteBySchedule_ScheduleIdAndOriginalDateGreaterThanEqual(eq(monday.getScheduleId()), any());
        verify(spaces).lockById(space.getSpaceId());
    }

    @Test
    void slotNotStartedYetIsDeletedCompletely() {
        when(schedules.countBySpace_SpaceId(space.getSpaceId())).thenReturn(2L);
        monday.setActiveFrom(java.time.OffsetDateTime.now().plusDays(3));
        service.delete(asTutor(tutor), space.getSpaceId(), monday.getScheduleId());
        verify(schedules).delete(monday);
    }

    @Test
    void cannotDeleteLastSlotOrAsLearner() {
        when(schedules.countBySpace_SpaceId(space.getSpaceId())).thenReturn(1L);
        assertThatThrownBy(() -> service.delete(asTutor(tutor), space.getSpaceId(), monday.getScheduleId()))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getFieldErrors()).containsKey(ClassScheduleRules.FIELD);
                });
        assertStatus(() -> service.delete(asLearner(learner), space.getSpaceId(), monday.getScheduleId()), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.delete(asTutor(tutor), space.getSpaceId(), UUID.randomUUID()), HttpStatus.NOT_FOUND);
        verify(schedules, never()).delete(any(ClassSchedule.class));
    }

    @Test
    void cannotAddMoreWeeklySlotsThanTotalSessionsOfCourse() {
        space.setTotalSessions(1);
        when(schedules.countBySpace_SpaceId(space.getSpaceId())).thenReturn(1L);
        assertThatThrownBy(() -> service.add(asTutor(tutor), space.getSpaceId(), req(6, 9, 0, 10, 0)))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getMessage()).contains("tăng tổng số buổi");
                });
        verify(schedules, never()).save(any());
    }

    @Test
    void tutorAddsSlot() {
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any())).thenReturn(List.of(monday));
        ClassScheduleResponse r = service.add(asTutor(tutor), space.getSpaceId(), req(6, 9, 0, 10, 30));
        assertThat(r.dayOfWeek()).isEqualTo(6);
        verify(schedules).save(any(ClassSchedule.class));
    }

    @Test
    void addRejectedWhenClassAlreadyHasMaxSlots() {
        when(schedules.countBySpace_SpaceId(space.getSpaceId())).thenReturn((long) ClassScheduleRules.MAX_SLOTS);
        assertStatus(() -> service.add(asTutor(tutor), space.getSpaceId(), req(6, 9, 0, 10, 0)), HttpStatus.CONFLICT);
    }

    @Test
    void onlyTutorOfActiveClassCanEdit() {
        assertStatus(() -> service.update(asLearner(learner), space.getSpaceId(), monday.getScheduleId(), req(2, 9, 0, 10, 0)),
                HttpStatus.FORBIDDEN);

        space.setStatus(LearningSpaceStatus.COMPLETED);
        assertStatus(() -> service.add(asTutor(tutor), space.getSpaceId(), req(2, 9, 0, 10, 0)), HttpStatus.CONFLICT);

        space.setStatus(LearningSpaceStatus.ACTIVE);
        tutor.setMembershipStatus(TutorMembershipStatus.FREE);
        assertStatus(() -> service.add(asTutor(tutor), space.getSpaceId(), req(2, 9, 0, 10, 0)), HttpStatus.CONFLICT);

        tutor.setMembershipStatus(TutorMembershipStatus.EXPIRED);
        assertStatus(() -> service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(2, 9, 0, 10, 0)),
                HttpStatus.CONFLICT);
        verify(schedules, never()).save(any());
    }

    @Test
    void invalidSlotAndUnknownScheduleRejected() {
        assertStatus(() -> service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(1, 10, 0, 9, 0)),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.update(asTutor(tutor), space.getSpaceId(), monday.getScheduleId(), req(1, 8, 0, 13, 0)),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.update(asTutor(tutor), space.getSpaceId(), UUID.randomUUID(), req(1, 9, 0, 10, 0)),
                HttpStatus.NOT_FOUND);
    }
}
