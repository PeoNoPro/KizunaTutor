package com.kizuna.features.profile;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.response.PageResponse;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.repository.ProfileAuditLogRepository;
import com.kizuna.features.profile.service.impl.ManagerUserProfileServiceImpl;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerUserProfileServiceTest {

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

    @Test
    @DisplayName("Tra cứu danh sách người dùng trả về phân trang và huy hiệu vai trò tiếng Việt")
    void listsUsersPaginated() {
        User learner = User.builder()
                .userId(UUID.randomUUID())
                .email("an@kizuna.vn")
                .fullName("Nguyễn Văn An")
                .phone("0987654321")
                .role(Role.builder().roleName("ROLE_LEARNER").build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        learner.setCreatedAt(OffsetDateTime.now());
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(learner)));

        PageResponse<UserProfileListItemDto> result = service.getUsers("ALL", "ALL", "an", 0, 10);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).roleBadge()).isEqualTo("Học viên");
        assertThat(result.content().get(0).accountStatus()).isEqualTo(UserAccountStatus.ACTIVE);
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Từ chối bộ lọc vai trò nội bộ hoặc trạng thái không hợp lệ")
    void rejectsUnsupportedRoleOrStatusFilters() {
        assertThatThrownBy(() -> service.getUsers("ROLE_ADMIN", "ALL", null, 0, 10))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.getUsers("ALL", "UNKNOWN_STATUS", null, 0, 10))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Xem chi tiết gia sư kèm thông tin giảng dạy và lịch sử trạng thái")
    void getsTutorDetailWithHistory() {
        UUID userId = UUID.randomUUID();
        User tutorUser = User.builder()
                .userId(userId)
                .email("tutor@kizuna.vn")
                .fullName("Trần Minh")
                .role(Role.builder().roleName("ROLE_TUTOR").build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        Tutor tutor = Tutor.builder()
                .tutorId(UUID.randomUUID())
                .tutorCode(10005L)
                .user(tutorUser)
                .headline("Gia sư N1")
                .hourlyRate(new BigDecimal("300000"))
                .experienceYears((short) 5)
                .highestJlptLevel("N1")
                .address("Hà Nội - Ba Đình")
                .verificationStatus(TutorVerificationStatus.APPROVED)
                .build();
        var historyItem = new UserProfileDetailDto.StatusHistoryItem(
                "USER_STATUS_CHANGE", "SUSPENDED", "ACTIVE", "Đã bổ sung hồ sơ hợp lệ.", "Quản lý A", OffsetDateTime.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser_UserId(userId)).thenReturn(Optional.of(tutor));
        when(profileAuditLogRepository.findUserStatusHistory(userId)).thenReturn(List.of(historyItem));

        UserProfileDetailDto detail = service.getUserDetail(userId);

        assertThat(detail.roleBadge()).isEqualTo("Gia sư");
        assertThat(detail.learnerDetails()).isNull();
        assertThat(detail.tutorDetails()).isNotNull();
        assertThat(detail.tutorDetails().fullAddress()).isEqualTo("Hà Nội - Ba Đình");
        assertThat(detail.statusHistory()).hasSize(1);
    }

    @Test
    @DisplayName("Chặn xem chi tiết tài khoản nội bộ (ROLE_ADMIN) và báo 404 nếu không tồn tại")
    void guardsInternalStaffAndMissingUser() {
        UUID staffId = UUID.randomUUID();
        User staff = User.builder()
                .userId(staffId)
                .role(Role.builder().roleName("ROLE_ADMIN").build())
                .build();
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.getUserDetail(staffId))
                .isInstanceOf(AccessDeniedException.class);

        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getUserDetail(missingId))
                .isInstanceOf(ResourceNotFoundException.class);

        UUID learnerId = UUID.randomUUID();
        User learnerUser = User.builder()
                .userId(learnerId)
                .role(Role.builder().roleName("ROLE_LEARNER").build())
                .build();
        when(userRepository.findById(learnerId)).thenReturn(Optional.of(learnerUser));
        when(learnerRepository.findByUser_UserId(learnerId))
                .thenReturn(Optional.of(Learner.builder().currentLevel("N5").targetLevel("N3").build()));
        when(profileAuditLogRepository.findUserStatusHistory(learnerId)).thenReturn(List.of());

        UserProfileDetailDto detail = service.getUserDetail(learnerId);
        assertThat(detail.learnerDetails()).isNotNull();
        assertThat(detail.tutorDetails()).isNull();
    }
}
