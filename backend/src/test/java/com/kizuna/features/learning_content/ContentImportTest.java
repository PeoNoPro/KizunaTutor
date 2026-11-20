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

    // ===== REQ-21, REQ-22, REQ-23: lớp service (kho học liệu, nhập tệp, lịch sử) =====

    private static final String[] KANJI_IMPORT_HEADER = {"Kanji", "Nghĩa tiếng Việt", "Số nét"};

    /** Tệp .xlsx (dạng byte) từ các dòng của một sheet. */
    private static byte[] xlsx(String[]... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    /** Tệp Kanji có đúng {@code dataRows} dòng dữ liệu, ghi theo luồng để không tốn bộ nhớ. */
    private static byte[] kanjiRows(int dataRows) throws Exception {
        try (org.apache.poi.xssf.streaming.SXSSFWorkbook wb = new org.apache.poi.xssf.streaming.SXSSFWorkbook(200);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Kanji");
            header.createCell(1).setCellValue("Nghĩa tiếng Việt");
            for (int r = 1; r <= dataRows; r++) {
                Row row = sheet.createRow(r);
                row.createCell(0).setCellValue("愛");
                row.createCell(1).setCellValue("Yêu");
            }
            wb.write(out);
            wb.dispose();
            return out.toByteArray();
        }
    }

    private static org.springframework.mock.web.MockMultipartFile excel(String name, byte[] content) {
        return new org.springframework.mock.web.MockMultipartFile("file", name,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    /** Tệp thật nhưng báo kích thước tùy ý: kiểm tra biên 15 MB mà không phải dựng tệp 15 MB. */
    private static org.springframework.mock.web.MockMultipartFile excelReportingSize(byte[] content, long reportedSize) {
        return new org.springframework.mock.web.MockMultipartFile("file", "kanji.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content) {
            @Override
            public long getSize() {
                return reportedSize;
            }
        };
    }

    @Nested
    class ServiceLevel {

        private final com.kizuna.features.learning_content.repository.LearningContentRepository store =
                org.mockito.Mockito.mock(com.kizuna.features.learning_content.repository.LearningContentRepository.class);
        private final com.kizuna.features.learning_content.repository.ContentImportHistoryRepository browse =
                org.mockito.Mockito.mock(com.kizuna.features.learning_content.repository.ContentImportHistoryRepository.class);
        private final com.kizuna.features.learning_content.service.impl.LearningContentServiceImpl service =
                new com.kizuna.features.learning_content.service.impl.LearningContentServiceImpl(store, browse,
                        org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class));
        private final java.util.UUID officerId = java.util.UUID.randomUUID();

        private com.kizuna.features.learning_content.dto.ContentImportResponse dryRun(
                org.springframework.web.multipart.MultipartFile file) {
            return service.importFile("kanji", file, true, officerId, "hv@kizuna.test");
        }

        // ----- REQ-21 -----

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.CsvSource({"kanji, KANJI", "vocabulary, VOCABULARY", "grammar, GRAMMAR"})
        void req21_threeContentTypesCanBeBrowsed(String path, ContentType expected) {
            service.listItems(path, null, null, null, 0, 10);

            org.mockito.Mockito.verify(browse).listItems(org.mockito.ArgumentMatchers.eq(expected),
                    org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                    org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(0),
                    org.mockito.ArgumentMatchers.eq(10));
        }

        @Test
        void req21_unknownContentTypeIsRejected() {
            assertThatThrownBy(() -> service.listItems("radicals", null, null, null, 0, 10))
                    .isInstanceOf(BusinessException.class).hasMessage("Loại học liệu không hợp lệ.");
            org.mockito.Mockito.verifyNoInteractions(browse);
        }

        @Test
        void req21_keywordOf100CharactersIsAccepted() {
            service.listItems("kanji", "k".repeat(100), null, null, 0, 10);

            org.mockito.Mockito.verify(browse).listItems(org.mockito.ArgumentMatchers.eq(ContentType.KANJI),
                    org.mockito.ArgumentMatchers.eq("k".repeat(100)), org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        void req21_keywordOf101CharactersIsRejected() {
            assertThatThrownBy(() -> service.listItems("kanji", "k".repeat(101), null, null, 0, 10))
                    .isInstanceOf(BusinessException.class).hasMessage("Từ khóa tối đa 100 ký tự.");
            org.mockito.Mockito.verifyNoInteractions(browse);
        }

        @Test
        void req21_invalidJlptFilterIsRejected() {
            assertThatThrownBy(() -> service.listItems("kanji", null, "N6", null, 0, 10))
                    .isInstanceOf(BusinessException.class);
            org.mockito.Mockito.verifyNoInteractions(browse);
        }

        // ----- REQ-22: kiểm tra tệp -----

        @Test
        void req22_missingOrEmptyFileIsRejected() {
            assertThatThrownBy(() -> dryRun(null)).hasMessage("Vui lòng chọn tệp Excel cần nhập.");
            assertThatThrownBy(() -> dryRun(excel("rong.xlsx", new byte[0]))).hasMessage("Vui lòng chọn tệp Excel cần nhập.");
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"kanji.xls", "kanji.csv", "kanji.xlsx.exe", "kanji"})
        void req22_onlyXlsxFilesAreAccepted(String name) {
            assertThatThrownBy(() -> dryRun(excel(name, new byte[]{1, 2, 3})))
                    .isInstanceOf(BusinessException.class).hasMessage("Chỉ nhận tệp Excel định dạng .xlsx.");
            org.mockito.Mockito.verifyNoInteractions(store);
        }

        @Test
        void req22_fileOfExactly15MegabytesPassesTheSizeCheck() throws Exception {
            byte[] content = xlsx(KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"});

            var report = dryRun(excelReportingSize(content,
                    com.kizuna.features.learning_content.service.LearningContentService.MAX_FILE_BYTES));

            assertThat(report.totalRows()).isEqualTo(1);
            assertThat(report.created()).isEqualTo(1);
        }

        @Test
        void req22_fileOneByteOver15MegabytesIsRejected() throws Exception {
            byte[] content = xlsx(KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"});

            assertThatThrownBy(() -> dryRun(excelReportingSize(content,
                    com.kizuna.features.learning_content.service.LearningContentService.MAX_FILE_BYTES + 1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Tệp vượt quá 15 MB. Vui lòng tách nhỏ tệp rồi nhập từng phần.");
            org.mockito.Mockito.verifyNoInteractions(store);
        }

        @Test
        void req22_corruptedXlsxIsRejected() {
            assertThatThrownBy(() -> dryRun(excel("hong.xlsx", "khong phai tep excel".getBytes())))
                    .isInstanceOf(BusinessException.class).hasMessageStartingWith("Không đọc được tệp.");
            org.mockito.Mockito.verifyNoInteractions(store);
        }

        @Test
        void req22_headerOnFifthRowIsFound() throws Exception {
            byte[] content = xlsx(new String[]{"Kho Kanji"}, new String[]{"Ghi chú"}, new String[]{}, new String[]{"x"},
                    KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"});

            assertThat(dryRun(excel("kanji.xlsx", content)).created()).isEqualTo(1);
        }

        @Test
        void req22_headerOnSixthRowIsNotFound() throws Exception {
            byte[] content = xlsx(new String[]{"1"}, new String[]{"2"}, new String[]{"3"}, new String[]{"4"},
                    new String[]{"5"}, KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"});

            assertThatThrownBy(() -> dryRun(excel("kanji.xlsx", content)))
                    .isInstanceOf(BusinessException.class).hasMessageStartingWith("Không tìm thấy dòng tiêu đề phù hợp.");
        }

        @Test
        void req22_missingRequiredColumnIsRejected() throws Exception {
            byte[] content = xlsx(new String[]{"Kanji", "Số nét"}, new String[]{"愛", "13"});

            assertThatThrownBy(() -> dryRun(excel("kanji.xlsx", content)))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("Kanji, Nghĩa tiếng Việt");
        }

        @Test
        void req22_fiftyThousandDataRowsAreAccepted() throws Exception {
            var parsed = ContentWorkbookReader.read(ContentType.KANJI, new ByteArrayInputStream(kanjiRows(50_000)));

            assertThat(parsed.totalRows()).isEqualTo(50_000);
        }

        @Test
        void req22_fiftyThousandAndOneDataRowsAreRejected() {
            assertThatThrownBy(() -> ContentWorkbookReader.read(ContentType.KANJI,
                    new ByteArrayInputStream(kanjiRows(50_001))))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("50.000 dòng dữ liệu");
        }

        @Test
        void req22_exactly300ErrorsAreAllShown() throws Exception {
            var report = dryRun(excel("kanji.xlsx", xlsx(invalidKanjiRows(300))));

            assertThat(report.issues()).hasSize(300);
            assertThat(report.issuesTruncated()).isFalse();
            assertThat(report.invalidRows()).isEqualTo(300);
        }

        @Test
        void req22_moreThan300ErrorsAreCutTo300ButStillCounted() throws Exception {
            var report = dryRun(excel("kanji.xlsx", xlsx(invalidKanjiRows(301))));

            assertThat(report.issues()).hasSize(300);
            assertThat(report.issuesTruncated()).isTrue();
            assertThat(report.invalidRows()).isEqualTo(301);
            assertThat(report.created()).isZero();
        }

        private String[][] invalidKanjiRows(int count) {
            String[][] rows = new String[count + 1][];
            rows[0] = KANJI_IMPORT_HEADER;
            for (int i = 1; i <= count; i++) rows[i] = new String[]{"AB", "Không phải Kanji", "1"};
            return rows;
        }

        @Test
        void req22_dryRunWritesNothing() throws Exception {
            dryRun(excel("kanji.xlsx", xlsx(KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"})));

            org.mockito.Mockito.verify(store, org.mockito.Mockito.never()).insert(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
            org.mockito.Mockito.verifyNoInteractions(browse);
        }

        // ----- REQ-22 + REQ-23: nhập thật và ghi lịch sử -----

        @Test
        void req23_importInsertsOnlyNewItemsAndHistoryMatchesTheResult() throws Exception {
            org.mockito.Mockito.when(store.loadExisting(ContentType.KANJI))
                    .thenReturn(new HashMap<>(Map.of("憎", Map.of("character", "憎"))));
            byte[] content = xlsx(KANJI_IMPORT_HEADER,
                    new String[]{"憎", "Ghét", "14"},       // đã có → bỏ qua
                    new String[]{"愛", "Yêu", "13"},        // mới
                    new String[]{"愛", "Yêu", "13"},        // trùng trong tệp
                    new String[]{"馮", "Cưỡi ngựa", "12"},  // mới
                    new String[]{"恋", "Yêu đương", null},  // Kanji mới thiếu số nét → lỗi
                    new String[]{"AB", "Sai", "1"});         // không phải Kanji → lỗi

            var report = service.importFile("kanji", excel("kanji.xlsx", content), false, officerId, "hv@kizuna.test");

            assertThat(report.dryRun()).isFalse();
            assertThat(report.totalRows()).isEqualTo(6);
            assertThat(report.created()).isEqualTo(2);
            assertThat(report.skippedExisting()).isEqualTo(1);
            assertThat(report.duplicatesInFile()).isEqualTo(1);
            assertThat(report.invalidRows()).isEqualTo(2);

            @SuppressWarnings("unchecked")
            org.mockito.ArgumentCaptor<java.util.List<Map<String, String>>> inserted =
                    org.mockito.ArgumentCaptor.forClass(java.util.List.class);
            var order = org.mockito.Mockito.inOrder(store, browse);
            order.verify(store).lock(ContentType.KANJI);
            order.verify(store).insert(org.mockito.ArgumentMatchers.eq(ContentType.KANJI), inserted.capture());
            order.verify(browse).logImport(org.mockito.ArgumentMatchers.eq(officerId),
                    org.mockito.ArgumentMatchers.eq(ContentType.KANJI), org.mockito.ArgumentMatchers.same(report),
                    org.mockito.ArgumentMatchers.argThat(items -> items.size() == 2));
            assertThat(inserted.getValue()).extracting(v -> v.get("character")).containsExactly("愛", "馮");
        }

        @Test
        void req23_failedInsertDoesNotWriteHistory() throws Exception {
            org.mockito.Mockito.doThrow(new IllegalStateException("db")).when(store)
                    .insert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

            assertThatThrownBy(() -> service.importFile("kanji",
                    excel("kanji.xlsx", xlsx(KANJI_IMPORT_HEADER, new String[]{"愛", "Yêu", "13"})),
                    false, officerId, "hv@kizuna.test")).isInstanceOf(IllegalStateException.class);

            org.mockito.Mockito.verify(browse, org.mockito.Mockito.never()).logImport(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        }
    }
}
