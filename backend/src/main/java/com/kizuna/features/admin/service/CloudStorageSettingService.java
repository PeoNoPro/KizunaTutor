package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * Lưu trữ đám mây (chuẩn S3: Cloudflare R2, Amazon S3, Backblaze B2, Wasabi, MinIO…) do Admin cấu hình ở trang
 * Cấu hình hệ thống. Khóa truy cập mã hóa như API key AI, không bao giờ trả ra giao diện hay ghi log.
 * Admin chưa cấu hình thì dùng cấu hình server (app.r2.*, đặt bằng biến môi trường R2_*).
 * Mọi chức năng lưu tệp dùng chung một S3Client chuyển đổi (xem CloudStorageConfiguration) nên đổi ở đây là áp dụng ngay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageSettingService {
    public static final String AUDIT_KEY = "CLOUD_STORAGE";
    private static final String PROVIDER = "STORAGE_PROVIDER", ENDPOINT = "STORAGE_ENDPOINT", REGION = "STORAGE_REGION", BUCKET = "STORAGE_BUCKET",
            ACCESS = "STORAGE_ACCESS_KEY", SECRET = "STORAGE_SECRET_KEY", PREVIOUS = "STORAGE_PREVIOUS_";
    public static final List<String> PROVIDERS = List.of("R2", "S3", "B2", "WASABI", "OTHER");
    private static final Map<String, String> LABELS = Map.of("R2", "Cloudflare R2", "S3", "Amazon S3", "B2", "Backblaze B2",
            "WASABI", "Wasabi", "OTHER", "Dịch vụ khác chuẩn S3 (MinIO…)");
    private static final Duration CACHE = Duration.ofSeconds(30);

    private final SystemSettingService settings;
    private final OperationsActorResolver actors;

    @Value("${app.r2.enabled:false}") private boolean serverEnabled;
    @Value("${app.r2.endpoint:}") private String serverEndpoint;
    @Value("${app.r2.region:auto}") private String serverRegion;
    @Value("${app.r2.bucket:}") private String serverBucket;
    @Value("${app.r2.access-key:}") private String serverAccessKey;
    @Value("${app.r2.secret-key:}") private String serverSecretKey;

    /** Đã dựng S3Client chuyển đổi lúc khởi động hay chưa (chưa thì lần bật đầu tiên cần khởi động lại backend). */
    private volatile boolean runtimeActive;
    private volatile Target cached;
    private volatile long cachedAt;
    /** Kho cũ để đọc dự phòng (null = chưa đọc DB); làm mới tối đa mỗi 30 giây như kho đang dùng. */
    private volatile Optional<Target> cachedPrevious;
    private volatile long previousAt;

    /** Kho đang dùng. Khóa chỉ dùng trong server; toString không in khóa. */
    public record Target(String provider, String endpoint, String region, String bucket, String accessKey, String secretKey, String source) {
        @Override public String toString() { return "Target[provider=" + provider + ", endpoint=" + endpoint + ", bucket=" + bucket + ", source=" + source + "]"; }
        public String fingerprint() { return provider + "|" + endpoint + "|" + region + "|" + bucket + "|" + accessKey + "|" + secretKey.hashCode(); }
        public String label() { return LABELS.getOrDefault(provider, provider) + " · " + bucket; }
    }

    public record StorageUpdate(
            @NotBlank(message = "Vui lòng chọn nhà cung cấp.") @Pattern(regexp = "R2|S3|B2|WASABI|OTHER", message = "Nhà cung cấp chưa được hỗ trợ.") String provider,
            @NotBlank(message = "Vui lòng nhập địa chỉ endpoint.") @Size(max = 200, message = "Endpoint tối đa 200 ký tự.") String endpoint,
            @Size(max = 40, message = "Vùng (region) tối đa 40 ký tự.") @Pattern(regexp = "^$|^[a-z0-9-]+$", message = "Vùng (region) chỉ gồm chữ thường, số và dấu -.") String region,
            @NotBlank(message = "Vui lòng nhập tên bucket.") @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9.-]{1,61}[A-Za-z0-9]", message = "Tên bucket gồm 3–63 ký tự chữ, số, dấu . hoặc - (Backblaze cho phép chữ hoa).") String bucket,
            /** Để trống thì giữ khóa đang có. */
            @Size(max = 200, message = "Access key tối đa 200 ký tự.") @Pattern(regexp = "^$|^[\\x21-\\x7E]+$", message = "Access key không được có khoảng trắng.") String accessKey,
            @Size(max = 200, message = "Secret key tối đa 200 ký tự.") @Pattern(regexp = "^$|^[\\x21-\\x7E]+$", message = "Secret key không được có khoảng trắng.") String secretKey,
            @NotNull(message = "Vui lòng tải lại dữ liệu.") Long expectedVersion,
            @NotBlank(message = "Vui lòng nhập lý do.") @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.") String reason) { }

    public record StorageTest(
            @NotBlank(message = "Vui lòng chọn nhà cung cấp.") @Pattern(regexp = "R2|S3|B2|WASABI|OTHER", message = "Nhà cung cấp chưa được hỗ trợ.") String provider,
            @NotBlank(message = "Vui lòng nhập địa chỉ endpoint.") @Size(max = 200) String endpoint,
            @Size(max = 40) String region,
            @NotBlank(message = "Vui lòng nhập tên bucket.") @Size(max = 63) String bucket,
            @Size(max = 200) String accessKey,
            @Size(max = 200) String secretKey) { }

    public void markRuntimeActive() { runtimeActive = true; }

    // ---- Đọc cấu hình ----

    private Optional<Target> fromDb(String prefix) {
        var bucket = settings.value(prefix + BUCKET);
        if (bucket.isEmpty()) return Optional.empty();
        var access = settings.value(prefix + ACCESS).map(settings::decrypt).orElse("");
        var secret = settings.value(prefix + SECRET).map(settings::decrypt).orElse("");
        if (access.isBlank() || secret.isBlank()) return Optional.empty();
        return Optional.of(new Target(settings.value(prefix + PROVIDER).orElse("OTHER"), settings.value(prefix + ENDPOINT).orElse(""),
                settings.value(prefix + REGION).orElse("auto"), bucket.get(), access, secret, prefix.isEmpty() ? "admin" : "previous"));
    }

    private Optional<Target> fromServer() {
        if (!serverEnabled || isBlank(serverEndpoint) || isBlank(serverBucket) || isBlank(serverAccessKey) || isBlank(serverSecretKey)) return Optional.empty();
        return Optional.of(new Target("R2", serverEndpoint.trim(), isBlank(serverRegion) ? "auto" : serverRegion.trim(), serverBucket.trim(),
                serverAccessKey.trim(), serverSecretKey.trim(), "server"));
    }

    private Optional<Target> load() { return fromDb("").or(this::fromServer); }

    /**
     * Kho đang dùng cho mọi thao tác tệp (đọc lại DB tối đa mỗi 30 giây, lưu cấu hình thì cập nhật ngay).
     * Lỗi đọc DB thì giữ kho đã biết, chưa biết thì dùng cấu hình server.
     */
    @Transactional(readOnly = true, timeout = 3)
    public Optional<Target> target() {
        var now = System.currentTimeMillis();
        var known = cached;
        if (known != null && now - cachedAt < CACHE.toMillis()) return Optional.of(known);
        try {
            var fresh = load();
            cached = fresh.orElse(null); cachedAt = now;
            return fresh;
        } catch (RuntimeException exception) {
            log.warn("[STORAGE] Chưa đọc được cấu hình lưu trữ: {}", exception.getClass().getSimpleName());
            return known != null ? Optional.of(known) : fromServer();
        }
    }

    /** Kho cũ (trước lần đổi gần nhất) để chép tệp sang kho mới. */
    @Transactional(readOnly = true, timeout = 3)
    public Optional<Target> previous() { return fromDb(PREVIOUS); }

    /**
     * Kho cũ dùng để ĐỌC DỰ PHÒNG: tệp chưa chép sang kho mới thì vẫn mở được (ảnh đại diện, tài liệu, bài nộp…).
     * Trùng kho đang dùng thì coi như không có. Lỗi đọc DB thì bỏ qua dự phòng, không làm hỏng thao tác chính.
     */
    @Transactional(readOnly = true, timeout = 3)
    public Optional<Target> fallback() {
        var now = System.currentTimeMillis();
        var known = cachedPrevious;
        if (known == null || now - previousAt >= CACHE.toMillis()) {
            try { known = fromDb(PREVIOUS); }
            catch (RuntimeException exception) { known = Optional.empty(); }
            cachedPrevious = known; previousAt = now;
        }
        var current = target();
        return known.filter(old -> current.map(t -> !t.fingerprint().equals(old.fingerprint())).orElse(true));
    }

    @Transactional(readOnly = true, timeout = 3)
    public Map<String, Object> view() {
        actors.requireAdmin();
        var current = load();
        var result = new LinkedHashMap<String, Object>();
        result.put("providers", PROVIDERS.stream().map(code -> Map.of("code", code, "label", LABELS.get(code))).toList());
        result.put("configured", current.isPresent());
        result.put("source", current.map(Target::source).orElse("none"));
        result.put("provider", current.map(Target::provider).orElse("R2"));
        result.put("providerLabel", current.map(t -> LABELS.getOrDefault(t.provider(), t.provider())).orElse(""));
        result.put("endpoint", current.map(Target::endpoint).orElse(""));
        result.put("region", current.map(Target::region).orElse("auto"));
        result.put("bucket", current.map(Target::bucket).orElse(""));
        result.put("maskedAccessKey", current.map(t -> SystemSettingService.mask(t.accessKey())).orElse(""));
        result.put("active", runtimeActive);
        result.put("previous", previous().map(Target::label).orElse(""));
        result.put("serverDefault", fromServer().map(Target::label).orElse(""));
        // Tải file nghe của gia sư (R2AudioController/R2StorageService của nhóm) chỉ bật khi server đặt R2_ENABLED=true.
        result.put("serverR2Enabled", serverEnabled);
        result.put("version", settings.version(AUDIT_KEY));
        return result;
    }

    // ---- Thử kết nối và lưu ----

    /** Ghi, đọc rồi xóa một tệp thử nhỏ để chắc chắn khóa có quyền đọc/ghi bucket. Thông báo lỗi không chứa khóa. */
    static Map<String, Object> probe(Target target) {
        long started = System.nanoTime();
        var key = "kizuna-connection-test/" + UUID.randomUUID() + ".txt";
        try (var client = build(target)) {
            client.putObject(PutObjectRequest.builder().bucket(target.bucket()).key(key).contentType("text/plain").build(), RequestBody.fromString("ok"));
            client.getObjectAsBytes(GetObjectRequest.builder().bucket(target.bucket()).key(key).build());
            client.deleteObject(DeleteObjectRequest.builder().bucket(target.bucket()).key(key).build());
            return Map.of("ok", true, "message", "Kết nối thành công: đọc, ghi được bucket " + target.bucket() + " trên " + LABELS.getOrDefault(target.provider(), target.provider()) + ".",
                    "latencyMs", (System.nanoTime() - started) / 1_000_000);
        } catch (S3Exception exception) {
            int status = exception.statusCode();
            var reason = status == 403 || status == 401 ? "Access key / secret key sai hoặc không có quyền đọc ghi bucket này."
                    : status == 404 ? "Không tìm thấy bucket " + target.bucket() + ". Kiểm tra tên bucket và endpoint."
                    : status == 301 || status == 400 ? "Sai vùng (region) hoặc endpoint không khớp với bucket."
                    : "Kho lưu trữ từ chối yêu cầu (mã " + status + ").";
            return Map.of("ok", false, "message", reason, "latencyMs", (System.nanoTime() - started) / 1_000_000);
        } catch (SdkException | IllegalArgumentException exception) {
            return Map.of("ok", false, "message", "Không kết nối được tới endpoint. Kiểm tra địa chỉ và kết nối mạng của server.", "latencyMs", (System.nanoTime() - started) / 1_000_000);
        }
    }

    /** Dựng S3Client cho một kho. Amazon S3 dùng dạng địa chỉ theo tên miền bucket, các dịch vụ khác dùng dạng đường dẫn. */
    public static S3Client build(Target target) {
        return S3Client.builder()
                .endpointOverride(URI.create(target.endpoint()))
                .region(Region.of(isBlank(target.region()) ? "auto" : target.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(target.accessKey(), target.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(!"S3".equals(target.provider())).build())
                .build();
    }

    /** Chỉ nhận https (hoặc máy local khi chạy thử MinIO) để khóa không đi qua kết nối không mã hóa. */
    static String checkEndpoint(String raw) {
        var url = raw.trim().replaceAll("/+$", "");
        if (!url.matches("https://[A-Za-z0-9.-]+(:\\d+)?") && !url.matches("http://(localhost|127\\.0\\.0\\.1)(:\\d+)?"))
            throw new BusinessException(400, "Endpoint phải có dạng https://tên-miền, ví dụ https://<account-id>.r2.cloudflarestorage.com.");
        return url;
    }

    private Target planned(String provider, String endpoint, String region, String bucket, String accessKey, String secretKey) {
        var current = load();
        var url = checkEndpoint(endpoint);
        var access = isBlank(accessKey) ? current.map(Target::accessKey).orElse("") : accessKey.trim();
        var secret = isBlank(secretKey) ? current.map(Target::secretKey).orElse("") : secretKey.trim();
        if (access.isBlank() || secret.isBlank()) throw new BusinessException(400, "Vui lòng nhập access key và secret key.");
        // Đổi sang nhà cung cấp / endpoint khác mà để trống khóa thì gần như chắc chắn sai: khóa gắn với từng tài khoản.
        boolean otherAccount = current.map(t -> !t.endpoint().equalsIgnoreCase(url)).orElse(true);
        if (otherAccount && (isBlank(accessKey) || isBlank(secretKey)))
            throw new BusinessException(400, "Đổi sang endpoint khác thì phải nhập access key và secret key của tài khoản mới.");
        return new Target(provider, url, isBlank(region) ? "auto" : region.trim(), bucket.trim(), access, secret, "admin");
    }

    @Transactional(readOnly = true, timeout = 40)
    public Map<String, Object> test(StorageTest r) {
        actors.requireAdmin();
        try { return probe(planned(r.provider(), r.endpoint(), r.region(), r.bucket(), r.accessKey(), r.secretKey())); }
        catch (BusinessException exception) { return Map.of("ok", false, "message", exception.getMessage(), "latencyMs", 0); }
    }

    /** Thử kết nối trước khi lưu: cấu hình sai sẽ làm mọi chức năng tải tệp của hệ thống lỗi. */
    @Transactional(timeout = 45)
    public Map<String, Object> update(StorageUpdate r) {
        actors.requireAdmin();
        var next = planned(r.provider(), r.endpoint(), r.region(), r.bucket(), r.accessKey(), r.secretKey());
        var before = load();
        if (before.isPresent() && before.get().fingerprint().equals(next.fingerprint()))
            throw new BusinessException(400, "Cấu hình mới trùng với cấu hình hiện hành.");
        var check = probe(next);
        if (!Boolean.TRUE.equals(check.get("ok"))) throw new BusinessException(400, "Chưa lưu: " + check.get("message"));
        var actor = settings.lockFor(AUDIT_KEY, r.expectedVersion());
        // Kho cũ khác bucket/endpoint thì giữ lại để Admin chép tệp sang kho mới.
        before.filter(old -> !old.endpoint().equalsIgnoreCase(next.endpoint()) || !old.bucket().equals(next.bucket())).ifPresent(old -> write(PREVIOUS, old));
        write("", next);
        var beforeLabel = before.map(Target::label).orElse("");
        var afterLabel = next.label() + (isBlank(r.accessKey()) ? "" : " · đổi khóa truy cập");
        settings.save(PROVIDER, "STRING", "Nhà cung cấp lưu trữ đám mây", before.map(Target::provider).orElse(""), next.provider(),
                actor.userId(), r.reason(), AUDIT_KEY, beforeLabel, afterLabel);
        cached = next; cachedAt = System.currentTimeMillis();
        cachedPrevious = null;
        return view();
    }

    public record StorageReset(
            @NotNull(message = "Vui lòng tải lại dữ liệu.") Long expectedVersion,
            @NotBlank(message = "Vui lòng nhập lý do.") @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.") String reason) { }

    /**
     * Bỏ kho Admin đã chọn, quay về kho trong cấu hình server (biến môi trường R2_*, như lúc nhóm cài đặt ban đầu).
     * Kho Admin vừa bỏ được giữ làm "kho cũ" để chép tệp tải lên trong thời gian dùng nó về lại kho mặc định.
     */
    @Transactional(timeout = 10)
    public Map<String, Object> resetToServer(StorageReset r) {
        actors.requireAdmin();
        var before = fromDb("");
        if (before.isEmpty()) throw new BusinessException(400, "Đang dùng cấu hình mặc định của server rồi.");
        var server = fromServer().orElseThrow(() -> new BusinessException(400, "Server chưa có cấu hình lưu trữ mặc định (biến môi trường R2_*), chưa quay về được."));
        var actor = settings.lockFor(AUDIT_KEY, r.expectedVersion());
        if (!before.get().endpoint().equalsIgnoreCase(server.endpoint()) || !before.get().bucket().equals(server.bucket())) write(PREVIOUS, before.get());
        for (var key : List.of(PROVIDER, ENDPOINT, REGION, BUCKET, ACCESS, SECRET)) settings.upsert(key, "", "Lưu trữ đám mây: dùng cấu hình server");
        settings.save(PROVIDER, "STRING", "Nhà cung cấp lưu trữ đám mây", before.get().provider(), "",
                actor.userId(), r.reason(), AUDIT_KEY, before.get().label(), server.label() + " (cấu hình mặc định của server)");
        cached = server; cachedAt = System.currentTimeMillis();
        cachedPrevious = null;
        return view();
    }

    private void write(String prefix, Target target) {
        settings.upsert(prefix + PROVIDER, target.provider(), "Nhà cung cấp lưu trữ đám mây");
        settings.upsert(prefix + ENDPOINT, target.endpoint(), "Endpoint lưu trữ đám mây");
        settings.upsert(prefix + REGION, target.region(), "Vùng lưu trữ đám mây");
        settings.upsert(prefix + BUCKET, target.bucket(), "Bucket lưu trữ đám mây");
        settings.upsert(prefix + ACCESS, settings.encrypt(target.accessKey()), "Access key lưu trữ đám mây (đã mã hóa)");
        settings.upsert(prefix + SECRET, settings.encrypt(target.secretKey()), "Secret key lưu trữ đám mây (đã mã hóa)");
    }

    private static boolean isBlank(String text) { return text == null || text.isBlank(); }
}
