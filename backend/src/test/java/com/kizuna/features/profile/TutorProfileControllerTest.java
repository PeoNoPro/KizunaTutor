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
import com.kizuna.features.profile.controller.TutorProfileController;
import com.kizuna.features.profile.dto.request.UpdateTutorProfileRequest;
import com.kizuna.features.profile.dto.response.TutorProfileDetailDto;
import com.kizuna.features.profile.service.TutorProfileService;
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

import java.math.BigDecimal;
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

@WebMvcTest(TutorProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class TutorProfileControllerTest {

    private static final String EMAIL = "tutor.ha@kizuna.vn";

    private static final String VALID_BODY = """
            {
              "fullName": "Nguyễn Thu Hà",
              "phone": "0912345678",
              "avatarUrl": "/api/profile/avatar/ha.png",
              "headline": "Gia sư tiếng Nhật giao tiếp chuyên sâu",
              "bio": "Cử nhân Nhật Bản học, 4 năm giảng dạy.",
              "experienceYears": 4,
              "hourlyRate": 280000,
              "highestJlptLevel": "N2",
              "province": "Hà Nội",
              "addressDetail": "Cầu Giấy"
            }""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TutorProfileService tutorProfileService;
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
                .email(EMAIL)
                .fullName("Nguyễn Thu Hà")
                .role(Role.builder().roleName(role).build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role)));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    private static TutorProfileDetailDto sample() {
        return new TutorProfileDetailDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                10001L,
                EMAIL,
                "Nguyễn Thu Hà",
                "0912345678",
                "/api/profile/avatar/ha.png",
                "Gia sư tiếng Nhật giao tiếp chuyên sâu",
                "Cử nhân Nhật Bản học",
                4,
                new BigDecimal("280000"),
                "N2",
                "Hà Nội",
                "Cầu Giấy",
                "Hà Nội - Cầu Giấy",
                new BigDecimal("5.0"),
                0,
                "APPROVED",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    @DisplayName("Gia sư xem hồ sơ giảng dạy của mình - 200")
    void tutorGetsOwnProfile() throws Exception {
        when(tutorProfileService.getMyProfile(EMAIL)).thenReturn(sample());

        mockMvc.perform(get("/api/profile/tutor/me").with(as("ROLE_TUTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andExpect(jsonPath("$.data.province").value("Hà Nội"))
                .andExpect(jsonPath("$.data.isOwner").value(true));
    }

    @Test
    @DisplayName("Gia sư cập nhật hồ sơ giảng dạy hợp lệ - 200 kèm thông điệp tiếng Việt")
    void tutorUpdatesProfile() throws Exception {
        when(tutorProfileService.updateMyProfile(eq(EMAIL), any(UpdateTutorProfileRequest.class)))
                .thenReturn(sample());

        mockMvc.perform(put("/api/profile/tutor/me").with(as("ROLE_TUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cập nhật hồ sơ giảng dạy thành công."))
                .andExpect(jsonPath("$.data.hourlyRate").value(280000));
    }

    @Test
    @DisplayName("Dữ liệu sai (học phí âm, kinh nghiệm > 50, JLPT N6) - 400 kèm fieldErrors")
    void rejectsInvalidInput() throws Exception {
        String body = """
                {
                  "fullName": "Nguyễn Thu Hà",
                  "phone": "12345",
                  "headline": " ",
                  "experienceYears": 65,
                  "hourlyRate": -50000,
                  "highestJlptLevel": "N6",
                  "province": ""
                }""";

        mockMvc.perform(put("/api/profile/tutor/me").with(as("ROLE_TUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='hourlyRate')].message").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='experienceYears')].message").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='highestJlptLevel')].message").exists());

        verify(tutorProfileService, never()).updateMyProfile(any(), any());
    }

    @Test
    @DisplayName("Học viên hoặc khách chưa đăng nhập gọi API hồ sơ gia sư - 403")
    void rejectsNonTutorOrAnonymous() throws Exception {
        mockMvc.perform(get("/api/profile/tutor/me").with(as("ROLE_LEARNER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/profile/tutor/me"))
                .andExpect(status().isForbidden());
        verify(tutorProfileService, never()).getMyProfile(any());
    }
}
