package com.kizuna.features.profile.unit;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.profile.service.AvatarStorageService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Dữ liệu và mock dùng chung cho unit test FE-02 (hồ sơ học viên / gia sư).
 * Repository và kho ảnh đại diện là mock; kiểm tra dữ liệu đầu vào (Bean Validation trên DTO) chạy bằng Validator thật.
 */
abstract class ProfileUnitTestBase {
    static final String EMAIL = "user@test.com";
    static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final String OLD_AVATAR = "/api/profile/avatar/" + USER_ID + "_11111111-1111-1111-1111-111111111111.jpg";
    static final String NEW_AVATAR = "/api/profile/avatar/" + USER_ID + "_22222222-2222-2222-2222-222222222222.jpg";

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    final UserRepository users = mock(UserRepository.class);
    final LearnerRepository learners = mock(LearnerRepository.class);
    final TutorRepository tutors = mock(TutorRepository.class);
    final AvatarStorageService avatars = mock(AvatarStorageService.class);

    {
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    static User user(String roleName) {
        return User.builder().userId(USER_ID).email(EMAIL).fullName("Nguyen Van A")
                .role(Role.builder().roleName(roleName).build()).avatarUrl(OLD_AVATAR).build();
    }

    /** Đăng nhập bằng tài khoản có vai trò roleName. */
    User loggedIn(String roleName) {
        User u = user(roleName);
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(u));
        return u;
    }

    /** SĐT đã thuộc tài khoản khác hay chưa (truy vấn exists theo Specification). */
    @SuppressWarnings("unchecked")
    void phoneTaken(boolean value) {
        when(users.exists(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(value);
    }

    /** Các lỗi Bean Validation trên DTO (rỗng = hợp lệ). */
    static <T> Set<ConstraintViolation<T>> violations(T dto) {
        return VALIDATOR.validate(dto);
    }

    /** Lỗi duy nhất có đúng thông báo message trên trường field. */
    static <T> boolean hasError(T dto, String field, String message) {
        return violations(dto).stream().anyMatch(v -> v.getPropertyPath().toString().equals(field) && v.getMessage().equals(message));
    }

    static String chars(int n) { return "a".repeat(n); }
}
