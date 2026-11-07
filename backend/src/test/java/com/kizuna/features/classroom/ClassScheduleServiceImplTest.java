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
import static org.mockito.Mockito.*;

class ClassScheduleServiceImplTest {

    private ClassroomService classroomService;
    private ClassScheduleRepository schedules;
    private ClassScheduleServiceImpl service;

    private Tutor tutor;
    private Learner learner;
    private LearningSpace space;
    private ClassSchedule monday;

    @BeforeEach
    void setUp() {
        classroomService = mock(ClassroomService.class);
        schedules = mock(ClassScheduleRepository.class);
        service = new ClassScheduleServiceImpl(classroomService, schedules, new ClassScheduleRules(schedules));
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
        return ClassSchedule.builder().scheduleId(UUID.randomUUID()).space(space).dayOfWeek(day)
                .startTime(LocalTime.of(sh, sm)).endTime(LocalTime.of(eh, em)).build();
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
        assertThat(monday.getStartTime()).isEqualTo(LocalTime.of(18, 0));
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
