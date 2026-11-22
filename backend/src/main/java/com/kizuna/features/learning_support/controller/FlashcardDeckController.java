package com.kizuna.features.learning_support.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.security.AuthenticatedUserHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * REST API quản lý Bộ thẻ Flashcards (/api/learner/decks).
 * Mọi thao tác đọc, tạo, cập nhật và xóa đều bắt buộc xác thực JWT và kiểm tra chủ sở hữu (owner_key).
 */
@Slf4j
@RestController
@RequestMapping("/api/learner/decks")
@RequiredArgsConstructor
public class FlashcardDeckController {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AuthenticatedUserHelper authenticatedUserHelper;

    private volatile boolean tableInitialized = false;

    private void ensureTable() {
        if (tableInitialized) return;
        synchronized (this) {
            if (tableInitialized) return;
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS learner_flashcard_decks (
                        id VARCHAR(120) PRIMARY KEY,
                        owner_key VARCHAR(255) NOT NULL,
                        title VARCHAR(255) NOT NULL,
                        description TEXT,
                        color VARCHAR(50) DEFAULT 'blue',
                        cards_json TEXT NOT NULL DEFAULT '[]',
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                    )
                    """);
            tableInitialized = true;
        }
    }

    private Optional<Map<String, Object>> findDeckRowById(String id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, owner_key, title, description, color, cards_json, created_at, updated_at FROM learner_flashcard_decks WHERE id = ?",
                id
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    private void assertOwner(Map<String, Object> existingRow, String ownerKey, String forbiddenMessage) {
        String existingOwner = existingRow.get("owner_key") != null
                ? String.valueOf(existingRow.get("owner_key")).trim().toLowerCase()
                : "";
        if (!ownerKey.equals(existingOwner)) {
            throw new BusinessException(403, forbiddenMessage);
        }
    }

    private List<Map<String, Object>> parseCardsJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Không thể đọc cards_json của bộ thẻ: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String toCardsJson(Object cardsObj) {
        try {
            return objectMapper.writeValueAsString(cardsObj != null ? cardsObj : List.of());
        } catch (Exception e) {
            log.warn("Không thể chuyển đổi cards thành JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private Map<String, Object> mapRowToDeck(Map<String, Object> row) {
        Map<String, Object> deck = new LinkedHashMap<>();
        deck.put("id", row.get("id"));
        deck.put("title", row.get("title"));
        deck.put("description", row.get("description") != null ? row.get("description") : "");
        deck.put("color", row.get("color") != null ? row.get("color") : "blue");
        deck.put("cards", parseCardsJson((String) row.get("cards_json")));
        deck.put("createdAt", row.get("created_at"));
        deck.put("updatedAt", row.get("updated_at"));
        return deck;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getDecks(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {
        String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
        ensureTable();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                SELECT id, owner_key, title, description, color, cards_json, created_at, updated_at
                FROM learner_flashcard_decks
                WHERE owner_key = ?
                ORDER BY updated_at DESC, created_at DESC
                """,
                ownerKey
        );

        return ResponseEntity.ok(rows.stream().map(this::mapRowToDeck).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDeck(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {
        String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
        ensureTable();

        Map<String, Object> row = findDeckRowById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ với mã: " + id));
        assertOwner(row, ownerKey, "Bạn không có quyền truy cập bộ thẻ của tài khoản khác.");

        return ResponseEntity.ok(mapRowToDeck(row));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrSaveDeck(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {
        String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
        ensureTable();

        String id = body.get("id") != null && !String.valueOf(body.get("id")).isBlank()
                ? String.valueOf(body.get("id")).trim()
                : "deck-" + System.currentTimeMillis();
        String title = body.get("title") != null && !String.valueOf(body.get("title")).isBlank()
                ? String.valueOf(body.get("title")).trim()
                : "Bộ Thẻ Mới";
        String description = body.get("description") != null ? String.valueOf(body.get("description")) : "";
        String color = body.get("color") != null ? String.valueOf(body.get("color")) : "blue";
        String cardsJson = toCardsJson(body.get("cards"));

        Optional<Map<String, Object>> existingOpt = findDeckRowById(id);
        if (existingOpt.isPresent()) {
            assertOwner(existingOpt.get(), ownerKey, "Không thể ghi đè bộ thẻ thuộc tài khoản khác.");
            jdbcTemplate.update(
                    """
                    UPDATE learner_flashcard_decks
                    SET title = ?, description = ?, color = ?, cards_json = ?, updated_at = NOW()
                    WHERE id = ? AND owner_key = ?
                    """,
                    title, description, color, cardsJson, id, ownerKey
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO learner_flashcard_decks (id, owner_key, title, description, color, cards_json, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """,
                    id, ownerKey, title, description, color, cardsJson
            );
        }

        Map<String, Object> saved = new LinkedHashMap<>();
        saved.put("id", id);
        saved.put("title", title);
        saved.put("description", description);
        saved.put("color", color);
        saved.put("cards", parseCardsJson(cardsJson));
        saved.put("updatedAt", OffsetDateTime.now().toString());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDeck(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {
        String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
        ensureTable();

        String title = body.get("title") != null && !String.valueOf(body.get("title")).isBlank()
                ? String.valueOf(body.get("title")).trim()
                : "Bộ Thẻ Mới";
        String description = body.get("description") != null ? String.valueOf(body.get("description")) : "";
        String color = body.get("color") != null ? String.valueOf(body.get("color")) : "blue";
        String cardsJson = toCardsJson(body.get("cards"));

        Optional<Map<String, Object>> existingOpt = findDeckRowById(id);
        if (existingOpt.isPresent()) {
            assertOwner(existingOpt.get(), ownerKey, "Bạn không có quyền chỉnh sửa bộ thẻ thuộc tài khoản khác.");
            jdbcTemplate.update(
                    """
                    UPDATE learner_flashcard_decks
                    SET title = ?, description = ?, color = ?, cards_json = ?, updated_at = NOW()
                    WHERE id = ? AND owner_key = ?
                    """,
                    title, description, color, cardsJson, id, ownerKey
            );
        } else {
            jdbcTemplate.update(
                    """
                    INSERT INTO learner_flashcard_decks (id, owner_key, title, description, color, cards_json, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                    """,
                    id, ownerKey, title, description, color, cardsJson
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("description", description);
        result.put("color", color);
        result.put("cards", parseCardsJson(cardsJson));
        result.put("updatedAt", OffsetDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDeck(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(value = "access_token", required = false) String cookieToken) {
        String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
        ensureTable();

        Map<String, Object> existingRow = findDeckRowById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ để xóa: " + id));
        assertOwner(existingRow, ownerKey, "Bạn không có quyền xóa bộ thẻ thuộc tài khoản khác.");

        jdbcTemplate.update("DELETE FROM learner_flashcard_decks WHERE id = ? AND owner_key = ?", id, ownerKey);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã xóa bộ thẻ thành công.",
                "id", id
        ));
    }
}
