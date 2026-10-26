package com.kizuna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.auth.LoginRequest;
import com.kizuna.dto.auth.LoginResponse;
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
}
