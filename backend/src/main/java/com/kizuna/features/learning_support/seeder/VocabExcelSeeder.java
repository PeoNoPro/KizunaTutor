package com.kizuna.features.learning_support.seeder;

import com.kizuna.features.learning_content.dto.ImportIssueResponse;
import com.kizuna.features.learning_content.repository.LearningContentRepository;
import com.kizuna.features.learning_content.service.ContentImportPlanner;
import com.kizuna.features.learning_content.service.ContentType;
import com.kizuna.features.learning_content.service.ContentWorkbookReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Nạp từ vựng từ "database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx" khi khởi động
 * (bật bằng app.seeder.excel-vocab.enabled=true). Chạy sau {@link KanjiExcelSeeder} khi bật cả hai.
 *
 * Dùng chung bộ đọc, quy tắc và cách ghi của chức năng nhập học liệu FE-13:
 * - Cột nhận theo tên tiêu đề; dòng thiếu từ hoặc nghĩa, JLPT sai, quá dài thì bỏ qua và ghi log.
 * - Từ đã có (cùng "từ" và "cách đọc") hoặc trùng trong tệp thì bỏ qua, nên chạy lại không bị nhân đôi.
 * - Từ mới được liên kết với các chữ Hán có trong bảng kanjis (vocabulary_kanjis).
 * - Mọi thay đổi nằm trong một giao dịch; lỗi chỉ ghi log, ứng dụng vẫn khởi động.
 * Không tạo bảng.
 */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seeder.excel-vocab.enabled", havingValue = "true")
public class VocabExcelSeeder implements CommandLineRunner {

    static final String FILE_NAME = "Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx";
    private static final int LOGGED_ISSUES = 10;

    /** Kết quả một lần nạp (để ghi log và kiểm thử). */
    record Summary(int rowsRead, int invalidRows, int duplicatesInFile, int inserted, int skippedExisting) {
    }

    private final LearningContentRepository content;
    private final TransactionTemplate transaction;

    public VocabExcelSeeder(LearningContentRepository content, PlatformTransactionManager transactionManager) {
        this.content = content;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        File file = SeederFiles.find(FILE_NAME);
        if (file == null) {
            log.error("[SEED TỪ VỰNG] Không tìm thấy database/{}; bỏ qua.", FILE_NAME);
            return;
        }
        log.info("[SEED TỪ VỰNG] Bắt đầu nạp từ {}", file.getAbsolutePath());
        try (InputStream in = new FileInputStream(file)) {
            Summary s = seed(in);
            log.info("[SEED TỪ VỰNG] Xong: đọc {} dòng, thêm mới {} từ, bỏ qua {} từ đã có, {} dòng trùng trong tệp, {} dòng lỗi.",
                    s.rowsRead(), s.inserted(), s.skippedExisting(), s.duplicatesInFile(), s.invalidRows());
            log.info("[SEED TỪ VỰNG] Hãy đặt lại app.seeder.excel-vocab.enabled=false để không nạp lại khi khởi động lần sau.");
        } catch (Exception e) {
            // Không chặn khởi động: giao dịch đã hoàn tác, chạy lại sau khi sửa tệp.
            log.error("[SEED TỪ VỰNG] Lỗi, không có thay đổi nào được lưu: {}", e.getMessage(), e);
        }
    }

    Summary seed(InputStream in) {
        ContentWorkbookReader.ParsedWorkbook parsed = ContentWorkbookReader.read(ContentType.VOCABULARY, in);
        logIssues(parsed.issues());
        return transaction.execute(status -> {
            content.lock(ContentType.VOCABULARY);
            Map<String, Map<String, String>> existing = content.loadExisting(ContentType.VOCABULARY);
            ContentImportPlanner.Plan plan = ContentImportPlanner.plan(ContentType.VOCABULARY, parsed.rows(), existing);
            logIssues(plan.issues());
            content.insert(ContentType.VOCABULARY, plan.inserts());
            return new Summary(parsed.totalRows(), parsed.invalidRows(), plan.duplicatesInFile(),
                    plan.inserts().size(), plan.skippedExisting());
        });
    }

    private static void logIssues(List<ImportIssueResponse> issues) {
        issues.stream().limit(LOGGED_ISSUES).forEach(issue ->
                log.warn("[SEED TỪ VỰNG] Sheet {} dòng {}: {}", issue.sheet(), issue.row(), issue.message()));
        if (issues.size() > LOGGED_ISSUES) {
            log.warn("[SEED TỪ VỰNG] ... và {} dòng khác có cảnh báo hoặc lỗi.", issues.size() - LOGGED_ISSUES);
        }
    }
}
