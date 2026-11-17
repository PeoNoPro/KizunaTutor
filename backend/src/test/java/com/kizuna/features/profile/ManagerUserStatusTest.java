package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateUserStatusRequest;
import com.kizuna.features.profile.dto.response.UserStatusUpdateResponseDto;
import com.kizuna.features.profile.repository.ProfileAuditLogRepository;
import com.kizuna.features.profile.service.impl.ManagerUserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerUserStatusTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private LearnerRepository learnerRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private ProfileAuditLogRepository profileAuditLogRepository;
    @InjectMocks
    private ManagerUserProfileServiceImpl service;

    private User manager;
    private User learner;

    @BeforeEach
    void setUp() {
        manager = User.builder()
                .userId(UUID.randomUUID())
                .email("manager@kizuna.vn")
                .role(Role.builder().roleName("ROLE_MANAGER").build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        learner = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@kizuna.vn")
                .role(Role.builder().roleName("ROLE_LEARNER").build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Cập nhật trạng thái SUSPENDED ghi đúng audit log và trả kết quả tiếng Việt")
    void updatesStatusAndWritesAuditLog() {
        when(userRepository.findById(learner.getUserId())).thenReturn(Optional.of(learner));
        when(userRepository.findByEmail("manager@kizuna.vn")).thenReturn(Optional.of(manager));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserStatusUpdateResponseDto result = service.updateUserStatus(
                learner.getUserId(),
                new UpdateUserStatusRequest(UserAccountStatus.SUSPENDED, "   Vi phạm hủy lịch học nhiều lần   "),
                "manager@kizuna.vn",
                "127.0.0.1"
        );

        assertThat(result.accountStatus()).isEqualTo(UserAccountStatus.SUSPENDED);
        assertThat(result.reason()).isEqualTo("Vi phạm hủy lịch học nhiều lần");
        verify(profileAuditLogRepository).recordStatusChange(
                manager.getUserId(),
                learner.getUserId(),
                UserAccountStatus.ACTIVE,
                UserAccountStatus.SUSPENDED,
                "Vi phạm hủy lịch học nhiều lần",
                "127.0.0.1"
        );
    }

    @Test
    @DisplayName("Từ chối lý do chỉ toàn khoảng trắng hoặc ngắn hơn 5 ký tự sau khi cắt khoảng trắng")
    void rejectsWhitespaceOnlyReason() {
        when(userRepository.findById(learner.getUserId())).thenReturn(Optional.of(learner));

        assertThatThrownBy(() -> service.updateUserStatus(
                learner.getUserId(),
                new UpdateUserStatusRequest(UserAccountStatus.BANNED, "     "),
                "manager@kizuna.vn",
                "127.0.0.1"
        )).isInstanceOf(BadRequestException.class);

        verify(userRepository, never()).saveAndFlush(any());
        verify(profileAuditLogRepository, never()).recordStatusChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Không cho phép đổi sang trạng thái PENDING/LOCKED hoặc thao tác trên tài khoản nội bộ")
    void rejectsDisallowedStatusesAndInternalAccounts() {
        when(userRepository.findById(learner.getUserId())).thenReturn(Optional.of(learner));

        assertThatThrownBy(() -> service.updateUserStatus(
                learner.getUserId(),
                new UpdateUserStatusRequest(UserAccountStatus.PENDING, "Lý do hợp lệ đủ dài"),
                "manager@kizuna.vn",
                null
        )).isInstanceOf(BadRequestException.class);

        UUID staffId = UUID.randomUUID();
        User csStaff = User.builder()
                .userId(staffId)
                .email("cs@kizuna.vn")
                .role(Role.builder().roleName("ROLE_CS_STAFF").build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        when(userRepository.findById(staffId)).thenReturn(Optional.of(csStaff));

        assertThatThrownBy(() -> service.updateUserStatus(
                staffId,
                new UpdateUserStatusRequest(UserAccountStatus.SUSPENDED, "Lý do hợp lệ đủ dài"),
                "manager@kizuna.vn",
                null
        )).isInstanceOf(AccessDeniedException.class);

        verify(userRepository, never()).saveAndFlush(any());
    }
}
