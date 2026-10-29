package com.kizuna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.auth.PendingRegistration;
import com.kizuna.dto.auth.RegisterRequest;
import com.kizuna.dto.auth.ResendOtpRequest;
import com.kizuna.dto.auth.VerifyOtpRequest;
import com.kizuna.service.IOtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class EmailOtpRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IOtpService otpService;

    @Autowired
    private com.kizuna.repository.UserRepository userRepository;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        userRepository.deleteNonCoreUsers();
    }

    @Test
    @DisplayName("Kiểm tra trang /register chứa Form và Modal 6 ô OTP cùng file script registerOtp.js")
    void testRegisterViewStructure() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("otpModal")))
                .andExpect(content().string(containsString("otp-digit-input")))
                .andExpect(content().string(containsString("otpCountdownTimer")))
                .andExpect(content().string(containsString("registerOtp.js")));
    }

    @Test
    @DisplayName("Kiểm tra luồng gửi OTP qua Gmail: POST /api/auth/register/send-otp")
    void testSendOtpEndpoint() throws Exception {
        String testEmail = "test.otp." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        RegisterRequest req = RegisterRequest.builder()
                .fullName("Nguyễn Văn Kiểm Thử")
                .email(testEmail)
                .password("mypassword123")
                .role("ROLE_LEARNER")
                .targetJlpt("N3")
                .build();

        mockMvc.perform(post("/api/auth/register/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value(testEmail));

        // Kiểm tra OTP đã được sinh và lưu trong Cache
        PendingRegistration pending = otpService.getPendingRegistration(testEmail);
        assertNotNull(pending);
        assertEquals(6, pending.getOtp().length());
        assertTrue(pending.getOtp().matches("^[0-9]{6}$"));
    }

    @Test
    @DisplayName("Kiểm tra chặn gửi OTP khi email đã tồn tại trong hệ thống")
    void testSendOtpDuplicateEmail() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .fullName("System Admin Duplicate")
                .email("admin@kizuna.vn") // Email đã có trong hệ thống
                .password("somepassword123")
                .role("ROLE_LEARNER")
                .build();

        mockMvc.perform(post("/api/auth/register/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("đã được đăng ký trong hệ thống")));
    }

    @Test
    @DisplayName("Kiểm tra quy tắc Cooldown 60s khi gửi lại mã OTP: POST /api/auth/register/resend-otp")
    void testResendOtpCooldown() throws Exception {
        String testEmail = "cooldown." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        // Bước 1: Khởi tạo OTP
        otpService.generateAndSaveOtp(testEmail, "User Cooldown", "hash123", "ROLE_LEARNER", "N5");

        // Bước 2: Thử gửi lại ngay lập tức (chưa đủ 60s) -> Phải bị chặn
        ResendOtpRequest resendReq = ResendOtpRequest.builder().email(testEmail).build();

        mockMvc.perform(post("/api/auth/register/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Vui lòng đợi thêm")));
    }

    @Test
    @DisplayName("Kiểm tra phòng chống Brute-force: Nhập sai OTP quá 5 lần thì hủy phiên")
    void testBruteForceProtection() throws Exception {
        String testEmail = "bruteforce." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
        otpService.generateAndSaveOtp(testEmail, "Brute Force Test", "hash123", "ROLE_LEARNER", "N5");

        VerifyOtpRequest wrongOtpReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp("000000") // Mã sai
                .build();

        // Thử sai 5 lần
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/register/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongOtpReq)))
                    .andExpect(status().isBadRequest());
        }

        // Lần thứ 6 -> Mã đã bị hủy khỏi cache vì vượt quá 5 lần thử
        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtpReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Bạn đã nhập sai mã OTP quá 5 lần")));
    }

    @Test
    @DisplayName("Kiểm tra Xác thực OTP thành công cho Học Viên (ROLE_LEARNER): Kích hoạt User, cấp JWT và chuyển hướng /learner/dashboard")
    void testVerifyOtpSuccessForLearner() throws Exception {
        String testEmail = "learner.active." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        // Khởi tạo mã OTP
        String otp = otpService.generateAndSaveOtp(testEmail, "Học Viên Chính Thức", "passwordHash", "ROLE_LEARNER", "N2");

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp(otp)
                .build();

        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.role").value("ROLE_LEARNER"))
                .andExpect(jsonPath("$.redirectUrl").value("/learner/dashboard"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    @DisplayName("Kiểm tra Xác thực OTP thành công cho Gia Sư (ROLE_TUTOR): Kích hoạt User, cấp JWT và chuyển hướng /tutor/dashboard")
    void testVerifyOtpSuccessForTutor() throws Exception {
        String testEmail = "tutor.active." + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";

        // Khởi tạo mã OTP cho Gia Sư
        String otp = otpService.generateAndSaveOtp(testEmail, "Gia Sư Kizuna Mới", "passwordHash", "ROLE_TUTOR", null);

        VerifyOtpRequest verifyReq = VerifyOtpRequest.builder()
                .email(testEmail)
                .otp(otp)
                .build();

        mockMvc.perform(post("/api/auth/register/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.role").value("ROLE_TUTOR"))
                .andExpect(jsonPath("$.redirectUrl").value("/tutor/dashboard"))
                .andExpect(cookie().exists("access_token"));
    }
}
