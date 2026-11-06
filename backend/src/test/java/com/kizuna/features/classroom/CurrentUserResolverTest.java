package com.kizuna.features.classroom;

import com.kizuna.core.entity.*;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.classroom.util.CurrentUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CurrentUserResolverTest {

    private JwtTokenProvider jwt;
    private UserRepository users;
    private TutorRepository tutors;
    private LearnerRepository learners;
    private CurrentUserResolver resolver;

    @BeforeEach
    void setUp() {
        jwt = mock(JwtTokenProvider.class);
        users = mock(UserRepository.class);
        tutors = mock(TutorRepository.class);
        learners = mock(LearnerRepository.class);
        resolver = new CurrentUserResolver(jwt, users, tutors, learners);
    }

    private User user(String role) {
        return User.builder().userId(UUID.randomUUID()).email("a@b.vn").fullName("A")
                .role(Role.builder().roleName(role).build()).build();
    }

    private void tokenFor(User u) {
        when(jwt.validateToken("tok")).thenReturn(true);
        when(jwt.getEmailFromToken("tok")).thenReturn(u.getEmail());
        when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
    }

    private void assertStatus(Runnable call, HttpStatus status) {
        assertThatThrownBy(call::run).isInstanceOf(ClassroomException.class)
                .extracting(e -> ((ClassroomException) e).getStatus()).isEqualTo(status);
    }

    @Test
    void missingOrMalformedHeaderIsUnauthorized() {
        assertStatus(() -> resolver.resolve(null), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> resolver.resolve("Basic abc"), HttpStatus.UNAUTHORIZED);
        assertStatus(() -> resolver.resolve("Bearer "), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidTokenIsUnauthorized() {
        when(jwt.validateToken("bad")).thenReturn(false);
        assertStatus(() -> resolver.resolve("Bearer bad"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unknownUserIsUnauthorized() {
        when(jwt.validateToken("tok")).thenReturn(true);
        when(jwt.getEmailFromToken("tok")).thenReturn("x@y.vn");
        when(users.findByEmail("x@y.vn")).thenReturn(Optional.empty());
        assertStatus(() -> resolver.resolve("Bearer tok"), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tutorResolved() {
        User u = user("ROLE_TUTOR");
        tokenFor(u);
        Tutor t = Tutor.builder().tutorId(UUID.randomUUID()).user(u).build();
        when(tutors.findByUser_UserId(u.getUserId())).thenReturn(Optional.of(t));

        CurrentActor actor = resolver.resolve("Bearer tok");
        assertThat(actor.isTutor()).isTrue();
        assertThat(actor.tutor()).isSameAs(t);
    }

    @Test
    void learnerResolved() {
        User u = user("ROLE_LEARNER");
        tokenFor(u);
        Learner l = Learner.builder().learnerId(UUID.randomUUID()).user(u).build();
        when(learners.findByUser_UserId(u.getUserId())).thenReturn(Optional.of(l));

        CurrentActor actor = resolver.resolve("Bearer tok");
        assertThat(actor.isLearner()).isTrue();
        assertThat(actor.learner()).isSameAs(l);
    }

    @Test
    void otherRoleIsForbidden() {
        User u = user("ROLE_ADMIN");
        tokenFor(u);
        assertStatus(() -> resolver.resolve("Bearer tok"), HttpStatus.FORBIDDEN);
    }

    @Test
    void suspendedAccountIsForbidden() {
        User u = user("ROLE_TUTOR");
        u.setAccountStatus(UserAccountStatus.SUSPENDED);
        tokenFor(u);
        assertStatus(() -> resolver.resolve("Bearer tok"), HttpStatus.FORBIDDEN);
    }

    @Test
    void tutorRoleWithoutProfileIsForbidden() {
        User u = user("ROLE_TUTOR");
        tokenFor(u);
        when(tutors.findByUser_UserId(u.getUserId())).thenReturn(Optional.empty());
        assertStatus(() -> resolver.resolve("Bearer tok"), HttpStatus.FORBIDDEN);
    }
}
