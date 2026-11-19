package com.kizuna.features.practice.storage;

import com.kizuna.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;

/**
 * Lưu file luyện đề trong thư mục trên máy chủ ({@code app.practice.storage-dir}). Khóa file do hệ thống tự tạo
 * (exam/&lt;mã đề&gt;/&lt;mã file&gt;.mp3) nên không nhận đường dẫn từ người dùng.
 */
@Component
public class LocalPracticeFileStorage implements PracticeFileStorage {
    private final Path root;

    public LocalPracticeFileStorage(@Value("${app.practice.storage-dir:${user.home}/kizuna-data/practice-files}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Đường dẫn thật của khóa trên máy chủ. */
    Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Khóa file không hợp lệ.");
        return path;
    }

    @Override
    public long save(String key, InputStream content, long maxBytes) {
        Path target = resolve(key), temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), "upload-", ".part");
            long total = copyLimited(content, temp, maxBytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            temp = null;
            return total;
        } catch (IOException exception) {
            throw new BusinessException(503, "Chưa lưu được tệp. Vui lòng thử lại sau.");
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { /* tệp tạm, bỏ qua */ }
        }
    }

    /** Chép luồng vào tệp, dừng và báo lỗi ngay khi vượt {@code maxBytes} (không giữ cả tệp trong bộ nhớ). */
    static long copyLimited(InputStream content, Path target, long maxBytes) throws IOException {
        long total = 0;
        try (InputStream in = content; OutputStream out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = in.read(buffer)) != -1; ) {
                total += read;
                if (total > maxBytes) throw new BusinessException(413, "Tệp vượt dung lượng cho phép (tối đa " + (maxBytes / 1_000_000) + " MB).");
                out.write(buffer, 0, read);
            }
        }
        if (total == 0) throw new BusinessException(400, "Tệp rỗng, vui lòng chọn lại.");
        return total;
    }

    @Override
    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) throw BusinessException.missing();
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); } catch (IOException ignored) { /* để lại tệp mồ côi còn hơn làm hỏng thao tác */ }
    }
}
