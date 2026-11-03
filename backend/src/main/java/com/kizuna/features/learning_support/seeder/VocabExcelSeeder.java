package com.kizuna.features.learning_support.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Seeder tự động nạp từ vựng JLPT từ file Excel:
 * "database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx"
 * 
 * Nạp vào bảng vocabularies đã có sẵn trong Database.
 * Tự động quét ký tự Hán tự để liên kết bảng trung gian vocabulary_kanjis.
 * 
 * TUYỆT ĐỐI KHÔNG TẠO BẢNG (Bảng đã có sẵn trong cơ sở dữ liệu).
 * 
 * Kích hoạt trong application.properties:
 * app.seeder.excel-vocab.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeder.excel-vocab.enabled", havingValue = "true")
public class VocabExcelSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== BẮT ĐẦU SEED TỪ VỰNG TỪ FILE EXCEL ==========");

        File file = new File("database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx");
        if (!file.exists()) {
            file = new File("../database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx");
        }

        if (!file.exists()) {
            log.error("Không tìm thấy file Excel tại database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx!");
            return;
        }

        log.info("Tìm thấy file từ vựng tại: {}", file.getAbsolutePath());

        // Cache map ký tự Kanji -> kanji_id từ bảng kanjis
        Map<String, Long> kanjiMap = new HashMap<>();
        try {
            jdbcTemplate.query("SELECT id, \"character\" FROM kanjis", (rs) -> {
                String ch = rs.getString("character");
                if (ch != null && !ch.trim().isEmpty()) {
                    kanjiMap.put(ch.trim(), rs.getLong("id"));
                }
            });
            log.info("Đã nạp {} chữ Hán vào bộ nhớ đệm để liên kết từ vựng.", kanjiMap.size());
        } catch (Exception e) {
            log.warn("Không thể tải bảng kanjis để map liên kết: {}", e.getMessage());
        }

        String insertVocabSql = "INSERT INTO vocabularies (" +
                "    jmdict_id, jlpt_level, word, reading, romaji, han_viet, part_of_speech, meaning, " +
                "    example_sentence, example_furigana, example_meaning" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        String insertLinkSql = "INSERT INTO vocabulary_kanjis (vocabulary_id, kanji_id) VALUES (?, ?) " +
                "ON CONFLICT (vocabulary_id, kanji_id) DO NOTHING";

        int totalImported = 0;
        int totalLinks = 0;

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Sheet "Tất cả từ vựng"
            int lastRow = sheet.getLastRowNum();
            log.info("Tổng số dòng từ vựng trong file Excel: {}", lastRow);

            List<Object[]> linkBatch = new ArrayList<>();

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String jlpt = getCellValue(row.getCell(1));
                String word = getCellValue(row.getCell(2));
                String reading = getCellValue(row.getCell(3));
                String romaji = getCellValue(row.getCell(4));
                String hanViet = getCellValue(row.getCell(5));
                String partOfSpeech = getCellValue(row.getCell(6));
                String meaning = getCellValue(row.getCell(7));
                String exampleSentence = getCellValue(row.getCell(8));
                String exampleFurigana = getCellValue(row.getCell(9));
                String exampleMeaning = getCellValue(row.getCell(10));
                String jmdictId = getCellValue(row.getCell(11));

                if (word == null || word.trim().isEmpty()) continue;
                word = word.trim();
                if (reading == null || reading.trim().isEmpty()) reading = word;
                if (meaning == null || meaning.trim().isEmpty()) meaning = "-";

                try {
                    Long vocabId = jdbcTemplate.queryForObject(
                            insertVocabSql,
                            Long.class,
                            jmdictId != null ? jmdictId.trim() : null,
                            jlpt != null ? jlpt.trim() : null,
                            word,
                            reading.trim(),
                            romaji != null ? romaji.trim() : null,
                            hanViet != null ? hanViet.trim() : null,
                            partOfSpeech != null ? partOfSpeech.trim() : null,
                            meaning.trim(),
                            exampleSentence != null ? exampleSentence.trim() : null,
                            exampleFurigana != null ? exampleFurigana.trim() : null,
                            exampleMeaning != null ? exampleMeaning.trim() : null
                    );

                    totalImported++;

                    // Quét các ký tự Kanji trong từ vựng để map bảng trung gian vocabulary_kanjis
                    if (vocabId != null && !kanjiMap.isEmpty()) {
                        Set<Long> linkedKanjiIds = new HashSet<>();
                        for (int i = 0; i < word.length(); i++) {
                            String ch = String.valueOf(word.charAt(i));
                            Long kId = kanjiMap.get(ch);
                            if (kId != null && linkedKanjiIds.add(kId)) {
                                linkBatch.add(new Object[]{vocabId, kId});
                            }
                        }
                    }

                    if (linkBatch.size() >= 200) {
                        jdbcTemplate.batchUpdate(insertLinkSql, linkBatch);
                        totalLinks += linkBatch.size();
                        linkBatch.clear();
                    }

                    if (totalImported % 500 == 0) {
                        log.info("Đã nạp {} / {} từ vựng...", totalImported, lastRow);
                    }
                } catch (Exception ex) {
                    log.warn("Bỏ qua từ vựng lỗi tại dòng {}: {} - {}", r, word, ex.getMessage());
                }
            }

            if (!linkBatch.isEmpty()) {
                jdbcTemplate.batchUpdate(insertLinkSql, linkBatch);
                totalLinks += linkBatch.size();
                linkBatch.clear();
            }

            log.info("========== HOÀN THÀNH SEED TỪ VỰNG! ==========");
            log.info("Tổng số từ vựng đã nạp: {}, tổng số liên kết vocabulary_kanjis: {}", totalImported, totalLinks);
            log.info("Gợi ý: Hãy đặt lại app.seeder.excel-vocab.enabled=false trong application.properties.");
        } catch (Exception e) {
            log.error("Lỗi khi nạp dữ liệu từ vựng từ Excel: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val)) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        }
        return cell.toString();
    }
}

