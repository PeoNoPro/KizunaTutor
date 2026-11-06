package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.features.classroom.dto.ClassroomCreateRequest;
import com.kizuna.features.classroom.dto.ClassroomSummaryResponse;
import com.kizuna.features.classroom.dto.EligibleLearnerResponse;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.repository.LearningMaterialRepository;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import com.kizuna.features.classroom.service.impl.ClassroomServiceImpl;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
    private ClassroomServiceImpl service;

    private Tutor tutor;
    private Learner learner;

    @BeforeEach
    void setUp() {
        spaces = mock(LearningSpaceRepository.class);
        materials = mock(LearningMaterialRepository.class);
        learners = mock(LearnerRepository.class);
        service = new ClassroomServiceImpl(spaces, materials, learners);
        tutor = tutor(TutorMembershipStatus.PREMIUM);
        learner = learner();
        when(spaces.save(any(LearningSpace.class))).thenAnswer(inv -> {
            LearningSpace s = inv.getArgument(0);
            s.setSpaceId(UUID.randomUUID());
            return s;
        });
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

        ClassroomSummaryResponse r = service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "  Lớp N4 cấp tốc  "));

        assertThat(r.title()).isEqualTo("Lớp N4 cấp tốc");
        assertThat(r.status()).isEqualTo(LearningSpaceStatus.ACTIVE);
        verify(spaces).save(argThat(s -> s.getTutor() == tutor && s.getLearner() == learner
                && s.getStatus() == LearningSpaceStatus.ACTIVE));
    }

    @Test
    void rejectsBlankOrTooLongTitle() {
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "   ")),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "a".repeat(151))),
                HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(null, "Lớp")), HttpStatus.BAD_REQUEST);
        verify(spaces, never()).save(any());
    }

    @Test
    void acceptsTitleOfExactly150Chars() {
        allowConnection();
        ClassroomSummaryResponse r = service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "a".repeat(150)));
        assertThat(r.title()).hasSize(150);
    }

    @Test
    void rejectsLearnerWithoutAcceptedConnection() {
        when(learners.findById(learner.getLearnerId())).thenReturn(Optional.of(learner));
        when(spaces.hasAcceptedConnection(tutor.getTutorId(), learner.getLearnerId())).thenReturn(false);

        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "Lớp")),
                HttpStatus.FORBIDDEN);
        verify(spaces, never()).save(any());
    }

    @Test
    void rejectsDuplicateOpenClassroom() {
        allowConnection();
        when(spaces.existsByTutor_TutorIdAndLearner_LearnerIdAndStatusIn(eq(tutor.getTutorId()), eq(learner.getLearnerId()),
                argThat(st -> st.contains(LearningSpaceStatus.ACTIVE) && st.contains(LearningSpaceStatus.PAUSED))))
                .thenReturn(true);

        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(learner.getLearnerId(), "Lớp")),
                HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
    }

    @Test
    void rejectsCreationWhenMembershipExpired() {
        Tutor expired = tutor(TutorMembershipStatus.EXPIRED);
        assertStatus(() -> service.create(asTutor(expired), new ClassroomCreateRequest(learner.getLearnerId(), "Lớp")),
                HttpStatus.CONFLICT);
        verify(spaces, never()).save(any());
    }

    @Test
    void rejectsUnknownLearner() {
        when(learners.findById(any())).thenReturn(Optional.empty());
        assertStatus(() -> service.create(asTutor(tutor), new ClassroomCreateRequest(UUID.randomUUID(), "Lớp")),
                HttpStatus.NOT_FOUND);
    }

    @Test
    void learnerCannotCreateClassroom() {
        assertStatus(() -> service.create(asLearner(learner), new ClassroomCreateRequest(learner.getLearnerId(), "Lớp")),
                HttpStatus.FORBIDDEN);
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
