package com.kizuna.features.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.booking.controller.TrialBookingController;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import com.kizuna.features.booking.service.TrialBookingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TrialBookingController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class})
class TrialBookingControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrialBookingService trialBookingService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.kizuna.core.security.JwtTokenVerifier jwtTokenVerifier;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.kizuna.features.admin.service.OperationsActorResolver operationsActorResolver;

    private static RequestPostProcessor as(String role, String email) {
        User user = User.builder()
                .userId(UUID.randomUUID())
                .email(email)
                .fullName("Test User")
                .role(Role.builder().roleName(role).build())
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();
        return authentication(new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    @DisplayName("Chưa đăng nhập gọi POST /api/v1/trial-bookings - Trả về 401 Unauthorized")
    void unauthenticatedCannotCreateTrialBooking() throws Exception {
        CreateTrialBookingRequestDTO dto = CreateTrialBookingRequestDTO.builder()
                .tutorId(UUID.randomUUID())
                .scheduledStartTime(OffsetDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/trial-bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tài khoản ROLE_TUTOR gọi POST /api/v1/trial-bookings - Bị chặn 403 Forbidden")
    void tutorCannotCreateTrialBooking() throws Exception {
        CreateTrialBookingRequestDTO dto = CreateTrialBookingRequestDTO.builder()
                .tutorId(UUID.randomUUID())
                .scheduledStartTime(OffsetDateTime.now().plusDays(1))
                .build();

        mockMvc.perform(post("/api/v1/trial-bookings")
                        .with(as("ROLE_TUTOR", "tutor@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tài khoản ROLE_LEARNER gọi POST /api/v1/trial-bookings - Thành công 201 Created")
    void learnerCanCreateTrialBooking() throws Exception {
        UUID tutorId = UUID.randomUUID();
        CreateTrialBookingRequestDTO dto = CreateTrialBookingRequestDTO.builder()
                .tutorId(tutorId)
                .scheduledStartTime(OffsetDateTime.now().plusDays(1))
                .durationMinutes(50)
                .build();

        TrialBookingResponseDTO responseDTO = TrialBookingResponseDTO.builder()
                .bookingId(UUID.randomUUID())
                .bookingStatus("PENDING")
                .statusLabel("Chờ gia sư xác nhận")
                .build();

        when(trialBookingService.createTrialBooking(eq("learner@test.com"), any(CreateTrialBookingRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/trial-bookings")
                        .with(as("ROLE_LEARNER", "learner@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Tài khoản ROLE_LEARNER gọi PUT /respond - Bị chặn 403 Forbidden")
    void learnerCannotRespondTrialBooking() throws Exception {
        UUID bookingId = UUID.randomUUID();
        RespondTrialBookingRequestDTO dto = RespondTrialBookingRequestDTO.builder()
                .action("ACCEPT")
                .meetingLink("https://meet.google.com/abc-xyz")
                .build();

        mockMvc.perform(put("/api/v1/trial-bookings/" + bookingId + "/respond")
                        .with(as("ROLE_LEARNER", "learner@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
