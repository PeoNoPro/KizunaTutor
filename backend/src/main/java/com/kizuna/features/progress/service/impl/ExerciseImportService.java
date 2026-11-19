package com.kizuna.features.progress.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.ClassroomService;
import com.kizuna.features.classroom.util.CurrentActor;
import com.kizuna.features.progress.entity.ExerciseQuestionType;
import com.kizuna.features.progress.util.ExerciseQuestionEngine;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.*;

/**
 * Nhập câu hỏi bài làm trực tiếp từ Excel (FE-07).
 * - File mẫu: sheet "CauHoi" (mỗi dòng một câu), sheet "ViDu" (mỗi dạng câu một ví dụ), sheet "HuongDan".
 * - Xem trước: đọc file, kiểm tra từng dòng bằng cùng quy tắc khi giao bài; KHÔNG lưu gì. Gia sư chọn thêm vào bài
 *   (trên trình soạn), sửa tiếp rồi mới lưu / giao bài như bình thường.
 * Cột: A Loại câu · B Đề bài (câu điền: đoạn có chỗ trống ___) · C Lựa chọn / các mục (cách nhau bằng |) · D Đáp án đúng · E Điểm.
 */
@Service
public class ExerciseImportService {

    public static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    public static final int MAX_ROWS = 200;
    static final String SHEET = "CauHoi";
    static final String[] HEADERS = {"Loại câu", "Đề bài / Câu có chỗ trống (___)", "Lựa chọn / Các mục (cách nhau bằng |)",
            "Đáp án đúng", "Điểm", "Câu nghe (Có / để trống)", "Số lần nghe (1–5, trống = không giới hạn)"};
    static final int COLS = HEADERS.length;
    static final int MAX_PLAYS = 5;

    /** Tên dạng câu trong file (khớp nhãn trên giao diện) — so khớp bỏ dấu, bỏ khoảng trắng và ký tự đặc biệt. */
    static final Map<ExerciseQuestionType, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put(ExerciseQuestionType.SINGLE_CHOICE, "Trắc nghiệm 1 đáp án");
        LABELS.put(ExerciseQuestionType.MULTI_CHOICE, "Trắc nghiệm nhiều đáp án");
        LABELS.put(ExerciseQuestionType.TRUE_FALSE, "Đúng / Sai");
        LABELS.put(ExerciseQuestionType.FILL_BLANK, "Điền khuyết");
        LABELS.put(ExerciseQuestionType.WORD_BANK, "Điền từ có sẵn");
        LABELS.put(ExerciseQuestionType.MATCHING, "Nối cột");
        LABELS.put(ExerciseQuestionType.ORDERING, "Sắp xếp thứ tự");
        LABELS.put(ExerciseQuestionType.WRITTEN, "Tự luận");
    }

    /**
     * Một dòng đã đọc: ok = hợp lệ (question có dữ liệu chuẩn hóa), ngược lại error là lý do.
     * listening = câu nghe (cần thêm file nghe trên trình soạn trước khi giao bài); maxPlays null = không giới hạn.
     */
    public record ImportRow(int row, boolean ok, String error, ExerciseQuestionType type, String prompt,
                            BigDecimal points, JsonNode data, boolean listening, Integer maxPlays) {
    }

    public record ImportPreview(int total, int valid, int invalid, BigDecimal validPoints, List<ImportRow> rows) {
    }

    private final ClassroomService classroomService;
    private final ExerciseQuestionEngine engine;
    private final ObjectMapper mapper;

    public ExerciseImportService(ClassroomService classroomService, ExerciseQuestionEngine engine, ObjectMapper mapper) {
        this.classroomService = classroomService;
        this.engine = engine;
        this.mapper = mapper;
    }

    // ================================================================== xem trước

    public ImportPreview preview(CurrentActor actor, UUID spaceId, MultipartFile file) {
        requireTutor(actor);
        classroomService.loadManageableSpace(actor, spaceId);
        if (file == null || file.isEmpty()) throw ClassroomException.field("file", "Vui lòng chọn file Excel (.xlsx).");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) throw ClassroomException.field("file", "Chỉ nhận file Excel .xlsx (tải file mẫu để điền).");
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ClassroomException(HttpStatus.PAYLOAD_TOO_LARGE, "File Excel tối đa 2 MB.", Map.of("file", "File Excel tối đa 2 MB."));
        }
        try (InputStream in = file.getInputStream(); Workbook book = WorkbookFactory.create(in)) {
            return read(book);
        } catch (ClassroomException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw ClassroomException.field("file", "Không đọc được file Excel. Hãy dùng file mẫu và lưu dạng .xlsx.");
        }
    }

    ImportPreview read(Workbook book) {
        Sheet sheet = book.getSheet(SHEET);
        if (sheet == null) sheet = book.getNumberOfSheets() > 0 ? book.getSheetAt(0) : null;
        if (sheet == null) throw ClassroomException.field("file", "File không có sheet nào.");
        DataFormatter fmt = new DataFormatter(Locale.ROOT);
        List<ImportRow> rows = new ArrayList<>();
        for (int r = 1; r <= sheet.getLastRowNum(); r++) { // dòng 1 là tiêu đề
            Row row = sheet.getRow(r);
            String[] c = new String[COLS];
            for (int i = 0; i < COLS; i++) c[i] = row == null ? "" : fmt.formatCellValue(row.getCell(i)).strip();
            if (Arrays.stream(c).allMatch(String::isEmpty)) continue;
            if (rows.size() >= MAX_ROWS) {
                throw ClassroomException.field("file", "File có quá " + MAX_ROWS + " câu hỏi. Mỗi bài có tối đa " + MAX_ROWS + " câu.");
            }
            rows.add(parseRow(r + 1, c));
        }
        if (rows.isEmpty()) throw ClassroomException.field("file", "File chưa có câu hỏi nào (điền từ dòng 2 của sheet \"" + SHEET + "\").");
        int valid = (int) rows.stream().filter(ImportRow::ok).count();
        BigDecimal points = rows.stream().filter(ImportRow::ok).map(ImportRow::points).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ImportPreview(rows.size(), valid, rows.size() - valid, points, rows);
    }

    /** Đọc một dòng thành câu hỏi; mọi lỗi trả về trong ImportRow (không ném lỗi). */
    ImportRow parseRow(int rowNumber, String[] c) {
        String label = "Dòng " + rowNumber;
        ExerciseQuestionType type = typeOf(c[0]);
        if (type == null) {
            return fail(rowNumber, label + ": loại câu \"" + c[0] + "\" không hợp lệ. Chọn một trong: " + String.join(", ", LABELS.values()) + ".", null);
        }
        BigDecimal points;
        try {
            points = c[4].isEmpty() ? BigDecimal.ONE : new BigDecimal(c[4].replace(',', '.'));
        } catch (NumberFormatException e) {
            return fail(rowNumber, label + ": điểm \"" + c[4] + "\" không phải là số.", type);
        }
        // Cột F: câu nghe; cột G: số lần nghe tối đa
        String listenKey = key(c[5]);
        boolean listening;
        if (listenKey.isEmpty()) listening = false;
        else if (Set.of("co", "c", "x", "yes", "y", "true", "1").contains(listenKey)) listening = true;
        else if (Set.of("khong", "k", "no", "n", "false", "0").contains(listenKey)) listening = false;
        else return fail(rowNumber, label + ": cột \"Câu nghe\" ghi \"Có\" hoặc để trống.", type);
        Integer maxPlays = null;
        if (!c[6].isEmpty()) {
            if (!listening) return fail(rowNumber, label + ": có số lần nghe nhưng cột \"Câu nghe\" chưa ghi \"Có\".", type);
            try {
                maxPlays = new BigDecimal(c[6].replace(',', '.')).intValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                maxPlays = -1;
            }
            if (maxPlays < 1 || maxPlays > MAX_PLAYS) {
                return fail(rowNumber, label + ": số lần nghe phải từ 1 đến " + MAX_PLAYS + ", hoặc để trống nếu không giới hạn.", type);
            }
        }
        String prompt = c[1].isEmpty() ? null : c[1];
        ObjectNode data = mapper.createObjectNode();
        try {
            switch (type) {
                case SINGLE_CHOICE -> {
                    List<String> options = split(c[2], "|");
                    data.set("options", array(options));
                    Integer idx = optionIndex(options, c[3]);
                    if (idx == null) return fail(rowNumber, label + ": đáp án đúng \"" + c[3] + "\" không có trong lựa chọn (ghi đúng nội dung lựa chọn hoặc số thứ tự 1, 2, 3…).", type);
                    data.put("correct", idx);
                }
                case MULTI_CHOICE -> {
                    List<String> options = split(c[2], "|");
                    data.set("options", array(options));
                    ArrayNode correct = mapper.createArrayNode();
                    for (String one : splitAny(c[3])) {
                        Integer idx = optionIndex(options, one);
                        if (idx == null) return fail(rowNumber, label + ": đáp án đúng \"" + one + "\" không có trong lựa chọn.", type);
                        correct.add(idx);
                    }
                    data.set("correct", correct);
                }
                case TRUE_FALSE -> {
                    String v = key(c[3]);
                    if (Set.of("dung", "d", "true", "t", "1", "o").contains(v)) data.put("correct", true);
                    else if (Set.of("sai", "s", "false", "f", "0", "x").contains(v)) data.put("correct", false);
                    else return fail(rowNumber, label + ": đáp án của câu Đúng / Sai phải là \"Đúng\" hoặc \"Sai\".", type);
                }
                case FILL_BLANK -> {
                    data.put("text", c[1]);
                    ArrayNode answers = mapper.createArrayNode();
                    List<String> perBlank = split(c[3], "|");
                    int blanks = (int) ExerciseQuestionEngine.BLANK.matcher(c[1] == null ? "" : c[1]).results().count();
                    if (!perBlank.isEmpty() && perBlank.size() != blanks) {
                        return fail(rowNumber, label + ": câu có " + blanks + " chỗ trống nhưng cột D ghi " + perBlank.size()
                                + " đáp án. Ghi đủ đáp án cho từng chỗ trống (cách nhau |) hoặc để trống cột D.", type);
                    }
                    for (String blank : perBlank) answers.add(array(split(blank, ";")));
                    data.set("answers", answers);
                    prompt = null;
                }
                case WORD_BANK -> {
                    data.put("text", c[1]);
                    data.set("answers", array(split(c[3], "|")));
                    data.set("distractors", array(split(c[2], "|")));
                    prompt = null;
                }
                case MATCHING -> {
                    ArrayNode pairs = mapper.createArrayNode();
                    for (String pair : split(c[2], "|")) {
                        int eq = pair.indexOf('=');
                        if (eq < 0) return fail(rowNumber, label + ": cặp nối \"" + pair + "\" phải có dạng \"vế trái = vế phải\".", type);
                        ObjectNode p = mapper.createObjectNode();
                        p.put("left", pair.substring(0, eq).strip());
                        p.put("right", pair.substring(eq + 1).strip());
                        pairs.add(p);
                    }
                    data.set("pairs", pairs);
                }
                case ORDERING -> data.set("items", array(split(c[2], "|")));
                case WRITTEN -> {
                    // Tự luận không có đáp án: bài nộp chờ chấm (AI gợi ý, gia sư duyệt). Cột D được bỏ qua.
                }
            }
            // Cùng quy tắc khi giao bài (strict). Điền khuyết: đáp án không bắt buộc; tự luận: không có đáp án.
            String normalized = engine.validate(type, prompt, points, data, true, false, label);
            return new ImportRow(rowNumber, true, null, type, prompt, points, engine.read(normalized), listening, maxPlays);
        } catch (ClassroomException e) {
            return fail(rowNumber, e.getMessage(), type);
        }
    }

    // ================================================================== file mẫu

    public byte[] template(CurrentActor actor, UUID spaceId) {
        requireTutor(actor);
        classroomService.loadAccessibleSpace(actor, spaceId);
        try (XSSFWorkbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle head = book.createCellStyle();
            Font bold = book.createFont();
            bold.setBold(true);
            head.setFont(bold);
            head.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            head.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle wrap = book.createCellStyle();
            wrap.setWrapText(true);
            wrap.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet main = book.createSheet(SHEET);
            header(main, head);
            // Ô "Loại câu" chọn từ danh sách
            DataValidationHelper dv = main.getDataValidationHelper();
            DataValidation list = dv.createValidation(dv.createExplicitListConstraint(LABELS.values().toArray(new String[0])),
                    new CellRangeAddressList(1, MAX_ROWS, 0, 0));
            list.setShowErrorBox(true);
            main.addValidationData(list);
            // Cột F "Câu nghe": chọn "Có" hoặc để trống; cột G: số lần nghe 1–5
            DataValidation listen = dv.createValidation(dv.createExplicitListConstraint(new String[]{"Có"}),
                    new CellRangeAddressList(1, MAX_ROWS, 5, 5));
            listen.setShowErrorBox(true);
            main.addValidationData(listen);
            DataValidation plays = dv.createValidation(dv.createExplicitListConstraint(new String[]{"1", "2", "3", "4", "5"}),
                    new CellRangeAddressList(1, MAX_ROWS, 6, 6));
            plays.setShowErrorBox(true);
            main.addValidationData(plays);

            Sheet ex = book.createSheet("ViDu");
            header(ex, head);
            String[][] samples = {
                    {"Trắc nghiệm 1 đáp án", "「新聞」の読み方は？", "しんぶん | しんもん | にゅうぶん", "しんぶん", "2"},
                    {"Trắc nghiệm nhiều đáp án", "Chọn các từ là động từ", "食べる | 赤い | 行く | 本", "食べる | 行く", "2"},
                    {"Đúng / Sai", "「右」 nghĩa là bên phải.", "", "Đúng", "1"},
                    {"Điền khuyết", "毎朝 ___ を飲みます。", "", "牛乳;ぎゅうにゅう", "2"},
                    {"Điền từ có sẵn", "私は ___ で ___ を買いました。", "学校 | 本棚", "駅 | 切符", "2"},
                    {"Nối cột", "Nối Kanji với nghĩa", "犬 = con chó | 猫 = con mèo | 鳥 = con chim", "", "3"},
                    {"Sắp xếp thứ tự", "Sắp xếp thành câu đúng", "私は | 毎日 | 日本語を | 勉強します", "", "2"},
                    {"Tự luận", "Viết 3 câu có dùng Kanji 会 và 社.", "", "", "5"},
                    {"Điền khuyết", "昨日、友達 ___ 映画を見に行きました。", "", "", "1"},
                    {"Trắc nghiệm 1 đáp án", "Nghe đoạn hội thoại. Hai người hẹn gặp ở đâu?", "駅 | 学校 | 病院", "駅", "2", "Có", "2"},
                    {"Điền khuyết", "Nghe và điền từ: 毎朝 ___ を飲みます。", "", "牛乳;ぎゅうにゅう", "2", "Có", ""}
            };
            for (int i = 0; i < samples.length; i++) {
                Row row = ex.createRow(i + 1);
                for (int j = 0; j < samples[i].length; j++) {
                    Cell cell = row.createCell(j);
                    if ((j == 4 || j == 6) && samples[i][j].matches("\\d+")) cell.setCellValue(Double.parseDouble(samples[i][j]));
                    else cell.setCellValue(samples[i][j]);
                    cell.setCellStyle(wrap);
                }
            }

            Sheet guide = book.createSheet("HuongDan");
            String[] lines = {
                    "CÁCH ĐIỀN FILE NHẬP CÂU HỎI (KizunaTutor – bài làm trực tiếp)",
                    "• Điền câu hỏi vào sheet \"CauHoi\", mỗi dòng một câu, bắt đầu từ dòng 2. Xem sheet \"ViDu\" để làm theo.",
                    "• Cột A – Loại câu: chọn trong danh sách (8 dạng câu như trên trình soạn).",
                    "• Cột E – Điểm: số lớn hơn 0, tối đa 100; để trống = 1 điểm.",
                    "• Trắc nghiệm: cột C ghi các lựa chọn cách nhau bằng |; cột D ghi đúng nội dung lựa chọn đúng (hoặc số thứ tự 1, 2, 3…).",
                    "  Nhiều đáp án: cột D ghi các đáp án đúng cách nhau bằng |.",
                    "• Đúng / Sai: cột D ghi Đúng hoặc Sai.",
                    "• Điền khuyết: cột B ghi câu có chỗ trống ___ (3 dấu gạch dưới). Cột D – đáp án KHÔNG bắt buộc:",
                    "  ghi đáp án từng chỗ trống cách nhau bằng |, nhiều cách viết đúng thì cách nhau bằng ; (vd 牛乳;ぎゅうにゅう).",
                    "  Học viên điền khớp đáp án → máy chấm đúng ngay; ô không có đáp án hoặc điền khác → AI gợi ý điểm, gia sư duyệt.",
                    "• Điền từ có sẵn: cột B câu có ___; cột D từ đúng từng chỗ trống (cách nhau |);",
                    "  cột C các từ gây nhiễu (cách nhau |). Tổng số từ (từ đúng + từ nhiễu) phải từ 2 trở lên.",
                    "• Nối cột: cột C ghi các cặp \"vế trái = vế phải\" cách nhau bằng |.",
                    "• Sắp xếp thứ tự: cột C ghi các mục THEO ĐÚNG THỨ TỰ, cách nhau bằng | (hệ thống tự xáo trộn khi học viên làm).",
                    "• Tự luận: chỉ cần cột B (đề bài), không có đáp án. Bài nộp được AI gợi ý điểm, gia sư duyệt.",
                    "• Cột F – Câu nghe: ghi Có nếu câu cần file nghe (để trống = câu thường). Cột G – Số lần nghe: 1–5, để trống = không giới hạn.",
                    "  Excel không chứa được file âm thanh: sau khi nhập, câu nghe được đánh dấu \"Thiếu file nghe\" trên trình soạn,",
                    "  bấm vào câu đó → \"Thêm file nghe\". Bài còn câu nghe thiếu file thì lưu nháp được nhưng chưa giao được.",
                    "• Tối đa " + MAX_ROWS + " câu, file .xlsx tối đa 2 MB."
            };
            for (int i = 0; i < lines.length; i++) guide.createRow(i).createCell(0).setCellValue(lines[i]);
            guide.setColumnWidth(0, 140 * 256);

            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ClassroomException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được file mẫu. Vui lòng thử lại.");
        }
    }

    // ================================================================== hỗ trợ

    private static void header(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(0);
        int[] widths = {26, 46, 46, 30, 8, 14, 18};
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void requireTutor(CurrentActor actor) {
        if (!actor.isTutor()) throw ClassroomException.forbidden("Chỉ gia sư của lớp mới được nhập câu hỏi.");
    }

    /** "Trắc nghiệm 1 đáp án" / "SINGLE_CHOICE" / "trac nghiem 1 dap an" → dạng câu. */
    static ExerciseQuestionType typeOf(String raw) {
        String k = key(raw);
        if (k.isEmpty()) return null;
        for (Map.Entry<ExerciseQuestionType, String> e : LABELS.entrySet()) {
            if (key(e.getValue()).equals(k) || key(e.getKey().name()).equals(k)) return e.getKey();
        }
        return null;
    }

    /** Bỏ dấu tiếng Việt, chữ thường, chỉ giữ chữ và số. */
    static String key(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.strip().toLowerCase(Locale.ROOT).replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.replaceAll("[^a-z0-9]", "");
    }

    static List<String> split(String s, String sep) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split(java.util.regex.Pattern.quote(sep))).map(String::strip).filter(x -> !x.isEmpty()).toList();
    }

    /** Tách theo | hoặc ; (danh sách đáp án). */
    static List<String> splitAny(String s) {
        if (s == null || s.isBlank()) return List.of();
        return Arrays.stream(s.split("[|;]")).map(String::strip).filter(x -> !x.isEmpty()).toList();
    }

    /** Vị trí lựa chọn theo nội dung (không phân biệt hoa thường, NFKC) hoặc theo số thứ tự 1-based. */
    static Integer optionIndex(List<String> options, String answer) {
        String a = answer == null ? "" : answer.strip();
        if (a.isEmpty()) return null;
        String na = Normalizer.normalize(a, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        for (int i = 0; i < options.size(); i++) {
            if (Normalizer.normalize(options.get(i), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).equals(na)) return i;
        }
        if (a.matches("\\d{1,2}")) {
            int n = Integer.parseInt(a);
            if (n >= 1 && n <= options.size()) return n - 1;
        }
        return null;
    }

    private ArrayNode array(List<String> values) {
        ArrayNode arr = mapper.createArrayNode();
        values.forEach(arr::add);
        return arr;
    }

    private static ImportRow fail(int row, String message, ExerciseQuestionType type) {
        return new ImportRow(row, false, message, type, null, null, null, false, null);
    }
}
