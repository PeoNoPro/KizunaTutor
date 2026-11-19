package com.kizuna.features.practice.service;

import com.kizuna.core.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Đọc và tạo tệp Excel đề luyện thi (FE-13). Hai sheet:
 * <ul>
 *   <li>{@code De}: cột A là nhãn, cột B là giá trị (tên đề, cấp độ, tháng thi, nguồn, số phút từng phần).</li>
 *   <li>{@code CauHoi}: dòng 1 là tiêu đề, mỗi dòng sau là một câu theo đúng thứ tự cột {@link #COLUMNS}.</li>
 * </ul>
 * Cú pháp trong chữ: furigana {@code {来月|らいげつ}}, gạch chân {@code __工事__}.
 * Mọi lỗi được gom lại theo dòng; còn lỗi thì không lưu gì.
 */
public final class PracticeExcel {
    private PracticeExcel() {}

    public static final List<String> COLUMNS = List.of("Phần thi", "Mondai", "Hướng dẫn mondai", "Mã nhóm", "Đoạn văn nhóm", "Ảnh nhóm",
            "Câu số", "Câu hỏi", "Lựa chọn 1", "Lựa chọn 2", "Lựa chọn 3", "Lựa chọn 4", "Đáp án", "Giải thích", "Lời thoại", "Ảnh câu");
    public static final Map<String, String> SECTION_LABELS = Map.of("VOCAB", "Từ vựng", "GRAMMAR_READING", "Ngữ pháp – Đọc hiểu", "LISTENING", "Nghe");
    public static final List<String> SECTION_ORDER = List.of("VOCAB", "GRAMMAR_READING", "LISTENING");
    /** Thời gian mặc định theo JLPT (phút): Từ vựng, Ngữ pháp – Đọc hiểu, Nghe. N1/N2 gộp hai phần đầu. */
    public static final Map<String, int[]> DEFAULT_MINUTES = Map.of("N5", new int[]{20, 40, 30}, "N4", new int[]{25, 55, 35},
            "N3", new int[]{30, 70, 40}, "N2", new int[]{105, 0, 50}, "N1", new int[]{110, 0, 55});

    public record Question(int row, String groupCode, int number, String stem, List<String> options, int correct,
                           String explanation, String script, String imageName) {}
    public record Group(String code, String passage, String imageName) {}
    public record Mondai(String section, int number, String instruction, List<Group> groups, List<Question> questions) {}
    public record Exam(String title, String level, LocalDate examMonth, String source, int minutesVocab, int minutesGrammar,
                       int minutesListening, List<Mondai> mondai) {
        public int questionCount() { return mondai.stream().mapToInt(m -> m.questions().size()).sum(); }
        public Set<String> imageNames() {
            var names = new LinkedHashSet<String>();
            mondai.forEach(m -> { m.groups().forEach(g -> { if (g.imageName() != null) names.add(g.imageName()); });
                m.questions().forEach(q -> { if (q.imageName() != null) names.add(q.imageName()); }); });
            return names;
        }
    }
    public record Problem(int row, String message) {}
    public static final class InvalidFile extends RuntimeException {
        public final List<Problem> problems;
        InvalidFile(List<Problem> problems) { super("Tệp đề còn lỗi."); this.problems = problems; }
    }

    // ------------------------------------------------------------------ đọc tệp

    public static Exam read(InputStream input) {
        Workbook book;
        try { book = new XSSFWorkbook(input); }
        catch (Exception exception) { throw new BusinessException(400, "Không đọc được tệp. Vui lòng dùng tệp .xlsx theo mẫu."); }
        try (book) {
            var problems = new ArrayList<Problem>();
            Sheet info = book.getSheet("De"), rows = book.getSheet("CauHoi");
            if (info == null || rows == null) throw new BusinessException(400, "Tệp thiếu sheet \"De\" hoặc \"CauHoi\". Vui lòng dùng tệp mẫu.");
            var formatter = new DataFormatter();
            var meta = new HashMap<String, String>();
            for (Row row : info) {
                String label = text(formatter, row.getCell(0)), value = text(formatter, row.getCell(1));
                if (label != null) meta.put(normalize(label), value);
            }
            String title = find(meta, "ten de"), level = upper(find(meta, "cap do")), source = find(meta, "nguon");
            if (title == null || title.length() > 200) problems.add(new Problem(0, "Sheet De: tên đề bắt buộc, tối đa 200 ký tự."));
            if (level == null || !DEFAULT_MINUTES.containsKey(level)) problems.add(new Problem(0, "Sheet De: cấp độ phải là N5, N4, N3, N2 hoặc N1."));
            if (source != null && source.length() > 300) problems.add(new Problem(0, "Sheet De: nguồn tối đa 300 ký tự."));
            LocalDate month = null;
            String monthText = find(meta, "thang thi");
            if (monthText != null) {
                try { month = YearMonth.parse(monthText.trim().replace('/', '-')).atDay(1); }
                catch (Exception exception) { problems.add(new Problem(0, "Sheet De: tháng thi ghi dạng YYYY-MM, ví dụ 2024-07.")); }
            }
            int[] defaults = DEFAULT_MINUTES.getOrDefault(level, new int[]{0, 0, 0});
            int vocab = minutes(meta, "phut tu vung", defaults[0], problems), grammar = minutes(meta, "phut ngu phap", defaults[1], problems),
                    listening = minutes(meta, "phut nghe", defaults[2], problems);

            Row header = rows.getRow(0);
            if (header == null || !"Phần thi".equals(text(formatter, header.getCell(0))) || !"Ảnh câu".equals(text(formatter, header.getCell(15))))
                throw new BusinessException(400, "Sheet CauHoi không đúng mẫu (dòng 1 phải là tiêu đề cột từ \"Phần thi\" đến \"Ảnh câu\").");

            var mondai = new LinkedHashMap<String, MondaiBuilder>();
            for (int r = 1; r <= rows.getLastRowNum(); r++) {
                Row row = rows.getRow(r);
                if (row == null) continue;
                String[] c = new String[COLUMNS.size()];
                boolean empty = true;
                for (int i = 0; i < c.length; i++) { c[i] = text(formatter, row.getCell(i)); if (c[i] != null) empty = false; }
                if (empty) continue;
                int line = r + 1, before = problems.size();
                String section = section(c[0]);
                if (section == null) problems.add(new Problem(line, "Phần thi phải là \"Từ vựng\", \"Ngữ pháp – Đọc hiểu\" hoặc \"Nghe\"."));
                Integer mondaiNo = integer(c[1]), number = integer(c[6]), correct = integer(c[12]);
                if (mondaiNo == null || mondaiNo < 1 || mondaiNo > 20) problems.add(new Problem(line, "Mondai phải là số từ 1 đến 20."));
                if (number == null || number < 1 || number > 200) problems.add(new Problem(line, "Câu số phải là số từ 1 đến 200."));
                var options = new ArrayList<String>();
                for (int i = 8; i <= 11; i++) {
                    if (c[i] == null) { for (int j = i + 1; j <= 11; j++) if (c[j] != null) { problems.add(new Problem(line, "Các lựa chọn phải điền liền nhau từ Lựa chọn 1.")); break; } break; }
                    options.add(c[i]);
                }
                if (options.size() < 2) problems.add(new Problem(line, "Mỗi câu cần ít nhất 2 lựa chọn."));
                if (correct == null || correct < 1 || correct > options.size()) problems.add(new Problem(line, "Đáp án phải là số thứ tự của một lựa chọn đã điền (1–" + Math.max(2, options.size()) + ")."));
                for (int i : new int[]{2, 4, 7, 8, 9, 10, 11, 13, 14}) {
                    String error = markup(c[i]);
                    if (error != null) problems.add(new Problem(line, COLUMNS.get(i) + ": " + error));
                }
                if (c[7] == null && c[14] == null && !"LISTENING".equals(section)) problems.add(new Problem(line, "Câu hỏi không được để trống."));
                if (problems.size() > before) continue;
                String key = section + "#" + mondaiNo;
                var builder = mondai.computeIfAbsent(key, k -> new MondaiBuilder(section, mondaiNo));
                if (c[2] != null) {
                    if (builder.instruction != null && !builder.instruction.equals(c[2])) problems.add(new Problem(line, "Hướng dẫn mondai khác với dòng trước của cùng mondai; chỉ cần ghi ở dòng đầu."));
                    else builder.instruction = c[2];
                }
                if (c[3] != null) {
                    var group = builder.groups.computeIfAbsent(c[3], code -> new String[2]);
                    if (c[4] != null) { if (group[0] != null && !group[0].equals(c[4])) problems.add(new Problem(line, "Đoạn văn của nhóm \"" + c[3] + "\" khác dòng trước; chỉ cần ghi ở dòng đầu của nhóm.")); else group[0] = c[4]; }
                    if (c[5] != null) group[1] = c[5];
                } else if (c[4] != null || c[5] != null) problems.add(new Problem(line, "Có đoạn văn hoặc ảnh nhóm thì phải ghi Mã nhóm."));
                if (builder.numbers.contains(number)) problems.add(new Problem(line, "Trùng câu số " + number + " trong cùng mondai."));
                builder.numbers.add(number);
                builder.questions.add(new Question(line, c[3], number, c[7], List.copyOf(options), correct, c[13], c[14], c[15]));
            }
            if (mondai.isEmpty() && problems.isEmpty()) problems.add(new Problem(0, "Sheet CauHoi chưa có câu hỏi nào."));
            if (!problems.isEmpty()) throw new InvalidFile(problems.size() > 200 ? problems.subList(0, 200) : problems);
            var result = mondai.values().stream()
                    .sorted(Comparator.comparingInt((MondaiBuilder m) -> SECTION_ORDER.indexOf(m.section)).thenComparingInt(m -> m.number))
                    .map(m -> new Mondai(m.section, m.number, m.instruction,
                            m.groups.entrySet().stream().map(e -> new Group(e.getKey(), e.getValue()[0], e.getValue()[1])).toList(), List.copyOf(m.questions)))
                    .toList();
            return new Exam(title, level, month, source, vocab, grammar, listening, result);
        } catch (IOException exception) {
            throw new BusinessException(400, "Không đọc được tệp.");
        }
    }

    private static final class MondaiBuilder {
        final String section; final int number; String instruction;
        final Map<String, String[]> groups = new LinkedHashMap<>(); final List<Question> questions = new ArrayList<>(); final Set<Integer> numbers = new HashSet<>();
        MondaiBuilder(String section, int number) { this.section = section; this.number = number; }
    }

    /** Kiểm cú pháp furigana {chữ|cách đọc} và gạch chân __...__; trả về mô tả lỗi hoặc null. */
    public static String markup(String value) {
        if (value == null) return null;
        int depth = 0; boolean bar = false;
        for (char ch : value.toCharArray()) {
            if (ch == '{') { if (depth > 0) return "furigana không được lồng nhau."; depth = 1; bar = false; }
            else if (ch == '|' && depth == 1) bar = true;
            else if (ch == '}') { if (depth == 0) return "thừa dấu }."; if (!bar) return "furigana phải có dạng {chữ|cách đọc}."; depth = 0; }
        }
        if (depth != 0) return "thiếu dấu } của furigana.";
        int marks = 0; for (int i = value.indexOf("__"); i >= 0; i = value.indexOf("__", i + 2)) marks++;
        return marks % 2 == 0 ? null : "thiếu một cặp __ của phần gạch chân.";
    }

    private static String section(String value) {
        if (value == null) return null;
        String v = normalize(value);
        if (v.equals("tu vung") || v.equals("vocab") || v.contains("文字") || v.contains("語彙")) return "VOCAB";
        if (v.startsWith("ngu phap") || v.equals("grammar_reading") || v.contains("文法") || v.contains("読解")) return "GRAMMAR_READING";
        if (v.equals("nghe") || v.equals("listening") || v.contains("聴解")) return "LISTENING";
        return null;
    }

    private static int minutes(Map<String, String> meta, String key, int fallback, List<Problem> problems) {
        String value = find(meta, key);
        if (value == null) return fallback;
        Integer number = integer(value);
        if (number == null || number < 0 || number > 300) { problems.add(new Problem(0, "Sheet De: số phút phải từ 0 đến 300.")); return fallback; }
        return number;
    }

    private static String find(Map<String, String> meta, String prefix) {
        return meta.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).map(Map.Entry::getValue).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static String text(DataFormatter formatter, Cell cell) {
        if (cell == null) return null;
        String value = formatter.formatCellValue(cell);
        if (value == null) return null;
        value = value.replace("\r\n", "\n").strip();
        return value.isEmpty() ? null : value;
    }

    private static Integer integer(String value) {
        if (value == null) return null;
        try { double d = Double.parseDouble(value.trim()); return d == Math.rint(d) ? (int) d : null; } catch (NumberFormatException e) { return null; }
    }

    private static String upper(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }

    /** Bỏ dấu tiếng Việt, gộp khoảng trắng và gạch ngang để so nhãn linh hoạt. */
    static String normalize(String value) {
        String plain = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT);
        return plain.replaceAll("[–—-]", " ").replaceAll("[():]", " ").replaceAll("\\s+", " ").strip();
    }

    // ------------------------------------------------------------------ tệp mẫu

    public static byte[] template() {
        try (var book = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var bold = book.createFont(); bold.setBold(true);
            var head = book.createCellStyle(); head.setFont(bold); head.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var wrap = book.createCellStyle(); wrap.setWrapText(true); wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet info = book.createSheet("De");
            String[][] meta = {{"Tên đề", "JLPT N5 – Tháng 7/2024"}, {"Cấp độ", "N5"}, {"Tháng thi (YYYY-MM)", "2024-07"}, {"Nguồn (không bắt buộc)", ""},
                    {"Phút Từ vựng", "20"}, {"Phút Ngữ pháp – Đọc hiểu", "40"}, {"Phút Nghe", "30"}};
            for (int i = 0; i < meta.length; i++) { Row row = info.createRow(i); row.createCell(0).setCellValue(meta[i][0]); row.getCell(0).setCellStyle(head); row.createCell(1).setCellValue(meta[i][1]); }
            info.setColumnWidth(0, 30 * 256); info.setColumnWidth(1, 40 * 256);

            Sheet rows = book.createSheet("CauHoi");
            Row header = rows.createRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) { header.createCell(i).setCellValue(COLUMNS.get(i)); header.getCell(i).setCellStyle(head); rows.setColumnWidth(i, (i == 2 || i == 4 || i == 7 || i >= 13 ? 40 : 14) * 256); }
            String[][] samples = {
                    {"Từ vựng", "1", "＿＿＿の ことばは ひらがなで どう かきますか。１・２・３・４から いちばん いい ものを ひとつ えらんで ください。", "", "", "",
                            "1", "いえの {前|まえ}で __工事__を して います。", "こうじ", "こうごと", "こじ", "ここと", "1", "工事「こうじ」", "", ""},
                    {"Ngữ pháp – Đọc hiểu", "4", "つぎの （１）から（３）の ぶんしょうを よんで、しつもんに こたえて ください。", "D1", "（ここに đoạn văn đọc hiểu）", "",
                            "22", "メールを 読んで、何を しなければ なりませんか。", "…", "…", "…", "…", "3", "", "", ""},
                    {"Nghe", "1", "もんだい１では、はじめに しつもんを きいて ください。", "", "", "", "1", "", "1", "2", "3", "4", "2", "",
                            "レストランで女の人と店の人が話しています。…", "nghe-1-1.png"}};
            for (int r = 0; r < samples.length; r++) { Row row = rows.createRow(r + 1); for (int i = 0; i < samples[r].length; i++) { row.createCell(i).setCellValue(samples[r][i]); row.getCell(i).setCellStyle(wrap); } }
            rows.createFreezePane(0, 1);

            Sheet guide = book.createSheet("HuongDan");
            String[] lines = {"Cách điền tệp đề luyện thi KizunaTutor",
                    "• Sheet De: tên đề, cấp độ (N5–N1), tháng thi YYYY-MM, nguồn (không bắt buộc), số phút từng phần (để trống = theo JLPT).",
                    "• Sheet CauHoi: mỗi dòng một câu. Phần thi: Từ vựng / Ngữ pháp – Đọc hiểu / Nghe. Câu số đánh như trong đề (không trùng trong một mondai).",
                    "• Hướng dẫn mondai chỉ cần ghi ở dòng đầu của mondai.",
                    "• Đọc hiểu: các câu dùng chung một đoạn văn thì ghi cùng Mã nhóm (ví dụ D1); đoạn văn chỉ cần ghi ở dòng đầu của nhóm.",
                    "• Furigana: {来月|らいげつ}. Gạch chân: __工事__. Chỗ trống（　）và ★ ghi như trong đề.",
                    "• Lựa chọn: điền liền nhau từ Lựa chọn 1 (2–4 lựa chọn). Đáp án: số thứ tự lựa chọn đúng.",
                    "• Nghe: Câu hỏi có thể để trống nếu chỉ nghe; ghi Lời thoại (script) để học viên xem sau khi nộp. Lựa chọn là tranh thì ghi 1, 2, 3, 4 và tải ảnh tranh.",
                    "• Ảnh nhóm / Ảnh câu: ghi đúng tên tệp ảnh (PNG/JPG) và tải các ảnh này lên cùng lúc với tệp Excel."};
            for (int i = 0; i < lines.length; i++) guide.createRow(i).createCell(0).setCellValue(lines[i]);
            guide.setColumnWidth(0, 150 * 256);
            book.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không tạo được tệp mẫu.", exception);
        }
    }
}
