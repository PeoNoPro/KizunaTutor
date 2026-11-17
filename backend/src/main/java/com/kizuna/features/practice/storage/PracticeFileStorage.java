package com.kizuna.features.practice.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * FE-13: nơi lưu file nghe/ảnh của ngân hàng đề. DB chỉ giữ "khóa file" (đường dẫn tương đối), không giữ nội dung
 * hay đường link đầy đủ, nên đổi nơi lưu (máy chủ → Cloudflare R2 / dịch vụ chuẩn S3) chỉ cần thêm một lớp cài đặt
 * giao diện này và đổi cấu hình; trang và dữ liệu giữ nguyên.
 */
public interface PracticeFileStorage {
    /** Ghi luồng dữ liệu vào khóa {@code key}; quá {@code maxBytes} thì hủy và báo lỗi. Trả về số byte đã ghi. */
    long save(String key, InputStream content, long maxBytes);

    /** Đọc file để trả cho trình duyệt (hỗ trợ tua/Range khi là file trên máy chủ). */
    Resource load(String key);

    /** Xóa file (dùng khi thay file của đề còn Nháp). Không có file thì bỏ qua. */
    void delete(String key);
}
