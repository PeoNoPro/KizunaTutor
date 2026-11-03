package com.kizuna.features.progress.util;

import com.kizuna.features.classroom.util.MaterialFileValidator.MaterialFileType;

import java.util.Locale;

/** Tên tệp và kiểu nội dung của tệp bài nộp (cùng quy tắc với tài liệu học tập FE-06). */
public final class SubmissionFileSupport {

    static final int FILE_NAME_MAX = 200;

    private SubmissionFileSupport() {
    }

    /** Bỏ đường dẫn, ký tự điều khiển và ký tự không hợp lệ; luôn kết thúc bằng phần mở rộng đã xác minh. */
    public static String cleanFileName(String original, MaterialFileType type) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "").strip();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0 ? name.substring(0, dot) : name).strip();
        if (base.isEmpty()) base = "bai-lam";
        if (base.length() > FILE_NAME_MAX) base = base.substring(0, FILE_NAME_MAX).strip();
        return base + "." + type.extension;
    }

    /** Loại tệp suy ra từ phần mở rộng (bảng không có cột loại tệp). */
    public static MaterialFileType typeOf(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (MaterialFileType t : MaterialFileType.values()) {
            if (t.extension.equals(ext)) return t;
        }
        return null;
    }
}
