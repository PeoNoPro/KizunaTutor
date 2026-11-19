package com.kizuna.features.storage;

import com.kizuna.core.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.r2", name = "enabled", havingValue = "true")
public class R2StorageService {
    private static final long MAX_AUDIO_BYTES = 25L * 1024 * 1024;
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "m4a", "webm", "mpeg", "mpga");

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final long urlTtlSeconds;

    public R2StorageService(S3Client s3, S3Presigner presigner,
                            @Value("${app.r2.bucket}") String bucket,
                            @Value("${app.r2.presigned-url-ttl-seconds:900}") long urlTtlSeconds) {
        if (bucket == null || bucket.isBlank()) throw new IllegalArgumentException("R2_BUCKET must be configured when R2 is enabled.");
        if (urlTtlSeconds < 1 || urlTtlSeconds > 604800) throw new IllegalArgumentException("R2 presigned URL TTL must be between 1 second and 7 days.");
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.urlTtlSeconds = urlTtlSeconds;
    }

    public UploadedAudio uploadAudio(MultipartFile file) {
        // Lỗi do người dùng gửi sai → BadRequestException (HTTP 400 kèm thông báo); IllegalArgumentException sẽ thành 503
        if (file == null || file.isEmpty()) throw new BadRequestException("Chọn tệp âm thanh trước khi tải lên.");
        if (file.getSize() > MAX_AUDIO_BYTES) throw new BadRequestException("Tệp âm thanh tối đa 25 MB.");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!AUDIO_EXTENSIONS.contains(extension)) throw new BadRequestException("Chỉ chấp nhận tệp MP3, WAV, OGG, M4A hoặc WEBM.");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            contentType = switch (extension) {
                case "mp3", "mpeg", "mpga" -> "audio/mpeg";
                case "wav" -> "audio/wav";
                case "ogg" -> "audio/ogg";
                case "m4a" -> "audio/mp4";
                default -> "audio/webm";
            };
        }
        String key = "audio/" + UUID.randomUUID() + "." + extension;
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new BadRequestException("Không đọc được tệp âm thanh. Vui lòng chọn lại tệp.");
        }
        return new UploadedAudio(key, contentType, presignedGetUrl(key));
    }

    public String presignedGetUrl(String key) {
        if (key == null || !key.startsWith("audio/") || key.contains("..")) {
            throw new BadRequestException("Mã tệp âm thanh không hợp lệ.");
        }
        var request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(urlTtlSeconds))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(request).url().toString();
    }

    public record UploadedAudio(String objectKey, String contentType, String downloadUrl) {}
}
