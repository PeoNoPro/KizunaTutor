package com.kizuna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OAuth2AndRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.kizuna.repository.UserRepository userRepository;

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        userRepository.deleteNonCoreUsers();
    }

    @Test
    @DisplayName("Kiểm tra trang /login chứa nút đăng nhập Google và nút thử nghiệm nhanh 1-Click")
    void testLoginPageRendering() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("oauth2/authorization/google")))
                .andExpect(content().string(containsString("Đăng nhập với Google")))
                .andExpect(content().string(containsString("/api/auth/mock-google-login")));
    }

    @Test
    @DisplayName("Kiểm tra trang /register chứa form đăng ký và nút Google")
    void testRegisterPageRendering() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tạo Tài Khoản Học Tập")))
                .andExpect(content().string(containsString("oauth2/authorization/google")))
                .andExpect(content().string(containsString("/api/auth/mock-google-login")));
    }

    @Test
    @DisplayName("Kiểm tra trang callback /oauth2/redirect hiển thị đúng view không bị trắng trang")
    void testOAuth2RedirectPageRendering() throws Exception {
        mockMvc.perform(get("/oauth2/redirect")
                        .param("token", "dummy-jwt-token")
                        .param("role", "ROLE_LEARNER")
                        .param("email", "test@kizuna.vn")
                        .param("name", "Test User"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Xác Thực Phiên Đăng Nhập")))
                .andExpect(content().string(containsString("kizuna_token")));
    }

    @Test
    @DisplayName("Kiểm tra luồng Mock Google Login tạo tài khoản, sinh JWT và redirect mượt mà về /oauth2/redirect")
    void testMockGoogleLoginFlow() throws Exception {
        String randomEmail = "google.test." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        mockMvc.perform(get("/api/auth/mock-google-login")
                        .param("email", randomEmail)
                        .param("name", "Google Learner Test")
                        .param("role", "ROLE_LEARNER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/oauth2/redirect?token=*&role=ROLE_LEARNER*"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    @DisplayName("Kiểm tra gửi form đăng ký POST /register tự động tạo tài khoản và chuyển hướng thẳng vào /learner/dashboard")
    void testManualRegistrationFormSubmit() throws Exception {
        String randomEmail = "learner.form." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        mockMvc.perform(post("/register")
                        .param("fullName", "Học Viên Mới Đăng Ký")
                        .param("email", randomEmail)
                        .param("password", "password123")
                        .param("targetJlpt", "N3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/learner/dashboard"))
                .andExpect(cookie().exists("access_token"));
    }

    @Test
    @DisplayName("Kiểm tra REST API POST /api/auth/register trả về JWT Token và thông tin user chuẩn xác")
    void testManualRegistrationApi() throws Exception {
        String randomEmail = "api.learner." + UUID.randomUUID().toString().substring(0, 8) + "@kizuna.vn";

        RegisterRequest request = RegisterRequest.builder()
                .fullName("API Test Learner")
                .email(randomEmail)
                .password("password123")
                .targetJlpt("N2")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(randomEmail))
                .andExpect(jsonPath("$.user.role").value("ROLE_LEARNER"))
                .andExpect(jsonPath("$.redirectUrl").value("/learner/dashboard"))
                .andExpect(cookie().exists("access_token"));
    }
}
