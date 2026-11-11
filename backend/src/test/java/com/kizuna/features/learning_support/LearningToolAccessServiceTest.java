package com.kizuna.features.learning_support;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.classroom.service.LearnerClassMembershipService;
import com.kizuna.features.learning_support.dto.LearningToolAccessResponse;
import com.kizuna.features.learning_support.service.LearningToolAccessService;
import com.kizuna.features.learning_support.service.impl.LearningToolAccessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningToolAccessServiceTest {

    private final LearnerClassMembershipService membershipService = mock(LearnerClassMembershipService.class);
    private final LearningToolAccessService service = new LearningToolAccessServiceImpl(membershipService);

    private static User user(String role) {
        return User.builder().userId(UUID.randomUUID()).email("u@kizuna.vn")
                .role(Role.builder().roleName(role).build()).build();
    }

    @Test
    @DisplayName("Học viên chưa có lớp nào còn mở thì bị khóa (403)")
    void learnerWithoutClassIsLocked() {
        User learner = user("ROLE_LEARNER");
        when(membershipService.countOpenClasses(learner.getUserId())).thenReturn(0L);

        LearningToolAccessResponse access = service.getAccess(learner);

        assertThat(access.allowed()).isFalse();
        assertThat(access.message()).isEqualTo(LearningToolAccessService.LOCKED_MESSAGE);
        assertThatThrownBy(() -> service.requireAccess(learner))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).status()).isEqualTo(403));
    }

    @Test
    @DisplayName("Học viên có lớp còn mở thì được dùng, kèm số lớp")
    void learnerWithClassIsAllowed() {
        User learner = user("ROLE_LEARNER");
        when(membershipService.countOpenClasses(learner.getUserId())).thenReturn(2L);

        LearningToolAccessResponse access = service.getAccess(learner);

        assertThat(access.allowed()).isTrue();
        assertThat(access.openClassCount()).isEqualTo(2);
        assertThatCode(() -> service.requireAccess(learner)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Gia sư không bị giới hạn và không cần tra lớp học")
    void tutorIsAlwaysAllowed() {
        assertThat(service.getAccess(user("ROLE_TUTOR")).allowed()).isTrue();
        verify(membershipService, never()).countOpenClasses(any());
    }

    @Test
    @DisplayName("Chưa đăng nhập thì báo 401")
    void anonymousRequiresLogin() {
        assertThatThrownBy(() -> service.requireAccess(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).status()).isEqualTo(401));
    }
}
