package com.kizuna.features.profile;

import com.kizuna.core.entity.Learner;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;
import com.kizuna.features.profile.service.impl.LearnerProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
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
class LearnerProfileServiceTest {

    private static final String EMAIL = "learner@kizuna.vn";

    @Mock
    private UserRepository userRepository;
    @Mock
    private LearnerRepository learnerRepository;
    @InjectMocks
    private LearnerProfileServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Nguyễn Văn An")
                .phone("0987654321")
                .role(Role.builder().roleName("ROLE_LEARNER").build())
                .build();
    }

    private static UpdateLearnerProfileRequest request(String phone) {
        return new UpdateLearnerProfileRequest("  Nguyễn Văn Bình ", phone, "", "N5", "N2", "Luyện thi N2");
    }

    @Test
    @DisplayName("Lấy hồ sơ: gộp thông tin tài khoản và học tập của chính học viên")
    void getsOwnProfile() {
        Learner learner = Learner.builder().user(user).currentLevel("BEGINNER").targetLevel("N3").learningGoals("Giao tiếp").build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(learnerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(learner));

        LearnerProfileResponseDto dto = service.getMyProfile(EMAIL);

        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.fullName()).isEqualTo("Nguyễn Văn An");
        assertThat(dto.currentLevel()).isEqualTo("BEGINNER");
        assertThat(dto.targetLevel()).isEqualTo("N3");
        assertThat(dto.learningGoals()).isEqualTo("Giao tiếp");
    }

    @Test
    @DisplayName("Lấy hồ sơ khi chưa có bản ghi học tập: trả các trường học tập rỗng, không tạo bản ghi")
    void getsProfileWithoutLearnerRow() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(learnerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());

        LearnerProfileResponseDto dto = service.getMyProfile(EMAIL);

        assertThat(dto.currentLevel()).isNull();
        verify(learnerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tài khoản không phải học viên hoặc đã bị xóa thì bị từ chối")
    void rejectsNonLearnerOrDeleted() {
        user.setRole(Role.builder().roleName("ROLE_TUTOR").build());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.getMyProfile(EMAIL)).isInstanceOf(AccessDeniedException.class);

        user.setDeletedAt(java.time.OffsetDateTime.now());
        assertThatThrownBy(() -> service.getMyProfile(EMAIL)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Cập nhật hợp lệ: ghi users và learners, cắt khoảng trắng, chuỗi rỗng thành null, giữ nguyên email")
    void updatesUserAndLearner() {
        Learner learner = Learner.builder().user(user).currentLevel("BEGINNER").build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.exists(any(Specification.class))).thenReturn(false);
        when(learnerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.of(learner));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(learnerRepository.saveAndFlush(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));

        LearnerProfileResponseDto dto = service.updateMyProfile(EMAIL, request("0912345678"));

        assertThat(dto.email()).isEqualTo(EMAIL);
        assertThat(dto.fullName()).isEqualTo("Nguyễn Văn Bình");
        assertThat(dto.phone()).isEqualTo("0912345678");
        assertThat(dto.avatarUrl()).isNull();
        assertThat(dto.currentLevel()).isEqualTo("N5");
        assertThat(dto.targetLevel()).isEqualTo("N2");
        assertThat(dto.learningGoals()).isEqualTo("Luyện thi N2");
    }

    @Test
    @DisplayName("Cập nhật khi chưa có bản ghi học tập: tạo mới bản ghi learners gắn với tài khoản")
    void createsLearnerRowOnUpdate() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(learnerRepository.findByUser_UserId(user.getUserId())).thenReturn(Optional.empty());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(learnerRepository.saveAndFlush(any(Learner.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateMyProfile(EMAIL, request(""));

        ArgumentCaptor<Learner> saved = ArgumentCaptor.forClass(Learner.class);
        verify(learnerRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(user.getPhone()).isNull();
    }

    @Test
    @DisplayName("Số điện thoại đã thuộc tài khoản khác: báo xung đột, không lưu")
    void rejectsPhoneUsedByAnotherAccount() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userRepository.exists(any(Specification.class))).thenReturn(true);

        assertThatThrownBy(() -> service.updateMyProfile(EMAIL, request("0912345678")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Số điện thoại này đã được liên kết với một tài khoản khác trong hệ thống.");
        verify(userRepository, never()).saveAndFlush(any());
    }
}
