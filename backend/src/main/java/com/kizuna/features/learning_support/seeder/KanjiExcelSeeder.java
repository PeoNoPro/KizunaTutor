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
import java.util.ArrayList;
import java.util.List;

/**
 * Seeder tự động đọc file Excel "database/kanji_vietnamese_full.xlsx"
 * và nạp vào bảng kanji_dictionary đã có sẵn trong Database.
 * 
 * TUYỆT ĐỐI KHÔNG TẠO BẢNG (Chỉ dùng INSERT ... ON CONFLICT DO UPDATE).
 * 
 * Cách kích hoạt trong application.properties:
 * app.seeder.excel-kanji.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seeder.excel-kanji.enabled", havingValue = "true")
public class KanjiExcelSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("========== BẮT ĐẦU SEED DỮ LIỆU KANJI TỪ FILE EXCEL ==========");

        File file = new File("database/kanji_vietnamese_full.xlsx");
        if (!file.exists()) {
            file = new File("../database/kanji_vietnamese_full.xlsx");
        }

        if (!file.exists()) {
            log.error("Không tìm thấy file Excel tại đường dẫn database/kanji_vietnamese_full.xlsx!");
            return;
        }

        log.info("Tìm thấy file Excel tại: {}", file.getAbsolutePath());

        String sql = "INSERT INTO kanji_dictionary (" +
                "    kanji_character, unicode_hex, stroke_count, han_viet, onyomi, kunyomi, meaning, jlpt_level" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?::jlpt_level_enum) " +
                "ON CONFLICT (kanji_character) DO UPDATE SET " +
                "    han_viet = EXCLUDED.han_viet, " +
                "    onyomi = EXCLUDED.onyomi, " +
                "    kunyomi = EXCLUDED.kunyomi, " +
                "    meaning = EXCLUDED.meaning, " +
                "    jlpt_level = EXCLUDED.jlpt_level, " +
                "    updated_at = CURRENT_TIMESTAMP";

        int totalImported = 0;
        int batchSize = 200;
        List<Object[]> batchArgs = new ArrayList<>(batchSize);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            log.info("Tổng số dòng trong Sheet: {}", lastRow);

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String kanji = getCellValue(row.getCell(0));
                if (kanji == null || kanji.trim().isEmpty()) continue;
                kanji = kanji.trim();

                String hanViet = getCellValue(row.getCell(1));
                String onyomi = getCellValue(row.getCell(2));
                String kunyomi = getCellValue(row.getCell(3));
                String meaning = getCellValue(row.getCell(4));
                String jlpt = getCellValue(row.getCell(5));

                int codePoint = kanji.codePointAt(0);
                String unicodeHex = String.format("%05x", codePoint);
                short strokeCount = 1;

                String jlptValue = null;
                if (jlpt != null && jlpt.trim().toUpperCase().matches("N[1-5]")) {
                    jlptValue = jlpt.trim().toUpperCase();
                }

                batchArgs.add(new Object[]{
                        kanji,
                        unicodeHex,
                        strokeCount,
                        hanViet != null ? hanViet.trim() : null,
                        onyomi != null ? onyomi.trim() : null,
                        kunyomi != null ? kunyomi.trim() : null,
                        meaning != null ? meaning.trim() : null,
                        jlptValue
                });

                if (batchArgs.size() >= batchSize) {
                    jdbcTemplate.batchUpdate(sql, batchArgs);
                    totalImported += batchArgs.size();
                    log.info("Đã nạp thành công {} / {} chữ Kanji...", totalImported, lastRow);
                    batchArgs.clear();
                }
            }

            if (!batchArgs.isEmpty()) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                totalImported += batchArgs.size();
                batchArgs.clear();
            }

            log.info("========== HOÀN THÀNH SEED EXCEL KANJI! TỔNG SỐ: {} KANJI ==========", totalImported);
            log.info("Gợi ý: Hãy đặt lại app.seeder.excel-kanji.enabled=false trong application.properties để tránh nạp lại khi khởi động lần sau.");
        } catch (Exception e) {
            log.error("Lỗi khi nạp dữ liệu Kanji từ Excel: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((int) cell.getNumericCellValue());
        return cell.toString();
    }
}

