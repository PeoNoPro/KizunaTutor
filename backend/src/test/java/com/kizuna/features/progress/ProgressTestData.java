package com.kizuna.features.progress;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;
import com.kizuna.features.progress.entity.Assignment;
import com.kizuna.features.progress.entity.AssignmentSubmission;
import com.kizuna.features.progress.entity.AssignmentSubmissionStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Dữ liệu dựng trong bộ nhớ cho unit test FE-07 (không ghi DB). */
final class ProgressTestData {

    static final OffsetDateTime NOW = OffsetDateTime.of(2026, 11, 10, 10, 0, 0, 0, ZoneOffset.ofHours(7));
    static final Clock CLOCK = Clock.fixed(NOW.toInstant(), ZoneOffset.ofHours(7));

    private ProgressTestData() {
    }

    static User user(String name, String role) {
        return User.builder().userId(UUID.randomUUID()).fullName(name).email(UUID.randomUUID() + "@test.vn")
                .role(Role.builder().roleName(role).build()).build();
    }

    static Tutor tutor() {
        return Tutor.builder().tutorId(UUID.randomUUID()).user(user("Gia sư A", "ROLE_TUTOR"))
                .membershipStatus(TutorMembershipStatus.PREMIUM).build();
    }

    static Learner learner() {
        return Learner.builder().learnerId(UUID.randomUUID()).user(user("Học viên B", "ROLE_LEARNER")).build();
    }

    static LearningSpace space(Tutor tutor, Learner learner) {
        return LearningSpace.builder().spaceId(UUID.randomUUID()).tutor(tutor).learner(learner)
                .title("Lớp N4").status(LearningSpaceStatus.ACTIVE).build();
    }

    static Assignment assignment(LearningSpace space, OffsetDateTime due) {
        Assignment a = Assignment.builder().assignmentId(UUID.randomUUID()).space(space).title("Bài 1")
                .dueDate(due).totalPoints(new BigDecimal("100.00")).build();
        a.setCreatedAt(NOW.minusDays(3));
        return a;
    }

    static AssignmentSubmission submission(Assignment a, Learner l, int attempt, OffsetDateTime at, boolean graded) {
        return AssignmentSubmission.builder().submissionId(UUID.randomUUID()).assignment(a).learner(l)
                .attemptNumber((short) attempt).submissionContent("Bài làm " + attempt).submittedAt(at)
                .finalized(graded).grade(graded ? new BigDecimal("80.00") : null)
                .status(graded ? AssignmentSubmissionStatus.GRADED : AssignmentSubmissionStatus.SUBMITTED).build();
    }

    static CurrentActor asTutor(Tutor t) {
        return new CurrentActor(t.getUser(), t, null);
    }

    static CurrentActor asLearner(Learner l) {
        return new CurrentActor(l.getUser(), null, l);
    }
}
