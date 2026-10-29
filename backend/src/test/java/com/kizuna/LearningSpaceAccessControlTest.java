package com.kizuna;


import com.kizuna.core.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import com.kizuna.features.classroom.repository.LearningSpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class LearningSpaceAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LearningSpaceRepository learningSpaceRepository;

    @BeforeEach
    @org.springframework.transaction.annotation.Transactional
    void setupCleanState() {
        // Đảm bảo tài khoản learner@kizuna.vn luôn ở Trạng thái 2 (chưa có lớp học) trước mỗi lần chạy test
        learningSpaceRepository.deleteByLearnerEmail("learner@kizuna.vn");
    }

    @org.junit.jupiter.api.AfterEach
    @org.springframework.transaction.annotation.Transactional
    void cleanUpAfterTest() {
        // Thu hồi liên kết lớp sau test để bảo đảm learner@kizuna.vn trở về Trạng thái 2
        learningSpaceRepository.deleteByLearnerEmail("learner@kizuna.vn");
    }

    @Test
    @DisplayName("Trạng thái 1: Khách vãng lai (Chưa đăng nhập) truy cập /kanji hiển thị màn hình rào cản")
    void testState1_UnauthenticatedAccess() throws Exception {
        // Kiểm tra với tab=practice
        mockMvc.perform(get("/kanji?tab=practice"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "UNAUTHENTICATED"))
                .andExpect(model().attribute("isUnlocked", false))
                .andExpect(content().string(containsString("Bạn cần đăng nhập và được Gia sư mở khóa để sử dụng chức năng này")))
                .andExpect(content().string(containsString("Đăng Nhập Ngay")))
                .andExpect(content().string(containsString("Đăng Ký Tài Khoản")));

        // Kiểm tra với tab=library (Thư viện Kanji cũng phải bị khóa đối với Guest)
        mockMvc.perform(get("/kanji?tab=library"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "UNAUTHENTICATED"))
                .andExpect(model().attribute("isUnlocked", false))
                .andExpect(content().string(containsString("Bạn cần đăng nhập và được Gia sư mở khóa để sử dụng chức năng này")));

        // Kiểm tra với tab=folders (Bộ Thẻ cũng phải bị khóa đối với Guest)
        mockMvc.perform(get("/kanji?tab=folders"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "UNAUTHENTICATED"))
                .andExpect(model().attribute("isUnlocked", false))
                .andExpect(content().string(containsString("Bạn cần đăng nhập và được Gia sư mở khóa để sử dụng chức năng này")));

        // Kiểm tra trang chủ Guest (Dashboard của Guest) tự động nhận diện UNAUTHENTICATED
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "UNAUTHENTICATED"))
                .andExpect(model().attribute("isUnlocked", false));
    }

    @Test
    @DisplayName("Trạng thái 2: Learner chưa được Gia sư thêm vào lớp (Unassigned Learner) hiển thị màn hình khóa")
    void testState2_UnassignedLearnerAccess() throws Exception {
        // 1. Đăng nhập tài khoản learner@kizuna.vn (chưa vào lớp)
        LoginRequest loginReq = LoginRequest.builder()
                .email("learner@kizuna.vn")
                .password("123456")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResp = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String token = loginResp.getToken();

        // 2. Truy cập /kanji với token
        mockMvc.perform(get("/kanji?tab=practice")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "UNASSIGNED_LEARNER"))
                .andExpect(model().attribute("isUnlocked", false))
                .andExpect(content().string(containsString("Chức năng này đang bị khóa. Bạn cần được Gia sư thêm vào Không gian học tập (Learning Space) để bắt đầu luyện tập!")))
                .andExpect(content().string(containsString("Khám Phá &amp; Tìm Gia Sư")))
                .andExpect(content().string(containsString("Nhập Mã Mời Của Gia Sư")));
    }

    @Test
    @DisplayName("Trạng thái 3: Học viên chính thức (Official Learner đã có Learning Space) mở khóa 100%")
    void testState3_OfficialLearnerAccess() throws Exception {
        // 1. Đăng nhập tài khoản learner@kizuna.vn
        LoginRequest loginReq = LoginRequest.builder()
                .email("learner@kizuna.vn")
                .password("123456")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResp = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String token = loginResp.getToken();

        // 2. Kích hoạt liên kết Learning Space qua mã mời của Gia sư
        mockMvc.perform(post("/api/v1/learning-spaces/join-by-code")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inviteCode", "KIZUNA2026"))))
                .andExpect(status().isOk());

        // 3. Truy cập /kanji
        mockMvc.perform(get("/kanji?tab=practice")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "ACTIVE_OFFICIAL_LEARNER"))
                .andExpect(model().attribute("isUnlocked", true));
    }

    @Test
    @DisplayName("Kích hoạt mở khóa Trạng thái 2 -> Trạng thái 3 qua API nhập mã mời của Gia sư")
    void testUpgradeState2ToState3_ViaInviteCode() throws Exception {
        // 1. Đăng nhập learner@kizuna.vn
        LoginRequest loginReq = LoginRequest.builder()
                .email("learner@kizuna.vn")
                .password("123456")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResp = objectMapper.readValue(loginResult.getResponse().getContentAsString(), LoginResponse.class);
        String token = loginResp.getToken();

        // 2. Nhập mã mời gia sư POST /api/v1/learning-spaces/join-by-code
        mockMvc.perform(post("/api/v1/learning-spaces/join-by-code")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("inviteCode", "KIZUNA2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Kiểm tra lại /kanji -> Đã được mở khóa (ACTIVE_OFFICIAL_LEARNER)
        mockMvc.perform(get("/kanji?tab=practice")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(model().attribute("accessState", "ACTIVE_OFFICIAL_LEARNER"))
                .andExpect(model().attribute("isUnlocked", true));
    }
}
