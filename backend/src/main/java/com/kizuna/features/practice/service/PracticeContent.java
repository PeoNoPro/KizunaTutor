package com.kizuna.features.practice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * Đọc câu hỏi kèm mondai, đoạn văn/ảnh nhóm và đoạn âm thanh — dùng chung cho trang xem trước của học vụ và bài làm
 * của học viên. {@code answers=false} thì không trả đáp án, giải thích hay lời thoại (bài chưa nộp).
 */
final class PracticeContent {
    private PracticeContent() {}

    private static final String SELECT = """
        SELECT q.question_id,q.number,q.stem,q.options::text AS options,q.correct,q.explanation,q.script,q.image_file_id,
               q.sort_order,m.mondai_id,m.section,m.number AS mondai_number,m.instruction,m.sort_order AS mondai_order,
               g.group_id,g.passage,g.image_file_id AS group_image,e.exam_id,e.title AS exam_title,e.audio_file_id
        FROM practice_questions q JOIN practice_mondai m ON m.mondai_id=q.mondai_id JOIN practice_exams e ON e.exam_id=q.exam_id
        LEFT JOIN practice_groups g ON g.group_id=q.group_id""";

    /** Toàn bộ đề theo mondai (trang xem trước của học vụ). */
    static List<Map<String, Object>> load(JdbcTemplate jdbc, ObjectMapper json, PracticeMediaTokens tokens, UUID examId, Object unused, boolean answers) {
        var rows = jdbc.queryForList(SELECT + " WHERE q.exam_id=? ORDER BY m.sort_order,q.sort_order", examId);
        var mondai = new LinkedHashMap<UUID, Map<String, Object>>();
        for (var r : rows) {
            var m = mondai.computeIfAbsent((UUID) r.get("mondai_id"), id -> {
                var out = new LinkedHashMap<String, Object>();
                out.put("mondaiId", id); out.put("section", r.get("section")); out.put("sectionLabel", PracticeExcel.SECTION_LABELS.get(r.get("section")));
                out.put("number", r.get("mondai_number")); out.put("instruction", r.get("instruction")); out.put("questions", new ArrayList<Map<String, Object>>());
                return out;
            });
            @SuppressWarnings("unchecked") var list = (List<Map<String, Object>>) m.get("questions");
            list.add(view(json, tokens, r, answers));
        }
        return List.copyOf(mondai.values());
    }

    /** Các câu theo mã (bài làm của học viên), giữ nguyên thứ tự truyền vào. */
    static List<Map<String, Object>> questions(JdbcTemplate jdbc, ObjectMapper json, PracticeMediaTokens tokens, List<UUID> ids, boolean answers) {
        if (ids.isEmpty()) return List.of();
        var rows = jdbc.queryForList(SELECT + " WHERE q.question_id IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")", ids.toArray());
        var byId = new HashMap<UUID, Map<String, Object>>();
        for (var r : rows) byId.put((UUID) r.get("question_id"), view(json, tokens, r, answers));
        return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
    }

    private static Map<String, Object> view(ObjectMapper json, PracticeMediaTokens tokens, Map<String, Object> r, boolean answers) {
        var q = new LinkedHashMap<String, Object>();
        q.put("questionId", r.get("question_id")); q.put("examId", r.get("exam_id")); q.put("examTitle", r.get("exam_title"));
        q.put("section", r.get("section")); q.put("sectionLabel", PracticeExcel.SECTION_LABELS.get(r.get("section")));
        q.put("mondaiId", r.get("mondai_id")); q.put("mondaiNumber", r.get("mondai_number")); q.put("instruction", r.get("instruction"));
        q.put("groupId", r.get("group_id")); q.put("passage", r.get("passage")); q.put("groupImageUrl", tokens.path((UUID) r.get("group_image")));
        q.put("number", r.get("number")); q.put("stem", r.get("stem")); q.put("options", options(json, r.get("options")));
        q.put("imageUrl", tokens.path((UUID) r.get("image_file_id")));
        if ("LISTENING".equals(r.get("section")) && r.get("audio_file_id") != null) {
            var audio = new LinkedHashMap<String, Object>();
            audio.put("url", tokens.path((UUID) r.get("audio_file_id")));
            q.put("audio", audio);
        }
        if (answers) { q.put("correct", r.get("correct")); q.put("explanation", r.get("explanation")); q.put("script", r.get("script")); }
        return q;
    }

    private static List<String> options(ObjectMapper json, Object value) {
        try { return json.readValue(String.valueOf(value), new TypeReference<List<String>>() {}); }
        catch (Exception exception) { return List.of(); }
    }
}
