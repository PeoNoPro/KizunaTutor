package com.kizuna.features.practice.storage;

import com.kizuna.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * FE-13: lưu file nghe (thư mục audio/) và ảnh (images/) luyện đề trên kho đám mây đang dùng (Cloudflare R2 hoặc kho Admin
 * chọn ở Cấu hình hệ thống), dùng chung S3Client / S3Presigner của hệ thống. Chưa có kho đám mây thì lưu trên máy chủ.
 * <ul>
 *   <li>Tải lên: chép theo luồng ra một tệp tạm (kiểm tra dung lượng, không giữ cả file 200 MB trong bộ nhớ), đẩy lên kho
 *       rồi xóa tệp tạm ngay. Không giữ bản đệm trên máy chủ.</li>
 *   <li>Phát / xem: trả đường dẫn ký sẵn của kho (có hạn) để trình duyệt tải thẳng từ R2 và vẫn tua được (Range),
 *       backend không phải đẩy dữ liệu file.</li>
 * </ul>
 * Không dùng {@code R2FileStore} của nhóm vì lớp đó đọc/ghi cả tệp bằng mảng byte trong bộ nhớ, không hợp với file nghe lớn.
 */
@Slf4j
@Primary
@Component
public class R2PracticeFileStorage implements PracticeFileStorage {
    private static final java.util.Set<String> AUDIO_EXT = java.util.Set.of("mp3", "m4a", "wav", "ogg", "webm");
    /** Đủ cho một lượt thi dài nhất (N1 khoảng 3 giờ) kể cả khi tạm dừng giữa chừng. */
    static final Duration LINK_TTL = Duration.ofHours(6);

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final LocalPracticeFileStorage local;

    @Autowired
    public R2PracticeFileStorage(ObjectProvider<S3Client> s3, ObjectProvider<S3Presigner> presigner,
                                 @Value("${app.r2.bucket:}") String bucket, LocalPracticeFileStorage local) {
        this(s3.getIfAvailable(), presigner.getIfAvailable(), bucket, local);
    }

    /** {@code s3} null (chưa cấu hình lưu trữ đám mây) thì lưu trên máy chủ như trước. */
    public R2PracticeFileStorage(S3Client s3, S3Presigner presigner, String bucket, LocalPracticeFileStorage local) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.local = local;
    }

    @Override
    public long save(String key, InputStream content, long maxBytes) {
        if (s3 == null) return local.save(key, content, maxBytes);
        Path temp = null;
        try {
            temp = Files.createTempFile("kizuna-practice-", ".part");
            long size = LocalPracticeFileStorage.copyLimited(content, temp, maxBytes);
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(r2Key(key)).contentType(contentType(key)).build(), RequestBody.fromFile(temp));
            return size;
        } catch (SdkException exception) {
            log.warn("Không tải được tệp luyện đề {} lên kho: {}", key, exception.getMessage());
            throw new BusinessException(503, "Chưa lưu được tệp lên kho lưu trữ. Vui lòng thử lại sau.");
        } catch (IOException exception) {
            throw new BusinessException(503, "Chưa lưu được tệp. Vui lòng thử lại sau.");
        } finally {
            if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { /* tệp tạm, bỏ qua */ }
        }
    }

    @Override
    public Optional<String> directUrl(String key) {
        if (s3 == null || presigner == null) return Optional.empty();
        try {
            var get = GetObjectRequest.builder().bucket(bucket).key(r2Key(key)).build();
            return Optional.of(presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(LINK_TTL).getObjectRequest(get).build())
                    .url().toString());
        } catch (SdkException exception) {
            log.warn("Không tạo được đường dẫn tải tệp luyện đề {}: {}", key, exception.getMessage());
            return Optional.empty();
        }
    }

    /** Chỉ dùng khi không ký được đường dẫn (thiếu presigner): backend đọc từ kho rồi trả cho trình duyệt. */
    @Override
    public Resource load(String key) {
        if (s3 == null) return local.load(key);
        try {
            return new InputStreamResource(s3.getObject(GetObjectRequest.builder().bucket(bucket).key(r2Key(key)).build()));
        } catch (NoSuchKeyException exception) {
            throw BusinessException.missing();
        } catch (SdkException exception) {
            log.warn("Không đọc được tệp luyện đề {} từ kho: {}", key, exception.getMessage());
            throw new BusinessException(503, "Chưa tải được tệp. Vui lòng thử lại sau.");
        }
    }

    @Override
    public void delete(String key) {
        if (s3 == null) { local.delete(key); return; }
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(r2Key(key)).build());
        } catch (SdkException exception) {
            log.warn("Không xóa được tệp luyện đề {} trên kho: {}", key, exception.getMessage());
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
            default -> "application/octet-stream";
        };
    }
}
