package com.kizuna.features.learning_support;

import com.kizuna.core.entity.Role;
import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.JwtAuthenticationFilter;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.core.security.JwtTokenVerifier;
import com.kizuna.core.security.RestAuthErrorHandlers;
import com.kizuna.core.security.SecurityConfig;
import com.kizuna.features.admin.service.OperationsActorResolver;
import com.kizuna.features.classroom.service.LearnerClassMembershipService;
import com.kizuna.features.learning_support.controller.LearningToolAccessController;
import com.kizuna.features.learning_support.service.LearningToolAccessService;
import com.kizuna.features.learning_support.service.impl.LearningToolAccessServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra LearningToolAccessWebConfig chặn đúng các đường dẫn công cụ học (kể cả alias)
 * và không chặn từ điển Kanji. Dùng controller giả để không phụ thuộc dữ liệu thật của từng công cụ.
 */
@WebMvcTest(controllers = LearningToolAccessController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthErrorHandlers.class,
        LearningToolAccessServiceImpl.class, LearningToolAccessWebTest.StubToolController.class})
class LearningToolAccessWebTest {

    @RestController
    static class StubToolController {
        @GetMapping({"/api/learner/decks", "/api/learner/grammar", "/api/learner/ai-writing/prompts",
                "/api/learner/minigame/play-data", "/api/minigame/play-data",
                "/api/learner/vocabularies/topics", "/api/learner/topics", "/api/learner/search", "/api/learner",
                "/api/kanji/search", "/api/learner/kanji/search"})
        String ok() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LearnerClassMembershipService membershipService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private JwtTokenVerifier jwtTokenVerifier;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private OperationsActorResolver operationsActorResolver;

    private static RequestPostProcessor as(String role) {
        User user = User.builder().userId(UUID.randomUUID()).email("u@kizuna.vn")
                .role(Role.builder().roleName(role).build()).build();
        return authentication(new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority(role))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/learner/decks", "/api/learner/grammar", "/api/learner/ai-writing/prompts",
            "/api/learner/minigame/play-data", "/api/minigame/play-data",
            "/api/learner/vocabularies/topics", "/api/learner/topics", "/api/learner/search", "/api/learner"})
    @DisplayName("Học viên chưa vào lớp bị chặn 403 ở mọi đường dẫn công cụ, kể cả alias")
    void lockedToolsReturn403WithoutClass(String path) throws Exception {
        when(membershipService.countOpenClasses(any())).thenReturn(0L);

        mockMvc.perform(get(path).with(as("ROLE_LEARNER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(LearningToolAccessService.LOCKED_MESSAGE));
    }

    @Test
    @DisplayName("Học viên đã vào lớp dùng được công cụ")
    void learnerWithClassPasses() throws Exception {
        when(membershipService.countOpenClasses(any())).thenReturn(1L);

        mockMvc.perform(get("/api/learner/decks").with(as("ROLE_LEARNER"))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/kanji/search", "/api/learner/kanji/search"})
    @DisplayName("Từ điển Kanji luôn mở, kể cả khi chưa vào lớp")
    void kanjiDictionaryStaysFree(String path) throws Exception {
        when(membershipService.countOpenClasses(any())).thenReturn(0L);

        mockMvc.perform(get(path).with(as("ROLE_LEARNER"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Chưa đăng nhập gọi công cụ thì nhận 401")
    void anonymousGets401() throws Exception {
        mockMvc.perform(get("/api/learner/grammar")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Gia sư không bị chặn")
    void tutorPasses() throws Exception {
        mockMvc.perform(get("/api/learner/decks").with(as("ROLE_TUTOR"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("API trạng thái trả allowed=false cho học viên chưa vào lớp")
    void accessEndpointReportsLocked() throws Exception {
        when(membershipService.countOpenClasses(any())).thenReturn(0L);

        mockMvc.perform(get("/api/learner/learning-tools/access").with(as("ROLE_LEARNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.openClassCount").value(0));
    }
}
