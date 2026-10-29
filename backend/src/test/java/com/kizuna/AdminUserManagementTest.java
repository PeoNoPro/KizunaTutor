package com.kizuna;


import com.kizuna.core.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.dto.CreateUserRequest;
import com.kizuna.features.admin.dto.UpdateStatusRequest;
import com.kizuna.features.admin.dto.UpdateUserRoleRequest;
import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminUserManagementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String adminToken;
    private User adminUser;

    @BeforeEach
    void setUp() throws Exception {
        // Đăng nhập tài khoản Quản Trị Viên để lấy JWT
        LoginRequest loginReq = LoginRequest.builder()
                .email("admin@kizuna.vn")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResp = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);
        adminToken = loginResp.getToken();
        adminUser = userRepository.findByEmail("admin@kizuna.vn").orElseThrow();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        userRepository.deleteNonCoreUsers();
    }

    @Test
    @DisplayName("Kiểm tra trang /admin/users hiển thị đúng Master Shell, KPI và bảng người dùng")
    void testAdminUserViewRendering() throws Exception {
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản Lý Người Dùng &amp; Phân Quyền")))
                .andExpect(content().string(containsString("kpiTotalUsers")))
                .andExpect(content().string(containsString("createUserModal")))
                .andExpect(content().string(containsString("userManagement.js")));
    }

    @Test
    @DisplayName("Kiểm tra chặn người dùng không có quyền Admin truy cập /admin/users")
    void testUnauthorizedAccessToAdminUsers() throws Exception {
        // Đăng nhập học viên
        LoginRequest loginReq = LoginRequest.builder()
                .email("learner@kizuna.vn")
                .password("123456")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse learnerResp = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);

        // Truy cập trái phép phân hệ admin -> Phải bị từ chối chuyển về /learner/dashboard
        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + learnerResp.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learner/dashboard"));
    }

    @Test
    @DisplayName("Kiểm tra REST API GET /api/admin/users trả về danh sách phân trang và 4 chỉ số KPI")
    void testGetUsersApiWithKpis() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.kpis.totalUsers").isNumber())
                .andExpect(jsonPath("$.kpis.internalStaff").isNumber())
                .andExpect(jsonPath("$.kpis.activeTutors").isNumber())
                .andExpect(jsonPath("$.kpis.lockedUsers").isNumber());
    }

    @Test
    @DisplayName("Kiểm tra Admin khởi tạo tài khoản nhân sự nội bộ mới: POST /api/admin/users")
    void testCreateInternalStaffUser() throws Exception {
        String randomEmail = "academic." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        CreateUserRequest req = CreateUserRequest.builder()
                .fullName("Trần Cán Bộ Học Vụ Test")
                .email(randomEmail)
                .phone("0981122334")
                .role("ROLE_ACADEMIC_OFFICER")
                .temporaryPassword("AcademicTemp123@")
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(randomEmail))
                .andExpect(jsonPath("$.role").value("ROLE_ACADEMIC_OFFICER"))
                .andExpect(jsonPath("$.roleBadge").value("Cán Bộ Học Vụ"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Kiểm tra Admin cập nhật thông tin và đổi quyền: PUT /api/admin/users/{id}")
    void testUpdateUserRole() throws Exception {
        String randomEmail = "manager." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("Lê Vận Hành Ban Đầu")
                .email(randomEmail)
                .phone("0912345678")
                .role("ROLE_CS_STAFF")
                .temporaryPassword("Password123@")
                .status("ACTIVE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String createdUserId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Nâng cấp quyền từ CS sang Manager
        UpdateUserRoleRequest updateReq = UpdateUserRoleRequest.builder()
                .fullName("Lê Vận Hành Được Thăng Chức")
                .phone("0988776655")
                .role("ROLE_MANAGER")
                .status("ACTIVE")
                .build();

        mockMvc.perform(put("/api/admin/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lê Vận Hành Được Thăng Chức"))
                .andExpect(jsonPath("$.role").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.roleBadge").value("Quản Lý Vận Hành"));
    }

    @Test
    @DisplayName("Kiểm tra Admin Khóa / Mở khóa tài khoản: PATCH /api/admin/users/{id}/status")
    void testToggleUserStatus() throws Exception {
        String randomEmail = "user.lock." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("Học Viên Bị Khóa")
                .email(randomEmail)
                .role("ROLE_LEARNER")
                .temporaryPassword("Password123@")
                .status("ACTIVE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String createdUserId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Thực hiện khóa tài khoản
        UpdateStatusRequest lockReq = UpdateStatusRequest.builder().status("INACTIVE").build();

        mockMvc.perform(patch("/api/admin/users/" + createdUserId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.statusBadge").value("Tạm khóa"));
    }

    @Test
    @DisplayName("Kiểm tra ngăn chặn Admin tự khóa hoặc tự xóa tài khoản của chính mình")
    void testPreventSelfLockAndSelfDelete() throws Exception {
        UUID adminId = adminUser.getUserId();

        // 1. Thử tự khóa chính mình
        UpdateStatusRequest lockSelfReq = UpdateStatusRequest.builder().status("INACTIVE").build();
        mockMvc.perform(patch("/api/admin/users/" + adminId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(lockSelfReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("không thể tự thay đổi trạng thái hoặc khóa tài khoản của chính mình")));

        // 2. Thử tự xóa chính mình
        mockMvc.perform(delete("/api/admin/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("không thể tự xóa tài khoản Quản trị viên của chính mình")));
    }

    @Test
    @DisplayName("Kiểm tra Xóa mềm tài khoản (Soft Delete): DELETE /api/admin/users/{id}")
    void testSoftDeleteUser() throws Exception {
        String randomEmail = "user.delete." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        CreateUserRequest createReq = CreateUserRequest.builder()
                .fullName("User Chờ Xóa")
                .email(randomEmail)
                .role("ROLE_LEARNER")
                .temporaryPassword("Password123@")
                .status("ACTIVE")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String createdUserId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Xóa mềm tài khoản
        mockMvc.perform(delete("/api/admin/users/" + createdUserId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra trong database: deletedAt phải có giá trị
        User deletedUser = userRepository.findById(UUID.fromString(createdUserId)).orElseThrow();
        assertNotNull(deletedUser.getDeletedAt());
    }

    @Test
    @DisplayName("Kiểm tra gửi email trực tiếp tới vuh2623832@gmail.com qua SMTP Google App Password")
    void testDirectDiagnosticEmailConnection() throws Exception {
        mockMvc.perform(post("/api/admin/test-email")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("to", "vuh2623832@gmail.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }
}
