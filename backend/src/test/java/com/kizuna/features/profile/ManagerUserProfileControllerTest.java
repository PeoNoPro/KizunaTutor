package com.kizuna.features.profile;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.profile.controller.ManagerUserProfileController;
import com.kizuna.features.profile.dto.request.UpdateUserStatusRequest;
import com.kizuna.features.profile.dto.response.PageResponse;
import com.kizuna.features.profile.dto.response.UserProfileDetailDto;
import com.kizuna.features.profile.dto.response.UserProfileListItemDto;
import com.kizuna.features.profile.dto.response.UserStatusUpdateResponseDto;
import com.kizuna.features.profile.service.ManagerUserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerUserProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class ManagerUserProfileControllerTest {

    private static final String MANAGER_EMAIL = "manager@kizuna.vn";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManagerUserProfileService managerUserProfileService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private OperationsActorResolver operationsActorResolver;

    private static RequestPostProcessor as(String role) {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email(MANAGER_EMAIL)
                .fullName("Quản lý vận hành")
                .role(Role.builder().roleName(role).build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @Test
    @DisplayName("ROLE_MANAGER và ROLE_ADMIN được xem danh sách và chi tiết người dùng - 200")
    void managerAndAdminCanListAndViewUsers() throws Exception {
        UUID id = UUID.randomUUID();
        var list = new PageResponse<>(
                List.of(new UserProfileListItemDto(id, "an@kizuna.vn", "Nguyễn Văn An", "0987654321", null,
                        "ROLE_LEARNER", "Học viên", UserAccountStatus.ACTIVE, OffsetDateTime.now())),
                0, 10, 1, 1, true);
        when(managerUserProfileService.getUsers("ROLE_LEARNER", "ACTIVE", "an", 0, 10)).thenReturn(list);

        mockMvc.perform(get("/api/profile/users")
                        .param("role", "ROLE_LEARNER")
                        .param("status", "ACTIVE")
                        .param("keyword", "an")
                        .with(as("ROLE_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].roleBadge").value("Học viên"));

        var detail = new UserProfileDetailDto(
                id, "an@kizuna.vn", "Nguyễn Văn An", "0987654321", null,
                "ROLE_LEARNER", "Học viên", UserAccountStatus.ACTIVE,
                OffsetDateTime.now(), OffsetDateTime.now(),
                new UserProfileDetailDto.LearnerDetails("N5", "N3", "Giao tiếp"),
                null,
                List.of());
        when(managerUserProfileService.getUserDetail(id)).thenReturn(detail);

        mockMvc.perform(get("/api/profile/users/{id}", id).with(as("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learnerDetails.targetLevel").value("N3"));
    }

    @Test
    @DisplayName("Cập nhật trạng thái hợp lệ - 200 kèm thông điệp tiếng Việt")
    void updatesStatusWhenValid() throws Exception {
        UUID id = UUID.randomUUID();
        var res = new UserStatusUpdateResponseDto(id, UserAccountStatus.SUSPENDED,
                "Vi phạm quy định hủy lịch học liên tục.", OffsetDateTime.now());
        when(managerUserProfileService.updateUserStatus(eq(id), any(UpdateUserStatusRequest.class), eq(MANAGER_EMAIL), any()))
                .thenReturn(res);

        mockMvc.perform(put("/api/profile/users/{id}/status", id)
                        .with(as("ROLE_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDED","reason":"Vi phạm quy định hủy lịch học liên tục."}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái tài khoản thành công."))
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));
    }

    @Test
    @DisplayName("Thiếu lý do hoặc lý do ngắn < 5 ký tự - 400, không gọi service")
    void rejectsShortOrBlankReason() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/profile/users/{id}/status", id)
                        .with(as("ROLE_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BANNED","reason":"Ngắn"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='reason')].message").exists());

        verify(managerUserProfileService, never()).updateUserStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Gia sư, học viên, CS staff hoặc chưa đăng nhập bị chặn - 403")
    void rejectsUnauthorizedRoles() throws Exception {
        mockMvc.perform(get("/api/profile/users").with(as("ROLE_TUTOR"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profile/users").with(as("ROLE_LEARNER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profile/users").with(as("ROLE_CS_STAFF"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profile/users")).andExpect(status().isForbidden());
        verify(managerUserProfileService, never()).getUsers(any(), any(), any(), any(int.class), any(int.class));
    }
}
