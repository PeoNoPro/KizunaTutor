package com.kizuna.features.verification;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ma trận xác thực/phân quyền cho route FE-03 (FR-019, FR-020, GB-05).
 * Dùng controller thăm dò để kiểm tra riêng tầng bảo mật, không chạm DB.
 */
@WebMvcTest(controllers = VerificationSecurityTest.ProbeController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class,
        VerificationSecurityTest.ProbeController.class})
class VerificationSecurityTest {

    @RestController
    static class ProbeController {
        @GetMapping("/api/tutor/verification/probe")
        String tutor() {
            return "ok";
        }

        @GetMapping("/api/academic/verifications/probe")
        String academic() {
            return "ok";
        }

        @GetMapping("/api/tutors/probe")
        String publicSearch() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private void givenToken(String token, String roleName, UserAccountStatus accountStatus) {
        String email = token + "@kizuna.test";
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email(email)
                .fullName("Người dùng thử")
                .role(Role.builder().roleName(roleName).build())
                .accountStatus(accountStatus)
                .build();
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void missingTokenIsUnauthorizedOnFe03Routes() throws Exception {
        mockMvc.perform(get("/api/tutor/verification/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/academic/verifications/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsUnauthorized() throws Exception {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);
        mockMvc.perform(get("/api/tutor/verification/probe").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tutorCanReachTutorRoutesButNotAcademicRoutes() throws Exception {
        givenToken("tutor", "ROLE_TUTOR", UserAccountStatus.ACTIVE);
        mockMvc.perform(get("/api/tutor/verification/probe").header("Authorization", "Bearer tutor"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/academic/verifications/probe").header("Authorization", "Bearer tutor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void academicOfficerCanReachAcademicRoutesButNotTutorRoutes() throws Exception {
        givenToken("officer", "ROLE_ACADEMIC_OFFICER", UserAccountStatus.ACTIVE);
        mockMvc.perform(get("/api/academic/verifications/probe").header("Authorization", "Bearer officer"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tutor/verification/probe").header("Authorization", "Bearer officer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void learnerAndAdminAreForbidden() throws Exception {
        givenToken("learner", "ROLE_LEARNER", UserAccountStatus.ACTIVE);
        givenToken("admin", "ROLE_ADMIN", UserAccountStatus.ACTIVE);
        mockMvc.perform(get("/api/tutor/verification/probe").header("Authorization", "Bearer learner"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/academic/verifications/probe").header("Authorization", "Bearer admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedAccountIsTreatedAsUnauthenticated() throws Exception {
        givenToken("suspended", "ROLE_TUTOR", UserAccountStatus.SUSPENDED);
        mockMvc.perform(get("/api/tutor/verification/probe").header("Authorization", "Bearer suspended"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routesOutsideFe03StayPublic() throws Exception {
        mockMvc.perform(get("/api/tutors/probe"))
                .andExpect(status().isOk());
    }
}
