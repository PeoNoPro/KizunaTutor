package com.kizuna.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Trình xuất dữ liệu Cẩm Nang Ngữ Pháp JLPT (526 cấu trúc N5 - N1 từ OpenJLPT) ra tệp Excel (.xlsx) chuẩn 100% tiếng Việt.
 * - Hỗ trợ nhiều Sheet theo từng cấp độ JLPT N5, N4, N3, N2, N1 và Sheet Tổng hợp 526 mẫu.
 * - Bao gồm toàn bộ câu ví dụ thực tế kèm bản dịch tiếng Việt 100% (không để sót tiếng Anh).
 * - Định dạng giao diện Retro-Brutalist chuyên nghiệp: cố định hàng tiêu đề (Freeze Pane), bật bộ lọc (Auto-Filter).
 */
@Slf4j
public class GrammarExcelGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public record GrammarRow(
            String level,
            String pattern,
            String romaji,
            String meaningVn,
            String formationVn,
            String notesVn,
            String exampleJp,
            String exampleFurigana,
            String exampleVn,
            String extraExamples,
            String tagsVn,
            String id
    ) {}

    public static void main(String[] args) {
        String outputPath = "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx";
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            outputPath = args[0];
        }
        generateExcel(outputPath);
    }

    public static File generateExcel(String outputPath) {
        log.info("[GrammarExcelGenerator] Bắt đầu xuất file Excel ngữ pháp OpenJLPT sang: {}", outputPath);
        File outputFile = new File(outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            String[] levels = {"n5", "n4", "n3", "n2", "n1"};
            String[] levelTitles = {
                    "JLPT N5 (81 mẫu)",
                    "JLPT N4 (98 mẫu)",
                    "JLPT N3 (101 mẫu)",
                    "JLPT N2 (123 mẫu)",
                    "JLPT N1 (123 mẫu)"
            };

            List<GrammarRow> allRules = new ArrayList<>();
            Map<String, List<GrammarRow>> rulesByLevel = new LinkedHashMap<>();

            for (String lvl : levels) {
                String upper = lvl.toUpperCase();
                List<GrammarRow> levelList = loadRulesForLevel(lvl, upper);
                rulesByLevel.put(upper, levelList);
                allRules.addAll(levelList);
            }

            // Tạo các kiểu Cell Style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyleLeft = createDataStyle(workbook, HorizontalAlignment.LEFT, false);
            CellStyle dataStyleCenter = createDataStyle(workbook, HorizontalAlignment.CENTER, false);
            CellStyle dataStyleWrap = createDataStyle(workbook, HorizontalAlignment.LEFT, true);
            CellStyle dataStyleJapanese = createJapaneseFontStyle(workbook);

            // 1. Tạo Sheet 1: Tất Cả 526 Mẫu Ngữ Pháp
            createGrammarSheet(
                    workbook,
                    "Tất cả ngữ pháp (526 mẫu)",
                    allRules,
                    headerStyle,
                    dataStyleLeft,
                    dataStyleCenter,
                    dataStyleWrap,
                    dataStyleJapanese
            );

            // 2. Tạo các Sheet theo từng cấp độ JLPT
            for (int i = 0; i < levels.length; i++) {
                String upper = levels[i].toUpperCase();
                createGrammarSheet(
                        workbook,
                        levelTitles[i],
                        rulesByLevel.get(upper),
                        headerStyle,
                        dataStyleLeft,
                        dataStyleCenter,
                        dataStyleWrap,
                        dataStyleJapanese
                );
            }

            // Ghi file ra đĩa
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            log.info("[GrammarExcelGenerator] Xuất file Excel thành công! Tổng cộng: {} mẫu câu. Dung lượng: {} bytes",
                    allRules.size(), outputFile.length());
            return outputFile;

        } catch (Exception e) {
            log.error("[GrammarExcelGenerator] Lỗi khi tạo file Excel ngữ pháp: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo file Excel ngữ pháp: " + e.getMessage(), e);
        }
    }

    private static void createGrammarSheet(
            XSSFWorkbook workbook,
            String sheetName,
            List<GrammarRow> rows,
            CellStyle headerStyle,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter,
            CellStyle dataStyleWrap,
            CellStyle dataStyleJapanese) {

        Sheet sheet = workbook.createSheet(sheetName);
        sheet.createFreezePane(0, 1); // Cố định hàng tiêu đề

        String[] headers = {
                "STT",
                "Cấp độ JLPT",
                "Mẫu cấu trúc ngữ pháp",
                "Phiên âm Romaji",
                "Ý nghĩa tiếng Việt",
                "Công thức kết hợp (Formation)",
                "Giải thích & Ghi chú (Notes)",
                "Câu ví dụ tiêu biểu (Tiếng Nhật)",
                "Câu ví dụ Furigana",
                "Dịch nghĩa câu ví dụ (Tiếng Việt)",
                "Các câu ví dụ mở rộng (Kèm dịch TV)",
                "Thẻ phân loại (Tags)",
                "Mã định danh (ID)"
        };

        // Header Row
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);

        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowIdx = 1;
        for (GrammarRow r : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(24);

            // 0. STT
            Cell c0 = row.createCell(0);
            c0.setCellValue(rowIdx - 1);
            c0.setCellStyle(dataStyleCenter);

            // 1. Cấp độ JLPT
            Cell c1 = row.createCell(1);
            c1.setCellValue(r.level());
            c1.setCellStyle(dataStyleCenter);

            // 2. Mẫu câu (Kanji/Kana)
            Cell c2 = row.createCell(2);
            c2.setCellValue(r.pattern());
            c2.setCellStyle(dataStyleJapanese);

            // 3. Romaji
            Cell c3 = row.createCell(3);
            c3.setCellValue(r.romaji());
            c3.setCellStyle(dataStyleLeft);

            // 4. Ý nghĩa tiếng Việt
            Cell c4 = row.createCell(4);
            c4.setCellValue(r.meaningVn());
            c4.setCellStyle(dataStyleLeft);

            // 5. Công thức kết hợp
            Cell c5 = row.createCell(5);
            c5.setCellValue(r.formationVn());
            c5.setCellStyle(dataStyleLeft);

            // 6. Ghi chú
            Cell c6 = row.createCell(6);
            c6.setCellValue(r.notesVn());
            c6.setCellStyle(dataStyleWrap);

            // 7. Câu ví dụ tiêu biểu (Tiếng Nhật)
            Cell c7 = row.createCell(7);
            c7.setCellValue(r.exampleJp() != null ? r.exampleJp() : "");
            c7.setCellStyle(dataStyleJapanese);

            // 8. Ví dụ Furigana
            Cell c8 = row.createCell(8);
            c8.setCellValue(r.exampleFurigana() != null ? r.exampleFurigana() : "");
            c8.setCellStyle(dataStyleJapanese);

            // 9. Dịch câu ví dụ (Tiếng Việt)
            Cell c9 = row.createCell(9);
            c9.setCellValue(r.exampleVn() != null ? r.exampleVn() : "");
            c9.setCellStyle(dataStyleLeft);

            // 10. Các ví dụ mở rộng
            Cell c10 = row.createCell(10);
            c10.setCellValue(r.extraExamples() != null ? r.extraExamples() : "");
            c10.setCellStyle(dataStyleWrap);

            // 11. Tags tiếng Việt
            Cell c11 = row.createCell(11);
            c11.setCellValue(r.tagsVn() != null ? r.tagsVn() : "");
            c11.setCellStyle(dataStyleCenter);

            // 12. ID
            Cell c12 = row.createCell(12);
            c12.setCellValue(r.id() != null ? r.id() : "");
            c12.setCellStyle(dataStyleCenter);
        }

        // Bật Auto-Filter
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, rows.size()), 0, headers.length - 1));

        // Thiết lập độ rộng cột tối ưu
        int[] colWidths = {
                2000,  // 0. STT
                3200,  // 1. Cấp độ
                5500,  // 2. Mẫu câu
                4500,  // 3. Romaji
                10000, // 4. Ý nghĩa tiếng Việt
                8500,  // 5. Công thức kết hợp
                12000, // 6. Ghi chú
                11000, // 7. Ví dụ JP
                11000, // 8. Ví dụ Furigana
                12000, // 9. Dịch ví dụ VN
                13000, // 10. Ví dụ mở rộng
                5500,  // 11. Tags
                4000   // 12. ID
        };
        for (int c = 0; c < colWidths.length; c++) {
            sheet.setColumnWidth(c, colWidths[c]);
        }
    }

    private static List<GrammarRow> loadRulesForLevel(String lvl, String levelUpper) {
        List<GrammarRow> list = new ArrayList<>();
        File file = new File("data/openjlpt/data/json/grammar/" + lvl + ".json");
        if (!file.exists()) {
            file = new File("C:/Users/Vuh26/Desktop/KizunaTutor/data/openjlpt/data/json/grammar/" + lvl + ".json");
        }
        if (!file.exists()) return list;

        try (InputStream is = new FileInputStream(file)) {
            List<Map<String, Object>> rawList = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : rawList) {
                String id = (String) map.get("id");
                String rawPattern = (String) map.get("pattern");
                String rawRomaji = (String) map.get("romaji");
                String pattern = cleanGrammarString(rawPattern);
                String romaji = cleanGrammarString(rawRomaji);
                String rawMeaning = (String) map.get("meaning");
                String rawFormation = (String) map.get("formation");
                String rawNotes = (String) map.get("notes");

                @SuppressWarnings("unchecked")
                List<String> tagsRaw = (List<String>) map.get("tags");
                List<String> tagsVnList = GrammarVietnameseDictionary.translateTags(tagsRaw);
                String tagsVn = String.join(", ", tagsVnList);

                String meaningVn = GrammarVietnameseDictionary.translateMeaning(id, pattern, rawMeaning, levelUpper);
                String formationVn = GrammarVietnameseDictionary.translateFormation(rawFormation);
                String notesVn = GrammarVietnameseDictionary.translateNotes(id, pattern, rawNotes, levelUpper);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
                String exampleJp = null;
                String exampleFurigana = null;
                String exampleVn = null;
                List<String> extraList = new ArrayList<>();

                if (exRaw != null && !exRaw.isEmpty()) {
                    for (int i = 0; i < exRaw.size(); i++) {
                        Map<String, Object> ex = exRaw.get(i);
                        String ja = (String) ex.get("ja");
                        String furigana = (String) ex.get("furigana");
                        String en = (String) ex.get("en");
                        String vn = GrammarVietnameseDictionary.translateExample(ja, en);

                        if (i == 0) {
                            exampleJp = ja;
                            exampleFurigana = furigana;
                            exampleVn = vn;
                        } else {
                            extraList.add(String.format("%d. %s\n   (%s)", i + 1, ja, vn));
                        }
                    }
                }

                String extraExamples = extraList.isEmpty() ? "" : String.join("\n", extraList);

                list.add(new GrammarRow(
                        levelUpper,
                        pattern != null ? pattern : "",
                        romaji != null ? romaji : "",
                        meaningVn != null ? meaningVn : "",
                        formationVn != null ? formationVn : "",
                        notesVn != null ? notesVn : "",
                        exampleJp,
                        exampleFurigana,
                        exampleVn,
                        extraExamples,
                        tagsVn,
                        id != null ? id : ""
                ));
            }
        } catch (Exception e) {
            log.error("[GrammarExcelGenerator] Lỗi đọc file {}.json: {}", lvl, e.getMessage());
        }
        return list;
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        // Màu nền Navy đậm #0F172A
        byte[] rgb = new byte[]{(byte) 15, (byte) 23, (byte) 42};
        style.setFillForegroundColor(new XSSFColor(rgb, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFFont font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN, IndexedColors.GREY_40_PERCENT.getIndex());
        return style;
    }

    private static CellStyle createDataStyle(XSSFWorkbook wb, HorizontalAlignment align, boolean wrap) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 9.5);
        style.setFont(font);

        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(wrap);
        setBorders(style, BorderStyle.HAIR, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static CellStyle createJapaneseFontStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Yu Gothic UI");
        font.setFontHeightInPoints((short) 10.5);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.HAIR, IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static void setBorders(CellStyle style, BorderStyle border, short color) {
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }

    private static String cleanGrammarString(String str) {
        if (str == null) return "";
        return str.replaceAll("（[a-zA-Z\\s/_\\-]+）|\\([a-zA-Z\\s/_\\-]+\\)", "").trim();
    }
}
