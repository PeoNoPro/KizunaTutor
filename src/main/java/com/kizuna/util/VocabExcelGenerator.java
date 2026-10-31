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
 * Trình xuất dữ liệu Kho Từ Vựng OpenJLPT (7.811 từ vựng N5 - N1) ra tệp Excel (.xlsx) chuẩn 100% tiếng Việt.
 * - Hỗ trợ nhiều Sheet theo từng cấp độ JLPT N5, N4, N3, N2, N1 và Sheet Tổng hợp.
 * - Định dạng giao diện chuyên nghiệp, cố định hàng tiêu đề (Freeze Pane) và bật bộ lọc tự động (Auto-Filter).
 */
@Slf4j
public class VocabExcelGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public record VocabRow(
            String level,
            String word,
            String reading,
            String romaji,
            String hanViet,
            String posVn,
            String meaningVn,
            String exampleJp,
            String exampleFurigana,
            String exampleVn,
            Integer jmdictId
    ) {}

    public static void main(String[] args) {
        String outputPath = "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Tu_Vung_OpenJLPT_Tieng_Viet.xlsx";
        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            outputPath = args[0];
        }
        generateExcel(outputPath);
    }

    public static File generateExcel(String outputPath) {
        log.info("[VocabExcelGenerator] Bắt đầu xuất file Excel từ vựng OpenJLPT sang: {}", outputPath);
        File outputFile = new File(outputPath);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Nạp từ điển 6.611 chữ Hán để tra âm Hán Việt
            Map<String, String> hanVietMap = loadHanVietMap();

            // Nạp từ vựng 5 cấp độ
            String[] levels = {"n5", "n4", "n3", "n2", "n1"};
            String[] levelTitles = {"JLPT N5 (674 từ)", "JLPT N4 (630 từ)", "JLPT N3 (1.659 từ)", "JLPT N2 (1.778 từ)", "JLPT N1 (3.070 từ)"};

            List<VocabRow> allVocabs = new ArrayList<>();
            Map<String, List<VocabRow>> vocabsByLevel = new LinkedHashMap<>();

            for (String lvl : levels) {
                String upper = lvl.toUpperCase();
                List<VocabRow> levelList = loadVocabsForLevel(lvl, upper, hanVietMap);
                vocabsByLevel.put(upper, levelList);
                allVocabs.addAll(levelList);
            }

            // Tạo các kiểu dáng Cell Style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyleLeft = createDataStyle(workbook, HorizontalAlignment.LEFT);
            CellStyle dataStyleCenter = createDataStyle(workbook, HorizontalAlignment.CENTER);
            CellStyle dataStyleJapanese = createJapaneseFontStyle(workbook);

            // 1. Tạo Sheet 1: Tất Cả 7.811 Từ Vựng
            createVocabSheet(workbook, "Tất cả từ vựng (7.811 từ)", allVocabs, headerStyle, dataStyleLeft, dataStyleCenter, dataStyleJapanese);

            // 2. Tạo các Sheet theo từng cấp độ JLPT
            for (int i = 0; i < levels.length; i++) {
                String upper = levels[i].toUpperCase();
                createVocabSheet(workbook, levelTitles[i], vocabsByLevel.get(upper), headerStyle, dataStyleLeft, dataStyleCenter, dataStyleJapanese);
            }

            // Ghi file ra đĩa
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            log.info("[VocabExcelGenerator] Xuất file Excel thành công! Tổng cộng: {} từ vựng. Dung lượng: {} bytes",
                    allVocabs.size(), outputFile.length());
            return outputFile;

        } catch (Exception e) {
            log.error("[VocabExcelGenerator] Lỗi khi tạo file Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo file Excel từ vựng: " + e.getMessage(), e);
        }
    }

    private static void createVocabSheet(
            XSSFWorkbook workbook,
            String sheetName,
            List<VocabRow> rows,
            CellStyle headerStyle,
            CellStyle dataStyleLeft,
            CellStyle dataStyleCenter,
            CellStyle dataStyleJapanese) {

        Sheet sheet = workbook.createSheet(sheetName);
        sheet.createFreezePane(0, 1); // Cố định dòng tiêu đề

        String[] headers = {
                "STT",
                "Cấp độ JLPT",
                "Từ vựng (Kanji/Kana)",
                "Cách đọc (Hiragana)",
                "Phiên âm Romaji",
                "Âm Hán Việt",
                "Từ loại (Tiếng Việt)",
                "Nghĩa tiếng Việt",
                "Câu ví dụ (Tiếng Nhật)",
                "Câu ví dụ Furigana",
                "Dịch nghĩa câu ví dụ (Tiếng Việt)",
                "Mã JMdict ID"
        };

        // Tạo dòng Header
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);

        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        // Tạo các dòng dữ liệu
        int rowIdx = 1;
        for (VocabRow v : rows) {
            Row r = sheet.createRow(rowIdx++);
            r.setHeightInPoints(22);

            // 0. STT
            Cell c0 = r.createCell(0);
            c0.setCellValue(rowIdx - 1);
            c0.setCellStyle(dataStyleCenter);

            // 1. Cấp độ JLPT
            Cell c1 = r.createCell(1);
            c1.setCellValue(v.level());
            c1.setCellStyle(dataStyleCenter);

            // 2. Từ vựng (Kanji/Kana)
            Cell c2 = r.createCell(2);
            c2.setCellValue(v.word());
            c2.setCellStyle(dataStyleJapanese);

            // 3. Cách đọc (Hiragana)
            Cell c3 = r.createCell(3);
            c3.setCellValue(v.reading());
            c3.setCellStyle(dataStyleJapanese);

            // 4. Romaji
            Cell c4 = r.createCell(4);
            c4.setCellValue(v.romaji());
            c4.setCellStyle(dataStyleLeft);

            // 5. Âm Hán Việt
            Cell c5 = r.createCell(5);
            c5.setCellValue(v.hanViet() != null ? v.hanViet() : "");
            c5.setCellStyle(dataStyleCenter);

            // 6. Từ loại
            Cell c6 = r.createCell(6);
            c6.setCellValue(v.posVn());
            c6.setCellStyle(dataStyleLeft);

            // 7. Nghĩa tiếng Việt
            Cell c7 = r.createCell(7);
            c7.setCellValue(v.meaningVn());
            c7.setCellStyle(dataStyleLeft);

            // 8. Câu ví dụ tiếng Nhật
            Cell c8 = r.createCell(8);
            c8.setCellValue(v.exampleJp() != null ? v.exampleJp() : "");
            c8.setCellStyle(dataStyleJapanese);

            // 9. Câu ví dụ Furigana
            Cell c9 = r.createCell(9);
            c9.setCellValue(v.exampleFurigana() != null ? v.exampleFurigana() : "");
            c9.setCellStyle(dataStyleJapanese);

            // 10. Dịch nghĩa câu ví dụ (Tiếng Việt)
            Cell c10 = r.createCell(10);
            c10.setCellValue(v.exampleVn() != null ? v.exampleVn() : "");
            c10.setCellStyle(dataStyleLeft);

            // 11. JMdict ID
            Cell c11 = r.createCell(11);
            if (v.jmdictId() != null) {
                c11.setCellValue(v.jmdictId());
            } else {
                c11.setCellValue("");
            }
            c11.setCellStyle(dataStyleCenter);
        }

        // Bật tính năng lọc tự động AutoFilter
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, rows.size()), 0, headers.length - 1));

        // Thiết lập độ rộng cột tối ưu
        int[] colWidths = {2000, 3200, 5000, 5000, 4200, 4200, 7500, 10000, 12000, 12000, 13000, 3800};
        for (int c = 0; c < colWidths.length; c++) {
            sheet.setColumnWidth(c, colWidths[c]);
        }
    }

    private static List<VocabRow> loadVocabsForLevel(String lvl, String levelUpper, Map<String, String> hanVietMap) {
        List<VocabRow> list = new ArrayList<>();
        File file = new File("data/openjlpt/data/json/vocab/" + lvl + ".json");
        if (!file.exists()) return list;

        try (InputStream is = new FileInputStream(file)) {
            List<Map<String, Object>> rawList = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> map : rawList) {
                String word = (String) map.get("word");
                String reading = (String) map.get("reading");
                String romaji = (String) map.get("romaji");
                Integer jmdictId = (Integer) map.get("jmdict_id");

                @SuppressWarnings("unchecked")
                List<String> meanings = (List<String>) map.get("meanings");
                String meaningEn = (meanings != null && !meanings.isEmpty()) ? String.join("; ", meanings) : "";

                @SuppressWarnings("unchecked")
                List<String> pos = (List<String>) map.get("pos");
                String posVn = mapPosToVietnamese(pos);

                String hanViet = extractHanViet(word, hanVietMap);
                String meaningVn = VocabularyVietnameseDictionary.translateMeaning(word, reading, meaningEn, levelUpper, hanViet);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
                String exampleJp = null;
                String exampleFurigana = null;
                String exampleVn = null;

                if (exRaw != null && !exRaw.isEmpty()) {
                    Map<String, Object> ex = exRaw.get(0);
                    exampleJp = (String) ex.get("ja");
                    exampleFurigana = (String) ex.get("furigana");
                    String exEn = (String) ex.get("en");
                    exampleVn = VocabularyVietnameseDictionary.translateExample(exampleJp, exEn);
                }

                list.add(new VocabRow(
                        levelUpper,
                        word != null ? word : "",
                        reading != null ? reading : "",
                        romaji != null ? romaji : "",
                        hanViet,
                        posVn,
                        meaningVn,
                        exampleJp,
                        exampleFurigana,
                        exampleVn,
                        jmdictId
                ));
            }
        } catch (Exception e) {
            log.error("[VocabExcelGenerator] Lỗi đọc file {}.json: {}", lvl, e.getMessage());
        }
        return list;
    }

    private static String mapPosToVietnamese(List<String> posList) {
        if (posList == null || posList.isEmpty()) return "Từ vựng";
        List<String> vnList = new ArrayList<>();
        for (String pos : posList) {
            if (pos == null) continue;
            String p = pos.toLowerCase();
            if (p.startsWith("v5")) vnList.add("Động từ nhóm 1 (Ngũ đoạn)");
            else if ("v1".equals(p)) vnList.add("Động từ nhóm 2 (Nhất đoạn)");
            else if ("vs".equals(p) || "vk".equals(p) || p.startsWith("vs-")) vnList.add("Động từ nhóm 3 (Bất quy tắc)");
            else if ("adj-i".equals(p)) vnList.add("Tính từ đuôi -i");
            else if ("adj-na".equals(p)) vnList.add("Tính từ đuôi -na");
            else if ("adj-no".equals(p)) vnList.add("Tính từ bổ nghĩa (no)");
            else if ("n".equals(p)) vnList.add("Danh từ");
            else if ("adv".equals(p) || p.startsWith("adv")) vnList.add("Phó từ");
            else if ("prt".equals(p)) vnList.add("Trợ từ");
            else if ("exp".equals(p)) vnList.add("Cụm từ");
            else if ("int".equals(p)) vnList.add("Thán từ");
            else if ("num".equals(p)) vnList.add("Số từ");
            else if ("counter".equals(p)) vnList.add("Từ đếm");
            else if ("vi".equals(p)) vnList.add("Nội động từ");
            else if ("vt".equals(p)) vnList.add("Ngoại động từ");
            else if ("pn".equals(p)) vnList.add("Đại từ");
        }
        return vnList.isEmpty() ? "Từ vựng" : String.join(", ", new LinkedHashSet<>(vnList));
    }

    private static Map<String, String> loadHanVietMap() {
        Map<String, String> map = new HashMap<>(7000);
        File file = new File("src/main/resources/data/kanji-dictionary-data.json");
        if (!file.exists()) return map;

        try (InputStream is = new FileInputStream(file)) {
            List<Map<String, Object>> list = objectMapper.readValue(is, new TypeReference<>() {});
            for (Map<String, Object> k : list) {
                String ch = (String) k.get("character");
                String hv = (String) k.get("han_viet");
                if (ch != null && hv != null) {
                    map.put(ch.trim(), hv.trim().toUpperCase());
                }
            }
        } catch (Exception e) {
            log.warn("[VocabExcelGenerator] Không thể nạp kanji-dictionary-data.json: {}", e.getMessage());
        }
        return map;
    }

    private static String extractHanViet(String word, Map<String, String> map) {
        if (word == null || map.isEmpty()) return null;
        List<String> list = new ArrayList<>();
        for (char c : word.toCharArray()) {
            String ch = String.valueOf(c);
            if (map.containsKey(ch)) {
                list.add(map.get(ch));
            }
        }
        return list.isEmpty() ? null : String.join(" ", list);
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

    private static CellStyle createDataStyle(XSSFWorkbook wb, HorizontalAlignment align) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName("Segoe UI");
        font.setFontHeightInPoints((short) 9.5);
        style.setFont(font);

        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
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
}
