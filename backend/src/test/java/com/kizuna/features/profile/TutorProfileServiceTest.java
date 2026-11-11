package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.service.impl.TutorProfileServiceImpl;
import com.kizuna.features.tutor_search.service.TutorSearchService;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorProfileServiceTest {

    private static final String EMAIL = "tutor.ha@kizuna.vn";

    @Mock
    private UserRepository userRepository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private TutorSearchService tutorSearchService;
    @InjectMocks
    private TutorProfileServiceImpl service;

    private User tutorUser;

    @BeforeEach
    void setUp() {
        tutorUser = User.builder()
                .userId(UUID.randomUUID())
                .email(EMAIL)
                .fullName("Nguyễn Thu Hà")
                .phone("0912345678")
                .role(Role.builder().roleName("ROLE_TUTOR").build())
                .build();
        lenient().when(tutorSearchService.getAvailableLocations())
                .thenReturn(List.of("Hà Nội", "TP. Hồ Chí Minh", "Đà Nẵng"));
    }

    private static UpdateTutorProfileRequest validRequest(String phone, String province, String detail) {
        return new UpdateTutorProfileRequest(
                "  Nguyễn Thu Hà  ",
                phone,
                "/api/profile/avatar/ha.png",
                "  Gia sư tiếng Nhật giao tiếp chuyên sâu  ",
                "  Cử nhân Nhật Bản học  ",
                4,
                new BigDecimal("280000"),
                "N2",
                province,
                detail
        );
    }

    @Test
    @DisplayName("Lấy hồ sơ gia sư: trả đầy đủ thông tin tài khoản, giảng dạy và tách Tỉnh/Quận")
    void getsTutorProfileWithParsedAddress() {
        Tutor tutor = Tutor.builder()
                .tutorId(UUID.randomUUID())
                .tutorCode(10001L)
                .user(tutorUser)
                .headline("Gia sư JLPT N2")
                .bio("Tận tâm")
                .experienceYears((short) 3)
                .hourlyRate(new BigDecimal("250000"))
                .highestJlptLevel("N2")
                .address("Hà Nội - Cầu Giấy")
                .avgRating(new BigDecimal("4.9"))
                .totalReviews(12)
                .verificationStatus(TutorVerificationStatus.APPROVED)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser_UserId(tutorUser.getUserId())).thenReturn(Optional.of(tutor));

        TutorProfileDetailDto result = service.getMyProfile(EMAIL);

        assertThat(result.email()).isEqualTo(EMAIL);
        assertThat(result.tutorCode()).isEqualTo(10001L);
        assertThat(result.province()).isEqualTo("Hà Nội");
        assertThat(result.addressDetail()).isEqualTo("Cầu Giấy");
        assertThat(result.fullAddress()).isEqualTo("Hà Nội - Cầu Giấy");
        assertThat(result.experienceYears()).isEqualTo(3);
        assertThat(result.verificationStatus()).isEqualTo("APPROVED");
        assertThat(result.isOwner()).isTrue();
    }

    @Test
    @DisplayName("Cập nhật hồ sơ gia sư đồng bộ users và tutors, giữ nguyên email và ghép địa chỉ chuẩn")
    void updatesUserAndTutorSynchronously() {
        Tutor existing = Tutor.builder().tutorId(UUID.randomUUID()).user(tutorUser).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(tutorUser));
        when(tutorRepository.findByUser_UserId(tutorUser.getUserId())).thenReturn(Optional.of(existing));
        when(userRepository.exists(any(Specification.class))).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tutorRepository.saveAndFlush(any(Tutor.class))).thenAnswer(inv -> inv.getArgument(0));

        TutorProfileDetailDto result = service.updateMyProfile(
                EMAIL,
                validRequest("0988777666", "TP. Hồ Chí Minh", "Quận 1")
        );

        ArgumentCaptor<Tutor> tutorCaptor = ArgumentCaptor.forClass(Tutor.class);
        verify(tutorRepository).saveAndFlush(tutorCaptor.capture());
        assertThat(tutorUser.getEmail()).isEqualTo(EMAIL);
        assertThat(tutorUser.getPhone()).isEqualTo("0988777666");
        assertThat(tutorCaptor.getValue().getPhone()).isEqualTo("0988777666");
        assertThat(tutorCaptor.getValue().getAddress()).isEqualTo("TP. Hồ Chí Minh - Quận 1");
        assertThat(tutorCaptor.getValue().getExperienceYears()).isEqualTo((short) 4);
        assertThat(result.province()).isEqualTo("TP. Hồ Chí Minh");
        assertThat(result.addressDetail()).isEqualTo("Quận 1");
    }

    @Test
    @DisplayName("Từ chối cập nhật nếu số điện thoại trùng với người dùng khác (409)")
    void rejectsDuplicatePhone() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(tutorUser));
        when(userRepository.exists(any(Specification.class))).thenReturn(true);

        assertThatThrownBy(() -> service.updateMyProfile(EMAIL, validRequest("0900000001", "Hà Nội", "Ba Đình")))
                .isInstanceOf(ConflictException.class);
        verify(tutorRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Từ chối Tỉnh/Thành phố không nằm trong danh sách hỗ trợ (400)")
    void rejectsUnsupportedProvince() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> service.updateMyProfile(EMAIL, validRequest("0912345678", "Sao Hỏa", "Khu 1")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tỉnh/Thành phố");
        verify(tutorRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("GB-15: từ chối địa chỉ chứa số nhà, tên đường hoặc tọa độ GPS")
    void rejectsDetailedStreetAddressOrGps() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(tutorUser));

        assertThatThrownBy(() -> service.updateMyProfile(
                EMAIL, validRequest("0912345678", "Hà Nội", "Số nhà 12 ngõ 5 Xuân Thủy")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.updateMyProfile(
                EMAIL, validRequest("0912345678", "Hà Nội", "Đường Láng")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.updateMyProfile(
                EMAIL, validRequest("0912345678", "Hà Nội", "21.0285, 105.8542")))
                .isInstanceOf(BadRequestException.class);
        verify(tutorRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Không tìm thấy hoặc sai vai trò trả đúng ngoại lệ")
    void guardsMissingOrNonTutorUser() {
        when(userRepository.findByEmail("missing@kizuna.vn")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getMyProfile("missing@kizuna.vn"))
                .isInstanceOf(ResourceNotFoundException.class);

        User learner = User.builder()
                .userId(UUID.randomUUID())
                .email("learner@kizuna.vn")
                .role(Role.builder().roleName("ROLE_LEARNER").build())
                .build();
        when(userRepository.findByEmail("learner@kizuna.vn")).thenReturn(Optional.of(learner));
        assertThatThrownBy(() -> service.getMyProfile("learner@kizuna.vn"))
                .isInstanceOf(AccessDeniedException.class);
    }
}
