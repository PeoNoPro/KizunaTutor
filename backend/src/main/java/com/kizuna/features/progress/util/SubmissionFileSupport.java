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
        return cleanFileName(original, type, "bai-lam");
    }

    /** Như trên, với tên mặc định riêng khi tên gốc rỗng (vd. "de-bai" cho tệp đề bài). */
    public static String cleanFileName(String original, MaterialFileType type, String fallbackBase) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}<>:\"|?*]", "").strip();
        int dot = name.lastIndexOf('.');
        String base = (dot >= 0 ? name.substring(0, dot) : name).strip();
        if (base.isEmpty()) base = fallbackBase;
        if (base.length() > FILE_NAME_MAX) base = base.substring(0, FILE_NAME_MAX).strip();
        return base + "." + type.extension;
    }

    /** Tệp đã kiểm tra, sẵn sàng ghi vào DB. */
    public record PreparedFile(String fileName, byte[] data) {
    }

    /**
     * Kiểm tra (định dạng, chữ ký, dung lượng) và đọc nội dung các tệp tải lên.
     * Đọc hết trước khi ghi để lỗi ở tệp nào cũng không ghi dở dang.
     */
    public static java.util.List<PreparedFile> prepare(com.kizuna.features.classroom.util.MaterialFileValidator validator,
                                                      java.util.List<org.springframework.web.multipart.MultipartFile> files,
                                                      String fallbackBase) {
        return files.stream().map(f -> {
            MaterialFileType type;
            try {
                type = validator.validate(f);
            } catch (com.kizuna.features.classroom.exception.ClassroomException e) {
                throw withFileName(e, f.getOriginalFilename());
            }
            try {
                return new PreparedFile(cleanFileName(f.getOriginalFilename(), type, fallbackBase), f.getBytes());
            } catch (java.io.IOException e) {
                throw com.kizuna.features.classroom.exception.ClassroomException.field("file",
                        "Không đọc được tệp. Vui lòng chọn lại tệp khác.");
            }
        }).toList();
    }

    /** Thêm tên tệp vào đầu thông báo lỗi để người dùng biết tệp nào hỏng (giữ nguyên mã lỗi 400/413). */
    public static com.kizuna.features.classroom.exception.ClassroomException withFileName(
            com.kizuna.features.classroom.exception.ClassroomException e, String originalName) {
        String name = originalName == null ? "" : originalName.substring(
                Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\')) + 1).strip();
        if (name.isEmpty()) return e;
        String message = "Tệp \"" + name + "\": " + e.getMessage();
        return new com.kizuna.features.classroom.exception.ClassroomException(e.getStatus(), message,
                java.util.Map.of("file", message));
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
