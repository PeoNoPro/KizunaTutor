package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.profile.controller.LearnerProfileController;
import com.kizuna.features.profile.dto.request.UpdateLearnerProfileRequest;
import com.kizuna.features.profile.dto.response.LearnerProfileResponseDto;
import com.kizuna.features.profile.service.LearnerProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LearnerProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class LearnerProfileControllerTest {

    private static final String EMAIL = "learner@kizuna.vn";
    private static final String VALID_BODY = """
            {"fullName":"Nguyễn Văn An","phone":"0987654321","avatarUrl":"","currentLevel":"N5",
             "targetLevel":"N3","learningGoals":"Luyện ngữ pháp N3"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearnerProfileService learnerProfileService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private OperationsActorResolver operationsActorResolver;

    /** Giống JwtAuthenticationFilter: principal là User, quyền là tên vai trò. */
    private static RequestPostProcessor as(String role) {
        User user = User.builder().userId(UUID.randomUUID()).email(EMAIL)
                .role(Role.builder().roleName(role).build()).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
    }

    private static LearnerProfileResponseDto sample() {
        return new LearnerProfileResponseDto(UUID.randomUUID(), EMAIL, "Nguyễn Văn An", "0987654321", null,
                "N5", "N3", "Luyện ngữ pháp N3", "Hà Nội", "Cầu Giấy", OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Test
    @DisplayName("Học viên xem hồ sơ của mình - 200, dùng email trong phiên đăng nhập")
    void learnerGetsOwnProfile() throws Exception {
        when(learnerProfileService.getMyProfile(EMAIL)).thenReturn(sample());

        mockMvc.perform(get("/api/profile/learner/me").with(as("ROLE_LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.currentLevel").value("N5"));
    }

    @Test
    @DisplayName("Học viên cập nhật hồ sơ hợp lệ - 200 kèm thông điệp tiếng Việt")
    void learnerUpdatesProfile() throws Exception {
        when(learnerProfileService.updateMyProfile(eq(EMAIL), any(UpdateLearnerProfileRequest.class))).thenReturn(sample());

        mockMvc.perform(put("/api/profile/learner/me").with(as("ROLE_LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ học viên thành công."));
    }

    @Test
    @DisplayName("Dữ liệu sai (họ tên trống, số điện thoại sai) - 400 kèm lỗi từng trường, không gọi service")
    void rejectsInvalidInput() throws Exception {
        String body = """
                {"fullName":"  ","phone":"12345","currentLevel":"N6","targetLevel":"BEGINNER"}""";

        mockMvc.perform(put("/api/profile/learner/me").with(as("ROLE_LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='phone')].message").value("Số điện thoại không đúng định dạng 10 chữ số."))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='fullName')].message").value("Vui lòng nhập họ và tên."));
        verify(learnerProfileService, never()).updateMyProfile(any(), any());
    }

    @Test
    @DisplayName("Gia sư gọi API hồ sơ học viên - 403")
    void tutorIsForbidden() throws Exception {
        mockMvc.perform(get("/api/profile/learner/me").with(as("ROLE_TUTOR")))
                .andExpect(status().isForbidden());
        verify(learnerProfileService, never()).getMyProfile(any());
    }

    @Test
    @DisplayName("Chưa đăng nhập - bị từ chối, không gọi service")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/profile/learner/me"))
                .andExpect(status().isForbidden());
        verify(learnerProfileService, never()).getMyProfile(any());
    }
}
