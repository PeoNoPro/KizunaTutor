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
import com.kizuna.features.profile.controller.ProfilePasswordController;
import com.kizuna.features.profile.service.ProfilePasswordService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfilePasswordController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class ProfilePasswordControllerTest {

    private static final String EMAIL = "user@kizuna.vn";
    private static final String VALID_BODY = """
            {"currentPassword":"MatKhauCu123","newPassword":"MatKhauMoi456","confirmPassword":"MatKhauMoi456"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfilePasswordService profilePasswordService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private OperationsActorResolver operationsActorResolver;

    private static RequestPostProcessor as(String role) {
        User user = User.builder().userId(UUID.randomUUID()).email(EMAIL).role(Role.builder().roleName(role).build()).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("Học viên và gia sư đổi mật khẩu - 200 kèm thông điệp tiếng Việt")
    void learnerAndTutorCanChangePassword() throws Exception {
        for (String role : List.of("ROLE_LEARNER", "ROLE_TUTOR")) {
            mockMvc.perform(put("/api/profile/password").with(as(role))
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công."));
        }
        verify(profilePasswordService, org.mockito.Mockito.times(2)).changeMyPassword(eq(EMAIL), any());
    }

    @Test
    @DisplayName("Mật khẩu mới yếu - 400 kèm lỗi trường, không gọi service")
    void rejectsWeakPassword() throws Exception {
        mockMvc.perform(put("/api/profile/password").with(as("ROLE_LEARNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"MatKhauCu123\",\"newPassword\":\"abc\",\"confirmPassword\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));
        verify(profilePasswordService, never()).changeMyPassword(any(), any());
    }

    @Test
    @DisplayName("Vai trò khác (quản lý) - 403")
    void managerIsForbidden() throws Exception {
        mockMvc.perform(put("/api/profile/password").with(as("ROLE_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
        verify(profilePasswordService, never()).changeMyPassword(any(), any());
    }
}
