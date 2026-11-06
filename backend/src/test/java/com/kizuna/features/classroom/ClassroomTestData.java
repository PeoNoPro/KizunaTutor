package com.kizuna.features.classroom;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.features.classroom.entity.LearningSpace;
import com.kizuna.features.classroom.entity.LearningSpaceStatus;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.payment.entity.TutorMembershipStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Đối tượng dựng sẵn cho unit test FE-06 (chỉ tồn tại trong bộ nhớ, không ghi vào DB). */
final class ClassroomTestData {

    private ClassroomTestData() {
    }

    static User user(String name, String role) {
        return User.builder().userId(UUID.randomUUID()).fullName(name).email(UUID.randomUUID() + "@test.vn")
                .role(Role.builder().roleName(role).build()).build();
    }

    static Tutor tutor(TutorMembershipStatus membership) {
        return Tutor.builder().tutorId(UUID.randomUUID()).user(user("Gia sư A", "ROLE_TUTOR"))
                .membershipStatus(membership).build();
    }

    static Learner learner() {
        return Learner.builder().learnerId(UUID.randomUUID()).user(user("Học viên B", "ROLE_LEARNER")).build();
    }

    static LearningSpace space(Tutor tutor, Learner learner, LearningSpaceStatus status) {
        LearningSpace space = LearningSpace.builder().spaceId(UUID.randomUUID()).tutor(tutor).learner(learner)
                .title("Lớp N4").status(status).build();
        space.setCreatedAt(OffsetDateTime.now());
        return space;
    }

    static CurrentActor asTutor(Tutor tutor) {
        return new CurrentActor(tutor.getUser(), tutor, null);
    }

    static CurrentActor asLearner(Learner learner) {
        return new CurrentActor(learner.getUser(), null, learner);
    }
}
