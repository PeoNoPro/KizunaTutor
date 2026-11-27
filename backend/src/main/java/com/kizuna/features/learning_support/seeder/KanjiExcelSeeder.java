package com.kizuna.features.learning_support.seeder;

import com.kizuna.features.learning_content.dto.ImportIssueResponse;
import com.kizuna.features.learning_content.repository.LearningContentRepository;
import com.kizuna.features.learning_content.service.ContentImportPlanner;
import com.kizuna.features.learning_content.service.ContentType;
import com.kizuna.features.learning_content.service.ContentWorkbookReader;
import com.kizuna.features.learning_content.service.ContentWorkbookReader.ContentRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Nạp Kanji từ "database/kanji_vietnamese_full.xlsx" khi khởi động (bật bằng app.seeder.excel-kanji.enabled=true).
 *
 * Dùng chung bộ đọc và quy tắc của chức năng nhập học liệu FE-13 (cột nhận theo tên tiêu đề, kiểm tra hợp lệ,
 * bỏ dòng trùng trong tệp), và ghi cả kanji_dictionary lẫn kanjis để từ vựng liên kết được với Kanji.
 * - Chữ MỚI: bắt buộc có cột "Số nét" (1–84); chữ mới thiếu số nét bị bỏ qua và ghi log, không ghi bừa số nét.
 * - Chữ ĐÃ CÓ: chỉ điền các ô đang trống (Hán Việt, âm đọc, nghĩa, JLPT, số nét = 0); không xóa hay ghi đè dữ liệu cũ.
 * - Mọi thay đổi nằm trong một giao dịch; lỗi chỉ ghi log, ứng dụng vẫn khởi động. Chạy lại nhiều lần vẫn an toàn.
 * Không tạo bảng.
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seeder.excel-kanji.enabled", havingValue = "true")
public class KanjiExcelSeeder implements CommandLineRunner {

    static final String FILE_NAME = "kanji_vietnamese_full.xlsx";
    private static final int LOGGED_ISSUES = 10;

    private static final String FILL_EMPTY_SQL = "UPDATE kanji_dictionary SET "
            + "han_viet = COALESCE(NULLIF(han_viet, ''), ?), "
            + "onyomi = COALESCE(NULLIF(onyomi, ''), ?), "
            + "kunyomi = COALESCE(NULLIF(kunyomi, ''), ?), "
            + "meaning = COALESCE(NULLIF(meaning, ''), ?), "
            + "jlpt_level = COALESCE(jlpt_level, ?::jlpt_level_enum), "
            + "stroke_count = CASE WHEN stroke_count = 0 THEN COALESCE(?::smallint, 0) ELSE stroke_count END "
            + "WHERE kanji_character = ? AND (NULLIF(han_viet, '') IS NULL OR NULLIF(onyomi, '') IS NULL "
            + "OR NULLIF(kunyomi, '') IS NULL OR NULLIF(meaning, '') IS NULL OR jlpt_level IS NULL OR stroke_count = 0)";

    private static final String ENSURE_KANJIS_SQL = "INSERT INTO kanjis (\"character\", han_viet, onyomi, kunyomi, meaning, jlpt_level) "
            + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (\"character\") DO NOTHING";

    /** Kết quả một lần nạp (để ghi log và kiểm thử). */
    record Summary(int rowsRead, int invalidRows, int duplicatesInFile, int inserted, int existing, int missingStrokes) {
    }

    private final LearningContentRepository content;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public KanjiExcelSeeder(LearningContentRepository content, JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.content = content;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        File file = SeederFiles.find(FILE_NAME);
        if (file == null) {
            log.error("[SEED KANJI] Không tìm thấy database/{}; bỏ qua.", FILE_NAME);
            return;
        }
        log.info("[SEED KANJI] Bắt đầu nạp từ {}", file.getAbsolutePath());
        try (InputStream in = new FileInputStream(file)) {
            Summary s = seed(in);
            log.info("[SEED KANJI] Xong: đọc {} dòng, thêm mới {} chữ, {} chữ đã có (chỉ điền ô trống), "
                            + "{} dòng trùng trong tệp, {} dòng lỗi, {} chữ mới thiếu số nét bị bỏ qua.",
                    s.rowsRead(), s.inserted(), s.existing(), s.duplicatesInFile(), s.invalidRows(), s.missingStrokes());
            log.info("[SEED KANJI] Hãy đặt lại app.seeder.excel-kanji.enabled=false để không nạp lại khi khởi động lần sau.");
        } catch (Exception e) {
            // Không chặn khởi động: giao dịch đã hoàn tác, chạy lại sau khi sửa tệp.
            log.error("[SEED KANJI] Lỗi, không có thay đổi nào được lưu: {}", e.getMessage(), e);
        }
    }

    Summary seed(InputStream in) {
        ContentWorkbookReader.ParsedWorkbook parsed = ContentWorkbookReader.read(ContentType.KANJI, in);
        logIssues(parsed.issues());
        return transaction.execute(status -> {
            content.lock(ContentType.KANJI);
            Map<String, Map<String, String>> existing = content.loadExisting(ContentType.KANJI);
            ContentImportPlanner.Plan plan = ContentImportPlanner.plan(ContentType.KANJI, parsed.rows(), existing);
            logIssues(plan.issues());

            content.insert(ContentType.KANJI, plan.inserts());
            List<Map<String, String>> known = existingRows(parsed.rows(), existing.keySet());
            fillEmpty(known);

            int missingStrokes = (int) plan.issues().stream()
                    .filter(issue -> issue.message() != null && issue.message().contains("Số nét")).count();
            return new Summary(parsed.totalRows(), parsed.invalidRows(), plan.duplicatesInFile(),
                    plan.inserts().size(), known.size(), missingStrokes);
        });
    }

    /** Dòng của chữ đã có trong kho, mỗi chữ lấy dòng xuất hiện đầu tiên trong tệp. */
    static List<Map<String, String>> existingRows(List<ContentRow> rows, Set<String> existingKeys) {
        Map<String, Map<String, String>> first = new LinkedHashMap<>();
        for (ContentRow row : rows) {
            String key = ContentType.KANJI.keyOf(row.values());
            if (existingKeys.contains(key)) first.putIfAbsent(key, row.values());
        }
        return new ArrayList<>(first.values());
    }

    private void fillEmpty(List<Map<String, String>> rows) {
        if (rows.isEmpty()) return;
        jdbc.batchUpdate(FILL_EMPTY_SQL, rows.stream().map(v -> new Object[]{
                v.get("hanViet"), v.get("onyomi"), v.get("kunyomi"), v.get("meaning"), v.get("jlpt"),
                v.get("strokes") == null ? null : Short.valueOf(v.get("strokes")), v.get("character")}).toList());
        // Chữ đã có ở kanji_dictionary nhưng chưa có ở kanjis thì thêm, để từ vựng liên kết được
        jdbc.batchUpdate(ENSURE_KANJIS_SQL, rows.stream().map(v -> new Object[]{
                v.get("character"), v.get("hanViet"), v.get("onyomi"), v.get("kunyomi"), v.get("meaning"), v.get("jlpt")}).toList());
    }

    private static void logIssues(List<ImportIssueResponse> issues) {
        issues.stream().limit(LOGGED_ISSUES).forEach(issue ->
                log.warn("[SEED KANJI] Sheet {} dòng {}: {}", issue.sheet(), issue.row(), issue.message()));
        if (issues.size() > LOGGED_ISSUES) {
            log.warn("[SEED KANJI] ... và {} dòng khác có cảnh báo hoặc lỗi.", issues.size() - LOGGED_ISSUES);
        }
    }
}
