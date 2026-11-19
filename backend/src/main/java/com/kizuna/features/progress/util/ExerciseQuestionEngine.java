package com.kizuna.features.progress.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Lõi câu hỏi của bài làm trực tiếp (FE-07):
 * - kiểm tra và chuẩn hóa dữ liệu câu hỏi gia sư nhập;
 * - tạo đề cho học viên (KHÔNG kèm đáp án; xáo trộn ngân hàng từ / cột phải / thứ tự cố định theo từng lượt);
 * - chấm từng câu: đúng hết mới có điểm (câu nhiều phần sai một phần = 0 điểm);
 *   so sánh chữ bỏ qua khoảng trắng thừa, hoa/thường, chữ toàn/nửa độ rộng (NFKC).
 * Chỗ trống trong câu điền khuyết / điền từ được đánh dấu bằng 3 dấu gạch dưới trở lên: "___".
 */
@Component
public class ExerciseQuestionEngine {

    public static final String FIELD = "questions";
    /**
     * Đánh dấu câu soạn theo cách mới (từ khi có AI gợi ý chấm): tự luận không có đáp án; điền khuyết đáp án không bắt buộc.
     * Câu cũ không có khóa này giữ nguyên cách chấm cũ.
     */
    public static final String REVIEW_KEY = "review";
    public static final Pattern BLANK = Pattern.compile("_{3,}");
    static final int MAX_OPTIONS = 10;
    static final int MAX_ITEMS = 20;
    static final int MAX_TEXT = 2000;
    static final BigDecimal MAX_POINTS = new BigDecimal("100");

    private final ObjectMapper mapper;

    public ExerciseQuestionEngine(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Kết quả chấm một câu. correct null = câu tự luận chờ gia sư chấm. */
    public record Grade(Boolean correct, BigDecimal score) {
    }

    // ================================================================== kiểm tra dữ liệu gia sư nhập

    /**
     * Kiểm tra câu hỏi và trả dữ liệu đã chuẩn hóa (chuỗi JSON).
     * @param strict true khi giao bài: bắt buộc đủ đáp án; false khi lưu nháp: chỉ kiểm tra cấu trúc
     * @param autoMode không còn dùng (giữ để tương thích): tự luận luôn chờ chấm, điền khuyết không bắt buộc đáp án
     * @param label "Câu 3" — để báo lỗi rõ câu nào
     */
    public String validate(ExerciseQuestionType type, String prompt, BigDecimal points, JsonNode data,
                           boolean strict, boolean autoMode, String label) {
        if (type == null) throw error(label, "chưa chọn dạng câu hỏi.");
        if (prompt != null && prompt.length() > MAX_TEXT) throw error(label, "đề bài tối đa " + MAX_TEXT + " ký tự.");
        if (points == null || points.signum() <= 0 || points.compareTo(MAX_POINTS) > 0) {
            throw error(label, "điểm của câu phải lớn hơn 0 và tối đa 100.");
        }
        JsonNode d = data == null || data.isNull() ? mapper.createObjectNode() : data;
        if (!d.isObject()) throw error(label, "dữ liệu câu hỏi không hợp lệ.");
        ObjectNode out = mapper.createObjectNode();
        switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE -> {
                List<String> options = texts(d.get("options"));
                if (options.size() > MAX_OPTIONS) throw error(label, "tối đa " + MAX_OPTIONS + " lựa chọn.");
                out.set("options", array(options));
                if (type == ExerciseQuestionType.SINGLE_CHOICE) {
                    Integer correct = d.hasNonNull("correct") && d.get("correct").canConvertToInt() ? d.get("correct").asInt() : null;
                    if (strict) {
                        requireMin(options, 2, label, "cần ít nhất 2 lựa chọn.");
                        requireNoBlank(options, label, "có lựa chọn đang để trống.");
                        if (correct == null || correct < 0 || correct >= options.size()) throw error(label, "chưa chọn đáp án đúng.");
                    }
                    if (correct != null) out.put("correct", correct);
                } else {
                    List<Integer> correct = ints(d.get("correct"));
                    if (strict) {
                        requireMin(options, 2, label, "cần ít nhất 2 lựa chọn.");
                        requireNoBlank(options, label, "có lựa chọn đang để trống.");
                        if (correct.isEmpty()) throw error(label, "chưa chọn đáp án đúng.");
                        for (int c : correct) if (c < 0 || c >= options.size()) throw error(label, "đáp án đúng không hợp lệ.");
                    }
                    out.set("correct", intArray(new TreeSet<>(correct)));
                }
            }
            case TRUE_FALSE -> {
                if (d.hasNonNull("correct") && d.get("correct").isBoolean()) out.put("correct", d.get("correct").asBoolean());
                else if (strict) throw error(label, "chưa chọn Đúng hoặc Sai.");
            }
            case FILL_BLANK -> {
                // Đáp án từng chỗ trống KHÔNG bắt buộc: ô có đáp án máy so trước, ô còn lại chờ chấm (AI gợi ý, gia sư duyệt).
                String text = text(d.get("text"));
                int blanks = countBlanks(text);
                List<List<String>> answers = new ArrayList<>();
                JsonNode a = d.get("answers");
                if (a != null && a.isArray()) for (JsonNode one : a) answers.add(texts(one));
                if (strict && blanks == 0) throw error(label, "câu cần có ít nhất một chỗ trống (gõ ___ để tạo chỗ trống).");
                out.put("text", text);
                ArrayNode arr = mapper.createArrayNode();
                for (int i = 0; i < blanks; i++) {
                    List<String> list = i < answers.size() ? answers.get(i) : List.of();
                    arr.add(array(list.stream().filter(s -> !s.isBlank()).toList()));
                }
                out.set("answers", arr);
                out.put(REVIEW_KEY, true);
            }
            case WORD_BANK -> {
                String text = text(d.get("text"));
                int blanks = countBlanks(text);
                if (isOptionSet(d)) {
                    validateOptionSets(d, text, blanks, strict, label, out);
                    break;
                }
                // Dạng cũ (bộ từ để kéo vào từng chỗ trống): giữ nguyên cho bài đã soạn trước đây
                List<String> answers = texts(d.get("answers"));
                List<String> distractors = texts(d.get("distractors")).stream().filter(s -> !s.isBlank()).toList();
                if (strict) {
                    if (blanks == 0) throw error(label, "câu cần có ít nhất một chỗ trống (gõ ___ để tạo chỗ trống).");
                    if (answers.size() != blanks) throw error(label, "số từ đúng không khớp với số chỗ trống.");
                    if (answers.size() + distractors.size() < 2) {
                        throw error(label, "cần ít nhất 2 từ để kéo thả (thêm từ gây nhiễu).");
                    }
                    requireNoBlank(answers, label, "có chỗ trống chưa có từ đúng.");
                }
                if (answers.size() + distractors.size() > MAX_ITEMS * 2) throw error(label, "ngân hàng từ quá dài.");
                out.put("text", text);
                out.set("answers", array(answers));
                out.set("distractors", array(distractors));
            }
            case MATCHING -> {
                ArrayNode pairs = mapper.createArrayNode();
                JsonNode p = d.get("pairs");
                int n = 0;
                if (p != null && p.isArray()) {
                    for (JsonNode one : p) {
                        String left = text(one.get("left"));
                        String right = text(one.get("right"));
                        if (strict && (left.isBlank() || right.isBlank())) throw error(label, "có cặp nối đang để trống.");
                        ObjectNode pair = mapper.createObjectNode();
                        pair.put("left", left);
                        pair.put("right", right);
                        pairs.add(pair);
                        n++;
                    }
                }
                if (n > MAX_ITEMS) throw error(label, "tối đa " + MAX_ITEMS + " cặp nối.");
                if (strict && n < 2) throw error(label, "cần ít nhất 2 cặp nối.");
                out.set("pairs", pairs);
            }
            case ORDERING -> {
                List<String> items = texts(d.get("items"));
                if (items.size() > MAX_ITEMS) throw error(label, "tối đa " + MAX_ITEMS + " mục.");
                if (strict) {
                    requireMin(items, 2, label, "cần ít nhất 2 mục để sắp xếp.");
                    requireNoBlank(items, label, "có mục đang để trống.");
                }
                out.set("items", array(items));
            }
            case WRITTEN -> {
                // Tự luận: gia sư chỉ ra đề, không nhập đáp án / bài mẫu. Bài nộp luôn chờ chấm (AI gợi ý, gia sư duyệt).
                out.put(REVIEW_KEY, true);
            }
        }
        if (strict && type != ExerciseQuestionType.FILL_BLANK && type != ExerciseQuestionType.WORD_BANK
                && (prompt == null || prompt.isBlank())) {
            throw error(label, "chưa nhập đề bài.");
        }
        return write(out);
    }

    /** Câu tự luận không có đáp án sẵn → cần gia sư chấm. */
    public boolean needsManualGrading(ExerciseQuestionType type, String data) {
        JsonNode d = read(data);
        if (type == ExerciseQuestionType.WRITTEN) return texts(d.get("accepted")).isEmpty();
        if (type == ExerciseQuestionType.FILL_BLANK && d.path(REVIEW_KEY).asBoolean(false)) {
            JsonNode answers = d.get("answers");
            if (answers == null || !answers.isArray() || answers.isEmpty()) return true;
            for (JsonNode one : answers) if (texts(one).stream().allMatch(String::isBlank)) return true;
        }
        return false;
    }

    /**
     * Câu có thể cần chấm (AI gợi ý, gia sư duyệt): tự luận chưa có đáp án, và điền khuyết soạn theo cách mới
     * (ô không có đáp án hoặc học viên điền không khớp đáp án sẽ chờ chấm). Dùng để suy ra cách chấm của bài.
     */
    public boolean mayNeedReview(ExerciseQuestionType type, String data) {
        return needsManualGrading(type, data)
                || (type == ExerciseQuestionType.FILL_BLANK && read(data).path(REVIEW_KEY).asBoolean(false));
    }

    // ================================================================== đề cho học viên (không có đáp án)

    /** Dữ liệu hiển thị cho học viên, KHÔNG chứa đáp án. seed cố định theo lượt + câu để tải lại vẫn cùng thứ tự xáo. */
    public JsonNode learnerView(ExerciseQuestionType type, String data, long seed) {
        JsonNode d = read(data);
        ObjectNode out = mapper.createObjectNode();
        switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE -> out.set("options", d.get("options") == null ? mapper.createArrayNode() : d.get("options"));
            case TRUE_FALSE, WRITTEN -> {
            }
            case FILL_BLANK -> out.set("segments", array(segments(text(d.get("text")))));
            case WORD_BANK -> {
                out.set("segments", array(segments(text(d.get("text")))));
                if (isOptionSet(d)) {
                    out.set("options", d.get("options"));
                    break;
                }
                List<String> bank = new ArrayList<>(texts(d.get("answers")));
                bank.addAll(texts(d.get("distractors")));
                out.set("bank", array(shuffled(bank, seed)));
            }
            case MATCHING -> {
                List<String> lefts = new ArrayList<>();
                List<String> rights = new ArrayList<>();
                JsonNode p = d.get("pairs");
                if (p != null) for (JsonNode one : p) {
                    lefts.add(text(one.get("left")));
                    rights.add(text(one.get("right")));
                }
                out.set("lefts", array(lefts));
                out.set("rights", array(shuffled(rights, seed)));
            }
            case ORDERING -> out.set("items", array(shuffled(texts(d.get("items")), seed)));
        }
        return out;
    }

    /** Đáp án đúng để hiện cho học viên sau khi nộp (nếu gia sư cho xem) hoặc cho gia sư khi chấm. */
    public JsonNode correctAnswer(ExerciseQuestionType type, String data) {
        JsonNode d = read(data);
        ObjectNode out = mapper.createObjectNode();
        switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE -> { if (d.has("correct")) out.set("correct", d.get("correct")); }
            case FILL_BLANK -> out.set("answers", d.get("answers") == null ? mapper.createArrayNode() : d.get("answers"));
            case WORD_BANK -> {
                if (isOptionSet(d)) {
                    out.set("options", d.get("options"));
                    out.set("correct", d.get("correct"));
                } else {
                    out.set("answers", d.get("answers") == null ? mapper.createArrayNode() : d.get("answers"));
                }
            }
            case MATCHING -> out.set("pairs", d.get("pairs") == null ? mapper.createArrayNode() : d.get("pairs"));
            case ORDERING -> out.set("items", d.get("items") == null ? mapper.createArrayNode() : d.get("items"));
            case WRITTEN -> out.set("accepted", d.get("accepted") == null ? mapper.createArrayNode() : d.get("accepted"));
        }
        return out;
    }

    // ================================================================== chấm

    /** Chấm một câu. Đúng hết mới được đủ điểm; câu tự luận không có đáp án sẵn → correct null (chờ gia sư). */
    public Grade grade(ExerciseQuestionType type, String data, BigDecimal points, JsonNode response) {
        JsonNode d = read(data);
        JsonNode r = response == null || response.isNull() ? mapper.createObjectNode() : response;
        boolean correct;
        switch (type) {
            case SINGLE_CHOICE -> correct = d.hasNonNull("correct") && r.hasNonNull("choice")
                    && r.get("choice").canConvertToInt() && r.get("choice").asInt() == d.get("correct").asInt();
            case MULTI_CHOICE -> correct = !ints(d.get("correct")).isEmpty()
                    && new TreeSet<>(ints(r.get("choices"))).equals(new TreeSet<>(ints(d.get("correct"))));
            case TRUE_FALSE -> correct = d.hasNonNull("correct") && r.hasNonNull("value") && r.get("value").isBoolean()
                    && r.get("value").asBoolean() == d.get("correct").asBoolean();
            case FILL_BLANK -> {
                JsonNode answers = d.get("answers");
                List<String> given = texts(r.get("blanks"));
                if (d.path(REVIEW_KEY).asBoolean(false)) return gradeFillBlankForReview(answers, given, points);
                correct = answers != null && answers.size() > 0 && given.size() == answers.size();
                for (int i = 0; correct && i < answers.size(); i++) {
                    String g = normalize(given.get(i));
                    correct = !g.isEmpty() && texts(answers.get(i)).stream().anyMatch(a -> normalize(a).equals(g));
                }
            }
            case WORD_BANK -> correct = isOptionSet(d)
                    ? d.hasNonNull("correct") && r.hasNonNull("choice") && r.get("choice").canConvertToInt()
                        && r.get("choice").asInt() == d.get("correct").asInt()
                    : sameTexts(texts(r.get("blanks")), texts(d.get("answers")));
            case MATCHING -> {
                List<String> rights = new ArrayList<>();
                JsonNode p = d.get("pairs");
                if (p != null) for (JsonNode one : p) rights.add(text(one.get("right")));
                correct = sameTexts(texts(r.get("matches")), rights);
            }
            case ORDERING -> correct = sameTexts(texts(r.get("order")), texts(d.get("items")));
            case WRITTEN -> {
                List<String> accepted = texts(d.get("accepted"));
                if (accepted.isEmpty()) return new Grade(null, BigDecimal.ZERO);
                String g = normalize(text(r.get("text")));
                correct = !g.isEmpty() && accepted.stream().anyMatch(a -> normalize(a).equals(g));
            }
            default -> correct = false;
        }
        return new Grade(correct, correct ? points : BigDecimal.ZERO);
    }

    /**
     * Điền khuyết cách mới: bỏ trống hết → sai (0 điểm); mọi ô đều có đáp án và khớp → đúng đủ điểm;
     * còn lại (ô không có đáp án, hoặc điền khác đáp án) → correct null = chờ chấm (AI gợi ý, gia sư duyệt).
     */
    private Grade gradeFillBlankForReview(JsonNode answers, List<String> given, BigDecimal points) {
        int blanks = answers == null ? 0 : answers.size();
        if (given.stream().allMatch(g -> normalize(g).isEmpty())) return new Grade(false, BigDecimal.ZERO);
        boolean allMatched = blanks > 0;
        for (int i = 0; allMatched && i < blanks; i++) {
            String g = i < given.size() ? normalize(given.get(i)) : "";
            List<String> accepted = texts(answers.get(i)).stream().filter(s -> !s.isBlank()).toList();
            allMatched = !g.isEmpty() && accepted.stream().anyMatch(a -> normalize(a).equals(g));
        }
        return allMatched ? new Grade(true, points) : new Grade(null, BigDecimal.ZERO);
    }

    /**
     * Điền từ có sẵn kiểu đề JLPT: mỗi phương án là một bộ từ cho đủ các chỗ trống, cách nhau bằng "/"
     * (vd "は/で/を" cho câu 3 chỗ trống); học viên chọn 1 phương án. Dạng cũ (answers + distractors) không có khóa options.
     */
    static boolean isOptionSet(JsonNode d) {
        return d != null && d.has("options");
    }

    /** Tách một phương án thành các từ theo dấu "/" (bỏ khoảng trắng hai đầu từng từ). */
    public static List<String> optionWords(String option) {
        if (option == null) return List.of();
        return java.util.Arrays.stream(option.split("/", -1)).map(String::strip).toList();
    }

    private void validateOptionSets(JsonNode d, String text, int blanks, boolean strict, String label, ObjectNode out) {
        List<String> options = new ArrayList<>();
        for (String o : texts(d.get("options"))) {
            List<String> words = optionWords(o);
            options.add(words.stream().allMatch(String::isBlank) ? "" : String.join("/", words));
        }
        Integer correct = d.hasNonNull("correct") && d.get("correct").canConvertToInt() ? d.get("correct").asInt() : null;
        if (options.size() > MAX_ITEMS) throw error(label, "tối đa " + MAX_ITEMS + " phương án.");
        if (strict) {
            if (blanks == 0) throw error(label, "câu cần có ít nhất một chỗ trống (gõ ___ để tạo chỗ trống).");
            if (options.size() < 2) throw error(label, "cần ít nhất 2 phương án để chọn.");
            for (int i = 0; i < options.size(); i++) {
                List<String> words = optionWords(options.get(i));
                if (words.size() != blanks || words.stream().anyMatch(String::isBlank)) {
                    throw error(label, "phương án " + (char) ('A' + i) + " phải có đủ " + blanks + " từ"
                            + (blanks > 1 ? ", cách nhau bằng dấu /" : "") + ".");
                }
            }
            if (new java.util.HashSet<>(options.stream().map(ExerciseQuestionEngine::normalize).toList()).size() != options.size()) {
                throw error(label, "có phương án bị trùng nhau.");
            }
            if (correct == null || correct < 0 || correct >= options.size()) throw error(label, "chưa chọn phương án đúng.");
        }
        out.put("text", text);
        out.set("options", array(options));
        if (correct == null) out.putNull("correct");
        else out.put("correct", correct);
    }

    /** Chuẩn hóa chữ khi so sánh: NFKC (toàn/nửa độ rộng), bỏ khoảng trắng đầu cuối, gộp khoảng trắng, chữ thường. */
    public static String normalize(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    // ================================================================== JSON

    public JsonNode read(String json) {
        try {
            return json == null || json.isBlank() ? mapper.createObjectNode() : mapper.readTree(json);
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    public String write(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw ClassroomException.badRequest("Dữ liệu không hợp lệ.");
        }
    }

    // ================================================================== hỗ trợ

    static int countBlanks(String text) {
        int n = 0;
        var m = BLANK.matcher(text == null ? "" : text);
        while (m.find()) n++;
        return n;
    }

    /** Tách câu theo chỗ trống: "A ___ B ___ C" → ["A ", " B ", " C"]. */
    static List<String> segments(String text) {
        return Arrays.asList(BLANK.split(text == null ? "" : text, -1));
    }

    private static boolean sameTexts(List<String> given, List<String> expected) {
        if (expected.isEmpty() || given.size() != expected.size()) return false;
        for (int i = 0; i < expected.size(); i++) {
            if (!normalize(given.get(i)).equals(normalize(expected.get(i))) || normalize(given.get(i)).isEmpty()) return false;
        }
        return true;
    }

    /** Xáo trộn cố định theo seed; nếu xáo ra đúng thứ tự gốc thì xoay vòng để học viên không nhận được đáp án sẵn. */
    static List<String> shuffled(List<String> items, long seed) {
        List<String> copy = new ArrayList<>(items);
        Collections.shuffle(copy, new Random(seed));
        if (copy.size() > 1 && copy.equals(items)) Collections.rotate(copy, 1);
        return copy;
    }

    private static String text(JsonNode n) {
        return n == null || n.isNull() ? "" : n.asText("").strip();
    }

    private static List<String> texts(JsonNode n) {
        List<String> out = new ArrayList<>();
        if (n != null && n.isArray()) for (JsonNode one : n) out.add(one == null || one.isNull() ? "" : one.asText("").strip());
        return out;
    }

    private static List<Integer> ints(JsonNode n) {
        List<Integer> out = new ArrayList<>();
        if (n != null && n.isArray()) for (JsonNode one : n) if (one.canConvertToInt()) out.add(one.asInt());
        return out;
    }

    private ArrayNode array(Collection<String> values) {
        ArrayNode a = mapper.createArrayNode();
        values.forEach(a::add);
        return a;
    }

    private ArrayNode intArray(Collection<Integer> values) {
        ArrayNode a = mapper.createArrayNode();
        values.forEach(a::add);
        return a;
    }

    private static void requireMin(List<String> list, int min, String label, String message) {
        if (list.size() < min) throw error(label, message);
    }

    private static void requireNoBlank(List<String> list, String label, String message) {
        if (list.stream().anyMatch(String::isBlank)) throw error(label, message);
    }

    private static ClassroomException error(String label, String message) {
        return ClassroomException.field(FIELD, label + ": " + message);
    }
}
