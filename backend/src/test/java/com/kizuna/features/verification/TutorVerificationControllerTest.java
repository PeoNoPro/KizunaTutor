package com.kizuna.features.verification;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.verification.controller.TutorVerificationController;
import com.kizuna.features.verification.controller.VerificationExceptionHandler;
import com.kizuna.features.verification.dto.response.LatestFeedbackDto;
import com.kizuna.features.verification.dto.response.TutorVerificationResponse;
import com.kizuna.features.verification.dto.response.VerificationDocumentResponse;
import com.kizuna.features.verification.dto.response.VerificationProfileDto;
import com.kizuna.features.verification.service.DocumentFile;
import com.kizuna.features.verification.service.TutorVerificationService;
import com.kizuna.features.verification.service.VerificationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Hợp đồng JSON của API xác minh phía gia sư (contracts/verification-api.md). Không chạm DB.
 */
@WebMvcTest(controllers = TutorVerificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class,
        VerificationExceptionHandler.class})
class TutorVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TutorVerificationService service;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserRepository userRepository;

    private static RequestPostProcessor asTutor() {
        User user = User.builder().userId(UUID.randomUUID()).email("gs@kizuna.test").fullName("Gia sư")
                .role(Role.builder().roleName("ROLE_TUTOR").build()).accountStatus(UserAccountStatus.ACTIVE).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_TUTOR"))));
    }

    private static TutorVerificationResponse sample(String status, String label, boolean editable) {
        return TutorVerificationResponse.builder()
                .status(status).statusLabel(label).editable(editable)
                .submittedAt(OffsetDateTime.parse("2026-11-03T09:15:00+07:00"))
                .profile(VerificationProfileDto.builder().fullName("Gia sư").experienceYears(3).highestJlptLevel("N2").build())
                .documents(List.of(VerificationDocumentResponse.builder()
                        .id(UUID.randomUUID()).type("JLPT_CERTIFICATE").typeLabel("Chứng chỉ JLPT")
                        .title("JLPT N2").fileName("JLPT N2.pdf").contentType("application/pdf")
                        .status("PENDING").statusLabel("Chờ thẩm định").fileAvailable(true).build()))
                .missingRequirements(List.of())
                .build();
    }

    @Test
    void getReturnsVietnameseLabelsAndNoStorageKeyOrInternalNote() throws Exception {
        when(service.getMyVerification()).thenReturn(sample("ADDITIONAL_INFO_REQUIRED", "Cần bổ sung thông tin", true)
                .toBuilder()
                .latestFeedback(LatestFeedbackDto.builder().kind("REQUEST_INFO").kindLabel("Yêu cầu bổ sung")
                        .content("Ảnh CCCD bị mờ").build())
                .build());

        mockMvc.perform(get("/api/tutor/verification").with(asTutor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("Cần bổ sung thông tin"))
                .andExpect(jsonPath("$.editable").value(true))
                .andExpect(jsonPath("$.latestFeedback.content").value("Ảnh CCCD bị mờ"))
                .andExpect(jsonPath("$.documents[0].typeLabel").value("Chứng chỉ JLPT"))
                .andExpect(jsonPath("$.documents[0].storageKey").doesNotExist())
                .andExpect(content().string(not(containsString("internalNote"))))
                .andExpect(content().string(not(containsString("notes"))));
    }

    @Test
    void getWithoutFeedbackReturnsNullLatestFeedback() throws Exception {
        when(service.getMyVerification()).thenReturn(sample("UNVERIFIED", "Chưa nộp hồ sơ", true));
        mockMvc.perform(get("/api/tutor/verification").with(asTutor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestFeedback").isEmpty());
    }

    @Test
    void invalidDraftFieldsReturnVietnameseFieldErrors() throws Exception {
        mockMvc.perform(put("/api/tutor/verification").with(asTutor())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"12345\",\"highestJlptLevel\":\"N9\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.phone").value("Số điện thoại gồm 10 chữ số và bắt đầu bằng số 0."))
                .andExpect(jsonPath("$.fields.highestJlptLevel").exists());
    }

    @Test
    void lockedProfileReturnsConflictWithSpecMessage() throws Exception {
        when(service.updateProfile(any())).thenThrow(VerificationException.locked());
        mockMvc.perform(put("/api/tutor/verification").with(asTutor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERIFICATION_LOCKED"))
                .andExpect(jsonPath("$.message").value(VerificationException.MSG_LOCKED));
    }

    @Test
    void uploadDocumentReturnsCreated() throws Exception {
        when(service.addDocument(eq("IDENTITY_DOCUMENT"), eq("CCCD"), any()))
                .thenReturn(VerificationDocumentResponse.builder().id(UUID.randomUUID())
                        .type("IDENTITY_DOCUMENT").typeLabel("Giấy tờ tùy thân").title("CCCD").build());
        MockMultipartFile file = new MockMultipartFile("file", "cccd.pdf", "application/pdf", "%PDF-".getBytes());

        mockMvc.perform(multipart("/api/tutor/verification/documents").file(file)
                        .param("type", "IDENTITY_DOCUMENT").param("title", "CCCD").with(asTutor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.typeLabel").value("Giấy tờ tùy thân"));
    }

    @Test
    void invalidFileReturnsMsg13() throws Exception {
        when(service.addDocument(any(), any(), any())).thenThrow(VerificationException.invalidFile());
        MockMultipartFile file = new MockMultipartFile("file", "virus.pdf", "application/pdf", new byte[]{1});
        mockMvc.perform(multipart("/api/tutor/verification/documents").file(file)
                        .param("type", "IDENTITY_DOCUMENT").param("title", "CCCD").with(asTutor()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE"))
                .andExpect(jsonPath("$.message").value(VerificationException.MSG_INVALID_FILE));
    }

    @Test
    void incompleteSubmissionListsMissingFieldsAndDocuments() throws Exception {
        when(service.submit(any())).thenThrow(VerificationException.incomplete(
                Map.of("phone", "Vui lòng nhập số điện thoại."), List.of("Giấy tờ tùy thân")));
        mockMvc.perform(post("/api/tutor/verification/submit").with(asTutor())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commitment\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SUBMISSION_INCOMPLETE"))
                .andExpect(jsonPath("$.message").value(VerificationException.MSG_INCOMPLETE))
                .andExpect(jsonPath("$.fields.phone").exists())
                .andExpect(jsonPath("$.missingDocuments[0]").value("Giấy tờ tùy thân"));
    }

    @Test
    void successfulSubmissionReturnsMsg12() throws Exception {
        when(service.submit(any())).thenReturn(sample("PENDING", "Đang chờ thẩm định", false).toBuilder()
                .message("Đã nộp hồ sơ xác minh thành công. Cán bộ học vụ sẽ thẩm định và phản hồi cho bạn qua email.")
                .build());
        mockMvc.perform(post("/api/tutor/verification/submit").with(asTutor())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"commitment\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusLabel").value("Đang chờ thẩm định"))
                .andExpect(jsonPath("$.editable").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Đã nộp hồ sơ xác minh thành công")));
    }

    @Test
    void downloadSetsSafeHeaders() throws Exception {
        byte[] content = "%PDF-1.4 %%EOF".getBytes(StandardCharsets.US_ASCII);
        UUID id = UUID.randomUUID();
        when(service.loadMyDocumentFile(id)).thenReturn(new DocumentFile(content, "application/pdf", "Chứng chỉ JLPT.pdf"));

        mockMvc.perform(get("/api/tutor/verification/documents/{id}/file", id).with(asTutor()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content))
                .andExpect(header().string("Content-Type", startsWith("application/pdf")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Content-Disposition", containsString("inline")));
    }

    @Test
    void downloadOfForeignDocumentIsNotFound() throws Exception {
        when(service.loadMyDocumentFile(any())).thenThrow(VerificationException.notFound());
        mockMvc.perform(get("/api/tutor/verification/documents/{id}/file", UUID.randomUUID()).with(asTutor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unexpectedErrorIsFriendlyAndHidesDetails() throws Exception {
        when(service.getMyVerification()).thenThrow(new IllegalStateException("NullPointer in db"));
        mockMvc.perform(get("/api/tutor/verification").with(asTutor()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Hệ thống đang bận, bạn vui lòng thử lại sau."))
                .andExpect(content().string(not(containsString("NullPointer"))));
    }
}
