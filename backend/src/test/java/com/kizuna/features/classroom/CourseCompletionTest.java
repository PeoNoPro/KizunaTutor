package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.dto.ClassroomSummaryResponse;
import com.kizuna.features.classroom.entity.ClassSchedule;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.impl.ClassroomServiceImpl;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.classroom.util.ClassSessionCalendar;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.core.repository.LearnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

/** FE-06: lớp tự chuyển "Đã hoàn thành" khi học xong buổi cuối (tính khi có người truy cập). */
class CourseCompletionTest {

    private LearningSpaceRepository spaces;
    private ClassScheduleRepository schedules;
    private ClassSessionCalendar calendar;
    private ClassroomServiceImpl service;
    private Tutor tutor;
    private Learner learner;
    private final List<ClassSchedule> slots = new ArrayList<>();

    @BeforeEach
    void setUp() {
        spaces = mock(LearningSpaceRepository.class);
        schedules = mock(ClassScheduleRepository.class);
        calendar = calendar(spaces, schedules);
        service = new ClassroomServiceImpl(spaces, mock(LearningMaterialRepository.class), mock(LearnerRepository.class),
                schedules, new ClassScheduleRules(schedules, calendar), calendar);
        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        when(schedules.findBySpace_SpaceIdIn(anyCollection())).thenAnswer(inv -> {
            java.util.Collection<UUID> ids = inv.getArgument(0);
            return slots.stream().filter(s -> ids.contains(s.getSpace().getSpaceId())).toList();
        });
    }

    /** Lớp học thứ Hai 08:00–09:00 hằng tuần, bắt đầu cách đây {@code weeksAgo} tuần, tổng {@code total} buổi. */
    private LearningSpace course(int weeksAgo, Integer total) {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        s.setCourseStartDate(LocalDate.now(ClassScheduleRules.VN_ZONE).minusWeeks(weeksAgo));
        s.setTotalSessions(total);
        slots.add(ClassSchedule.builder().scheduleId(UUID.randomUUID()).space(s).dayOfWeek(1)
                .startTime(LocalTime.of(8, 0)).endTime(LocalTime.of(9, 0)).build());
        return s;
    }

    @Test
    void progressFinishedOnlyWhenLastSessionEnded() {
        LocalDateTime now = LocalDateTime.now(ClassScheduleRules.VN_ZONE);
        LearningSpace done = course(10, 3);
        LearningSpace ongoing = course(10, 50);
        LearningSpace unlimited = course(10, null);
        assertThat(calendar.progress(done, now).finished()).isTrue();
        assertThat(calendar.progress(ongoing, now).finished()).isFalse();
        assertThat(calendar.progress(unlimited, now).finished()).isFalse();
    }

    @Test
    void listMineCompletesFinishedClassAndPutsItLast() {
        LearningSpace done = course(10, 3);
        LearningSpace ongoing = course(10, 50);
        when(spaces.findByTutor_TutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(List.of(done, ongoing));

        List<ClassroomSummaryResponse> list = service.listMine(asTutor(tutor));

        assertThat(done.getStatus()).isEqualTo(LearningSpaceStatus.COMPLETED);
        assertThat(ongoing.getStatus()).isEqualTo(LearningSpaceStatus.ACTIVE);
        assertThat(list).extracting(ClassroomSummaryResponse::spaceId)
                .containsExactly(ongoing.getSpaceId(), done.getSpaceId());
        assertThat(list.get(1).readOnly()).isTrue();
        verify(spaces).saveAll(List.of(done));
    }

    @Test
    void detailCompletesFinishedClassForLearnerToo() {
        LearningSpace done = course(10, 3);
        when(spaces.findWithParticipantsBySpaceId(done.getSpaceId())).thenReturn(Optional.of(done));

        ClassroomSummaryResponse detail = service.getDetail(asLearner(learner), done.getSpaceId());

        assertThat(detail.status()).isEqualTo(LearningSpaceStatus.COMPLETED);
        assertThat(detail.startedSessions()).isEqualTo(3);
    }

    @Test
    void finishedClassNoLongerBlocksNewClassWithSameLearner() {
        LearningSpace done = course(10, 3);
        when(spaces.findByTutor_TutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(List.of(done));
        when(spaces.findEligibleLearnerIds(tutor.getTutorId())).thenReturn(List.of());

        service.listEligibleLearners(asTutor(tutor));

        assertThat(done.getStatus()).isEqualTo(LearningSpaceStatus.COMPLETED);
    }

    @Test
    void completedClassIsReadOnlyButTutorCanStillGrade() {
        LearningSpace done = space(tutor, learner, LearningSpaceStatus.COMPLETED);
        LearningSpace closed = space(tutor, learner, LearningSpaceStatus.CLOSED);
        when(spaces.findWithParticipantsBySpaceId(done.getSpaceId())).thenReturn(Optional.of(done));
        when(spaces.findWithParticipantsBySpaceId(closed.getSpaceId())).thenReturn(Optional.of(closed));

        assertThat(service.isReadOnly(done)).isTrue();
        assertThatThrownBy(() -> service.loadManageableSpace(asTutor(tutor), done.getSpaceId()))
                .isInstanceOf(ClassroomException.class);
        assertThat(service.loadGradableSpace(asTutor(tutor), done.getSpaceId())).isSameAs(done);
        assertThat(service.isGradeLocked(done)).isFalse();

        assertThat(service.isGradeLocked(closed)).isTrue();
        assertThatThrownBy(() -> service.loadGradableSpace(asTutor(tutor), closed.getSpaceId()))
                .isInstanceOfSatisfying(ClassroomException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThatThrownBy(() -> service.loadGradableSpace(asLearner(learner), done.getSpaceId()))
                .isInstanceOfSatisfying(ClassroomException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void expiredMembershipStillLocksGrading() {
        Tutor expired = tutor(TutorMembershipStatus.EXPIRED);
        LearningSpace done = space(expired, learner, LearningSpaceStatus.COMPLETED);
        assertThat(service.isGradeLocked(done)).isTrue();
        verify(spaces, never()).saveAll(any());
    }
}
