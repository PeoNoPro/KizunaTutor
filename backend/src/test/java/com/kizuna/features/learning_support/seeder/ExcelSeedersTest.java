package com.kizuna.features.learning_support.seeder;

import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.learning_content.repository.LearningContentRepository;
import com.kizuna.features.learning_content.service.ContentType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** JOB-10 / JOB-11: nạp Kanji và từ vựng từ Excel, dùng chung quy tắc của FE-13. Không chạm DB thật. */
class ExcelSeedersTest {

    private final LearningContentRepository content = mock(LearningContentRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final PlatformTransactionManager tx = mock(PlatformTransactionManager.class);

    /** Một sheet, dòng đầu là tiêu đề. */
    private static InputStream workbook(String sheetName, String[]... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    if (rows[r][c] != null) row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Nested
    class KanjiSeeder {
        private final KanjiExcelSeeder seeder = new KanjiExcelSeeder(content, jdbc, tx);

        @Test
        @SuppressWarnings("unchecked")
        void newKanjiNeedStrokesAndExistingKanjiOnlyFillEmptyFields() throws Exception {
            when(content.loadExisting(ContentType.KANJI)).thenReturn(Map.of("山", Map.of("character", "山")));

            var summary = seeder.seed(workbook("Kanji",
                    new String[]{"Kanji", "Hán Việt", "Onyomi", "Kunyomi", "Nghĩa tiếng Việt", "JLPT", "Số nét"},
                    new String[]{"川", "XUYÊN", "セン", "かわ", "Sông", "N5", "3"},
                    new String[]{"森", "SÂM", "シン", "もり", "Rừng", "N4", null},
                    new String[]{"山", "SƠN", "サン", "やま", "Núi", "N5", "3"},
                    new String[]{"山", "SƠN", "サン", "やま", "Núi (trùng)", "N5", "3"}));

            ArgumentCaptor<List<Map<String, String>>> inserted = ArgumentCaptor.forClass(List.class);
            verify(content).lock(ContentType.KANJI);
            verify(content).insert(eq(ContentType.KANJI), inserted.capture());
            assertThat(inserted.getValue()).extracting(v -> v.get("character")).containsExactly("川");

            // Chữ đã có: chỉ điền ô trống (câu UPDATE dùng COALESCE), lấy dòng đầu tiên trong tệp, rồi bảo đảm có ở bảng kanjis
            ArgumentCaptor<List<Object[]>> fill = ArgumentCaptor.forClass(List.class);
            verify(jdbc).batchUpdate(argThat((String sql) -> sql.startsWith("UPDATE kanji_dictionary") && sql.contains("COALESCE")), fill.capture());
            assertThat(fill.getValue()).hasSize(1);
            assertThat(fill.getValue().get(0)).contains("SƠN", "Núi", "山");
            verify(jdbc).batchUpdate(argThat((String sql) -> sql.startsWith("INSERT INTO kanjis") && sql.contains("DO NOTHING")), anyList());

            assertThat(summary.inserted()).isEqualTo(1);
            assertThat(summary.existing()).isEqualTo(1);
            assertThat(summary.missingStrokes()).isEqualTo(1);
            assertThat(summary.duplicatesInFile()).isEqualTo(1);
        }

        @Test
        void wrongHeaderIsRejectedBeforeAnyWrite() throws Exception {
            InputStream file = workbook("Sheet1", new String[]{"Cột A", "Cột B"}, new String[]{"x", "y"});

            assertThatThrownBy(() -> seeder.seed(file)).isInstanceOf(BusinessException.class);
            verifyNoInteractions(content, jdbc);
        }
    }

    @Nested
    class VocabularySeeder {
        private final VocabExcelSeeder seeder = new VocabExcelSeeder(content, tx);

        @Test
        @SuppressWarnings("unchecked")
        void skipsWordsAlreadyInLibraryAndDuplicatesInFile() throws Exception {
            when(content.loadExisting(ContentType.VOCABULARY))
                    .thenReturn(Map.of("会う\u0000あう", Map.of("word", "会う", "reading", "あう")));

            var summary = seeder.seed(workbook("Tất cả từ vựng",
                    new String[]{"STT", "Cấp độ JLPT", "Từ vựng (Kanji/Kana)", "Cách đọc (Hiragana)", "Nghĩa tiếng Việt"},
                    new String[]{"1", "N5", "会う", "あう", "Gặp"},
                    new String[]{"2", "N5", "青", "あお", "Màu xanh"},
                    new String[]{"3", "N5", "青", "あお", "Màu xanh"},
                    new String[]{"4", "N5", "これ", null, "Cái này"},
                    new String[]{"5", "N5", "無", "む", null}));

            ArgumentCaptor<List<Map<String, String>>> inserted = ArgumentCaptor.forClass(List.class);
            verify(content).lock(ContentType.VOCABULARY);
            verify(content).insert(eq(ContentType.VOCABULARY), inserted.capture());
            assertThat(inserted.getValue()).extracting(v -> v.get("word")).containsExactly("青", "これ");
            // Từ viết bằng kana để trống cách đọc thì dùng chính từ đó
            assertThat(inserted.getValue().get(1).get("reading")).isEqualTo("これ");

            assertThat(summary.inserted()).isEqualTo(2);
            assertThat(summary.skippedExisting()).isEqualTo(1);
            assertThat(summary.duplicatesInFile()).isEqualTo(1);
            assertThat(summary.invalidRows()).isEqualTo(1); // dòng thiếu nghĩa
        }
    }
}
