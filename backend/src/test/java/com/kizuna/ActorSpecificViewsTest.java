package com.kizuna;


import com.kizuna.core.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.auth.dto.LoginRequest;
import com.kizuna.features.auth.dto.LoginResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ActorSpecificViewsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("Admin: Truy cập /admin/kanji-management và API /api/admin/kanji/list thành công")
    void testAdminKanjiManagement() throws Exception {
        String token = loginAndGetToken("admin@kizuna.vn", "admin123");

        mockMvc.perform(get("/admin/kanji-management")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/kanji/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Tutor: Truy cập /tutor/kanji-library và API /api/tutor/kanji/catalog thành công")
    void testTutorKanjiLibrary() throws Exception {
        String token = loginAndGetToken("tutor@kizuna.vn", "123456");

        mockMvc.perform(get("/tutor/kanji-library")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/tutor/kanji/catalog")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/dictionary và API /api/learner/kanji/search thành công")
    void testLearnerDictionary() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/dictionary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/learner/kanji/search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/dashboard thành công")
    void testLearnerDashboard() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/flashcards thành công")
    void testLearnerFlashcards() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/flashcards")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/practice-canvas thành công")
    void testLearnerPracticeCanvas() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/practice-canvas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/my-progress và /learner/progress thành công")
    void testLearnerMyProgress() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/my-progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/learner/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/find-tutor thành công với Master Shell Layout")
    void testLearnerFindTutor() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/find-tutor")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/trial-schedule, /learner/my-classroom, /learner/assignments thành công")
    void testLearnerAdditionalViews() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/trial-schedule")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/learner/my-classroom")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/learner/assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Truy cập /learner/handwriting-search thành công")
    void testLearnerHandwritingSearch() throws Exception {
        String token = loginAndGetToken("learner@kizuna.vn", "123456");

        mockMvc.perform(get("/learner/handwriting-search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Learner: Tra cứu chữ viết tay có kanjiId và kiểm tra endpoint thêm kanji")
    void testHandwritingSearchMatches() throws Exception {
        String searchBody = """
            {
                "strokeCount": 4,
                "candidates": ["方", "万", "力"]
            }
            """;
        mockMvc.perform(post("/api/kanji/search-by-strokes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(searchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches").isArray())
                .andExpect(jsonPath("$.matches[0].character").value("方"))
                .andExpect(jsonPath("$.matches[0].kanjiId").isNotEmpty());
    }
}
