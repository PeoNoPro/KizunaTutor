package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Tutor;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.dto.AssignmentRequest;
import com.kizuna.features.progress.dto.AssignmentSummaryResponse;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.repository.AssignmentRepository;
import com.kizuna.features.progress.repository.AssignmentSubmissionRepository;
import com.kizuna.features.progress.service.impl.AssignmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kizuna.features.progress.ProgressTestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Thời gian mở bài nộp tệp (migration 017): trước giờ mở học viên không thấy bài, gia sư vẫn thấy. */
class AssignmentOpensAtTest {

    private AssignmentRepository assignments;
    private AssignmentServiceImpl service;
    private LearningSpace space;
    private CurrentActor tutorActor;
    private CurrentActor learnerActor;
    private Assignment future;
    private Assignment openNow;

    @BeforeEach
    void setUp() {
        ClassroomService classrooms = mock(ClassroomService.class);
        assignments = mock(AssignmentRepository.class);
        AssignmentSubmissionRepository submissions = mock(AssignmentSubmissionRepository.class);
        service = new AssignmentServiceImpl(classrooms, assignments, submissions,
                mock(com.kizuna.features.progress.repository.AssignmentFileRepository.class),
                mock(com.kizuna.features.progress.repository.SubmissionFileRepository.class),
                mock(com.kizuna.features.progress.service.AssignmentAttachmentService.class), CLOCK);
        Tutor tutor = tutor();
        Learner learner = learner();
        space = space(tutor, learner);
        tutorActor = asTutor(tutor);
        learnerActor = asLearner(learner);
        when(classrooms.loadAccessibleSpace(any(), eq(space.getSpaceId()))).thenReturn(space);
        when(classrooms.loadManageableSpace(tutorActor, space.getSpaceId())).thenReturn(space);
        when(assignments.save(any(Assignment.class))).thenAnswer(inv -> {
            Assignment a = inv.getArgument(0);
            if (a.getAssignmentId() == null) a.setAssignmentId(UUID.randomUUID());
            return a;
        });

        future = assignment(space, NOW.plusDays(5));
        future.setOpensAt(NOW.plusDays(1));
        openNow = assignment(space, NOW.plusDays(5));
        when(assignments.findBySpace_SpaceIdOrderByCreatedAtDesc(space.getSpaceId())).thenReturn(List.of(future, openNow));
        when(assignments.findByAssignmentIdAndSpace_SpaceId(future.getAssignmentId(), space.getSpaceId())).thenReturn(Optional.of(future));
    }

    @Test
    void learnerDoesNotSeeAssignmentBeforeOpenTimeButTutorDoes() {
        assertThat(service.list(learnerActor, space.getSpaceId())).extracting(AssignmentSummaryResponse::assignmentId)
                .containsExactly(openNow.getAssignmentId());
        List<AssignmentSummaryResponse> forTutor = service.list(tutorActor, space.getSpaceId());
        assertThat(forTutor).hasSize(2);
        assertThat(forTutor.get(0).opened()).isFalse();
        assertThat(forTutor.get(0).opensAt()).isEqualTo(NOW.plusDays(1));
        assertThatThrownBy(() -> service.detail(learnerActor, space.getSpaceId(), future.getAssignmentId()))
                .isInstanceOfSatisfying(ClassroomException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void openTimeMustBeBeforeDueDateAndIsSaved() {
        assertThatThrownBy(() -> service.create(tutorActor, space.getSpaceId(),
                new AssignmentRequest("Bài", "Nội dung", NOW.plusDays(1), null, NOW.plusDays(2))))
                .hasMessage("Thời gian mở bài phải trước hạn nộp.");
        AssignmentSummaryResponse r = service.create(tutorActor, space.getSpaceId(),
                new AssignmentRequest("Bài", "Nội dung", NOW.plusDays(3), null, NOW.plusDays(1)));
        assertThat(r.opensAt()).isEqualTo(NOW.plusDays(1));
        assertThat(r.opened()).isFalse();
    }
}
