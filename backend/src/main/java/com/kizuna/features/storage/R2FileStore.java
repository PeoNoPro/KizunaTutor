package com.kizuna.features.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Collection;
import java.util.List;

/**
 * Nội dung tệp (tài liệu, đề bài, bài nộp, đính kèm hỗ trợ, giấy tờ xác minh) trên Cloudflare R2.
 * Bật R2 (app.r2.enabled=true): tệp mới ghi lên R2, cột BYTEA trong DB để NULL. Tắt R2: các service giữ cách lưu DB cũ.
 * Tệp cũ còn trong DB vẫn đọc từ DB trước, nên không cần chuyển dữ liệu cũ ngay.
 */
@Slf4j
@Component
public class R2FileStore {

    private final S3Client s3;
    private final String bucket;

    @Autowired
    public R2FileStore(ObjectProvider<S3Client> s3, @Value("${app.r2.bucket:}") String bucket) {
        this(s3.getIfAvailable(), bucket);
    }

    public R2FileStore(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    /** Store tắt R2: các service dùng trong test / khi chạy không cấu hình R2. */
    public static R2FileStore disabled() {
        return new R2FileStore((S3Client) null, null);
    }

    public boolean enabled() {
        return s3 != null;
    }

    /**
     * Ghi tệp lên R2 khi bật; trả nội dung cần ghi vào cột DB (null khi đã lên R2, giữ nguyên khi tắt R2).
     * Lỗi R2 ném ra ngoài để transaction rollback, không lưu bản ghi thiếu tệp.
     */
    public byte[] put(String key, byte[] content, String contentType) {
        if (s3 == null) {
            return content;
        }
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(content));
        return null;
    }

    /** Nội dung từ DB nếu có (tệp cũ), nếu không thì đọc từ R2; không có ở đâu thì null. */
    public byte[] read(byte[] fromDb, String key) {
        if (fromDb != null && fromDb.length > 0) {
            return fromDb;
        }
        if (s3 == null) {
            return null;
        }
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (SdkException e) {
            log.warn("Không đọc được tệp {} từ R2: {}", key, e.getMessage());
            return null;
        }
    }

    /** Xóa tệp trên R2 sau khi transaction commit (rollback thì tệp vẫn còn cho bản ghi cũ). */
    public void deleteAfterCommit(Collection<String> keys) {
        if (s3 == null || keys.isEmpty()) {
            return;
        }
        List<String> copy = List.copyOf(keys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    copy.forEach(R2FileStore.this::deleteQuietly);
                }
            });
        } else {
            copy.forEach(this::deleteQuietly);
        }
    }

    private void deleteQuietly(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("Không xóa được tệp {} trên R2: {}", key, e.getMessage());
        }
    }
}
