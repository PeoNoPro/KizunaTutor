package com.kizuna.features.learning_content;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.learning_content.service.ContentImportPlanner;
import com.kizuna.features.learning_content.service.ContentImportPlanner.Plan;
import com.kizuna.features.learning_content.service.ContentType;
import com.kizuna.features.learning_content.service.ContentWorkbookReader;
import com.kizuna.features.learning_content.service.ContentWorkbookReader.ParsedWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContentImportTest {

    private static final String[] KANJI_HEADER = {"Kanji", "Hán Việt", "Onyomi", "Kunyomi", "Nghĩa tiếng Việt", "JLPT"};
    private static final String[] VOCAB_HEADER = {"STT", "Cấp độ JLPT", "Từ vựng (Kanji/Kana)", "Cách đọc (Hiragana)",
            "Phiên âm Romaji", "Âm Hán Việt", "Từ loại (Tiếng Việt)", "Nghĩa tiếng Việt", "Câu ví dụ (Tiếng Nhật)",
            "Câu ví dụ Furigana", "Dịch nghĩa câu ví dụ (Tiếng Việt)", "Mã JMdict ID"};
    private static final String[] GRAMMAR_HEADER = {"STT", "Cấp độ JLPT", "Mẫu cấu trúc ngữ pháp", "Phiên âm Romaji",
            "Ý nghĩa tiếng Việt", "Công thức kết hợp (Formation)", "Giải thích & Ghi chú (Notes)",
            "Câu ví dụ tiêu biểu (Tiếng Nhật)", "Câu ví dụ Furigana", "Dịch nghĩa câu ví dụ (Tiếng Việt)",
            "Các câu ví dụ mở rộng (Kèm dịch TV)", "Thẻ phân loại (Tags)", "Mã định danh (ID)"};

    /** sheets: tên sheet → các dòng (dòng đầu là tiêu đề) */
    private static ParsedWorkbook read(ContentType type, Map<String, String[][]> sheets) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            sheets.forEach((name, rows) -> {
                Sheet sheet = wb.createSheet(name);
                for (int r = 0; r < rows.length; r++) {
                    Row row = sheet.createRow(r);
                    for (int c = 0; c < rows[r].length; c++) {
                        if (rows[r][c] != null) row.createCell(c).setCellValue(rows[r][c]);
                    }
                }
            });
            wb.write(out);
            return ContentWorkbookReader.read(type, new ByteArrayInputStream(out.toByteArray()));
        }
    }

    private static Map<String, String[][]> one(String[]... rows) {
        Map<String, String[][]> m = new java.util.LinkedHashMap<>();
        m.put("Sheet1", rows);
        return m;
    }

    @Nested
    class Reader {

        @Test
        void docTepKanjiTheoTenCot_khongPhuThuocViTri() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(
                    new String[]{"JLPT", "Nghĩa tiếng Việt", "Kanji"},
                    new String[]{"n2", "Ghét", "憎"}));
            assertThat(parsed.rows()).hasSize(1);
            assertThat(parsed.rows().get(0).values())
                    .containsEntry("character", "憎").containsEntry("meaning", "Ghét").containsEntry("jlpt", "N2");
        }

        @Test
        void tepSaiLoai_baoThieuCot() {
            assertThatThrownBy(() -> read(ContentType.KANJI, one(VOCAB_HEADER, new String[]{"1", "N5", "会う"})))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Kanji, Nghĩa tiếng Việt");
        }

        @Test
        void dongLoi_biGhiLaiVaBoQua_dongTrongBoQuaImLang() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(KANJI_HEADER,
                    new String[]{"憎", "TĂNG", null, null, "Ghét", "N2"},
                    new String[]{"AB", null, null, null, "x", null},
                    new String[]{null, null, null, null, null, null},
                    new String[]{"馮", null, null, null, "Cưỡi ngựa", "N6"},
                    new String[]{"愛", null, null, null, null, "N3"}));
            assertThat(parsed.rows()).hasSize(1);
            assertThat(parsed.totalRows()).isEqualTo(4);
            assertThat(parsed.invalidRows()).isEqualTo(3);
            assertThat(parsed.issues()).extracting(i -> i.row()).containsExactly(3, 5, 6);
            assertThat(parsed.issues().get(1).message()).contains("N6");
            assertThat(parsed.issues().get(2).message()).contains("Thiếu Nghĩa tiếng Việt");
        }

        @Test
        void tuVung_cachDocTrongThiDungChinhTu_vaQuaDaiBiLoi() throws Exception {
            String[] longPos = new String[VOCAB_HEADER.length];
            longPos[2] = "あ";
            longPos[6] = "x".repeat(101);
            longPos[7] = "Ah";
            String[] kana = new String[VOCAB_HEADER.length];
            kana[1] = "N4";
            kana[2] = "ああ";
            kana[7] = "như thế";
            ParsedWorkbook parsed = read(ContentType.VOCABULARY, one(VOCAB_HEADER, kana, longPos));
            assertThat(parsed.rows()).hasSize(1);
            assertThat(parsed.rows().get(0).values()).containsEntry("reading", "ああ");
            assertThat(parsed.issues().get(0).message()).contains("Từ loại dài quá 100");
        }

        @Test
        void docMoiSheetCoTieuDe_boQuaSheetKhac() throws Exception {
            Map<String, String[][]> sheets = new java.util.LinkedHashMap<>();
            sheets.put("Hướng dẫn", new String[][]{{"Đây là tệp ngữ pháp"}});
            sheets.put("Tất cả", new String[][]{GRAMMAR_HEADER, grammar("wa", "〜は", "Thì; là")});
            sheets.put("N5", new String[][]{GRAMMAR_HEADER, grammar("wa", "〜は", "Thì; là")});
            ParsedWorkbook parsed = read(ContentType.GRAMMAR, sheets);
            assertThat(parsed.sheets()).containsExactly("Tất cả", "N5");
            assertThat(parsed.rows()).hasSize(2);
        }
    }

    private static String[] grammar(String code, String pattern, String meaning) {
        String[] row = new String[GRAMMAR_HEADER.length];
        row[1] = "N5";
        row[2] = pattern;
        row[4] = meaning;
        row[12] = code;
        return row;
    }

    @Nested
    class Planner {

        private Map<String, Map<String, String>> existing(String key, Map<String, String> record) {
            Map<String, Map<String, String>> m = new HashMap<>();
            m.put(key, record);
            return m;
        }

        @Test
        void mucDaCo_luonGiuNguyen_chiThemMucMoi() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(
                    new String[]{"Kanji", "Nghĩa tiếng Việt", "Số nét"},
                    new String[]{"憎", "Nội dung khác bản cũ", "14"},
                    new String[]{"馮", "Cưỡi ngựa", "12"}));
            Plan plan = ContentImportPlanner.plan(ContentType.KANJI, parsed.rows(),
                    existing("憎", Map.of("character", "憎")));
            assertThat(plan.skippedExisting()).isEqualTo(1);
            assertThat(plan.inserts()).extracting(v -> v.get("character")).containsExactly("馮");
        }

        @Test
        void trungTrongTep_giuDongDau_canhBaoKhiKhacNoiDung() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(
                    new String[]{"Kanji", "Nghĩa tiếng Việt", "Số nét"},
                    new String[]{"愛", "Yêu", "13"},
                    new String[]{"愛", "Yêu", "13"},
                    new String[]{"愛", "Tình yêu", "13"}));
            Plan plan = ContentImportPlanner.plan(ContentType.KANJI, parsed.rows(), Map.of());
            assertThat(plan.duplicatesInFile()).isEqualTo(2);
            assertThat(plan.issues()).hasSize(1);
            assertThat(plan.issues().get(0).message()).contains("dòng 2");
            assertThat(plan.inserts()).hasSize(1);
            assertThat(plan.inserts().get(0)).containsEntry("meaning", "Yêu");
        }

        @Test
        void kanjiMoiKhongCoSoNet_laLoi() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(
                    new String[]{"Kanji", "Nghĩa tiếng Việt", "Số nét"},
                    new String[]{"愛", "Yêu", null},
                    new String[]{"馮", "Cưỡi ngựa", "12"}));
            Plan plan = ContentImportPlanner.plan(ContentType.KANJI, parsed.rows(), Map.of());
            assertThat(plan.inserts()).extracting(v -> v.get("character")).containsExactly("馮");
            assertThat(plan.invalidRows()).isEqualTo(1);
            assertThat(plan.issues().get(0).message()).contains("cần cột Số nét");
        }

        @Test
        void kanjiDaCoKhongCanSoNet() throws Exception {
            ParsedWorkbook parsed = read(ContentType.KANJI, one(KANJI_HEADER,
                    new String[]{"憎", "TĂNG", null, null, "Ghét", "N2"}));
            Plan plan = ContentImportPlanner.plan(ContentType.KANJI, parsed.rows(),
                    existing("憎", Map.of("character", "憎")));
            assertThat(plan.invalidRows()).isZero();
            assertThat(plan.skippedExisting()).isEqualTo(1);
        }

        @Test
        void nguPhap_maDaCoKhongPhanBietHoaThuong_mauThuocMaKhacLaLoi() throws Exception {
            ParsedWorkbook parsed = read(ContentType.GRAMMAR, one(GRAMMAR_HEADER,
                    grammar("wa-2", "〜は", "Thì"),
                    grammar("WA", "〜は", "Thì; là"),
                    grammar("ga", "〜が", "Chủ ngữ"),
                    grammar("ga-2", "〜が", "Trùng mẫu trong tệp")));
            Plan plan = ContentImportPlanner.plan(ContentType.GRAMMAR, parsed.rows(),
                    existing("wa", Map.of("code", "wa", "pattern", "〜は")));
            assertThat(plan.skippedExisting()).isEqualTo(1);
            assertThat(plan.invalidRows()).isEqualTo(2);
            assertThat(plan.issues()).extracting(i -> i.message())
                    .anyMatch(m -> m.contains("đã có với mã wa"))
                    .anyMatch(m -> m.contains("đã có với mã ga"));
            assertThat(plan.inserts()).extracting(v -> v.get("code")).containsExactly("ga");
        }

        @Test
        void tuVung_khoaLaTuVaCachDoc() throws Exception {
            String[] a = new String[VOCAB_HEADER.length];
            a[2] = "相";
            a[3] = "あい";
            a[7] = "Cùng nhau";
            String[] b = a.clone();
            b[3] = "そう";
            ParsedWorkbook parsed = read(ContentType.VOCABULARY, one(VOCAB_HEADER, a, b));
            Plan plan = ContentImportPlanner.plan(ContentType.VOCABULARY, parsed.rows(),
                    existing("相\u0000あい", Map.of("word", "相", "reading", "あい")));
            assertThat(plan.skippedExisting()).isEqualTo(1);
            assertThat(plan.inserts()).extracting(v -> v.get("reading")).containsExactly("そう");
        }
    }
}
