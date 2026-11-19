package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chép toàn bộ tệp từ kho cũ (trước lần đổi gần nhất) sang kho đang dùng, chạy nền và báo tiến độ.
 * Chỉ chép, không xóa gì ở kho cũ; tệp đã có ở kho mới (cùng tên, cùng dung lượng) thì bỏ qua nên bấm lại an toàn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageCopyService {
    private final CloudStorageSettingService storage;
    private final OperationsActorResolver actors;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Map<String, Object> status = Map.of("state", "IDLE");

    public Map<String, Object> status() {
        actors.requireAdmin();
        return status;
    }

    public Map<String, Object> start() {
        actors.requireAdmin();
        var from = storage.previous().orElseThrow(() -> new BusinessException(400, "Chưa có kho cũ để chép (chưa đổi kho lần nào)."));
        var to = storage.target().orElseThrow(() -> new BusinessException(400, "Chưa cấu hình kho lưu trữ đang dùng."));
        if (from.endpoint().equalsIgnoreCase(to.endpoint()) && from.bucket().equals(to.bucket()))
            throw new BusinessException(400, "Kho cũ và kho mới là một.");
        if (!running.compareAndSet(false, true)) throw new BusinessException(409, "Đang chép tệp, vui lòng chờ xong.");
        status = progress("RUNNING", from, to, 0, 0, 0, 0, null);
        var worker = new Thread(() -> copy(from, to), "cloud-storage-copy");
        worker.setDaemon(true);
        worker.start();
        return status;
    }

    private Map<String, Object> progress(String state, CloudStorageSettingService.Target from, CloudStorageSettingService.Target to,
                                         int scanned, int copied, int skipped, int failed, String message) {
        var map = new LinkedHashMap<String, Object>();
        map.put("state", state); map.put("from", from.label()); map.put("to", to.label());
        map.put("scanned", scanned); map.put("copied", copied); map.put("skipped", skipped); map.put("failed", failed);
        map.put("updatedAt", OffsetDateTime.now().toString());
        if (message != null) map.put("message", message);
        return map;
    }

    private void copy(CloudStorageSettingService.Target from, CloudStorageSettingService.Target to) {
        int scanned = 0, copied = 0, skipped = 0, failed = 0;
        try (S3Client source = CloudStorageSettingService.build(from); S3Client target = CloudStorageSettingService.build(to)) {
            for (var object : source.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(from.bucket()).build()).contents()) {
                scanned++;
                if (object.key().startsWith("kizuna-connection-test/")) { skipped++; continue; }
                try {
                    if (exists(target, to.bucket(), object)) skipped++;
                    else { transfer(source, from.bucket(), target, to.bucket(), object.key()); copied++; }
                } catch (SdkException | IOException exception) {
                    failed++;
                    log.warn("[STORAGE] Chưa chép được tệp {}: {}", object.key(), exception.getClass().getSimpleName());
                }
                status = progress("RUNNING", from, to, scanned, copied, skipped, failed, null);
            }
            status = progress("DONE", from, to, scanned, copied, skipped, failed,
                    failed == 0 ? "Đã chép xong." : "Đã chép xong, còn " + failed + " tệp lỗi. Bấm chép lại để thử các tệp đó.");
        } catch (SdkException exception) {
            log.warn("[STORAGE] Dừng chép tệp: {}", exception.getClass().getSimpleName());
            status = progress("FAILED", from, to, scanned, copied, skipped, failed, "Mất kết nối với kho cũ hoặc kho mới. Đã chép được " + copied + " tệp; bấm chép lại để tiếp tục.");
        } finally {
            running.set(false);
        }
    }

    private static boolean exists(S3Client target, String bucket, S3Object object) {
        try { return target.headObject(HeadObjectRequest.builder().bucket(bucket).key(object.key()).build()).contentLength() == object.size(); }
        catch (NoSuchKeyException exception) { return false; }
        catch (S3Exception exception) { if (exception.statusCode() == 404) return false; throw exception; }
    }

    /** Qua tệp tạm trên đĩa để không giữ cả tệp lớn (file nghe tới 200 MB) trong bộ nhớ. */
    private static void transfer(S3Client source, String fromBucket, S3Client target, String toBucket, String key) throws IOException {
        Path temp = Files.createTempFile("kizuna-copy-", ".part");
        try {
            String contentType;
            try (var in = source.getObject(GetObjectRequest.builder().bucket(fromBucket).key(key).build())) {
                contentType = in.response().contentType();
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            var put = PutObjectRequest.builder().bucket(toBucket).key(key);
            if (contentType != null && !contentType.isBlank()) put.contentType(contentType);
            target.putObject(put.build(), RequestBody.fromFile(temp));
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
