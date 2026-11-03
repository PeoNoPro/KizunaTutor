package com.kizuna.features.classroom.service.impl;

import com.kizuna.features.classroom.exception.ClassroomException;
import com.kizuna.features.classroom.service.MaterialStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Lưu tệp tài liệu vào Supabase Storage qua REST API (bucket riêng tư).
 * Khóa dịch vụ chỉ đọc từ biến môi trường SUPABASE_SERVICE_ROLE_KEY, không bao giờ được log hoặc trả về client.
 */
@Slf4j
@Service
public class SupabaseMaterialStorageService implements MaterialStorageService {

    private final String baseUrl;
    private final String serviceKey;
    private final String bucket;
    private final int ttlSeconds;
    private final RestClient restClient;

    public SupabaseMaterialStorageService(
            @Value("${app.storage.supabase.url:}") String url,
            @Value("${app.storage.supabase.service-key:}") String serviceKey,
            @Value("${app.storage.supabase.bucket:learning-materials}") String bucket,
            @Value("${app.storage.supabase.signed-url-ttl-seconds:300}") int ttlSeconds) {
        this.baseUrl = url == null ? "" : url.trim().replaceAll("/+$", "");
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.bucket = bucket;
        this.ttlSeconds = ttlSeconds;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isEmpty() && !serviceKey.isEmpty();
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            log.warn("Supabase Storage chưa được cấu hình (thiếu SUPABASE_URL hoặc SUPABASE_SERVICE_ROLE_KEY).");
            throw ClassroomException.storageUnavailable();
        }
    }

    private String storageUrl(String suffix) {
        return baseUrl + "/storage/v1" + suffix;
    }

    @Override
    public void upload(String objectPath, byte[] content, String contentType) {
        ensureConfigured();
        try {
            restClient.post()
                    .uri(storageUrl("/object/" + bucket + "/" + objectPath))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Tải tệp lên Supabase Storage thất bại: {}", e.getClass().getSimpleName());
            throw ClassroomException.storageUnavailable();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String createSignedUrl(String objectPath) {
        ensureConfigured();
        try {
            Map<String, Object> body = restClient.post()
                    .uri(storageUrl("/object/sign/" + bucket + "/" + objectPath))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", ttlSeconds))
                    .retrieve()
                    .body(Map.class);
            Object signed = body == null ? null : body.get("signedURL");
            if (signed == null) signed = body == null ? null : body.get("signedUrl");
            if (signed == null) throw ClassroomException.storageUnavailable();
            String path = signed.toString();
            return path.startsWith("http") ? path : storageUrl(path.startsWith("/") ? path : "/" + path);
        } catch (RestClientException e) {
            log.error("Tạo liên kết tạm thời Supabase Storage thất bại: {}", e.getClass().getSimpleName());
            throw ClassroomException.storageUnavailable();
        }
    }

    @Override
    public int signedUrlTtlSeconds() {
        return ttlSeconds;
    }

    @Override
    public void delete(String objectPath) {
        if (!isConfigured() || objectPath == null || objectPath.isBlank()) return;
        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(storageUrl("/object/" + bucket))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(objectPath)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Không làm hỏng nghiệp vụ vì lỗi dọn tệp; chỉ ghi cảnh báo để xử lý thủ công.
            log.warn("Không xóa được tệp cũ trên Supabase Storage: {}", objectPath);
        }
    }
}
