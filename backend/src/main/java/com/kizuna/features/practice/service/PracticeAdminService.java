package com.kizuna.features.practice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.practice.storage.PracticeFileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

/**
 * FE-13: Ngân hàng đề của cán bộ học vụ — tạo đề từ Excel, xem/sửa câu, tải file nghe (phát cả file khi thi theo đề), đăng/ẩn.
 * Quyền ROLE_ACADEMIC_OFFICER do SecurityConfig kiểm (đường dẫn /api/academic/practice/**).
 */
@Service
@RequiredArgsConstructor
public class PracticeAdminService {
    static final Map<String, String> STATUS_LABELS = Map.of("DRAFT", "Nháp", "PUBLISHED", "Đã đăng", "HIDDEN", "Đã ẩn");
    private static final Set<String> AUDIO_TYPES = Set.of("audio/mpeg", "audio/mp3", "audio/mp4", "audio/x-m4a", "audio/aac", "audio/wav", "audio/x-wav", "audio/ogg", "audio/webm");
    private static final Map<String, String> IMAGE_TYPES = Map.of("image/png", "png", "image/jpeg", "jpg", "image/webp", "webp");
    private static final long MAX_AUDIO = 200_000_000L, MAX_IMAGE = 5_000_000L, MAX_EXCEL = 5_000_000L;

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final PracticeFileStorage storage;
    private final PracticeMediaTokens tokens;

    // ------------------------------------------------------------------ danh sách / chi tiết

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(String level, String status) {
        var sql = new StringBuilder("""
            SELECT e.exam_id,e.title,e.level,e.exam_month,e.status,e.updated_at,e.published_at,e.audio_file_id IS NOT NULL AS has_audio,
                   (SELECT count(*) FROM practice_questions q WHERE q.exam_id=e.exam_id) AS questions,
                   (SELECT count(*) FROM practice_questions q JOIN practice_mondai m ON m.mondai_id=q.mondai_id WHERE q.exam_id=e.exam_id AND m.section='LISTENING') AS listening,
                   (SELECT count(*) FROM practice_attempt_answers a JOIN practice_questions q ON q.question_id=a.question_id WHERE q.exam_id=e.exam_id) AS answers
            FROM practice_exams e WHERE true""");
        var args = new ArrayList<Object>();
        if (level != null && !level.isBlank()) { sql.append(" AND e.level=?"); args.add(level(level)); }
        if (status != null && !status.isBlank()) { if (!STATUS_LABELS.containsKey(status)) throw BusinessException.invalid(); sql.append(" AND e.status=?"); args.add(status); }
        sql.append(" ORDER BY e.level DESC, e.exam_month DESC NULLS LAST, e.title");
        return jdbc.queryForList(sql.toString(), args.toArray()).stream().map(r -> {
            var out = new LinkedHashMap<String, Object>();
            out.put("examId", r.get("exam_id")); out.put("title", r.get("title")); out.put("level", r.get("level"));
            out.put("examMonth", month(r.get("exam_month"))); out.put("status", r.get("status")); out.put("statusLabel", STATUS_LABELS.get(r.get("status")));
            out.put("questionCount", r.get("questions")); out.put("listeningCount", r.get("listening"));
            out.put("hasAudio", r.get("has_audio")); out.put("used", ((Number) r.get("answers")).longValue() > 0); out.put("updatedAt", r.get("updated_at"));
            return (Map<String, Object>) out;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(UUID examId) {
        var e = exam(examId, false);
        var out = new LinkedHashMap<String, Object>();
        out.put("examId", examId); out.put("title", e.get("title")); out.put("level", e.get("level")); out.put("examMonth", month(e.get("exam_month")));
        out.put("source", e.get("source")); out.put("minutesVocab", e.get("minutes_vocab")); out.put("minutesGrammar", e.get("minutes_grammar"));
        out.put("minutesListening", e.get("minutes_listening")); out.put("status", e.get("status")); out.put("statusLabel", STATUS_LABELS.get(e.get("status")));
        out.put("version", e.get("version")); out.put("publishedAt", e.get("published_at"));
        out.put("used", used(examId));
        var audio = (UUID) e.get("audio_file_id");
        out.put("audio", audio == null ? null : fileView(audio));
        out.put("mondai", PracticeContent.load(jdbc, json, tokens, examId, null, true));
        return out;
    }

    // ------------------------------------------------------------------ tạo / nhập lại

    @Transactional
    public Map<String, Object> create(UUID actor, MultipartFile excel, List<MultipartFile> images) {
        var parsed = parse(excel);
        UUID examId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO practice_exams(exam_id,title,level,exam_month,source,minutes_vocab,minutes_grammar,minutes_listening,status,created_by)
            VALUES (?,?,?,?,?,?,?,?,'DRAFT',?)""", examId, parsed.title(), parsed.level(), parsed.examMonth(), parsed.source(),
                parsed.minutesVocab(), parsed.minutesGrammar(), parsed.minutesListening(), actor);
        saveContent(actor, examId, parsed, images);
        return detail(examId);
    }

    /** Nhập lại nội dung cho đề còn Nháp và chưa có ai làm: thay toàn bộ câu hỏi và ảnh (giữ file nghe). */
    @Transactional
    public Map<String, Object> reimport(UUID actor, UUID examId, MultipartFile excel, List<MultipartFile> images) {
        var e = exam(examId, true);
        if (!"DRAFT".equals(e.get("status")) || used(examId)) throw new BusinessException(409, "Chỉ nhập lại được đề đang Nháp và chưa có học viên làm.");
        var parsed = parse(excel);
        var oldImages = jdbc.queryForList("SELECT storage_key FROM practice_files WHERE exam_id=? AND kind='IMAGE'", String.class, examId);
        jdbc.update("DELETE FROM practice_questions WHERE exam_id=?", examId);
        jdbc.update("DELETE FROM practice_groups WHERE mondai_id IN (SELECT mondai_id FROM practice_mondai WHERE exam_id=?)", examId);
        jdbc.update("DELETE FROM practice_mondai WHERE exam_id=?", examId);
        jdbc.update("DELETE FROM practice_files WHERE exam_id=? AND kind='IMAGE'", examId);
        afterCommit(() -> oldImages.forEach(storage::delete));
        jdbc.update("""
            UPDATE practice_exams SET title=?,level=?,exam_month=?,source=?,minutes_vocab=?,minutes_grammar=?,minutes_listening=?,updated_at=now(),version=version+1
            WHERE exam_id=?""", parsed.title(), parsed.level(), parsed.examMonth(), parsed.source(), parsed.minutesVocab(), parsed.minutesGrammar(), parsed.minutesListening(), examId);
        saveContent(actor, examId, parsed, images);
        return detail(examId);
    }

    private PracticeExcel.Exam parse(MultipartFile excel) {
        if (excel == null || excel.isEmpty()) throw new BusinessException(400, "Vui lòng chọn tệp Excel đề.");
        if (excel.getSize() > MAX_EXCEL) throw new BusinessException(413, "Tệp Excel tối đa 5 MB.");
        try (InputStream in = excel.getInputStream()) {
            return PracticeExcel.read(in);
        } catch (PracticeExcel.InvalidFile invalid) {
            throw new InvalidExam(invalid.problems);
        } catch (IOException exception) {
            throw new BusinessException(400, "Không đọc được tệp Excel.");
        }
    }

    /** Lỗi tệp đề theo từng dòng: controller trả danh sách để trang hiện cho học vụ sửa. */
    public static final class InvalidExam extends RuntimeException {
        public final List<PracticeExcel.Problem> problems;
        InvalidExam(List<PracticeExcel.Problem> problems) { super("Tệp đề còn lỗi."); this.problems = problems; }
    }

    private void saveContent(UUID actor, UUID examId, PracticeExcel.Exam parsed, List<MultipartFile> images) {
        // Ảnh: chỉ nhận đúng các tên được nhắc trong tệp Excel.
        var byName = new HashMap<String, MultipartFile>();
        for (var file : images == null ? List.<MultipartFile>of() : images) if (file != null && !file.isEmpty()) byName.put(baseName(file.getOriginalFilename()), file);
        var missing = parsed.imageNames().stream().filter(name -> !byName.containsKey(baseName(name))).toList();
        if (!missing.isEmpty()) throw new InvalidExam(missing.stream().map(n -> new PracticeExcel.Problem(0, "Chưa tải lên ảnh \"" + n + "\".")).toList());
        var imageIds = new HashMap<String, UUID>();
        for (String name : parsed.imageNames()) imageIds.put(name, storeImage(actor, examId, byName.get(baseName(name))));

        int mondaiOrder = 0, questionOrder = 0;
        for (var m : parsed.mondai()) {
            UUID mondaiId = UUID.randomUUID();
            jdbc.update("INSERT INTO practice_mondai(mondai_id,exam_id,section,number,instruction,sort_order) VALUES (?,?,?,?,?,?)",
                    mondaiId, examId, m.section(), m.number(), m.instruction(), mondaiOrder++);
            var groupIds = new HashMap<String, UUID>();
            int groupOrder = 0;
            for (var g : m.groups()) {
                UUID groupId = UUID.randomUUID(); groupIds.put(g.code(), groupId);
                jdbc.update("INSERT INTO practice_groups(group_id,mondai_id,code,passage,image_file_id,sort_order) VALUES (?,?,?,?,?,?)",
                        groupId, mondaiId, g.code(), g.passage(), g.imageName() == null ? null : imageIds.get(g.imageName()), groupOrder++);
            }
            for (var q : m.questions()) {
                jdbc.update("""
                    INSERT INTO practice_questions(question_id,exam_id,mondai_id,group_id,number,stem,options,correct,explanation,script,image_file_id,sort_order)
                    VALUES (?,?,?,?,?,?,CAST(? AS jsonb),?,?,?,?,?)""", UUID.randomUUID(), examId, mondaiId,
                        q.groupCode() == null ? null : groupIds.get(q.groupCode()), q.number(), q.stem(), toJson(q.options()), q.correct(),
                        q.explanation(), q.script(), q.imageName() == null ? null : imageIds.get(q.imageName()), questionOrder++);
            }
        }
    }

    private UUID storeImage(UUID actor, UUID examId, MultipartFile file) {
        String ext = IMAGE_TYPES.get(String.valueOf(file.getContentType()).toLowerCase(Locale.ROOT));
        if (ext == null) throw new BusinessException(400, "Ảnh \"" + file.getOriginalFilename() + "\" phải là PNG, JPG hoặc WEBP.");
        if (file.getSize() > MAX_IMAGE) throw new BusinessException(413, "Ảnh \"" + file.getOriginalFilename() + "\" tối đa 5 MB.");
        UUID fileId = UUID.randomUUID();
        String key = "exam/" + examId + "/" + fileId + "." + ext;
        long size;
        try { size = storage.save(key, file.getInputStream(), MAX_IMAGE); }
        catch (IOException exception) { throw new BusinessException(400, "Không đọc được ảnh \"" + file.getOriginalFilename() + "\"."); }
        rollbackDelete(key);
        jdbc.update("INSERT INTO practice_files(file_id,exam_id,kind,storage_key,original_name,content_type,size_bytes,created_by) VALUES (?,?,'IMAGE',?,?,?,?,?)",
                fileId, examId, key, limit(file.getOriginalFilename(), 255), file.getContentType(), size, actor);
        return fileId;
    }

    // ------------------------------------------------------------------ sửa

    @Transactional
    public Map<String, Object> updateInfo(UUID examId, Map<String, Object> body) {
        var e = exam(examId, true);
        expectVersion(e, body);
        String title = text(body.get("title"), 200, true);
        String level = level(String.valueOf(body.get("level")));
        if (!level.equals(e.get("level")) && used(examId)) throw new BusinessException(409, "Đề đã có học viên làm nên không đổi cấp độ được.");
        LocalDate month = null;
        Object m = body.get("examMonth");
        if (m != null && !String.valueOf(m).isBlank()) {
            try { month = java.time.YearMonth.parse(String.valueOf(m)).atDay(1); } catch (Exception ex) { throw new BusinessException(400, "Tháng thi ghi dạng YYYY-MM."); }
        }
        jdbc.update("UPDATE practice_exams SET title=?,level=?,exam_month=?,source=?,minutes_vocab=?,minutes_grammar=?,minutes_listening=?,updated_at=now(),version=version+1 WHERE exam_id=?",
                title, level, month, text(body.get("source"), 300, false), minutes(body.get("minutesVocab")), minutes(body.get("minutesGrammar")),
                minutes(body.get("minutesListening")), examId);
        return detail(examId);
    }

    @Transactional
    public Map<String, Object> updateQuestion(UUID examId, UUID questionId, Map<String, Object> body) {
        exam(examId, true);
        var rows = jdbc.queryForList("SELECT question_id FROM practice_questions WHERE question_id=? AND exam_id=? FOR UPDATE", questionId, examId);
        if (rows.isEmpty()) throw BusinessException.missing();
        String stem = text(body.get("stem"), 5000, false);
        @SuppressWarnings("unchecked") var raw = body.get("options") instanceof List<?> list ? (List<Object>) list : List.of();
        var options = raw.stream().map(o -> o == null ? "" : String.valueOf(o).strip()).toList();
        if (options.size() < 2 || options.size() > 4 || options.stream().anyMatch(String::isEmpty)) throw new BusinessException(400, "Mỗi câu cần 2–4 lựa chọn, không để trống lựa chọn nào.");
        int correct = body.get("correct") instanceof Number n ? n.intValue() : -1;
        if (correct < 1 || correct > options.size()) throw new BusinessException(400, "Đáp án phải là số thứ tự của một lựa chọn.");
        String explanation = text(body.get("explanation"), 5000, false), script = text(body.get("script"), 10000, false);
        for (String value : concat(stem, explanation, script, options)) {
            String error = PracticeExcel.markup(value);
            if (error != null) throw new BusinessException(400, "Sai cú pháp: " + error);
        }
        jdbc.update("UPDATE practice_questions SET stem=?,options=CAST(? AS jsonb),correct=?,explanation=?,script=? WHERE question_id=?",
                stem, toJson(options), correct, explanation, script, questionId);
        touch(examId);
        return detail(examId);
    }

    /**
     * Đổi / bỏ ảnh của một câu ({@code target=question}) hoặc ảnh dùng chung của đoạn đọc chứa câu đó ({@code target=group}).
     * {@code file == null} là bỏ ảnh. Ảnh cũ chỉ xóa khỏi nơi lưu khi đề chưa có học viên làm.
     */
    @Transactional
    public Map<String, Object> setImage(UUID actor, UUID examId, UUID questionId, String target, MultipartFile file) {
        exam(examId, true);
        var rows = jdbc.queryForList("SELECT group_id,image_file_id FROM practice_questions WHERE question_id=? AND exam_id=? FOR UPDATE", questionId, examId);
        if (rows.isEmpty()) throw BusinessException.missing();
        boolean group = "group".equals(target);
        if (!group && !"question".equals(target)) throw BusinessException.invalid();
        UUID groupId = (UUID) rows.get(0).get("group_id");
        if (group && groupId == null) throw new BusinessException(400, "Câu này không thuộc đoạn đọc nào.");
        UUID previous = group ? jdbc.queryForObject("SELECT image_file_id FROM practice_groups WHERE group_id=?", UUID.class, groupId) : (UUID) rows.get(0).get("image_file_id");
        UUID next = file == null || file.isEmpty() ? null : storeImage(actor, examId, file);
        if (group) jdbc.update("UPDATE practice_groups SET image_file_id=? WHERE group_id=?", next, groupId);
        else jdbc.update("UPDATE practice_questions SET image_file_id=? WHERE question_id=?", next, questionId);
        if (previous != null && !used(examId)) {
            String oldKey = jdbc.queryForObject("SELECT storage_key FROM practice_files WHERE file_id=?", String.class, previous);
            jdbc.update("DELETE FROM practice_files WHERE file_id=? AND NOT EXISTS (SELECT 1 FROM practice_questions WHERE image_file_id=?) AND NOT EXISTS (SELECT 1 FROM practice_groups WHERE image_file_id=?)",
                    previous, previous, previous);
            afterCommit(() -> { if (jdbc.queryForList("SELECT 1 FROM practice_files WHERE file_id=?", previous).isEmpty()) storage.delete(oldKey); });
        }
        touch(examId);
        return detail(examId);
    }

    /** Tải file nghe theo luồng (không qua multipart nên không vướng giới hạn tải tệp chung của hệ thống). */
    @Transactional
    public Map<String, Object> uploadAudio(UUID actor, UUID examId, String contentType, String fileName, InputStream body) {
        var e = exam(examId, true);
        String type = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!AUDIO_TYPES.contains(type)) throw new BusinessException(400, "File nghe phải là MP3, M4A, WAV hoặc OGG.");
        UUID fileId = UUID.randomUUID();
        String ext = switch (type) { case "audio/wav", "audio/x-wav" -> "wav"; case "audio/ogg" -> "ogg"; case "audio/webm" -> "webm"; case "audio/mp4", "audio/x-m4a", "audio/aac" -> "m4a"; default -> "mp3"; };
        String key = "exam/" + examId + "/" + fileId + "." + ext;
        long size = storage.save(key, body, MAX_AUDIO);
        rollbackDelete(key);
        jdbc.update("INSERT INTO practice_files(file_id,exam_id,kind,storage_key,original_name,content_type,size_bytes,created_by) VALUES (?,?,'AUDIO',?,?,?,?,?)",
                fileId, examId, key, limit(fileName == null || fileName.isBlank() ? "nghe." + ext : fileName, 255), type, size, actor);
        UUID previous = (UUID) e.get("audio_file_id");
        jdbc.update("UPDATE practice_exams SET audio_file_id=?,updated_at=now(),version=version+1 WHERE exam_id=?", fileId, examId);
        // File cũ chỉ xóa khi đề chưa ai làm (lượt cũ có thể đang nghe file đó).
        if (previous != null && !used(examId)) {
            String oldKey = jdbc.queryForObject("SELECT storage_key FROM practice_files WHERE file_id=?", String.class, previous);
            jdbc.update("DELETE FROM practice_files WHERE file_id=?", previous);
            afterCommit(() -> storage.delete(oldKey));
        }
        return detail(examId);
    }

    @Transactional
    public Map<String, Object> publish(UUID examId) {
        exam(examId, true);
        long questions = count("SELECT count(*) FROM practice_questions WHERE exam_id=?", examId);
        if (questions == 0) throw new BusinessException(409, "Đề chưa có câu hỏi nào.");
        long listening = count("SELECT count(*) FROM practice_questions q JOIN practice_mondai m ON m.mondai_id=q.mondai_id WHERE q.exam_id=? AND m.section='LISTENING'", examId);
        Object audio = jdbc.queryForObject("SELECT audio_file_id FROM practice_exams WHERE exam_id=?", Object.class, examId);
        if (listening > 0 && audio == null) throw new BusinessException(409, "Đề có phần Nghe nhưng chưa tải file nghe.");
        jdbc.update("UPDATE practice_exams SET status='PUBLISHED',published_at=coalesce(published_at,now()),updated_at=now(),version=version+1 WHERE exam_id=?", examId);
        return detail(examId);
    }

    @Transactional
    public Map<String, Object> hide(UUID examId) {
        exam(examId, true);
        jdbc.update("UPDATE practice_exams SET status='HIDDEN',updated_at=now(),version=version+1 WHERE exam_id=?", examId);
        return detail(examId);
    }

    /** Chỉ xóa được đề Nháp chưa từng đăng và chưa có ai làm. */
    @Transactional
    public void delete(UUID examId) {
        var e = exam(examId, true);
        if (!"DRAFT".equals(e.get("status")) || e.get("published_at") != null || used(examId))
            throw new BusinessException(409, "Chỉ xóa được đề Nháp chưa từng đăng. Đề đã đăng thì hãy Ẩn.");
        var keys = jdbc.queryForList("SELECT storage_key FROM practice_files WHERE exam_id=?", String.class, examId);
        jdbc.update("DELETE FROM practice_questions WHERE exam_id=?", examId);
        jdbc.update("DELETE FROM practice_groups WHERE mondai_id IN (SELECT mondai_id FROM practice_mondai WHERE exam_id=?)", examId);
        jdbc.update("DELETE FROM practice_mondai WHERE exam_id=?", examId);
        jdbc.update("UPDATE practice_exams SET audio_file_id=NULL WHERE exam_id=?", examId);
        jdbc.update("DELETE FROM practice_files WHERE exam_id=?", examId);
        jdbc.update("DELETE FROM practice_exams WHERE exam_id=?", examId);
        afterCommit(() -> keys.forEach(storage::delete));
    }

    // ------------------------------------------------------------------ tiện ích

    private Map<String, Object> exam(UUID examId, boolean lock) {
        var rows = jdbc.queryForList("SELECT * FROM practice_exams WHERE exam_id=?" + (lock ? " FOR UPDATE" : ""), examId);
        if (rows.isEmpty()) throw BusinessException.missing();
        return rows.get(0);
    }

    private boolean used(UUID examId) {
        return count("SELECT count(*) FROM practice_attempt_answers a JOIN practice_questions q ON q.question_id=a.question_id WHERE q.exam_id=?", examId) > 0;
    }

    private long count(String sql, Object... args) { Long n = jdbc.queryForObject(sql, Long.class, args); return n == null ? 0 : n; }

    private void touch(UUID examId) { jdbc.update("UPDATE practice_exams SET updated_at=now(),version=version+1 WHERE exam_id=?", examId); }

    private Map<String, Object> fileView(UUID fileId) {
        var rows = jdbc.queryForList("SELECT original_name,size_bytes,content_type FROM practice_files WHERE file_id=?", fileId);
        if (rows.isEmpty()) return null;
        var out = new LinkedHashMap<String, Object>(); var r = rows.get(0);
        out.put("fileId", fileId); out.put("name", r.get("original_name")); out.put("sizeBytes", r.get("size_bytes")); out.put("url", tokens.path(fileId));
        return out;
    }

    private void expectVersion(Map<String, Object> exam, Map<String, Object> body) {
        if (!(body.get("version") instanceof Number n) || n.longValue() != ((Number) exam.get("version")).longValue()) throw BusinessException.conflict();
    }

    private static String level(String value) {
        String v = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!PracticeExcel.DEFAULT_MINUTES.containsKey(v)) throw new BusinessException(400, "Cấp độ phải là N5, N4, N3, N2 hoặc N1.");
        return v;
    }

    private static int minutes(Object value) {
        int n = value instanceof Number number ? number.intValue() : -1;
        if (n < 0 || n > 300) throw new BusinessException(400, "Số phút phải từ 0 đến 300.");
        return n;
    }

    private static String text(Object value, int max, boolean required) {
        String v = value == null ? null : String.valueOf(value).strip();
        if (v == null || v.isEmpty()) { if (required) throw new BusinessException(400, "Vui lòng nhập đủ thông tin bắt buộc."); return null; }
        if (v.codePointCount(0, v.length()) > max) throw new BusinessException(400, "Nội dung tối đa " + max + " ký tự.");
        return v;
    }

    private static List<String> concat(String a, String b, String c, List<String> rest) {
        var all = new ArrayList<String>(); all.add(a); all.add(b); all.add(c); all.addAll(rest); return all;
    }

    private String toJson(Object value) {
        try { return json.writeValueAsString(value); } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    static String month(Object value) { return value == null ? null : value.toString().substring(0, 7); }

    private static String baseName(String name) {
        if (name == null) return "";
        String n = name.replace('\\', '/'); n = n.substring(n.lastIndexOf('/') + 1);
        return n.strip().toLowerCase(Locale.ROOT);
    }

    private static String limit(String value, int max) { return value == null ? null : value.length() <= max ? value : value.substring(0, max); }

    /** File đã ghi nhưng transaction bị hủy (ví dụ lỗi ở ảnh sau) thì xóa để không để lại tệp mồ côi. */
    private void rollbackDelete(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) storage.delete(key); }
        });
    }

    private void afterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { task.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { task.run(); }
        });
    }

    @SuppressWarnings("unused") private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
}
