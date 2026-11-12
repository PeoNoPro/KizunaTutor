package com.kizuna.features.learning_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.learning_support.controller.GrammarController;
import com.kizuna.features.learning_support.controller.VocabularyController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for VocabularyController & GrammarController.
 * Covers UC-LS-02 (Vocabulary Lookup) and UC-LS-03 (Grammar Lookup).
 * Fully isolated using Mockito.
 */
class VocabularyAndGrammarControllerTest {

    private JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        objectMapper = new ObjectMapper();
    }

    // =========================================================================
    // UC-LS-02: TRA CỨU TỪ VỰNG THEO CHỦ ĐỀ & JLPT (VocabularyController)
    // =========================================================================
    @Nested
    @DisplayName("UC-LS-02: Tra cứu từ vựng (VocabularyController)")
    class VocabularyTests {

        private VocabularyController vocabularyController;

        @BeforeEach
        void init() {
            vocabularyController = new VocabularyController(jdbcTemplate);
        }

        @Test
        @DisplayName("Happy Path: Lấy danh sách 7 chủ đề từ vựng (N5-N1, LIFE_DAILY, WORKPLACE) với số lượng từ DB")
        void getTopics_happyPath_shouldReturnTopicsWithCounts() {
            // [Arrange] Mock DB count aggregation
            doAnswer(invocation -> {
                RowCallbackHandler rch = invocation.getArgument(1);
                var rs = Mockito.mock(java.sql.ResultSet.class);
                when(rs.getString("jlpt_level")).thenReturn("N5", "N3", (String) null);
                when(rs.getInt("total")).thenReturn(674, 1659);
                rch.processRow(rs);
                rch.processRow(rs);
                return null;
            }).when(jdbcTemplate).query(contains("GROUP BY jlpt_level"), any(RowCallbackHandler.class));

            // [Act] When calling getTopics
            ResponseEntity<List<Map<String, Object>>> response = vocabularyController.getTopics();

            // [Assert] Then returns list of topics
            assertNotNull(response.getBody());
            assertEquals(7, response.getBody().size());

            Map<String, Object> topicN5 = response.getBody().get(0);
            assertEquals("JLPT_N5", topicN5.get("id"));
            assertEquals("JLPT N5 Căn bản", topicN5.get("titleVn"));

            Map<String, Object> topicWork = response.getBody().get(6);
            assertEquals("WORKPLACE", topicWork.get("id"));
            assertEquals("Tiếng Nhật công sở", topicWork.get("titleVn"));
        }

        @Test
        @DisplayName("Happy Path: Tra cứu từ vựng theo chủ đề JLPT_N5 và từ khóa tìm kiếm")
        void getVocabularies_withTopicAndKeyword_shouldQueryWithParameters() {
            // [Arrange] Given topic "JLPT_N5" and search "nihon"
            doAnswer(invocation -> {
                RowCallbackHandler rch = invocation.getArgument(1);
                var rs = Mockito.mock(java.sql.ResultSet.class);
                when(rs.getLong("id")).thenReturn(101L);
                when(rs.getString("word")).thenReturn("日本");
                when(rs.getString("reading")).thenReturn("にほん");
                when(rs.getString("romaji")).thenReturn("nihon");
                when(rs.getString("han_viet")).thenReturn("NHẬT BẢN");
                when(rs.getString("meaning")).thenReturn("Nước Nhật, Nhật Bản");
                when(rs.getString("jlpt_level")).thenReturn("N5");
                rch.processRow(rs);
                return null;
            }).when(jdbcTemplate).query(contains("FROM vocabularies WHERE 1=1"), any(RowCallbackHandler.class), any(Object[].class));

            // [Act] When calling getVocabularies
            ResponseEntity<List<Map<String, Object>>> response = vocabularyController.getVocabularies(
                    "JLPT_N5", "nihon", null, 0, 50);

            // [Assert] Then query is constructed with N5 and keyword params
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals("日本", response.getBody().get(0).get("word"));
            assertEquals("にほん", response.getBody().get(0).get("reading"));
            assertEquals("N5", response.getBody().get(0).get("level"));
        }

        @Test
        @DisplayName("Boundary Case: Phân trang trang âm hoặc limit âm -> Đảm bảo page và limit không bị âm")
        void getVocabularies_boundaryPaging_shouldClampNegativePage() {
            // [Arrange] page = -5, limit = -10
            // [Act] When calling with negative bounds
            assertDoesNotThrow(() -> vocabularyController.getVocabularies("JLPT_N5", "", "", -5, -10));

            // [Assert] Verify query was called without exception
            verify(jdbcTemplate, times(1)).query(anyString(), any(RowCallbackHandler.class), any(Object[].class));
        }
    }

    // =========================================================================
    // UC-LS-03: KHO NGỮ PHÁP OPENJLPT & PHÂN TÍCH (GrammarController)
    // =========================================================================
    @Nested
    @DisplayName("UC-LS-03: Kho ngữ pháp OpenJLPT (GrammarController)")
    class GrammarTests {

        private GrammarController grammarController;

        @BeforeEach
        void init() {
            grammarController = new GrammarController(jdbcTemplate, objectMapper);
        }

        @Test
        @DisplayName("Happy Path: Lọc danh sách ngữ pháp theo cấp độ N5 và tìm kiếm từ khóa")
        void getGrammars_withFilterAndSearch_shouldReturnMatchingGrammars() {
            // [Arrange] Mock DB returning grammar rows
            List<Map<String, Object>> mockGrammars = List.of(
                    Map.of(
                            "id", 1,
                            "grammarCode", "N5-001",
                            "jlptLevel", "N5",
                            "pattern", "〜は〜です",
                            "romaji", "wa ... desu",
                            "meaning", "Thì, là, ở",
                            "tags", "trợ từ"
                    ),
                    Map.of(
                            "id", 2,
                            "grammarCode", "N4-001",
                            "jlptLevel", "N4",
                            "pattern", "〜てから",
                            "romaji", "te kara",
                            "meaning", "Sau khi làm V",
                            "tags", "thời gian"
                    )
            );
            when(jdbcTemplate.queryForList(contains("SELECT id, grammar_code"))).thenReturn(mockGrammars);

            // [Act] When filtering with level = "N5" and keyword = "desu"
            ResponseEntity<Map<String, Object>> response = grammarController.getGrammars("N5", "desu", 0, 10);

            // [Assert] Then only N5 grammar matching "desu" is returned
            assertNotNull(response.getBody());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
            assertEquals(1, content.size());
            assertEquals("N5-001", content.get(0).get("grammarCode"));
            assertEquals("〜は〜です", content.get(0).get("pattern"));
        }

        @Test
        @DisplayName("Happy Path: Xem chi tiết ngữ pháp theo ID hoặc grammarCode")
        void getGrammarDetail_whenFound_shouldReturn200() {
            // [Arrange] Given grammar in DB
            List<Map<String, Object>> mockGrammars = List.of(
                    Map.of("id", 10, "grammarCode", "N3-050", "pattern", "〜わけがない", "meaning", "Không có lý nào")
            );
            when(jdbcTemplate.queryForList(contains("SELECT id, grammar_code"))).thenReturn(mockGrammars);

            // [Act] When getting detail by grammarCode
            ResponseEntity<?> response = grammarController.getGrammarDetail("N3-050");

            // [Assert] Then returns 200 OK with grammar details
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            @SuppressWarnings("unchecked")
            Map<String, Object> detail = (Map<String, Object>) response.getBody();
            assertEquals("〜わけがない", detail.get("pattern"));
        }

        @Test
        @DisplayName("Abnormal Case: Tra cứu chi tiết mẫu ngữ pháp không tồn tại -> Trả về 404 Not Found")
        void getGrammarDetail_whenNotFound_shouldReturn404() {
            // [Arrange] Given empty grammar list
            when(jdbcTemplate.queryForList(contains("SELECT id, grammar_code"))).thenReturn(Collections.emptyList());

            // [Act] When getting non-existent grammar
            ResponseEntity<?> response = grammarController.getGrammarDetail("UNKNOWN-999");

            // [Assert] Then returns 404 Not Found
            assertEquals(404, response.getStatusCode().value());
        }
    }
}
