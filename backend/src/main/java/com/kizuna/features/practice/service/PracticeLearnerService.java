package com.kizuna.features.practice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * FE-13: Luyện đề của học viên — luôn làm theo một đề đã đăng (chế độ EXAM):
 * <ul>
 *   <li>Làm bài mới: toàn bộ câu của các phần đã chọn theo thứ tự trong đề; thời gian = tổng số phút các phần.</li>
 *   <li>Làm lại câu sai: chỉ câu của đề đó có lần trả lời gần nhất bị sai, không tính giờ.</li>
 * </ul>
 * Chỉ còn chế độ làm theo đề (EXAM); chế độ trộn ngẫu nhiên đã bỏ hẳn 2026-11-19 (cả nhãn và ràng buộc trong DB).
 * Đáp án, giải thích và lời thoại chỉ trả về sau khi nộp. Quá hạn (cộng 2 phút trễ mạng) thì tự nộp khi mở lại bài.
 */
@Service
@RequiredArgsConstructor
public class PracticeLearnerService {
    private static final int GRACE_SECONDS = 120;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final PracticeMediaTokens tokens;

    /** Dữ liệu cho hộp chọn: mỗi cấp độ có những đề nào và bao nhiêu câu từng phần. */
    @Transactional(readOnly = true)
    public Map<String, Object> options() {
        var exams = jdbc.queryForList("""
            SELECT e.exam_id,e.title,e.level,e.exam_month,e.minutes_vocab,e.minutes_grammar,e.minutes_listening,
                   count(*) FILTER (WHERE m.section='VOCAB') AS vocab,count(*) FILTER (WHERE m.section='GRAMMAR_READING') AS grammar,
                   count(*) FILTER (WHERE m.section='LISTENING') AS listening
            FROM practice_exams e JOIN practice_questions q ON q.exam_id=e.exam_id JOIN practice_mondai m ON m.mondai_id=q.mondai_id
            WHERE e.status='PUBLISHED' GROUP BY e.exam_id ORDER BY e.level DESC,e.exam_month DESC NULLS LAST,e.title""");
        var levels = new LinkedHashMap<String, Map<String, Object>>();
        for (String level : List.of("N5", "N4", "N3", "N2", "N1")) {
            var entry = new LinkedHashMap<String, Object>(); entry.put("level", level); entry.put("exams", new ArrayList<Map<String, Object>>());
            levels.put(level, entry);
        }
        for (var r : exams) {
            var entry = levels.get(String.valueOf(r.get("level")));
            if (entry == null) continue;
            var exam = new LinkedHashMap<String, Object>();
            exam.put("examId", r.get("exam_id")); exam.put("title", r.get("title")); exam.put("examMonth", PracticeAdminService.month(r.get("exam_month")));
            exam.put("counts", Map.of("VOCAB", r.get("vocab"), "GRAMMAR_READING", r.get("grammar"), "LISTENING", r.get("listening")));
            exam.put("minutes", Map.of("VOCAB", r.get("minutes_vocab"), "GRAMMAR_READING", r.get("minutes_grammar"), "LISTENING", r.get("minutes_listening")));
            @SuppressWarnings("unchecked") var list = (List<Map<String, Object>>) entry.get("exams"); list.add(exam);
        }
        return Map.of("levels", List.copyOf(levels.values()), "sectionLabels", PracticeExcel.SECTION_LABELS);
    }

    // ------------------------------------------------------------------ bắt đầu

    @Transactional
    public Map<String, Object> start(UUID learner, Map<String, Object> body) {
        String level = String.valueOf(body.get("level")).trim().toUpperCase(Locale.ROOT);
        if (!PracticeExcel.DEFAULT_MINUTES.containsKey(level)) throw new BusinessException(400, "Vui lòng chọn cấp độ.");
        String mode = "EXAM";
        boolean wrongOnly = Boolean.TRUE.equals(body.get("wrongOnly"));
        var sections = new ArrayList<String>();
        if (body.get("sections") instanceof List<?> list) for (Object s : list) if (PracticeExcel.SECTION_LABELS.containsKey(String.valueOf(s)) && !sections.contains(String.valueOf(s))) sections.add(String.valueOf(s));
        if (sections.isEmpty()) throw new BusinessException(400, "Vui lòng chọn ít nhất một phần thi.");
        sections.sort(Comparator.comparingInt(PracticeExcel.SECTION_ORDER::indexOf));

        var in = String.join(",", Collections.nCopies(sections.size(), "?"));
        List<UUID> ids;
        OffsetDateTime deadline = null;
        UUID examId;
        try { examId = UUID.fromString(String.valueOf(body.get("examId"))); } catch (Exception e) { throw new BusinessException(400, "Vui lòng chọn đề."); }
        var exam = jdbc.queryForList("SELECT * FROM practice_exams WHERE exam_id=? AND status='PUBLISHED' AND level=?", examId, level);
        if (exam.isEmpty()) throw new BusinessException(404, "Đề không còn mở cho học viên.");
        var args = new ArrayList<Object>(); args.add(examId); args.addAll(sections);
        String wrong = wrongOnly ? " AND q.question_id IN (" + WRONG + ")" : "";
        if (wrongOnly) args.add(learner);
        ids = jdbc.queryForList("SELECT q.question_id FROM practice_questions q JOIN practice_mondai m ON m.mondai_id=q.mondai_id WHERE q.exam_id=? AND m.section IN (" + in + ")"
                + wrong + " ORDER BY m.sort_order,q.sort_order", UUID.class, args.toArray());
        if (!wrongOnly) {
            var e = exam.get(0);
            int minutes = 0;
            for (String s : sections) minutes += ((Number) e.get(switch (s) { case "VOCAB" -> "minutes_vocab"; case "GRAMMAR_READING" -> "minutes_grammar"; default -> "minutes_listening"; })).intValue();
            if (minutes > 0) deadline = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(minutes);
        }
        if (ids.isEmpty()) throw new BusinessException(409, wrongOnly ? "Bạn chưa có câu sai nào ở lựa chọn này. Hãy làm bài mới trước." : "Chưa có câu hỏi phù hợp với lựa chọn này.");

        UUID attemptId = UUID.randomUUID();
        jdbc.update("INSERT INTO practice_attempts(attempt_id,learner_id,level,mode,exam_id,sections,wrong_only,question_count,deadline_at) VALUES (?,?,?,?,?,?,?,?,?)",
                attemptId, learner, level, mode, examId, String.join(",", sections), wrongOnly, ids.size(), deadline);
        int position = 0;
        for (UUID id : ids) jdbc.update("INSERT INTO practice_attempt_answers(attempt_id,question_id,position) VALUES (?,?,?)", attemptId, id, position++);
        return Map.of("attemptId", attemptId);
    }

    /** Câu có lần trả lời gần nhất (trong các bài đã nộp) bị sai. Tham số: mã học viên. */
    private static final String WRONG = """
        SELECT x.question_id FROM (SELECT a.question_id,a.is_correct,row_number() OVER (PARTITION BY a.question_id ORDER BY t.submitted_at DESC) AS rn
          FROM practice_attempt_answers a JOIN practice_attempts t ON t.attempt_id=a.attempt_id
          WHERE t.learner_id=? AND t.submitted_at IS NOT NULL AND a.chosen IS NOT NULL) x WHERE x.rn=1 AND x.is_correct=false""";

    // ------------------------------------------------------------------ làm bài

    @Transactional
    public Map<String, Object> attempt(UUID learner, UUID attemptId) {
        var a = owned(learner, attemptId, true);
        if (a.get("submitted_at") == null && expired(a)) { grade(attemptId); a = owned(learner, attemptId, false); }
        boolean submitted = a.get("submitted_at") != null;
        var answers = jdbc.queryForList("SELECT question_id,position,chosen,flagged,is_correct FROM practice_attempt_answers WHERE attempt_id=? ORDER BY position", attemptId);
        var questions = PracticeContent.questions(jdbc, json, tokens, answers.stream().map(r -> (UUID) r.get("question_id")).toList(), submitted);
        var byId = new HashMap<UUID, Map<String, Object>>(); answers.forEach(r -> byId.put((UUID) r.get("question_id"), r));
        for (var q : questions) {
            var ans = byId.get((UUID) q.get("questionId"));
            q.put("position", ans.get("position")); q.put("chosen", ans.get("chosen")); q.put("flagged", ans.get("flagged"));
            if (submitted) q.put("isCorrect", ans.get("is_correct"));
        }
        var out = summary(a);
        out.put("questions", questions);
        out.put("serverTime", OffsetDateTime.now(ZoneOffset.UTC));
        if (a.get("exam_id") != null) {
            var e = jdbc.queryForList("SELECT title,minutes_vocab,minutes_grammar,minutes_listening FROM practice_exams WHERE exam_id=?", a.get("exam_id"));
            if (!e.isEmpty()) out.put("minutes", Map.of("VOCAB", e.get(0).get("minutes_vocab"), "GRAMMAR_READING", e.get(0).get("minutes_grammar"), "LISTENING", e.get(0).get("minutes_listening")));
        }
        if (submitted) out.put("sectionScores", sectionScores(attemptId));
        return out;
    }

    @Transactional
    public Map<String, Object> answer(UUID learner, UUID attemptId, UUID questionId, Map<String, Object> body) {
        var a = owned(learner, attemptId, true);
        if (a.get("submitted_at") != null) throw new BusinessException(409, "Bài đã nộp, không sửa đáp án được nữa.");
        // Hết giờ: chấm và lưu ngay (không ném lỗi, nếu ném thì transaction bị hủy và mất kết quả chấm), báo trang tải lại bài.
        if (expired(a)) { grade(attemptId); return Map.of("saved", false, "expired", true, "message", "Đã hết giờ làm bài, hệ thống đã tự nộp."); }
        Integer chosen = body.get("chosen") instanceof Number n ? n.intValue() : null;
        if (chosen != null && (chosen < 1 || chosen > 4)) throw BusinessException.invalid();
        boolean flagged = Boolean.TRUE.equals(body.get("flagged"));
        int changed = jdbc.update("UPDATE practice_attempt_answers SET chosen=?,flagged=?,answered_at=CASE WHEN ? IS NULL THEN answered_at ELSE now() END WHERE attempt_id=? AND question_id=?",
                chosen, flagged, chosen, attemptId, questionId);
        if (changed == 0) throw BusinessException.missing();
        return Map.of("saved", true);
    }

    @Transactional
    public Map<String, Object> submit(UUID learner, UUID attemptId) {
        var a = owned(learner, attemptId, true);
        if (a.get("submitted_at") == null) grade(attemptId);
        return attempt(learner, attemptId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(UUID learner, int page, int size) {
        if (page < 0 || size < 1 || size > 50) throw BusinessException.invalid();
        var rows = jdbc.queryForList("SELECT a.*,e.title AS exam_title FROM practice_attempts a LEFT JOIN practice_exams e ON e.exam_id=a.exam_id WHERE a.learner_id=? ORDER BY a.started_at DESC LIMIT ? OFFSET ?", learner, size, page * size);
        Long total = jdbc.queryForObject("SELECT count(*) FROM practice_attempts WHERE learner_id=?", Long.class, learner);
        return Map.of("items", rows.stream().map(this::summary).toList(), "page", page, "size", size, "totalElements", total == null ? 0 : total);
    }

    // ------------------------------------------------------------------ chấm điểm

    private void grade(UUID attemptId) {
        jdbc.update("""
            UPDATE practice_attempt_answers a SET is_correct=(a.chosen IS NOT NULL AND a.chosen=q.correct)
            FROM practice_questions q WHERE q.question_id=a.question_id AND a.attempt_id=?""", attemptId);
        jdbc.update("""
            UPDATE practice_attempts SET submitted_at=now(),
                   correct_count=(SELECT count(*) FROM practice_attempt_answers WHERE attempt_id=? AND is_correct)
            WHERE attempt_id=? AND submitted_at IS NULL""", attemptId, attemptId);
    }

    private List<Map<String, Object>> sectionScores(UUID attemptId) {
        return jdbc.queryForList("""
            SELECT m.section,count(*) AS total,count(*) FILTER (WHERE a.is_correct) AS correct
            FROM practice_attempt_answers a JOIN practice_questions q ON q.question_id=a.question_id JOIN practice_mondai m ON m.mondai_id=q.mondai_id
            WHERE a.attempt_id=? GROUP BY m.section ORDER BY CASE m.section WHEN 'VOCAB' THEN 0 WHEN 'GRAMMAR_READING' THEN 1 ELSE 2 END""", attemptId)
            .stream().map(r -> {
                var out = new LinkedHashMap<String, Object>();
                out.put("section", r.get("section")); out.put("label", PracticeExcel.SECTION_LABELS.get(r.get("section")));
                out.put("total", r.get("total")); out.put("correct", r.get("correct"));
                return (Map<String, Object>) out;
            }).toList();
    }

    // ------------------------------------------------------------------ tiện ích

    private Map<String, Object> owned(UUID learner, UUID attemptId, boolean lock) {
        var rows = jdbc.queryForList("SELECT * FROM practice_attempts WHERE attempt_id=?" + (lock ? " FOR UPDATE" : ""), attemptId);
        if (rows.isEmpty() || !learner.equals(rows.get(0).get("learner_id"))) throw new BusinessException(404, "Không tìm thấy bài làm.");
        return rows.get(0);
    }

    private static boolean expired(Map<String, Object> a) {
        Object deadline = a.get("deadline_at");
        if (deadline == null) return false;
        var time = deadline instanceof OffsetDateTime o ? o : ((java.sql.Timestamp) deadline).toInstant().atOffset(ZoneOffset.UTC);
        return OffsetDateTime.now(ZoneOffset.UTC).isAfter(time.plusSeconds(GRACE_SECONDS));
    }

    private Map<String, Object> summary(Map<String, Object> a) {
        var out = new LinkedHashMap<String, Object>();
        out.put("attemptId", a.get("attempt_id")); out.put("level", a.get("level")); out.put("mode", a.get("mode"));
        out.put("modeLabel", "Theo đề"); out.put("wrongOnly", a.get("wrong_only"));
        var sections = Arrays.stream(String.valueOf(a.get("sections")).split(",")).filter(s -> !s.isBlank()).toList();
        out.put("sections", sections); out.put("sectionLabels", sections.stream().map(PracticeExcel.SECTION_LABELS::get).toList());
        out.put("examId", a.get("exam_id"));
        // Danh sách lịch sử đã JOIN sẵn tên đề (tránh một truy vấn cho mỗi dòng); trang một bài thì đọc riêng.
        if (a.containsKey("exam_title")) out.put("examTitle", a.get("exam_title"));
        else if (a.get("exam_id") != null) out.put("examTitle", jdbc.queryForList("SELECT title FROM practice_exams WHERE exam_id=?", String.class, a.get("exam_id")).stream().findFirst().orElse(null));
        out.put("questionCount", a.get("question_count")); out.put("correctCount", a.get("correct_count"));
        out.put("startedAt", a.get("started_at")); out.put("deadlineAt", a.get("deadline_at")); out.put("submittedAt", a.get("submitted_at"));
        out.put("submitted", a.get("submitted_at") != null);
        return out;
    }
}
