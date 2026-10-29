package com.kizuna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.auth.LoginRequest;
import com.kizuna.dto.auth.LoginResponse;
import com.kizuna.entity.RoleEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthAndDashboardRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Kiểm tra đăng nhập và điều hướng chính xác cho toàn bộ 6 Actor")
    void testLoginAndRedirectFor6Actors() throws Exception {
        String[][] testActors = {
                {"admin@kizuna.vn", "admin123", "ROLE_ADMIN", "/admin/dashboard", "Quản Trị Viên"},
                {"manager@kizuna.vn", "123456", "ROLE_MANAGER", "/manager/dashboard", "Quản Lý Vận Hành"},
                {"cs@kizuna.vn", "123456", "ROLE_CS_STAFF", "/cs/dashboard", "Chăm Sóc Khách Hàng"},
                {"academic@kizuna.vn", "123456", "ROLE_ACADEMIC_OFFICER", "/academic/dashboard", "Cán Bộ Học Vụ"},
                {"tutor@kizuna.vn", "123456", "ROLE_TUTOR", "/tutor/dashboard", "Gia Sư Kizuna"},
                {"learner@kizuna.vn", "123456", "ROLE_LEARNER", "/learner/dashboard", "Học Viên"}
        };

        for (String[] actor : testActors) {
            String email = actor[0];
            String password = actor[1];
            String expectedRole = actor[2];
            String expectedRoute = actor[3];
            String expectedBadge = actor[4];

            LoginRequest loginReq = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value(email))
                    .andExpect(jsonPath("$.user.role").value(expectedRole))
                    .andExpect(jsonPath("$.redirectUrl").value(expectedRoute))
                    .andReturn();

            String responseJson = result.getResponse().getContentAsString();
            LoginResponse response = objectMapper.readValue(responseJson, LoginResponse.class);

            assertThat(response.getToken()).isNotBlank();
            assertThat(response.getRedirectUrl()).isEqualTo(expectedRoute);
            assertThat(response.getUser().getRoleBadge()).isEqualTo(expectedBadge);

            // Kiểm tra truy cập Dashboard với JWT Bearer Token hợp lệ
            mockMvc.perform(get(expectedRoute)
                            .header("Authorization", "Bearer " + response.getToken()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Kiểm tra chặn người dùng chưa xác thực truy cập Dashboard (Redirect to /login)")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Kiểm tra ngăn chặn truy cập chéo vai trò giữa các Actor")
    void testCrossRoleAccessPrevented() throws Exception {
        // Đăng nhập vai trò Learner
        LoginRequest learnerReq = LoginRequest.builder()
                .email("learner@kizuna.vn")
                .password("123456")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(learnerReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse learnerResp = objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponse.class);

        // Learner cố ý truy cập Dashboard của Admin -> Phải bị chặn hoặc chuyển hướng về đúng dashboard của Learner
        mockMvc.perform(get("/admin/dashboard")
                        .header("Authorization", "Bearer " + learnerResp.getToken()))
                .andExpect(status().is3xxRedirection());
    }
}
