package com.kizuna.features.progress.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.service.SystemSettingService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FE-15: gửi lời nhắc chấm bài cho AI theo cấu hình AI của Quản trị viên ({@link SystemSettingService#aiChoice()}).
 * Gemini gọi generateContent (ép JSON); OpenAI và dịch vụ chuẩn OpenAI gọi {baseUrl}/chat/completions.
 * Khóa API gửi qua header, không nằm trong URL hay log.
 */
@Component
public class ExerciseAiClient {

    /** Lỗi gọi AI: thông điệp ngắn, an toàn để lưu nội bộ (không chứa khóa). */
    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) {
            super(message);
        }
    }

    /** Nội dung AI trả về và model đã dùng. */
    public record Reply(String text, String model) {
    }

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 45_000;

    private final SystemSettingService settings;
    private final ObjectMapper mapper;
    private final RestTemplate http;

    public ExerciseAiClient(SystemSettingService settings, ObjectMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.http = new RestTemplate(factory);
    }

    public Reply complete(String prompt) {
        SystemSettingService.AiChoice ai = settings.aiChoice();
        if (ai == null || ai.apiKey() == null || ai.apiKey().isBlank()) {
            throw new AiUnavailableException("Chưa cấu hình khóa AI");
        }
        String model = ai.model() == null ? "" : ai.model().strip();
        if (!model.matches("[A-Za-z0-9._:/-]{1,80}")) {
            throw new AiUnavailableException("Tên model AI không hợp lệ");
        }
        try {
            String text = "GEMINI".equals(ai.provider())
                    ? callGemini(prompt, model, ai.apiKey())
                    : callOpenAiCompatible(prompt, model, ai.apiKey(), ai.baseUrl(), "OPENAI".equals(ai.provider()));
            if (text == null || text.isBlank()) {
                throw new AiUnavailableException("AI không trả nội dung");
            }
            return new Reply(text, ai.provider() + ":" + model);
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            // Chỉ giữ tên lớp lỗi: thông điệp lỗi HTTP có thể chứa phần thân phản hồi dài
            throw new AiUnavailableException("Gọi AI lỗi: " + e.getClass().getSimpleName());
        }
    }

    private String callGemini(String prompt, String model, String apiKey) throws Exception {
        if (model.contains("/")) {
            throw new AiUnavailableException("Tên model AI không hợp lệ");
        }
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", 0.2, "responseMimeType", "application/json"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        ResponseEntity<String> response = http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
        JsonNode parts = mapper.readTree(response.getBody()).path("candidates").path(0).path("content").path("parts");
        return parts.isArray() && !parts.isEmpty() ? parts.get(0).path("text").asText(null) : null;
    }

    private String callOpenAiCompatible(String prompt, String model, String apiKey, String baseUrl, boolean jsonMode)
            throws Exception {
        // Địa chỉ đã được kiểm tra khi Quản trị viên lưu cấu hình AI
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new AiUnavailableException("Địa chỉ dịch vụ AI không hợp lệ");
        }
        Map<String, Object> body = new HashMap<>(Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", List.of(Map.of("role", "user", "content", prompt))));
        if (jsonMode) body.put("response_format", Map.of("type", "json_object"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        ResponseEntity<String> response = http.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
        JsonNode content = mapper.readTree(response.getBody()).path("choices").path(0).path("message").path("content");
        return content.isMissingNode() ? null : content.asText(null);
    }
}
