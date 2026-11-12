package com.kizuna.features.learning_content.service.impl;

import com.kizuna.core.dto.PageResponse;
import com.kizuna.core.exception.BusinessException;
import com.kizuna.features.learning_content.dto.*;
import com.kizuna.features.learning_content.repository.ContentImportHistoryRepository;
import com.kizuna.features.learning_content.repository.LearningContentRepository;
import com.kizuna.features.learning_content.service.ContentImportPlanner;
import com.kizuna.features.learning_content.service.ContentImportPlanner.Plan;
import com.kizuna.features.learning_content.service.ContentType;
import com.kizuna.features.learning_content.service.ContentWorkbookReader;
import com.kizuna.features.learning_content.service.LearningContentService;
import com.kizuna.features.learning_content.service.ContentType.Field;
import com.kizuna.features.learning_content.service.ContentWorkbookReader.ParsedWorkbook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Cán bộ học vụ nhập kho học liệu (Kanji, từ vựng, ngữ pháp) từ tệp Excel.
 * Mục đã có trong hệ thống luôn giữ nguyên; chỉ thêm mục mới.
 * Hai bước: kiểm tra (dryRun, không ghi gì) rồi nhập thật với cùng tệp.
 * Lượt nhập thật chạy trong một giao dịch: lỗi giữa chừng thì không ghi dòng nào.
 */
@Slf4j
@Service
public class LearningContentServiceImpl implements LearningContentService {

    static final int MAX_ISSUES = 300;

    private final LearningContentRepository store;
    private final ContentImportHistoryRepository browse;
    private final TransactionTemplate transaction;

    public LearningContentServiceImpl(LearningContentRepository store, ContentImportHistoryRepository browse,
                                PlatformTransactionManager transactionManager) {
        this.store = store;
        this.browse = browse;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public ContentSummaryResponse summary() {
        List<ContentTypeSummaryResponse> types = Arrays.stream(ContentType.values())
                .map(t -> new ContentTypeSummaryResponse(t.name(), t.label(), store.count(t),
                        t.fields().stream().filter(Field::required).map(Field::header).toList(),
                        t.fields().stream().filter(f -> !f.required()).map(Field::header).toList()))
                .toList();
        return new ContentSummaryResponse(types, MAX_FILE_BYTES);
    }

    // ===== Xem kho và lịch sử =====

    private static final Set<String> JLPT_LEVELS = Set.of("N1", "N2", "N3", "N4", "N5");
    private static final Set<Integer> PAGE_SIZES = Set.of(10, 20, 50);

    @Override
    public PageResponse<Map<String, Object>> listItems(String typePath, String keyword, String jlpt, String sort,
                                                     int page, int size) {
        ContentType type = requireType(typePath);
        String level = jlpt == null || jlpt.isBlank() || "ALL".equalsIgnoreCase(jlpt) ? null : jlpt.trim().toUpperCase(Locale.ROOT);
        if (level != null && !JLPT_LEVELS.contains(level)) throw BusinessException.invalid();
        String text = keyword == null ? null : keyword.strip();
        if (text != null && text.length() > 100) throw new BusinessException(400, "Từ khóa tối đa 100 ký tự.");
        return browse.listItems(type, text, level, sort, Math.max(0, page), pageSize(size));
    }

    @Override
    public PageResponse<ImportHistoryResponse> history(String typePath, int page, int size) {
        ContentType type = typePath == null || typePath.isBlank() || "ALL".equalsIgnoreCase(typePath)
                ? null : requireType(typePath);
        return browse.history(type, Math.max(0, page), pageSize(size));
    }

    @Override
    public ImportHistoryDetailResponse historyDetail(UUID id) {
        return browse.historyDetail(id).orElseThrow(BusinessException::missing);
    }

    private static int pageSize(int size) {
        return PAGE_SIZES.contains(size) ? size : 10;
    }

    private static ContentType requireType(String typePath) {
        return ContentType.fromPath(typePath)
                .orElseThrow(() -> new BusinessException(400, "Loại học liệu không hợp lệ."));
    }

    // ===== Nhập tệp =====

    @Override
    public ContentImportResponse importFile(String typePath, MultipartFile file, boolean dryRun, UUID officerId,
                                   String officerEmail) {
        ContentType type = requireType(typePath);
        String fileName = requireExcel(file);

        // Đọc tệp trước khi mở giao dịch để không giữ kết nối CSDL trong lúc phân tích
        ParsedWorkbook parsed;
        try (InputStream in = file.getInputStream()) {
            parsed = ContentWorkbookReader.read(type, in);
        } catch (IOException e) {
            throw new BusinessException(400, "Không đọc được tệp. Vui lòng chọn lại tệp.");
        }

        if (dryRun) {
            Plan plan = ContentImportPlanner.plan(type, parsed.rows(), store.loadExisting(type));
            return report(type, fileName, true, parsed, plan);
        }

        // Thêm dữ liệu và ghi lịch sử trong cùng một giao dịch
        ContentImportResponse result = transaction.execute(status -> {
            store.lock(type);
            Plan p = ContentImportPlanner.plan(type, parsed.rows(), store.loadExisting(type));
            store.insert(type, p.inserts());
            ContentImportResponse r = report(type, fileName, false, parsed, p);
            browse.logImport(officerId, type, r, p.inserts().stream().map(type::describeItem).toList());
            return r;
        });
        log.info("Nhập học liệu {} từ tệp '{}' bởi {}: thêm {}, đã có {}, lỗi {}",
                type, fileName, officerEmail, result.created(), result.skippedExisting(), result.invalidRows());
        return result;
    }

    static String requireExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "Vui lòng chọn tệp Excel cần nhập.");
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").trim();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessException(400, "Chỉ nhận tệp Excel định dạng .xlsx.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(400, "Tệp vượt quá 15 MB. Vui lòng tách nhỏ tệp rồi nhập từng phần.");
        }
        return name.replaceAll("^.*[\\\\/]", "");
    }

    static ContentImportResponse report(ContentType type, String fileName, boolean dryRun, ParsedWorkbook parsed, Plan plan) {
        List<ImportIssueResponse> issues = new ArrayList<>(parsed.issues());
        issues.addAll(plan.issues());
        issues.sort(Comparator.comparing((ImportIssueResponse i) -> "ERROR".equals(i.level()) ? 0 : 1)
                .thenComparing(i -> parsed.sheets().indexOf(i.sheet()))
                .thenComparingInt(ImportIssueResponse::row));
        boolean truncated = issues.size() > MAX_ISSUES;
        int created = plan.inserts().size();
        return new ContentImportResponse(type.name(), type.label(), fileName, dryRun, parsed.sheets(),
                parsed.totalRows(), created, plan.skippedExisting(), plan.duplicatesInFile(),
                parsed.invalidRows() + plan.invalidRows(),
                truncated ? issues.subList(0, MAX_ISSUES) : issues, truncated,
                message(dryRun, created));
    }

    private static String message(boolean dryRun, int created) {
        if (created == 0) {
            return dryRun ? "Tệp không có mục mới nào cần thêm." : "Không có mục mới nào được thêm.";
        }
        return dryRun ? "Kiểm tra xong: sẽ thêm " + created + " mục mới." : "Đã thêm " + created + " mục mới.";
    }
}
