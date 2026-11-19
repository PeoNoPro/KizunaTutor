package com.kizuna.features.learning_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.security.AuthenticatedUserHelper;
import com.kizuna.core.security.JwtTokenProvider;
import com.kizuna.features.learning_support.controller.FlashcardDeckController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for FlashcardDeckController.
 * Covers UC-62 (Study Flashcards & PR #20 Security for Decks).
 * Validates authentication (HTTP 401), owner_key authorization (HTTP 403), CRUD, and edge cases.
 */
class FlashcardDeckControllerTest {

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;
    private JwtTokenProvider jwtTokenProvider;
    private AuthenticatedUserHelper authenticatedUserHelper;
    private FlashcardDeckController controller;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
        jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        authenticatedUserHelper = new AuthenticatedUserHelper(jwtTokenProvider);
        controller = new FlashcardDeckController(jdbcTemplate, objectMapper, authenticatedUserHelper);
    }

    private void mockAuth(String token, String email) {
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(token)).thenReturn(email);
    }

    @Nested
    @DisplayName("PR #20 Authentication & Authorization Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("Abnormal Case: Yêu cầu không có JWT Token -> Ném lỗi HTTP 401")
        void unauthenticated_shouldThrow401() {
            // [Arrange] No token provided

            // [Act & Assert] When calling endpoints without token -> Throws 401
            BusinessException ex1 = assertThrows(BusinessException.class,
                    () -> controller.getDecks(null, null));
            assertEquals(401, ex1.status());

            BusinessException ex2 = assertThrows(BusinessException.class,
                    () -> controller.getDeck("deck-123", null, null));
            assertEquals(401, ex2.status());

            BusinessException ex3 = assertThrows(BusinessException.class,
                    () -> controller.deleteDeck("deck-123", null, null));
            assertEquals(401, ex3.status());
        }

        @Test
        @DisplayName("Abnormal Case: Truy cập/sửa/xóa bộ thẻ của tài khoản khác -> Ném lỗi HTTP 403 (owner_key guard)")
        void crossOwnerAccess_shouldThrow403() {
            // [Arrange] Logged in as "alice@kizuna.vn", target deck belongs to "bob@kizuna.vn"
            mockAuth("alice-jwt", "alice@kizuna.vn");

            Map<String, Object> bobsDeck = new HashMap<>();
            bobsDeck.put("id", "deck-bob-01");
            bobsDeck.put("owner_key", "bob@kizuna.vn");
            bobsDeck.put("title", "Bob Private Kanji");
            bobsDeck.put("cards_json", "[]");

            when(jdbcTemplate.queryForList(anyString(), eq("deck-bob-01")))
                    .thenReturn(List.of(bobsDeck));

            // [Act & Assert] Get deck of Bob
            BusinessException getEx = assertThrows(BusinessException.class,
                    () -> controller.getDeck("deck-bob-01", "Bearer alice-jwt", null));
            assertEquals(403, getEx.status());

            // [Act & Assert] Update deck of Bob
            BusinessException putEx = assertThrows(BusinessException.class,
                    () -> controller.updateDeck("deck-bob-01", Map.of("title", "Hack"), "Bearer alice-jwt", null));
            assertEquals(403, putEx.status());

            // [Act & Assert] Delete deck of Bob
            BusinessException delEx = assertThrows(BusinessException.class,
                    () -> controller.deleteDeck("deck-bob-01", "Bearer alice-jwt", null));
            assertEquals(403, delEx.status());

            verify(jdbcTemplate, never()).update(contains("DELETE"), any(), any());
        }
    }

    @Nested
    @DisplayName("CRUD Operations Tests")
    class CrudTests {

        @Test
        @DisplayName("Happy Path: Lấy danh sách bộ thẻ thuộc sở hữu của người dùng đang đăng nhập")
        void getDecks_happyPath_shouldReturnOwnedDecks() {
            // [Arrange] Given authenticated user "alice@kizuna.vn" with 2 decks
            mockAuth("alice-jwt", "alice@kizuna.vn");

            Map<String, Object> deck1 = Map.of(
                    "id", "deck-1",
                    "owner_key", "alice@kizuna.vn",
                    "title", "Kanji N5",
                    "description", "50 chu N5",
                    "color", "emerald",
                    "cards_json", "[{\"front\":\"日\",\"back\":\"Mặt trời\"}]"
            );
            when(jdbcTemplate.queryForList(contains("WHERE owner_key = ?"), eq("alice@kizuna.vn")))
                    .thenReturn(List.of(deck1));

            // [Act] When calling getDecks
            ResponseEntity<List<Map<String, Object>>> response = controller.getDecks("Bearer alice-jwt", null);

            // [Assert] Then returns list containing Alice's deck
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("Kanji N5", response.getBody().get(0).get("title"));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards = (List<Map<String, Object>>) response.getBody().get(0).get("cards");
            assertEquals(1, cards.size());
            assertEquals("日", cards.get(0).get("front"));
        }

        @Test
        @DisplayName("Happy Path: Tạo bộ thẻ mới thành công")
        void createOrSaveDeck_happyPath_shouldInsertAndReturnDeck() {
            // [Arrange] Given authenticated user and new deck payload
            mockAuth("user-jwt", "user@kizuna.vn");

            Map<String, Object> requestBody = Map.of(
                    "id", "new-deck-100",
                    "title", "Từ vựng N4",
                    "description", "Chủ đề văn phòng",
                    "color", "blue",
                    "cards", List.of(Map.of("front", "会社", "back", "Công ty"))
            );
            when(jdbcTemplate.queryForList(anyString(), eq("new-deck-100"))).thenReturn(Collections.emptyList());

            // [Act] When calling createOrSaveDeck
            ResponseEntity<Map<String, Object>> response = controller.createOrSaveDeck(
                    requestBody, "Bearer user-jwt", null);

            // [Assert] Then insert query is executed and created deck is returned
            assertNotNull(response.getBody());
            assertEquals("new-deck-100", response.getBody().get("id"));
            assertEquals("Từ vựng N4", response.getBody().get("title"));
            verify(jdbcTemplate, times(1)).update(
                    contains("INSERT INTO learner_flashcard_decks"),
                    eq("new-deck-100"),
                    eq("user@kizuna.vn"),
                    eq("Từ vựng N4"),
                    eq("Chủ đề văn phòng"),
                    eq("blue"),
                    anyString()
            );
        }

        @Test
        @DisplayName("Happy Path: Cập nhật bộ thẻ của chính mình thành công")
        void updateDeck_happyPath_shouldUpdateOwnedDeck() {
            // [Arrange] Given authenticated user owning the existing deck
            mockAuth("user-jwt", "user@kizuna.vn");

            Map<String, Object> existingRow = Map.of(
                    "id", "deck-update-1",
                    "owner_key", "user@kizuna.vn",
                    "title", "Ten Cu",
                    "cards_json", "[]"
            );
            when(jdbcTemplate.queryForList(anyString(), eq("deck-update-1"))).thenReturn(List.of(existingRow));

            Map<String, Object> updateBody = Map.of(
                    "title", "Ten Moi Da Sua",
                    "description", "Mo ta moi",
                    "color", "indigo",
                    "cards", List.of()
            );

            // [Act] When updating deck
            ResponseEntity<Map<String, Object>> response = controller.updateDeck(
                    "deck-update-1", updateBody, "Bearer user-jwt", null);

            // [Assert] Then update query is triggered with matching id and owner_key
            assertNotNull(response.getBody());
            assertEquals("Ten Moi Da Sua", response.getBody().get("title"));
            verify(jdbcTemplate, times(1)).update(
                    contains("UPDATE learner_flashcard_decks"),
                    eq("Ten Moi Da Sua"),
                    eq("Mo ta moi"),
                    eq("indigo"),
                    anyString(),
                    eq("deck-update-1"),
                    eq("user@kizuna.vn")
            );
        }

        @Test
        @DisplayName("Happy Path: Xóa bộ thẻ của chính mình thành công")
        void deleteDeck_happyPath_shouldDeleteAndReturnSuccess() {
            // [Arrange] Given authenticated user owning the deck
            mockAuth("user-jwt", "user@kizuna.vn");

            Map<String, Object> existingRow = Map.of(
                    "id", "deck-del-1",
                    "owner_key", "user@kizuna.vn"
            );
            when(jdbcTemplate.queryForList(anyString(), eq("deck-del-1"))).thenReturn(List.of(existingRow));

            // [Act] When calling deleteDeck
            ResponseEntity<Map<String, Object>> response = controller.deleteDeck(
                    "deck-del-1", "Bearer user-jwt", null);

            // [Assert] Then deck is deleted from DB
            assertNotNull(response.getBody());
            assertTrue((Boolean) response.getBody().get("success"));
            verify(jdbcTemplate, times(1)).update(
                    contains("DELETE FROM learner_flashcard_decks WHERE id = ? AND owner_key = ?"),
                    eq("deck-del-1"),
                    eq("user@kizuna.vn")
            );
        }

        @Test
        @DisplayName("Abnormal Case: Xóa bộ thẻ không tồn tại -> Ném ResourceNotFoundException")
        void deleteDeck_whenNotFound_shouldThrowResourceNotFoundException() {
            // [Arrange] Deck not found in DB
            mockAuth("user-jwt", "user@kizuna.vn");
            when(jdbcTemplate.queryForList(anyString(), eq("deck-not-exist"))).thenReturn(Collections.emptyList());

            // [Act & Assert] When calling deleteDeck -> Throws ResourceNotFoundException
            assertThrows(ResourceNotFoundException.class,
                    () -> controller.deleteDeck("deck-not-exist", "Bearer user-jwt", null));
        }
    }
}
