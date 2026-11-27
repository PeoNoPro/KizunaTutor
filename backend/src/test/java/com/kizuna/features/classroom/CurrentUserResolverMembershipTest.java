package com.kizuna.features.classroom;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import com.kizuna.features.payment.service.TutorMembershipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Chức năng lớp học, tài liệu, bài tập của gia sư chỉ dùng được khi có gói hội viên còn hạn.
 * Gói hội viên được hỏi qua TutorMembershipService (Service công khai của feature payment — Luật 3 AGENTS.md).
 */
class CurrentUserResolverMembershipTest {

    private JwtTokenProvider jwt;
    private UserRepository users;
    private TutorRepository tutors;
    private LearnerRepository learners;
    private TutorMembershipService membershipService;
    private CurrentUserResolver resolver;

    @BeforeEach
    void setUp() {
        jwt = mock(JwtTokenProvider.class);
        users = mock(UserRepository.class);
        tutors = mock(TutorRepository.class);
        learners = mock(LearnerRepository.class);
        membershipService = mock(TutorMembershipService.class);
        resolver = new CurrentUserResolver(jwt, users, tutors, learners);
        resolver.setMembershipService(membershipService);
    }

    private User signedIn(String role) {
        User u = User.builder().userId(UUID.randomUUID()).email("a@b.vn").fullName("A")
                .role(Role.builder().roleName(role).build()).build();
        when(jwt.validateToken("tok")).thenReturn(true);
        when(jwt.getEmailFromToken("tok")).thenReturn(u.getEmail());
        when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        return u;
    }

    private Tutor tutorOf(User u) {
        Tutor t = Tutor.builder().tutorId(UUID.randomUUID()).user(u).build();
        when(tutors.findByUser_UserId(u.getUserId())).thenReturn(Optional.of(t));
        return t;
    }

    @Test
    void tutorWithActiveMembershipIsResolved() {
        Tutor t = tutorOf(signedIn("ROLE_TUTOR"));
        when(membershipService.hasActiveMembership(eq(t.getTutorId()), any())).thenReturn(true);

        CurrentActor actor = resolver.resolve("Bearer tok");

        assertThat(actor.isTutor()).isTrue();
        assertThat(actor.tutor()).isSameAs(t);
    }

    @Test
    void tutorWithoutActiveMembershipIsForbiddenWithFriendlyMessage() {
        Tutor t = tutorOf(signedIn("ROLE_TUTOR"));
        when(membershipService.hasActiveMembership(eq(t.getTutorId()), any())).thenReturn(false);

        assertThatThrownBy(() -> resolver.resolve("Bearer tok"))
                .isInstanceOf(ClassroomException.class)
                .hasMessage("Bạn cần đăng ký gói hội viên còn hiệu lực để sử dụng chức năng này.")
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void learnerIsNotCheckedForMembership() {
        User u = signedIn("ROLE_LEARNER");
        when(learners.findByUser_UserId(u.getUserId()))
                .thenReturn(Optional.of(com.kizuna.core.entity.Learner.builder().learnerId(UUID.randomUUID()).user(u).build()));

        assertThat(resolver.resolve("Bearer tok").isLearner()).isTrue();
        verifyNoInteractions(membershipService);
    }
}
