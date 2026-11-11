package com.kizuna.features.learning_content.repository;

import com.kizuna.features.learning_content.service.ContentType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.*;
import java.util.function.Function;

/**
 * Đọc / thêm học liệu trên các bảng đã có sẵn (kanji_dictionary, kanjis, vocabularies, vocabulary_kanjis, grammars).
 * Không tạo bảng mới, không sửa bản ghi đã có. Tên trường trong map trùng với {@link ContentType.Field#name()}.
 */
@Repository
@RequiredArgsConstructor
public class LearningContentRepository {

    private static final int BATCH = 500;

    private final JdbcTemplate jdbc;

    public long count(ContentType type) {
        String table = switch (type) {
            case KANJI -> "kanji_dictionary";
            case VOCABULARY -> "vocabularies";
            case GRAMMAR -> "grammars";
        };
        Long n = jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
        return n == null ? 0 : n;
    }

    /** Khóa giao dịch: hai lượt nhập cùng loại không chạy chồng lên nhau. */
    public void lock(ContentType type) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))", rs -> null, "content-import:" + type.name());
    }

    /** Khóa của bản ghi hiện có (theo {@link ContentType#keyOf}); ngữ pháp kèm mẫu câu để chặn mẫu trùng mã khác. */
    public Map<String, Map<String, String>> loadExisting(ContentType type) {
        Map<String, Map<String, String>> result = new HashMap<>();
        switch (type) {
            case KANJI -> jdbc.query("SELECT kanji_character FROM kanji_dictionary", rs -> {
                Map<String, String> m = Map.of("character", rs.getString(1).trim());
                result.put(type.keyOf(m), m);
            });
            case VOCABULARY -> jdbc.query("SELECT word, reading FROM vocabularies", rs -> {
                Map<String, String> m = Map.of("word", rs.getString(1), "reading", rs.getString(2));
                result.put(type.keyOf(m), m);
            });
            case GRAMMAR -> jdbc.query("SELECT grammar_code, pattern FROM grammars WHERE grammar_code IS NOT NULL", rs -> {
                Map<String, String> m = Map.of("code", rs.getString(1), "pattern", rs.getString(2));
                result.put(type.keyOf(m), m);
            });
        }
        return result;
    }

    public void insert(ContentType type, List<Map<String, String>> rows) {
        if (rows.isEmpty()) return;
        switch (type) {
            case KANJI -> insertKanji(rows);
            case VOCABULARY -> insertVocabulary(rows);
            case GRAMMAR -> insertGrammar(rows);
        }
    }

    // ===== Kanji: kanji_dictionary (tra cứu, luyện viết) + kanjis (liên kết từ vựng) =====

    private void insertKanji(List<Map<String, String>> rows) {
        batch("INSERT INTO kanji_dictionary (kanji_character, unicode_hex, stroke_count, han_viet, onyomi, kunyomi, "
                        + "meaning, jlpt_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jlpt_level_enum)",
                rows, v -> new Object[]{v.get("character"), unicodeHex(v.get("character")),
                        Short.valueOf(v.get("strokes")), v.get("hanViet"), v.get("onyomi"), v.get("kunyomi"),
                        v.get("meaning"), v.get("jlpt")});
        // Bảng kanjis có thể đã có chữ này: giữ nguyên bản cũ
        batch("INSERT INTO kanjis (\"character\", han_viet, onyomi, kunyomi, meaning, jlpt_level) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (\"character\") DO NOTHING",
                rows, v -> new Object[]{v.get("character"), v.get("hanViet"), v.get("onyomi"),
                        v.get("kunyomi"), v.get("meaning"), v.get("jlpt")});
    }

    static String unicodeHex(String character) {
        return String.format(Locale.ROOT, "%05x", character.codePointAt(0));
    }

    // ===== Từ vựng: vocabularies + liên kết chữ Hán vocabulary_kanjis =====

    private void insertVocabulary(List<Map<String, String>> rows) {
        batch("INSERT INTO vocabularies (jmdict_id, jlpt_level, word, reading, romaji, han_viet, part_of_speech, "
                        + "meaning, example_sentence, example_furigana, example_meaning) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                rows, v -> new Object[]{v.get("jmdictId"), v.get("jlpt"), v.get("word"), v.get("reading"),
                        v.get("romaji"), v.get("hanViet"), v.get("partOfSpeech"), v.get("meaning"),
                        v.get("exampleSentence"), v.get("exampleFurigana"), v.get("exampleMeaning")});
        linkVocabularyKanji(rows);
    }

    /** Gắn từ mới với các chữ Hán có trong bảng kanjis (giống VocabExcelSeeder). */
    private void linkVocabularyKanji(List<Map<String, String>> rows) {
        for (int from = 0; from < rows.size(); from += BATCH) {
            List<Map<String, String>> chunk = rows.subList(from, Math.min(rows.size(), from + BATCH));
            String[] words = chunk.stream().map(v -> v.get("word")).distinct().toArray(String[]::new);
            String[] readings = chunk.stream().map(v -> v.get("reading")).distinct().toArray(String[]::new);
            jdbc.update(con -> {
                var ps = con.prepareStatement("INSERT INTO vocabulary_kanjis (vocabulary_id, kanji_id) "
                        + "SELECT DISTINCT v.id, k.id FROM vocabularies v "
                        + "CROSS JOIN LATERAL regexp_split_to_table(v.word, '') AS ch "
                        + "JOIN kanjis k ON k.\"character\" = ch "
                        + "WHERE v.word = ANY (?) AND v.reading = ANY (?) "
                        + "ON CONFLICT (vocabulary_id, kanji_id) DO NOTHING");
                Array w = con.createArrayOf("varchar", words);
                Array r = con.createArrayOf("varchar", readings);
                ps.setArray(1, w);
                ps.setArray(2, r);
                return ps;
            });
        }
    }

    // ===== Ngữ pháp: grammars (khóa grammar_code) =====

    private void insertGrammar(List<Map<String, String>> rows) {
        batch("INSERT INTO grammars (grammar_code, jlpt_level, pattern, romaji, meaning, formation, notes, "
                        + "example_sentence, example_furigana, example_meaning, extended_examples, tags) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                rows, v -> new Object[]{v.get("code"), v.get("jlpt"), v.get("pattern"), v.get("romaji"),
                        v.get("meaning"), v.get("formation"), v.get("notes"), v.get("exampleSentence"),
                        v.get("exampleFurigana"), v.get("exampleMeaning"), v.get("extendedExamples"), v.get("tags")});
    }

    private void batch(String sql, List<Map<String, String>> rows, Function<Map<String, String>, Object[]> args) {
        List<Object[]> buffer = new ArrayList<>(BATCH);
        for (Map<String, String> row : rows) {
            buffer.add(args.apply(row));
            if (buffer.size() == BATCH) {
                jdbc.batchUpdate(sql, buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) jdbc.batchUpdate(sql, buffer);
    }
}
