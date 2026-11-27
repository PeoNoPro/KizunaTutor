package com.kizuna.features.learning_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.repository.LearnerRepository;
import com.kizuna.core.repository.RoleRepository;
import com.kizuna.core.repository.TutorRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.core.security.AuthenticatedUserHelper;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.auth.controller.AuthController;
import com.kizuna.features.auth.controller.GoogleOAuthController;
import com.kizuna.features.auth.service.AuthService;
import com.kizuna.features.auth.service.EmailService;
import com.kizuna.features.auth.service.OtpService;
import com.kizuna.features.learning_support.controller.AgentMemoryController;
import com.kizuna.features.learning_support.controller.FlashcardDeckController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class Pr20SecurityRegressionTest {

    private JwtTokenProvider jwtTokenProvider;
    private AuthenticatedUserHelper authenticatedUserHelper;
    private JdbcTemplate jdbcTemplate;
    private FlashcardDeckController flashcardDeckController;
    private AgentMemoryController agentMemoryController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        authenticatedUserHelper = new AuthenticatedUserHelper(jwtTokenProvider);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        flashcardDeckController = new FlashcardDeckController(jdbcTemplate, objectMapper, authenticatedUserHelper);
        agentMemoryController = new AgentMemoryController(jdbcTemplate, objectMapper, authenticatedUserHelper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Google OAuth2 không chuyển hướng sang mock login khi dùng cấu hình mẫu")
    void shouldRedirectToNotConfiguredInsteadOfMockLogin() throws Exception {
        GoogleOAuthController controller = new GoogleOAuthController();
        ReflectionTestUtils.setField(controller, "googleClientId", "1029384756-kizunatutor-google-client.apps.googleusercontent.com");
        ReflectionTestUtils.setField(controller, "redirectUri", "http://localhost:3000/login/oauth2/code/google");

        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        controller.authorizeGoogle(response);

        verify(response, times(1)).sendRedirect("/login?error=oauth2_not_configured");
        verify(response, never()).sendRedirect(contains("mock-google-login"));
    }

    @Test
    @DisplayName("Endpoint mock-google-login bị chặn 403 khi chưa bật cờ dev cục bộ")
    void shouldBlockMockGoogleLoginByDefault() {
        AuthController authController = new AuthController(
                Mockito.mock(AuthService.class),
                Mockito.mock(OtpService.class),
                Mockito.mock(EmailService.class),
                Mockito.mock(UserRepository.class),
                Mockito.mock(RoleRepository.class),
                Mockito.mock(LearnerRepository.class),
                Mockito.mock(TutorRepository.class),
                Mockito.mock(PasswordEncoder.class),
                jwtTokenProvider
        );
        ReflectionTestUtils.setField(authController, "mockGoogleEnabled", false);

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        when(req.getRemoteAddr()).thenReturn("127.0.0.1");
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);

        BusinessException ex = assertThrows(BusinessException.class, () -> authController.mockGoogleLogin(req, res));
        assertEquals(403, ex.status());
    }

    @Test
    @DisplayName("Flashcard API yêu cầu đăng nhập (HTTP 401) khi thiếu JWT")
    void shouldRequireAuthForFlashcardDecks() {
        BusinessException exGet = assertThrows(BusinessException.class,
                () -> flashcardDeckController.getDecks(null, null));
        assertEquals(401, exGet.status());

        BusinessException exPost = assertThrows(BusinessException.class,
                () -> flashcardDeckController.createOrSaveDeck(Map.of("title", "Hack"), null, null));
        assertEquals(401, exPost.status());

        BusinessException exDelete = assertThrows(BusinessException.class,
                () -> flashcardDeckController.deleteDeck("deck-1", null, null));
        assertEquals(401, exDelete.status());
    }

    @Test
    @DisplayName("Flashcard API chặn đọc/sửa/ghi đè/xóa bộ thẻ của người khác (HTTP 403)")
    void shouldBlockCrossOwnerFlashcardOperations() {
        when(jwtTokenProvider.validateToken("alice-jwt")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("alice-jwt")).thenReturn("alice@kizuna.vn");

        Map<String, Object> bobsDeck = Map.of(
                "id", "deck-bob-1",
                "owner_key", "bob@kizuna.vn",
                "title", "Bob Private Deck",
                "cards_json", "[]"
        );
        when(jdbcTemplate.queryForList(contains("WHERE id = ?"), eq("deck-bob-1")))
                .thenReturn(List.of(bobsDeck));

        BusinessException getEx = assertThrows(BusinessException.class,
                () -> flashcardDeckController.getDeck("deck-bob-1", "Bearer alice-jwt", null));
        assertEquals(403, getEx.status());

        BusinessException postEx = assertThrows(BusinessException.class,
                () -> flashcardDeckController.createOrSaveDeck(
                        Map.of("id", "deck-bob-1", "title", "Hijack"), "Bearer alice-jwt", null));
        assertEquals(403, postEx.status());

        BusinessException putEx = assertThrows(BusinessException.class,
                () -> flashcardDeckController.updateDeck(
                        "deck-bob-1", Map.of("title", "Hijack"), "Bearer alice-jwt", null));
        assertEquals(403, putEx.status());

        BusinessException delEx = assertThrows(BusinessException.class,
                () -> flashcardDeckController.deleteDeck("deck-bob-1", "Bearer alice-jwt", null));
        assertEquals(403, delEx.status());

        verify(jdbcTemplate, never()).update(contains("UPDATE learner_flashcard_decks"), any(), any(), any(), any(), any(), any());
        verify(jdbcTemplate, never()).update(contains("DELETE FROM learner_flashcard_decks"), any(), any());
    }

    @Test
    @DisplayName("AgentMemory API bắt buộc JWT (401) và bỏ qua userId giả mạo từ client")
    void shouldBindAgentMemoryToJwtAndIgnoreSpoofedUserId() {
        BusinessException unauthPost = assertThrows(BusinessException.class,
                () -> agentMemoryController.appendMemory(Map.of("userId", "victim@kizuna.vn"), null, null));
        assertEquals(401, unauthPost.status());

        BusinessException unauthGet = assertThrows(BusinessException.class,
                () -> agentMemoryController.getRecent("shodo", null, null));
        assertEquals(401, unauthGet.status());

        when(jwtTokenProvider.validateToken("alice-jwt")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("alice-jwt")).thenReturn("alice@kizuna.vn");

        var postRes = agentMemoryController.appendMemory(
                Map.of("userId", "victim@kizuna.vn", "feature", "shodo", "score", 92),
                "Bearer alice-jwt",
                null
        );
        verify(jdbcTemplate, times(1)).update(
                contains("INSERT INTO learner_agent_memories"),
                eq("alice@kizuna.vn"),
                eq("shodo"),
                isNull(),
                eq(92),
                eq(""),
                anyString()
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) postRes.getBody().get("data");
        assertEquals("alice@kizuna.vn", data.get("userId"));

        agentMemoryController.getRecent("shodo", "Bearer alice-jwt", null);
        verify(jdbcTemplate, times(1)).queryForList(
                contains("WHERE user_key = ? AND feature = ?"),
                eq("alice@kizuna.vn"),
                eq("shodo")
        );
    }
}
