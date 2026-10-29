package com.kizuna;


import com.kizuna.core.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.dto.CreateUserRequest;
import com.kizuna.features.auth.dto.FirstTimePasswordChangeRequest;
import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class StaffCreationAndFirstTimePasswordChangeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Đăng nhập tài khoản Quản Trị Viên để lấy JWT
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@kizuna.vn")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResp = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        adminToken = loginResp.getToken();
    }

    @AfterEach
    void tearDown() {
        // Đảm bảo sau khi chạy xong luôn làm sạch DB, chỉ giữ lại 6 tài khoản mẫu
        userRepository.deleteNonCoreUsers();
    }

    @Test
    @DisplayName("Quy trình hoàn chỉnh: Admin tạo nhân viên -> Mật khẩu tạm -> Đăng nhập bị chặn ép đổi -> Đổi pass thành công -> Đăng nhập bình thường")
    void testEndToEnd_StaffCreation_And_FirstTimePasswordChange() throws Exception {
        String staffEmail = "academic.staff." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";
        String tempPassword = "TempPassword123@";
        String newStrongPassword = "NewStrongPass2026!";

        // 1. Admin gọi POST /api/admin/users/create-staff
        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("Trần Cán Bộ Học Vụ Mới")
                .email(staffEmail)
                .phone("0987654321")
                .role("ROLE_ACADEMIC_OFFICER")
                .temporaryPassword(tempPassword)
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/admin/users/create-staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(staffEmail))
                .andExpect(jsonPath("$.role").value("ROLE_ACADEMIC_OFFICER"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        // Kiểm tra trong DB: cờ must_change_password phải là true
        User createdUser = userRepository.findByEmail(staffEmail).orElseThrow();
        assertTrue(createdUser.isMustChangePassword());

        // 2. Nhân viên đăng nhập lần đầu bằng mật khẩu tạm
        LoginRequest staffLogin = LoginRequest.builder()
                .email(staffEmail)
                .password(tempPassword)
                .build();

        MvcResult staffLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.user.mustChangePassword").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/change-password-first-time"))
                .andReturn();

        LoginResponse staffLoginResp = objectMapper.readValue(staffLoginResult.getResponse().getContentAsString(), LoginResponse.class);
        String staffToken = staffLoginResp.getToken();
        assertNotNull(staffToken);

        // 3. Thử đổi mật khẩu với mật khẩu tạm thời sai -> Phải trả về 400 Bad Request
        FirstTimePasswordChangeRequest wrongCurrentPassReq = FirstTimePasswordChangeRequest.builder()
                .currentPassword("WrongPassword123@")
                .newPassword(newStrongPassword)
                .confirmPassword(newStrongPassword)
                .build();

        mockMvc.perform(post("/api/auth/change-password-first-time")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCurrentPassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu tạm thời hiện tại không chính xác."));

        // 4. Thử đổi mật khẩu với mật khẩu xác nhận không khớp -> 400 Bad Request
        FirstTimePasswordChangeRequest mismatchReq = FirstTimePasswordChangeRequest.builder()
                .currentPassword(tempPassword)
                .newPassword(newStrongPassword)
                .confirmPassword("DifferentConfirmPass123@")
                .build();

        mockMvc.perform(post("/api/auth/change-password-first-time")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu mới và mật khẩu xác nhận không trùng khớp."));

        // 5. Thử đặt mật khẩu mới trùng mật khẩu tạm thời -> 400 Bad Request
        FirstTimePasswordChangeRequest reusePassReq = FirstTimePasswordChangeRequest.builder()
                .currentPassword(tempPassword)
                .newPassword(tempPassword)
                .confirmPassword(tempPassword)
                .build();

        mockMvc.perform(post("/api/auth/change-password-first-time")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reusePassReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mật khẩu mới không được trùng với mật khẩu tạm thời."));

        // 6. Thực hiện đổi mật khẩu hợp lệ với độ mạnh cao
        FirstTimePasswordChangeRequest validChangeReq = FirstTimePasswordChangeRequest.builder()
                .currentPassword(tempPassword)
                .newPassword(newStrongPassword)
                .confirmPassword(newStrongPassword)
                .build();

        mockMvc.perform(post("/api/auth/change-password-first-time")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validChangeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.redirectUrl").value("/academic/dashboard"));

        // Kiểm tra lại trong DB: cờ must_change_password phải được xóa về false
        User updatedUser = userRepository.findByEmail(staffEmail).orElseThrow();
        assertFalse(updatedUser.isMustChangePassword());

        // 7. Nhân viên đăng nhập lại bằng mật khẩu mới
        LoginRequest staffRelogin = LoginRequest.builder()
                .email(staffEmail)
                .password(newStrongPassword)
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffRelogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andExpect(jsonPath("$.user.mustChangePassword").value(false))
                .andExpect(jsonPath("$.redirectUrl").value("/academic/dashboard"));
    }

    @Test
    @DisplayName("Admin gọi POST /api/admin/users/{id}/resend-credentials cấp lại mật khẩu tạm thời thành công")
    void testResendCredentials() throws Exception {
        String staffEmail = "manager.resend." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("Trần Vận Hành Cần Cấp Lại")
                .email(staffEmail)
                .phone("0912345678")
                .role("ROLE_MANAGER")
                .temporaryPassword("OldTempPass123@")
                .status("ACTIVE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/users/create-staff")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        User created = userRepository.findByEmail(staffEmail).orElseThrow();

        // Admin bấm gửi lại thông tin tài khoản
        mockMvc.perform(post("/api/admin/users/" + created.getUserId() + "/resend-credentials")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(staffEmail))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        User refreshed = userRepository.findById(created.getUserId()).orElseThrow();
        assertTrue(refreshed.isMustChangePassword());
    }

    @Test
    @DisplayName("Admin gọi Diagnostic Health-Check API: /api/admin/test-email")
    void testDiagnosticEmailEndpoint() throws Exception {
        // Gọi API test email đồng bộ (có thể trả về 200 nếu kết nối được SMTP hoặc 500 kèm chi tiết chẩn đoán)
        mockMvc.perform(post("/api/admin/test-email")
                        .param("to", "vuh2623832@gmail.com")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 200 || status == 500, "Status phải là 200 hoặc 500 (chẩn đoán lỗi)");
                })
                .andExpect(jsonPath("$.recipient").value("vuh2623832@gmail.com"))
                .andExpect(jsonPath("$.sender").value("kizunatutor@gmail.com"));
    }
}
