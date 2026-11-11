package com.kizuna.features.learning_content.service;

import com.kizuna.features.learning_content.dto.ImportIssueResponse;
import com.kizuna.features.learning_content.service.ContentWorkbookReader.ContentRow;

import java.util.*;

/**
 * Quyết định dòng hợp lệ nào được thêm mới. Không chạm cơ sở dữ liệu.
 *
 * Quy tắc: mục đã có trong hệ thống LUÔN giữ nguyên, chỉ thêm mục mới.
 * - Trùng trong tệp (cùng khóa): giữ dòng xuất hiện đầu tiên. Dòng sau giống hệt thì chỉ đếm
 *   (tệp mẫu có sheet "Tất cả" và các sheet theo cấp độ lặp lại nhau); khác nội dung thì cảnh báo.
 * - Đã có trong hệ thống (cùng khóa): bỏ qua, không ghi đè.
 * - Ngữ pháp: mẫu câu đã thuộc về mã khác (trong hệ thống hoặc trong tệp) là lỗi, không nhập.
 * - Kanji mới bắt buộc có Số nét (cột stroke_count NOT NULL).
 */
public final class ContentImportPlanner {

    public record Plan(List<Map<String, String>> inserts, int skippedExisting, int duplicatesInFile,
                       int invalidRows, List<ImportIssueResponse> issues) {
    }

    private ContentImportPlanner() {
    }

    /**
     * @param existing bản ghi đang có, theo khóa {@link ContentType#keyOf}
     */
    public static Plan plan(ContentType type, List<ContentRow> rows, Map<String, Map<String, String>> existing) {
        List<Map<String, String>> inserts = new ArrayList<>();
        List<ImportIssueResponse> issues = new ArrayList<>();
        Map<String, ContentRow> firstByKey = new HashMap<>();
        Map<String, String> patternOwner = new HashMap<>();
        if (type == ContentType.GRAMMAR) {
            existing.values().forEach(record -> {
                String pattern = record.get("pattern");
                if (pattern != null) patternOwner.put(pattern, record.get("code"));
            });
        }
        int skipped = 0;
        int duplicates = 0;
        int invalid = 0;

        for (ContentRow row : rows) {
            Map<String, String> values = row.values();
            String key = type.keyOf(values);

            ContentRow first = firstByKey.putIfAbsent(key, row);
            if (first != null) {
                duplicates++;
                if (!first.values().equals(values)) {
                    issues.add(ImportIssueResponse.warning(row.sheet(), row.row(), "Trùng " + type.describeKey(values)
                            + " với dòng " + first.row() + " (sheet " + first.sheet()
                            + ") nhưng nội dung khác. Hệ thống giữ dòng xuất hiện trước."));
                }
                continue;
            }

            if (existing.containsKey(key)) {
                skipped++;
                continue;
            }

            if (type == ContentType.GRAMMAR) {
                String owner = patternOwner.get(values.get("pattern"));
                if (owner != null) {
                    invalid++;
                    issues.add(ImportIssueResponse.error(row.sheet(), row.row(), "Mẫu \"" + values.get("pattern")
                            + "\" đã có với mã " + owner + "."));
                    continue;
                }
                patternOwner.put(values.get("pattern"), values.get("code"));
            }

            if (type == ContentType.KANJI && values.get("strokes") == null) {
                invalid++;
                issues.add(ImportIssueResponse.error(row.sheet(), row.row(), "Chữ " + values.get("character")
                        + " chưa có trong hệ thống nên cần cột Số nét."));
                continue;
            }
            inserts.add(values);
        }
        return new Plan(inserts, skipped, duplicates, invalid, issues);
    }
}
