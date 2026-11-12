package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.features.classroom.dto.ClassScheduleRequest;
import com.kizuna.features.classroom.dto.ClassroomCreateRequest;
import com.kizuna.features.classroom.dto.ClassroomSummaryResponse;
import com.kizuna.features.classroom.dto.EligibleLearnerResponse;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.ClassScheduleRepository;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.impl.ClassroomServiceImpl;
import com.kizuna.features.classroom.util.ClassScheduleRules;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.classroom.ClassroomTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClassroomServiceImplTest {

    private LearningSpaceRepository spaces;
    private LearningMaterialRepository materials;
    private LearnerRepository learners;
    private ClassScheduleRepository schedules;
    private ClassroomServiceImpl service;

    private Tutor tutor;
    private Learner learner;

    @BeforeEach
    void setUp() {
        spaces = mock(LearningSpaceRepository.class);
        materials = mock(LearningMaterialRepository.class);
        learners = mock(LearnerRepository.class);
        schedules = mock(ClassScheduleRepository.class);
        var cal = calendar(spaces, schedules);
        service = new ClassroomServiceImpl(spaces, materials, learners, schedules, new ClassScheduleRules(schedules, cal), cal);
        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        when(spaces.save(any(LearningSpace.class))).thenAnswer(inv -> {
            LearningSpace s = inv.getArgument(0);
            s.setSpaceId(UUID.randomUUID());
            return s;
        });
    }

    private static final List<ClassScheduleRequest> SLOTS =
            List.of(new ClassScheduleRequest(1, LocalTime.of(19, 0), LocalTime.of(20, 30)));

    private static final LocalDate TODAY = LocalDate.now(ClassScheduleRules.VN_ZONE);

    private static ClassroomCreateRequest req(UUID learnerId, String title) {
        return new ClassroomCreateRequest(learnerId, title, SLOTS, TODAY, 24);
    }

    private static ClassroomCreateRequest reqWith(UUID learnerId, List<ClassScheduleRequest> slots) {
        return new ClassroomCreateRequest(learnerId, "Lớp", slots, TODAY, 24);
    }

    private static void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    private void allowConnection() {
        when(learners.findById(learner.getLearnerId())).thenReturn(Optional.of(learner));
        when(spaces.hasAcceptedConnection(tutor.getTutorId(), learner.getLearnerId())).thenReturn(true);
    }

    // ----- Danh sách lớp -----

    @Test
    void tutorListsOwnClassrooms() {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findByTutor_TutorIdOrderByCreatedAtDesc(tutor.getTutorId())).thenReturn(List.of(s));
        when(materials.countBySpace_SpaceId(s.getSpaceId())).thenReturn(2L);

        List<ClassroomSummaryResponse> result = service.listMine(asTutor(tutor));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).learnerName()).isEqualTo("Học viên B");
        assertThat(result.get(0).canManage()).isTrue();
        assertThat(result.get(0).materialCount()).isEqualTo(2);
        verify(spaces, never()).findByLearner_LearnerIdOrderByCreatedAtDesc(any());
    }

    @Test
    void learnerListsOwnClassroomsWithoutManageRight() {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findByLearner_LearnerIdOrderByCreatedAtDesc(learner.getLearnerId())).thenReturn(List.of(s));

        List<ClassroomSummaryResponse> result = service.listMine(asLearner(learner));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).tutorName()).isEqualTo("Gia sư A");
        assertThat(result.get(0).canManage()).isFalse();
    }

    @Test
    void expiredMembershipMakesClassroomReadOnly() {
        Tutor expired = tutor(TutorMembershipStatus.EXPIRED);
        LearningSpace s = space(expired, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findByTutor_TutorIdOrderByCreatedAtDesc(expired.getTutorId())).thenReturn(List.of(s));

        ClassroomSummaryResponse r = service.listMine(asTutor(expired)).get(0);
        assertThat(r.readOnly()).isTrue();
        assertThat(r.canManage()).isFalse();
    }

    // ----- Học viên đủ điều kiện -----

    @Test
    void eligibleLearnersComeFromAcceptedConnections() {
        when(spaces.findEligibleLearnerIds(tutor.getTutorId())).thenReturn(List.of(learner.getLearnerId()));
        when(learners.findAllById(List.of(learner.getLearnerId()))).thenReturn(List.of(learner));

        List<EligibleLearnerResponse> result = service.listEligibleLearners(asTutor(tutor));

        assertThat(result).extracting(EligibleLearnerResponse::learnerId).containsExactly(learner.getLearnerId());
    }

    @Test
    void eligibleLearnersEmptyWhenNoConnection() {
        when(spaces.findEligibleLearnerIds(tutor.getTutorId())).thenReturn(List.of());
        assertThat(service.listEligibleLearners(asTutor(tutor))).isEmpty();
        verify(learners, never()).findAllById(any());
    }

    @Test
    void learnerCannotListEligibleLearners() {
        assertStatus(() -> service.listEligibleLearners(asLearner(learner)), HttpStatus.FORBIDDEN);
    }

    // ----- Tạo lớp (UC-93) -----

    @Test
    void createsActiveClassroomForConnectedLearner() {
        allowConnection();

        ClassroomSummaryResponse r = service.create(asTutor(tutor), req(learner.getLearnerId(), "  Lớp N4 cấp tốc  "));

        assertThat(r.title()).isEqualTo("Lớp N4 cấp tốc");
        assertThat(r.status()).isEqualTo(LearningSpaceStatus.ACTIVE);
        verify(spaces).save(argThat(s -> s.getTutor() == tutor && s.getLearner() == learner
                && s.getStatus() == LearningSpaceStatus.ACTIVE));
    }

    @Test
    void rejectsBlankOrTooLongTitle() {
        assertStatus(() -> service.create(asTutor(tutor), req(learner.getLearnerId(), "   ")),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), req(learner.getLearnerId(), "a".repeat(151))),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), req(null, "Lớp")), HttpStatus.BAD_REQUEST);
        verify(spaces, never()).save(any());
    }

    @Test
    void acceptsTitleOfExactly150Chars() {
        allowConnection();
        ClassroomSummaryResponse r = service.create(asTutor(tutor), req(learner.getLearnerId(), "a".repeat(150)));
        assertThat(r.title()).hasSize(150);
    }

    @Test
    void rejectsLearnerWithoutAcceptedConnection() {
        when(learners.findById(learner.getLearnerId())).thenReturn(Optional.of(learner));
        when(spaces.hasAcceptedConnection(tutor.getTutorId(), learner.getLearnerId())).thenReturn(false);

        assertStatus(() -> service.create(asTutor(tutor), req(learner.getLearnerId(), "Lớp")),
                HttpStatus.FORBIDDEN);
        verify(spaces, never()).save(any());
    }

    @Test
    void rejectsDuplicateOpenClassroom() {
        allowConnection();
        when(spaces.existsByTutor_TutorIdAndLearner_LearnerIdAndStatusIn(eq(tutor.getTutorId()), eq(learner.getLearnerId()),
                argThat(st -> st.contains(LearningSpaceStatus.ACTIVE) && st.contains(LearningSpaceStatus.PAUSED))))
                .thenReturn(true);

        assertStatus(() -> service.create(asTutor(tutor), req(learner.getLearnerId(), "Lớp")),
                HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
    }

    @Test
    void rejectsCreationWhenMembershipExpired() {
        Tutor expired = tutor(TutorMembershipStatus.EXPIRED);
        assertStatus(() -> service.create(asTutor(expired), req(learner.getLearnerId(), "Lớp")),
                HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
    }

    @Test
    void freeTutorWithoutMembershipCannotCreateClassOrListLearners() {
        Tutor free = tutor(TutorMembershipStatus.FREE);
        assertThatThrownBy(() -> service.create(asTutor(free), req(learner.getLearnerId(), "Lớp")))
                .isInstanceOf(ClassroomException.class)
                .hasMessageContaining("chưa có gói hội viên");
        assertStatus(() -> service.listEligibleLearners(asTutor(free)), HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
        verify(spaces, never()).findEligibleLearnerIds(any());
    }

    @Test
    void standardMembershipCanCreateClass() {
        Tutor standard = tutor(TutorMembershipStatus.STANDARD);
        when(learners.findById(learner.getLearnerId())).thenReturn(Optional.of(learner));
        when(spaces.hasAcceptedConnection(standard.getTutorId(), learner.getLearnerId())).thenReturn(true);
        assertThat(service.create(asTutor(standard), req(learner.getLearnerId(), "Lớp")).canManage()).isTrue();
    }

    @Test
    void freeTutorsClassroomIsReadOnly() {
        Tutor free = tutor(TutorMembershipStatus.FREE);
        LearningSpace s = space(free, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findWithParticipantsBySpaceId(s.getSpaceId())).thenReturn(Optional.of(s));

        ClassroomSummaryResponse r = service.getDetail(asTutor(free), s.getSpaceId());
        assertThat(r.readOnly()).isTrue();
        assertThat(r.canManage()).isFalse();
        assertStatus(() -> service.loadManageableSpace(asTutor(free), s.getSpaceId()), HttpStatus.CONFLICT);
    }

    @Test
    void rejectsUnknownLearner() {
        when(learners.findById(any())).thenReturn(Optional.empty());
        assertStatus(() -> service.create(asTutor(tutor), req(UUID.randomUUID(), "Lớp")),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void learnerCannotCreateClassroom() {
        assertStatus(() -> service.create(asLearner(learner), req(learner.getLearnerId(), "Lớp")),
                HttpStatus.FORBIDDEN);
    }

    // ----- Lịch học khi tạo lớp -----

    @Test
    void createSavesWeeklySchedules() {
        allowConnection();
        service.create(asTutor(tutor), req(learner.getLearnerId(), "Lớp"));
        verify(schedules).saveAll(argThat(list -> {
            var it = list.iterator();
            var s = it.next();
            return !it.hasNext() && s.getDayOfWeek() == 1 && s.getStartTime().equals(LocalTime.of(19, 0));
        }));
    }

    @Test
    void createSavesCourseStartDateTotalSessionsAndSlotActivation() {
        allowConnection();
        LocalDate start = TODAY.plusDays(10);
        ClassroomSummaryResponse r = service.create(asTutor(tutor),
                new ClassroomCreateRequest(learner.getLearnerId(), "Lớp", SLOTS, start, 30));
        assertThat(r.totalSessions()).isEqualTo(30);
        assertThat(r.courseStartDate()).isEqualTo(start);
        verify(spaces).save(argThat(s -> start.equals(s.getCourseStartDate()) && Integer.valueOf(30).equals(s.getTotalSessions())));
        // Buổi hằng tuần bắt đầu có hiệu lực từ 00:00 ngày bắt đầu khóa (giờ Việt Nam)
        verify(schedules).saveAll(argThat(list -> {
            var s = list.iterator().next();
            return s.getActiveFrom() != null
                    && s.getActiveFrom().atZoneSameInstant(ClassScheduleRules.VN_ZONE).toLocalDate().equals(start);
        }));
    }

    @Test
    void createRequiresValidStartDateAndTotalSessions() {
        allowConnection();
        UUID id = learner.getLearnerId();
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, null, 24)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, TODAY.minusDays(1), 24)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, TODAY.plusYears(2), 24)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, TODAY, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, TODAY, 0)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(id, "Lớp", SLOTS, TODAY, 501)), HttpStatus.BAD_REQUEST);
        verify(spaces, never()).save(any());
    }

    @Test
    void totalSessionsCannotBeFewerThanSessionsPerWeek() {
        allowConnection();
        List<ClassScheduleRequest> twoPerWeek = List.of(
                new ClassScheduleRequest(1, LocalTime.of(19, 0), LocalTime.of(20, 0)),
                new ClassScheduleRequest(2, LocalTime.of(19, 0), LocalTime.of(20, 0)));
        assertThatThrownBy(() -> service.create(asTutor(tutor),
                new ClassroomCreateRequest(learner.getLearnerId(), "Lớp", twoPerWeek, TODAY, 1)))
                .isInstanceOfSatisfying(ClassroomException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getMessage()).isEqualTo("Lịch học có 2 buổi mỗi tuần nên tổng số buổi cần từ 2 buổi trở lên.");
                });
        assertThat(service.create(asTutor(tutor),
                new ClassroomCreateRequest(learner.getLearnerId(), "Lớp", twoPerWeek, TODAY, 2)).totalSessions()).isEqualTo(2);
    }

    @Test
    void createRequiresAtLeastOneValidSlot() {
        allowConnection();
        UUID id = learner.getLearnerId();
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id, List.of())), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id, null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id,
                List.of(new ClassScheduleRequest(8, LocalTime.of(9, 0), LocalTime.of(10, 0))))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id,
                List.of(new ClassScheduleRequest(2, LocalTime.of(10, 0), LocalTime.of(9, 0))))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id,
                List.of(new ClassScheduleRequest(2, LocalTime.of(9, 0), LocalTime.of(9, 15))))), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), reqWith(id, List.of(
                new ClassScheduleRequest(3, LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new ClassScheduleRequest(3, LocalTime.of(9, 30), LocalTime.of(11, 0))))), HttpStatus.BAD_REQUEST);
        verify(spaces, never()).save(any());
    }

    @Test
    void createRejectsSlotOverlappingTutorsOtherClass() {
        allowConnection();
        LearningSpace other = space(tutor, learner(), LearningSpaceStatus.ACTIVE);
        when(schedules.findOpenSchedulesOfParticipants(eq(tutor.getTutorId()), eq(learner.getLearnerId()), any()))
                .thenReturn(List.of(com.kizuna.features.classroom.entity.ClassSchedule.builder()
                        .scheduleId(UUID.randomUUID()).space(other).dayOfWeek(1)
                        .startTime(LocalTime.of(20, 0)).endTime(LocalTime.of(21, 0)).build()));

        assertStatus(() -> service.create(asTutor(tutor), req(learner.getLearnerId(), "Lớp")), HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
    }

    @Test
    void adjacentSlotsDoNotConflict() {
        allowConnection();
        LearningSpace other = space(tutor, learner(), LearningSpaceStatus.ACTIVE);
        when(schedules.findOpenSchedulesOfParticipants(any(), any(), any()))
                .thenReturn(List.of(com.kizuna.features.classroom.entity.ClassSchedule.builder()
                        .scheduleId(UUID.randomUUID()).space(other).dayOfWeek(1)
                        .startTime(LocalTime.of(20, 30)).endTime(LocalTime.of(21, 30)).build()));

        assertThat(service.create(asTutor(tutor), req(learner.getLearnerId(), "Lớp")).title()).isEqualTo("Lớp");
    }

    // ----- Đổi tên lớp -----

    @Test
    void tutorRenamesActiveClassroom() {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findWithParticipantsBySpaceId(s.getSpaceId())).thenReturn(Optional.of(s));
        assertThat(service.rename(asTutor(tutor), s.getSpaceId(), "  Lớp N3  ").title()).isEqualTo("Lớp N3");
    }

    @Test
    void renameRejectsBlankTitleLearnerAndReadOnlyClass() {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        LearningSpace closed = space(tutor, learner, LearningSpaceStatus.COMPLETED);
        when(spaces.findWithParticipantsBySpaceId(s.getSpaceId())).thenReturn(Optional.of(s));
        when(spaces.findWithParticipantsBySpaceId(closed.getSpaceId())).thenReturn(Optional.of(closed));

        assertStatus(() -> service.rename(asTutor(tutor), s.getSpaceId(), "  "), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.rename(asLearner(learner), s.getSpaceId(), "Lớp"), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.rename(asTutor(tutor), closed.getSpaceId(), "Lớp"), HttpStatus.CONFLICT);
        assertThat(s.getTitle()).isEqualTo("Lớp N4");
    }

    // ----- Quyền truy cập lớp -----

    @Test
    void outsidersCannotAccessClassroom() {
        LearningSpace s = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        when(spaces.findWithParticipantsBySpaceId(s.getSpaceId())).thenReturn(Optional.of(s));

        assertStatus(() -> service.getDetail(asLearner(learner()), s.getSpaceId()), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.getDetail(asTutor(tutor(TutorMembershipStatus.PREMIUM)), s.getSpaceId()), HttpStatus.FORBIDDEN);
        assertThat(service.getDetail(asLearner(learner), s.getSpaceId()).spaceId()).isEqualTo(s.getSpaceId());
    }

    @Test
    void missingClassroomIsNotFound() {
        when(spaces.findWithParticipantsBySpaceId(any())).thenReturn(Optional.empty());
        assertStatus(() -> service.getDetail(asTutor(tutor), UUID.randomUUID()), HttpStatus.NOT_FOUND);
    }

    @Test
    void onlyTutorOfActiveClassroomCanManage() {
        LearningSpace active = space(tutor, learner, LearningSpaceStatus.ACTIVE);
        LearningSpace closed = space(tutor, learner, LearningSpaceStatus.COMPLETED);
        when(spaces.findWithParticipantsBySpaceId(active.getSpaceId())).thenReturn(Optional.of(active));
        when(spaces.findWithParticipantsBySpaceId(closed.getSpaceId())).thenReturn(Optional.of(closed));

        assertThat(service.loadManageableSpace(asTutor(tutor), active.getSpaceId())).isSameAs(active);
        assertStatus(() -> service.loadManageableSpace(asLearner(learner), active.getSpaceId()), HttpStatus.FORBIDDEN);
        assertStatus(() -> service.loadManageableSpace(asTutor(tutor), closed.getSpaceId()), HttpStatus.CONFLICT);
    }
}
