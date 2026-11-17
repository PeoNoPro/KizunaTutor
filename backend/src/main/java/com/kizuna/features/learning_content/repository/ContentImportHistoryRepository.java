package com.kizuna.features.learning_content.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.dto.PageResponse;
import com.kizuna.features.learning_content.dto.*;
import com.kizuna.features.learning_content.service.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Xem kho học liệu và lịch sử nhập.
 * Lịch sử dùng bảng có sẵn system_audit_logs (action_type = LEARNING_CONTENT_IMPORT, chi tiết trong cột details JSONB),
 * không tạo bảng mới.
 */
@Repository
@RequiredArgsConstructor
public class ContentImportHistoryRepository {

    public static final String ACTION = "LEARNING_CONTENT_IMPORT";
    public static final int MAX_LOGGED_ITEMS = 2000;

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    // ===== Danh sách học liệu =====

    /** sort: LEVEL (N5 → N1) | NEWEST (mới thêm gần đây). jlpt: N1..N5 hoặc trống. */
    public PageResponse<Map<String, Object>> listItems(ContentType type, String keyword, String jlpt, String sort,
                                                     int page, int size) {
        String table;
        String select;
        String[] searchColumns;
        String levelOrder;
        String jlptColumn;
        switch (type) {
            case KANJI -> {
                table = "kanji_dictionary";
                select = "kanji_character AS \"character\",han_viet AS \"hanViet\", onyomi, kunyomi, meaning, "
                        + "jlpt_level::text AS jlpt, stroke_count AS strokes, created_at AS \"createdAt\"";
                searchColumns = new String[]{"kanji_character", "han_viet", "onyomi", "kunyomi", "meaning"};
                levelOrder = "jlpt_level NULLS LAST, stroke_count, kanji_character";
                jlptColumn = "jlpt_level::text";
            }
            case VOCABULARY -> {
                table = "vocabularies";
                select = "id, word, reading, romaji, han_viet AS \"hanViet\", part_of_speech AS \"partOfSpeech\", meaning, "
                        + "example_sentence AS \"exampleSentence\", example_furigana AS \"exampleFurigana\", "
                        + "example_meaning AS \"exampleMeaning\", jmdict_id AS \"jmdictId\", jlpt_level AS jlpt, "
                        + "created_at AS \"createdAt\"";
                searchColumns = new String[]{"word", "reading", "romaji", "han_viet", "meaning"};
                levelOrder = "jlpt_level DESC NULLS LAST, id";
                jlptColumn = "jlpt_level";
            }
            default -> {
                table = "grammars";
                select = "id, grammar_code AS code, pattern, romaji, meaning, formation, notes, "
                        + "example_sentence AS \"exampleSentence\", example_furigana AS \"exampleFurigana\", "
                        + "example_meaning AS \"exampleMeaning\", extended_examples AS \"extendedExamples\", tags, "
                        + "jlpt_level AS jlpt, created_at AS \"createdAt\"";
                searchColumns = new String[]{"grammar_code", "pattern", "romaji", "meaning", "tags"};
                levelOrder = "jlpt_level DESC NULLS LAST, id";
                jlptColumn = "jlpt_level";
            }
        }

        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            String like = "%" + escapeLike(keyword.trim()) + "%";
            where.append(" AND (");
            for (int i = 0; i < searchColumns.length; i++) {
                where.append(i > 0 ? " OR " : "").append(searchColumns[i]).append(" ILIKE ? ESCAPE '\\'");
                args.add(like);
            }
            where.append(")");
        }
        if (jlpt != null && !jlpt.isBlank()) {
            where.append(" AND ").append(jlptColumn).append(" = ?");
            args.add(jlpt);
        }

        Long total = jdbc.queryForObject("SELECT count(*) FROM " + table + where, Long.class, args.toArray());
        String order = "NEWEST".equalsIgnoreCase(sort) ? "created_at DESC NULLS LAST, " + levelOrder : levelOrder;
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Map<String, Object>> items = jdbc.query("SELECT " + select + " FROM " + table + where
                + " ORDER BY " + order + " LIMIT ? OFFSET ?", (rs, i) -> rowToMap(rs), pageArgs.toArray());
        return page(items, page, size, total == null ? 0 : total);
    }

    private static Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        for (int c = 1; c <= meta.getColumnCount(); c++) {
            Object value = rs.getObject(c);
            if (value instanceof Timestamp ts) value = ts.toInstant().atOffset(ZoneOffset.UTC);
            if (value instanceof String s) value = s.strip();
            row.put(meta.getColumnLabel(c), value);
        }
        return row;
    }

    static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    // ===== Lịch sử nhập =====

    /** Ghi một lần nhập thật. Gọi trong cùng giao dịch với lệnh thêm dữ liệu. */
    public void logImport(UUID actorId, ContentType type, ContentImportResponse report, List<String> addedItems) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("type", type.name());
        details.put("fileName", report.fileName());
        details.put("sheets", report.sheets());
        details.put("totalRows", report.totalRows());
        details.put("created", report.created());
        details.put("skippedExisting", report.skippedExisting());
        details.put("duplicatesInFile", report.duplicatesInFile());
        details.put("invalidRows", report.invalidRows());
        details.put("addedItems", addedItems.size() > MAX_LOGGED_ITEMS ? addedItems.subList(0, MAX_LOGGED_ITEMS) : addedItems);
        details.put("addedTruncated", addedItems.size() > MAX_LOGGED_ITEMS);
        String json;
        try {
            json = mapper.writeValueAsString(details);
        } catch (Exception e) {
            throw new IllegalStateException("Không ghi được lịch sử nhập", e);
        }
        jdbc.update("INSERT INTO system_audit_logs (actor_user_id, action_type, target_entity, details) "
                + "VALUES (?, ?, ?, ?::jsonb)", actorId, ACTION, tableOf(type), json);
    }

    public PageResponse<ImportHistoryResponse> history(ContentType type, int page, int size) {
        String where = " WHERE l.action_type = ?" + (type == null ? "" : " AND l.details->>'type' = ?");
        List<Object> args = new ArrayList<>(List.of(ACTION));
        if (type != null) args.add(type.name());
        Long total = jdbc.queryForObject("SELECT count(*) FROM system_audit_logs l" + where, Long.class, args.toArray());
        args.add(size);
        args.add((long) page * size);
        List<ImportHistoryResponse> items = jdbc.query(HISTORY_SELECT + where + " ORDER BY l.created_at DESC LIMIT ? OFFSET ?",
                (rs, i) -> toHistory(rs).item(), args.toArray());
        return page(items, page, size, total == null ? 0 : total);
    }

    public Optional<ImportHistoryDetailResponse> historyDetail(UUID logId) {
        List<ImportHistoryDetailResponse> rows = jdbc.query(HISTORY_SELECT + " WHERE l.action_type = ? AND l.log_id = ?",
                (rs, i) -> toHistory(rs), ACTION, logId);
        return rows.stream().findFirst();
    }

    private static final String HISTORY_SELECT = "SELECT l.log_id, l.created_at, l.details::text AS details, "
            + "u.full_name, u.email FROM system_audit_logs l LEFT JOIN users u ON u.user_id = l.actor_user_id";

    private ImportHistoryDetailResponse toHistory(ResultSet rs) throws SQLException {
        Map<String, Object> d;
        try {
            d = mapper.readValue(rs.getString("details"), new TypeReference<>() { });
        } catch (Exception e) {
            d = Map.of();
        }
        String typeName = String.valueOf(d.getOrDefault("type", ""));
        String label = Arrays.stream(ContentType.values()).filter(t -> t.name().equals(typeName))
                .map(ContentType::label).findFirst().orElse(typeName);
        Timestamp created = rs.getTimestamp("created_at");
        ImportHistoryResponse item = new ImportHistoryResponse(
                rs.getObject("log_id", UUID.class),
                created == null ? null : created.toInstant().atOffset(ZoneOffset.UTC),
                rs.getString("full_name"), rs.getString("email"), typeName, label,
                (String) d.get("fileName"), num(d, "totalRows"), num(d, "created"), num(d, "skippedExisting"),
                num(d, "duplicatesInFile"), num(d, "invalidRows"));
        @SuppressWarnings("unchecked")
        List<String> added = d.get("addedItems") instanceof List<?> l ? (List<String>) l : List.of();
        return new ImportHistoryDetailResponse(item, added, Boolean.TRUE.equals(d.get("addedTruncated")));
    }

    private static <T> PageResponse<T> page(List<T> items, int page, int size, long total) {
        return new PageResponse<>(items, page, size, total, Math.max(1, (total + size - 1) / size));
    }

    private static int num(Map<String, Object> d, String key) {
        return d.get(key) instanceof Number n ? n.intValue() : 0;
    }

    private static String tableOf(ContentType type) {
        return switch (type) {
            case KANJI -> "kanji_dictionary";
            case VOCABULARY -> "vocabularies";
            case GRAMMAR -> "grammars";
        };
    }
}
