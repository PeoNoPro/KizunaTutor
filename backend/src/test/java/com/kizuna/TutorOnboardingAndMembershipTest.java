package com.kizuna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.features.payment.dto.CreatePaymentRequest;
import com.kizuna.features.profile.dto.SaveTutorProfileRequest;
import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.*;
import com.kizuna.features.learning_support.entity.*;
import com.kizuna.features.payment.entity.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.verification.entity.*;
import com.kizuna.features.tutor_search.entity.*;
import com.kizuna.features.ticket.entity.*;
import com.kizuna.features.notification.entity.*;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.features.payment.repository.MembershipPlanRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử toàn diện State Machine 3 giai đoạn phân quyền Gia Sư (Tutor Lifecycle):
 * DRAFT_PROFILE -> PENDING_VERIFICATION -> REJECTED -> APPROVED_UNPAID -> ACTIVE_PAID
 * Phục vụ kiểm định: UC 9 - 16, UC 20, UC 71 - 77, UC 94.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TutorOnboardingAndMembershipTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MembershipPlanRepository planRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TUTOR_EMAIL = "tutor.statemachine@kizuna.vn";
    private static final String ACADEMIC_EMAIL = "academic@kizuna.vn";

    @BeforeEach
    void setupUser() {
        Role tutorRole = userRepository.findByEmail("tutor@kizuna.vn")
                .map(User::getRole)
                .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElseThrow());

        User user = userRepository.findByEmail(TUTOR_EMAIL).orElseGet(() -> {
            User newUser = User.builder()
                    .email(TUTOR_EMAIL)
                    .fullName("Sensei Test State Machine")
                    .passwordHash(passwordEncoder.encode("tutor123"))
                    .role(tutorRole)
                    .accountStatus(UserAccountStatus.ACTIVE)
                    .mustChangePassword(false)
                    .build();
            return userRepository.save(newUser);
        });

        // Reset hoặc tạo mới Tutor ở trạng thái DRAFT_PROFILE
        Tutor tutor = tutorRepository.findByUser_Email(TUTOR_EMAIL).orElseGet(() -> Tutor.builder().user(user).build());
        tutor.setTutorState(TutorState.DRAFT_PROFILE);
        tutor.setVerificationStatus(TutorVerificationStatus.UNVERIFIED);
        tutor.setVerificationFeedback(null);
        tutorRepository.save(tutor);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest req = LoginRequest.builder().email(email).password(password).build();
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), LoginResponse.class);
        return response.getToken();
    }

    @Test
    @DisplayName("Kiểm thử chu trình duyệt hồ sơ & kích hoạt gói thành viên gia sư (3-Stage State Machine)")
    void testTutorLifecycleStateMachine() throws Exception {
        String tutorToken = loginAndGetToken(TUTOR_EMAIL, "tutor123");
        String academicToken = loginAndGetToken(ACADEMIC_EMAIL, "123456");

        // GIAI ĐOẠN 1: DRAFT_PROFILE (UC 9 - 12)
        // 1. Lấy thông tin hồ sơ ban đầu
        mockMvc.perform(get("/api/tutor/profile")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorState").value("DRAFT_PROFILE"));

        // 2. Lưu bản nháp hồ sơ (UC 9-12)
        SaveTutorProfileRequest draftReq = SaveTutorProfileRequest.builder()
                .fullName("Sensei Test State Machine")
                .phone("0981234567")
                .highestJlptLevel("N2")
                .experienceYears((short) 3)
                .hourlyRate(BigDecimal.valueOf(250000))
                .headline("Chuyên luyện thi JLPT N2")
                .bio("Kinh nghiệm 3 năm luyện thi cấp tốc")
                .certificateUrls("https://drive.google.com/test-cert")
                .build();

        mockMvc.perform(post("/api/tutor/profile/draft")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("0981234567"));

        // 3. Nộp hồ sơ thẩm định -> PENDING_VERIFICATION (UC 13, 16)
        mockMvc.perform(post("/api/tutor/profile/submit")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorState").value("PENDING_VERIFICATION"));

        Tutor tutor = tutorRepository.findByUser_Email(TUTOR_EMAIL).orElseThrow();
        UUID tutorId = tutor.getTutorId();
        assertThat(tutor.getTutorState()).isEqualTo(TutorState.PENDING_VERIFICATION);

        // GIAI ĐOẠN REJECTED: Academic từ chối yêu cầu bổ sung thông tin (UC 15, 16)
        mockMvc.perform(post("/api/academic/tutors/" + tutorId + "/reject")
                        .header("Authorization", "Bearer " + academicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("feedback", "Ảnh chứng chỉ mờ, vui lòng nộp lại bản scan rõ nét."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorState").value("REJECTED"))
                .andExpect(jsonPath("$.data.verificationFeedback").value("Ảnh chứng chỉ mờ, vui lòng nộp lại bản scan rõ nét."));

        // Gia sư nộp lại hồ sơ sau khi bổ sung chứng chỉ
        draftReq.setCertificateUrls("https://drive.google.com/test-cert-hd");
        mockMvc.perform(post("/api/tutor/profile/submit")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draftReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorState").value("PENDING_VERIFICATION"));

        // GIAI ĐOẠN 2: APPROVED_UNPAID: Ban Học Vụ phê duyệt (UC 20)
        mockMvc.perform(post("/api/academic/tutors/" + tutorId + "/approve")
                        .header("Authorization", "Bearer " + academicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tutorState").value("APPROVED_UNPAID"));

        tutor = tutorRepository.findById(tutorId).orElseThrow();
        assertThat(tutor.getTutorState()).isEqualTo(TutorState.APPROVED_UNPAID);

        // GIAI ĐOẠN 3: GÓI THÀNH VIÊN & THANH TOÁN (UC 71 - 77, UC 94)
        // 1. Xem danh sách gói
        MvcResult plansResult = mockMvc.perform(get("/api/tutor/memberships/plans")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        MembershipPlan firstPlan = planRepository.findByIsActiveTrueOrderByPriceAsc().get(0);

        // 2. Tạo liên kết thanh toán VNPay
        CreatePaymentRequest payReq = CreatePaymentRequest.builder()
                .planId(firstPlan.getPlanId())
                .paymentMethod("VNPAY")
                .build();

        mockMvc.perform(post("/api/tutor/memberships/payment-url")
                        .header("Authorization", "Bearer " + tutorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentUrl").exists());

        // 3. Kích hoạt trực tiếp (Direct Activate / VNPay Callback Success) -> ACTIVE_PAID (UC 94)
        mockMvc.perform(post("/api/tutor/memberships/direct-activate/" + firstPlan.getPlanId())
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        tutor = tutorRepository.findById(tutorId).orElseThrow();
        assertThat(tutor.getTutorState()).isEqualTo(TutorState.ACTIVE_PAID);

        // 4. Mở khóa toàn diện tính năng giảng dạy (UC 43-45)
        mockMvc.perform(get("/tutor/kanji-library")
                        .header("Authorization", "Bearer " + tutorToken))
                .andExpect(status().isOk());
    }
}
