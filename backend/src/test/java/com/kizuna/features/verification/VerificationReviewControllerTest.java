package com.kizuna.features.verification;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.verification.controller.VerificationExceptionHandler;
import com.kizuna.features.verification.controller.VerificationReviewController;
import com.kizuna.features.verification.dto.response.DecisionResultResponse;
import com.kizuna.features.verification.dto.response.PageResponse;
import com.kizuna.features.verification.dto.response.VerificationQueueItemResponse;
import com.kizuna.features.verification.dto.response.VerificationSummaryResponse;
import com.kizuna.features.verification.service.VerificationException;
import com.kizuna.features.verification.service.VerificationReviewService;
import com.kizuna.features.verification.dto.request.VerificationQueueQuery;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hợp đồng JSON của API thẩm định phía cán bộ học vụ (contracts/verification-api.md). Không chạm DB.
 */
@WebMvcTest(controllers = VerificationReviewController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class,
        VerificationExceptionHandler.class})
class VerificationReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerificationReviewService service;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private static RequestPostProcessor as(String role) {
        User user = User.builder().userId(UUID.randomUUID()).email("u@kizuna.test").fullName("Người dùng")
                .role(Role.builder().roleName(role).build()).accountStatus(UserAccountStatus.ACTIVE).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void tutorCannotAccessOfficerApi() throws Exception {
        mockMvc.perform(get("/api/academic/verifications/summary").with(as("ROLE_TUTOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void summaryReturnsCounts() throws Exception {
        when(service.getSummary()).thenReturn(VerificationSummaryResponse.builder()
                .totalTutors(10).pendingCount(3).pendingNewToday(1).needsInfoCount(2).approvedCount(4).build());
        mockMvc.perform(get("/api/academic/verifications/summary").with(as("ROLE_ACADEMIC_OFFICER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingCount").value(3))
                .andExpect(jsonPath("$.pendingNewToday").value(1));
    }

    @Test
    void queuePassesFiltersAndReturnsPage() throws Exception {
        when(service.getQueue(any())).thenReturn(new PageResponse<>(List.of(VerificationQueueItemResponse.builder()
                .tutorId(UUID.randomUUID()).fullName("Gia sư B").statusLabel("Đang chờ thẩm định").build()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/academic/verifications").with(as("ROLE_ACADEMIC_OFFICER"))
                        .param("status", "PENDING").param("keyword", "gia sư").param("jlptLevel", "N2")
                        .param("submittedFrom", "2026-11-01").param("sort", "newest").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].statusLabel").value("Đang chờ thẩm định"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<VerificationQueueQuery> captor = ArgumentCaptor.forClass(VerificationQueueQuery.class);
        verify(service).getQueue(captor.capture());
        assertEquals("N2", captor.getValue().getJlptLevel());
        assertEquals(LocalDate.of(2026, 11, 1), captor.getValue().getSubmittedFrom());
        assertEquals("newest", captor.getValue().getSort());
    }

    @Test
    void secondDecisionReturnsConflictWithGb01Message() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.approve(id)).thenThrow(VerificationException.alreadyProcessed());
        mockMvc.perform(post("/api/academic/verifications/{id}/approve", id).with(as("ROLE_ACADEMIC_OFFICER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(VerificationException.MSG_ALREADY_PROCESSED));
    }

    @Test
    void rejectValidationReturnsSpecMessageAndFields() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.reject(eq(id), any())).thenThrow(VerificationException.validation(
                "Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận.", Map.of("reasonCode", "Vui lòng chọn lý do từ chối.")));
        mockMvc.perform(post("/api/academic/verifications/{id}/reject", id).with(as("ROLE_ACADEMIC_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận."))
                .andExpect(jsonPath("$.fields.reasonCode").exists());
    }

    @Test
    void requestInfoReturnsSuccessMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.requestInfo(eq(id), any())).thenReturn(DecisionResultResponse.builder()
                .message("Đã gửi yêu cầu bổ sung thông tin.").status("ADDITIONAL_INFO_REQUIRED").statusLabel("Cần bổ sung thông tin").build());
        mockMvc.perform(post("/api/academic/verifications/{id}/request-info", id).with(as("ROLE_ACADEMIC_OFFICER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Ảnh CCCD bị mờ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã gửi yêu cầu bổ sung thông tin."));
    }

    @Test
    void invalidDateParameterIsFriendlyValidationError() throws Exception {
        mockMvc.perform(get("/api/academic/verifications").with(as("ROLE_ACADEMIC_OFFICER")).param("submittedFrom", "03/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
}
