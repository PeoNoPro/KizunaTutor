package com.kizuna.features.learning_content.service;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.learning_content.dto.ImportIssueResponse;
import com.kizuna.features.learning_content.service.ContentType.Field;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Đọc tệp Excel học liệu thành các dòng đã kiểm tra hợp lệ.
 * - Đọc mọi sheet có dòng tiêu đề chứa đủ cột bắt buộc (tìm trong 5 dòng đầu); sheet khác bỏ qua.
 * - Dòng lỗi được ghi vào danh sách issues và không đưa vào kết quả; dòng trống bỏ qua.
 * Không chạm cơ sở dữ liệu.
 */
public final class ContentWorkbookReader {

    public static final int MAX_DATA_ROWS = 50_000;
    private static final int HEADER_SCAN_ROWS = 5;
    private static final Pattern JLPT = Pattern.compile("^N?([1-5])$");
    private static final Pattern BRACKETS = Pattern.compile("\\([^)]*\\)|（[^）]*）");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    public record ContentRow(String sheet, int row, Map<String, String> values) {
    }

    public record ParsedWorkbook(List<ContentRow> rows, List<ImportIssueResponse> issues, List<String> sheets,
                                 int totalRows, int invalidRows) {
    }

    private ContentWorkbookReader() {
    }

    public static ParsedWorkbook read(ContentType type, InputStream in) {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            return read(type, workbook);
        } catch (BusinessException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(400, "Không đọc được tệp. Vui lòng chọn tệp Excel (.xlsx) còn nguyên vẹn.");
        }
    }

    static ParsedWorkbook read(ContentType type, Workbook workbook) {
        DataFormatter formatter = new DataFormatter();
        List<ContentRow> rows = new ArrayList<>();
        List<ImportIssueResponse> issues = new ArrayList<>();
        List<String> sheets = new ArrayList<>();
        int total = 0;
        int invalid = 0;

        for (Sheet sheet : workbook) {
            HeaderMatch header = findHeader(type, sheet, formatter);
            if (header == null) continue;
            sheets.add(sheet.getSheetName());

            for (int r = header.rowIndex() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Map<String, String> raw = new HashMap<>();
                boolean empty = true;
                for (Map.Entry<String, Integer> col : header.columns().entrySet()) {
                    String value = clean(formatter.formatCellValue(row.getCell(col.getValue())));
                    if (value != null) {
                        raw.put(col.getKey(), value);
                        empty = false;
                    }
                }
                if (empty) continue;
                if (++total > MAX_DATA_ROWS) {
                    throw new BusinessException(400, "Tệp có quá nhiều dòng. Mỗi lần nhập tối đa "
                            + String.format(Locale.ROOT, "%,d", MAX_DATA_ROWS).replace(',', '.') + " dòng dữ liệu.");
                }
                String error = validate(type, raw);
                if (error != null) {
                    invalid++;
                    issues.add(ImportIssueResponse.error(sheet.getSheetName(), r + 1, error));
                } else {
                    rows.add(new ContentRow(sheet.getSheetName(), r + 1, raw));
                }
            }
        }

        if (sheets.isEmpty()) {
            String required = String.join(", ", type.requiredFields().stream().map(Field::header).toList());
            throw new BusinessException(400, "Không tìm thấy dòng tiêu đề phù hợp. Tệp " + type.label()
                    + " cần có các cột: " + required + ". Hãy kiểm tra bạn đã chọn đúng loại học liệu cho tệp này.");
        }
        return new ParsedWorkbook(rows, issues, sheets, total, invalid);
    }

    /** Chuẩn hóa tên tiêu đề: bỏ phần trong ngoặc, bỏ dấu tiếng Việt, chữ thường, chỉ giữ chữ và số. */
    static String normalizeHeader(String header) {
        if (header == null) return "";
        String text = BRACKETS.matcher(header).replaceAll(" ");
        text = text.replace('đ', 'd').replace('Đ', 'D');
        text = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        text = NON_ALNUM.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return text.trim();
    }

    private record HeaderMatch(int rowIndex, Map<String, Integer> columns) {
    }

    private static HeaderMatch findHeader(ContentType type, Sheet sheet, DataFormatter formatter) {
        int last = Math.min(sheet.getLastRowNum(), HEADER_SCAN_ROWS - 1);
        for (int r = sheet.getFirstRowNum(); r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            Map<String, Integer> columns = new LinkedHashMap<>();
            for (Cell cell : row) {
                String normalized = normalizeHeader(formatter.formatCellValue(cell));
                if (normalized.isEmpty()) continue;
                for (Field field : type.fields()) {
                    if (field.aliases().contains(normalized)) {
                        columns.putIfAbsent(field.name(), cell.getColumnIndex());
                        break;
                    }
                }
            }
            boolean complete = type.requiredFields().stream().allMatch(f -> columns.containsKey(f.name()));
            if (complete) return new HeaderMatch(r, columns);
        }
        return null;
    }

    /** Kiểm tra và chuẩn hóa một dòng (sửa trực tiếp map). Trả về thông báo lỗi hoặc null. */
    static String validate(ContentType type, Map<String, String> values) {
        for (Field field : type.requiredFields()) {
            if (!values.containsKey(field.name())) {
                return "Thiếu " + field.header() + ".";
            }
        }

        String jlpt = values.get("jlpt");
        if (jlpt != null) {
            var matcher = JLPT.matcher(jlpt.toUpperCase(Locale.ROOT).replace(" ", ""));
            if (!matcher.matches()) return "Cấp độ JLPT \"" + jlpt + "\" không hợp lệ (chỉ nhận N1 – N5).";
            values.put("jlpt", "N" + matcher.group(1));
        }

        if (type == ContentType.KANJI) {
            String ch = values.get("character");
            if (ch.codePointCount(0, ch.length()) != 1 || !Character.isIdeographic(ch.codePointAt(0))) {
                return "Cột Kanji phải là đúng một chữ Hán (đang là \"" + ch + "\").";
            }
            String strokes = values.get("strokes");
            if (strokes != null) {
                try {
                    int n = Integer.parseInt(strokes.replace(".0", ""));
                    if (n < 1 || n > 84) return "Số nét phải từ 1 đến 84.";
                    values.put("strokes", String.valueOf(n));
                } catch (NumberFormatException e) {
                    return "Số nét \"" + strokes + "\" không phải là số.";
                }
            }
        }

        if (type == ContentType.VOCABULARY) {
            // Từ viết bằng kana thường để trống cách đọc: dùng chính từ đó làm cách đọc
            values.putIfAbsent("reading", values.get("word"));
        }

        for (Field field : type.fields()) {
            String value = values.get(field.name());
            if (value == null || field.maxLength() == 0) continue;
            if (value.codePointCount(0, value.length()) > field.maxLength()) {
                return field.header() + " dài quá " + field.maxLength() + " ký tự.";
            }
        }
        return null;
    }

    /** Bỏ khoảng trắng đầu/cuối và ký tự điều khiển (giữ xuống dòng); chuỗi rỗng → null. */
    static String clean(String value) {
        if (value == null) return null;
        String text = value.replace("\r\n", "\n").replaceAll("[\\p{Cc}&&[^\\n\\t]]", "").strip();
        return text.isEmpty() ? null : text;
    }
}
