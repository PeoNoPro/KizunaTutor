package com.kizuna.features.practice.storage;

import com.kizuna.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * FE-13: lưu file nghe (thư mục audio/) và ảnh (images/) luyện đề trên kho đám mây đang dùng (Cloudflare R2 hoặc kho Admin
 * chọn ở Cấu hình hệ thống; chưa cấu hình kho nào thì chỉ lưu trên máy chủ), dùng chung S3Client của hệ thống.
 * Kho đám mây là nơi lưu chính; thư mục trên máy chủ chỉ làm bộ nhớ đệm để phát lại nhanh và vẫn tua được
 * (HTTP Range) với file nghe dài. Máy chủ mất bộ nhớ đệm thì lần đọc sau tự tải lại từ kho.
 * File cũ chỉ có trên máy chủ (tải lên trước khi bật R2) vẫn đọc được như trước.
 */
@Slf4j
@Primary
@Component
public class R2PracticeFileStorage implements PracticeFileStorage {
    private static final java.util.Set<String> AUDIO_EXT = java.util.Set.of("mp3", "m4a", "wav", "ogg", "webm");

    private final S3Client s3;
    private final String bucket;
    private final LocalPracticeFileStorage cache;

    @Autowired
    public R2PracticeFileStorage(ObjectProvider<S3Client> s3, @Value("${app.r2.bucket:}") String bucket, LocalPracticeFileStorage cache) {
        this(s3.getIfAvailable(), bucket, cache);
    }

    /** {@code s3} null (chưa cấu hình lưu trữ đám mây) thì chỉ lưu trên máy chủ như trước. */
    public R2PracticeFileStorage(S3Client s3, String bucket, LocalPracticeFileStorage cache) {
        this.s3 = s3;
        this.bucket = bucket;
        this.cache = cache;
    }

    @Override
    public long save(String key, InputStream content, long maxBytes) {
        // Ghi xuống máy chủ trước (kiểm tra dung lượng theo luồng, không giữ cả file trong bộ nhớ) rồi đẩy lên R2.
        long size = cache.save(key, content, maxBytes);
        if (s3 == null) return size;
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(r2Key(key)).contentType(contentType(key)).build(),
                    RequestBody.fromFile(cache.resolve(key)));
        } catch (SdkException exception) {
            log.warn("Không tải được tệp luyện đề {} lên R2: {}", key, exception.getMessage());
            cache.delete(key);
            throw new BusinessException(503, "Chưa lưu được tệp lên kho lưu trữ. Vui lòng thử lại sau.");
        }
        return size;
    }

    @Override
    public Resource load(String key) {
        Path path = cache.resolve(key);
        if (!Files.isRegularFile(path) && s3 != null) download(key, path);
        return cache.load(key);
    }

    private void download(String key, Path target) {
        Path temp = null;
        try {
            Files.createDirectories(target.getParent());
            temp = Files.createTempFile(target.getParent(), "r2-", ".part");
            try (InputStream in = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(r2Key(key)).build())) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            temp = null;
        } catch (NoSuchKeyException exception) {
            throw BusinessException.missing();
        } catch (SdkException | IOException exception) {
            log.warn("Không đọc được tệp luyện đề {} từ R2: {}", key, exception.getMessage());
            throw new BusinessException(503, "Chưa tải được tệp. Vui lòng thử lại sau.");
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { /* tệp tạm, bỏ qua */ }
        }
    }

    @Override
    public void delete(String key) {
        cache.delete(key);
        if (s3 == null) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(r2Key(key)).build());
        } catch (SdkException exception) {
            log.warn("Không xóa được tệp luyện đề {} trên R2: {}", key, exception.getMessage());
        }
    }

    /**
     * Theo cách đặt tên chung của nhóm trong bucket (thư mục theo loại, tên tệp là mã duy nhất):
     * file nghe vào {@code audio/<mã file>.mp3}, ảnh đề vào {@code images/<mã file>.png}.
     */
    static String r2Key(String key) {
        String name = key.substring(key.lastIndexOf('/') + 1);
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return (AUDIO_EXT.contains(ext) ? "audio/" : "images/") + name;
    }

    private static String contentType(String key) {
        String ext = key.substring(key.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (ext) {
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "webm" -> "audio/webm";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }
}
