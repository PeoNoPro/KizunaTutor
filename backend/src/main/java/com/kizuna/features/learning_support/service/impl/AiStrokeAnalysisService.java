package com.kizuna.features.learning_support.service.impl;


import com.kizuna.core.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.learning_support.dto.ScoreResult;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Service tích hợp Trí tuệ Nhân tạo (Google Gemini / OpenAI)
 * đóng vai trò Kizuna Sensei (Chuyên gia Thư pháp Nhật Bản & Giảng viên Kanji)
 * nhận xét nét viết học viên theo mô hình thuần văn bản (Text-Only Prompting)
 * dựa trên các chỉ số động học tọa độ (x, y, t) và hình học Topo Vector.
 */
@Service
@Slf4j
public class AiStrokeAnalysisService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${app.ai.provider:GEMINI}")
    private String defaultProvider;

    @Value("${app.ai.gemini-api-key:}")
    private String configuredGeminiKey;

    @Value("${app.ai.gemini-model:gemini-3.8-flash}")
    private String configuredGeminiModel;

    @Value("${app.ai.openai-api-key:}")
    private String configuredOpenAiKey;

    public AiStrokeAnalysisService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Kiểm tra trạng thái cấu hình AI của máy chủ (để trả về cho frontend)
     */
    public Map<String, Object> getAiStatus() {
        boolean hasServerGemini = (configuredGeminiKey != null && !configuredGeminiKey.isBlank())
                || (System.getenv("GEMINI_API_KEY") != null && !System.getenv("GEMINI_API_KEY").isBlank());
        boolean hasServerOpenAi = (configuredOpenAiKey != null && !configuredOpenAiKey.isBlank())
                || (System.getenv("OPENAI_API_KEY") != null && !System.getenv("OPENAI_API_KEY").isBlank());

        return Map.of(
                "hasServerKey", hasServerGemini || hasServerOpenAi,
                "provider", defaultProvider,
                "geminiModel", configuredGeminiModel,
                "hasGeminiKey", hasServerGemini,
                "hasOpenAiKey", hasServerOpenAi
        );
    }

    /**
     * Phân tích và sinh lời nhận xét thư pháp từ AI Sensei (Thuần văn bản - Text Only)
     */
    public String generateAiSenseiFeedback(
            KanjiDictionary kanji,
            ScoreResult vectorResult,
            String clientApiKey,
            String clientProvider,
            String base64ImageData) {

        String provider = (clientProvider != null && !clientProvider.isBlank()) ? clientProvider.toUpperCase() : defaultProvider.toUpperCase();
        String effectiveKey = resolveApiKey(clientApiKey, provider);

        if (effectiveKey == null || effectiveKey.isBlank()) {
            return buildFallbackSenseiNote(kanji, vectorResult);
        }

        try {
            if ("OPENAI".equalsIgnoreCase(provider)) {
                return callOpenAiSensei(kanji, vectorResult, effectiveKey);
            } else {
                return callGeminiSensei(kanji, vectorResult, effectiveKey);
            }
        } catch (Exception e) {
            log.warn("[Kizuna Sensei] Lỗi khi gọi AI API ({}: {}). Sử dụng nhận xét kỹ thuật dự phòng.", provider, e.getMessage());
            return buildFallbackSenseiNote(kanji, vectorResult);
        }
    }

    private String resolveApiKey(String clientKey, String provider) {
        if (clientKey != null && !clientKey.isBlank()) {
            return clientKey.trim();
        }
        if ("OPENAI".equalsIgnoreCase(provider)) {
            if (configuredOpenAiKey != null && !configuredOpenAiKey.isBlank()) {
                return configuredOpenAiKey.trim();
            }
            return System.getenv("OPENAI_API_KEY");
        }
        // Mặc định GEMINI
        if (configuredGeminiKey != null && !configuredGeminiKey.isBlank()) {
            return configuredGeminiKey.trim();
        }
        return System.getenv("GEMINI_API_KEY");
    }

    /**
     * Gọi Google Gemini API bằng Text-Only Prompt siêu tinh gọn (~100-130 tokens)
     */
    private String callGeminiSensei(
            KanjiDictionary kanji,
            ScoreResult vectorResult,
            String apiKey) throws Exception {

        String prompt = buildPrompt(kanji, vectorResult);
        List<String> candidateModels = new ArrayList<>();
        if (configuredGeminiModel != null && !configuredGeminiModel.isBlank()) {
            candidateModels.add(configuredGeminiModel.trim());
        }
        if (!candidateModels.contains("gemini-3.8-flash")) {
            candidateModels.add("gemini-3.8-flash");
        }
        if (!candidateModels.contains("gemini-flash-latest")) {
            candidateModels.add("gemini-flash-latest");
        }

        List<Map<String, Object>> partsList = List.of(Map.of("text", prompt));

        Map<String, Object> reqBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", partsList
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 350
                )
        );

        String jsonPayload = objectMapper.writeValueAsString(reqBody);
        String lastErrorMsg = null;

        for (String model : candidateModels) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(12))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode candidates = root.get("candidates");
                    if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                        if (!textNode.isMissingNode()) {
                            log.info("[AI Sensei] Gọi thành công Gemini Model: {}", model);
                            return textNode.asText().trim();
                        }
                    }
                } else {
                    lastErrorMsg = "Gemini HTTP " + response.statusCode() + " (" + model + "): " + response.body();
                    log.warn("[AI Sensei] Thử model {} thất bại ({}). Thử model tiếp theo...", model, response.statusCode());
                }
            } catch (Exception ex) {
                lastErrorMsg = ex.getMessage();
                log.warn("[AI Sensei] Lỗi kết nối model {}: {}", model, ex.getMessage());
            }
        }

        throw new RuntimeException("Tất cả các Gemini models đều thất bại. Lỗi cuối: " + lastErrorMsg);
    }

    /**
     * Gọi OpenAI GPT-4o-mini API bằng Text-Only Prompt
     */
    private String callOpenAiSensei(
            KanjiDictionary kanji,
            ScoreResult vectorResult,
            String apiKey) throws Exception {

        String prompt = buildPrompt(kanji, vectorResult);
        String url = "https://api.openai.com/v1/chat/completions";

        List<Map<String, Object>> messages = List.of(
                Map.of("role", "system", "content", "Bạn là Kizuna Sensei - Chuyên gia Thư pháp Nhật Bản (Shodo) kiêm Giảng viên Kanji tại KizunaTutor."),
                Map.of("role", "user", "content", prompt)
        );

        Map<String, Object> reqBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 350
        );

        String jsonPayload = objectMapper.writeValueAsString(reqBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI HTTP Error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && !choices.isEmpty()) {
            JsonNode textNode = choices.get(0).path("message").path("content");
            if (!textNode.isMissingNode()) {
                return textNode.asText().trim();
            }
        }

        throw new RuntimeException("Phản hồi rỗng từ OpenAI API");
    }

    /**
     * Khởi tạo Prompt Text-Only siêu tinh gọn, đưa trực tiếp kết quả phân tích động học
     * từ thuật toán StrokeSensoryAnalyzer và Topo Vector vào để AI nhận xét chuẩn mực.
     */
    private String buildPrompt(KanjiDictionary kanji, ScoreResult vr) {
        String kanjiChar = kanji != null ? kanji.getKanjiCharacter() : vr.getKanji();
        String hanViet = kanji != null && kanji.getHanViet() != null ? kanji.getHanViet() : "—";
        String meaning = kanji != null && kanji.getMeaning() != null ? kanji.getMeaning() : "—";
        int stdStrokes = kanji != null && kanji.getStrokeCount() != null ? kanji.getStrokeCount() : vr.getStandardStrokes();

        StringBuilder errorBullets = new StringBuilder();
        if (vr.getDeductions() != null && !vr.getDeductions().isEmpty()) {
            for (ScoreResult.ScoreDeduction d : vr.getDeductions()) {
                errorBullets.append(String.format("* %s: %s\n", d.title(), d.description()));
            }
        } else {
            errorBullets.append("* Nét vẽ chuẩn mực, đúng bút thuận và cấu trúc cân đối.\n");
        }

        String sensoryFeel = vr.getSensoryAssessment() != null
                ? vr.getSensoryAssessment()
                : "Lực bút tương đối ổn định.";
        String speedText = vr.getAverageVelocity() != null
                ? String.format("%.2f px/ms", vr.getAverageVelocity())
                : "Ổn định";
        int stability = vr.getStabilityScore() != null ? vr.getStabilityScore() : 85;

        return String.format("""
                Vai trò: Kizuna Sensei (Giảng viên Thư pháp & Kanji KizunaTutor).
                Nhiệm vụ: Viết lời nhận xét 80-120 từ cho học viên viết chữ Kanji "%s" (%s: %s).

                Dữ liệu hình học & cảm quan nét vẽ:
                - Điểm: %d/100 (%s) | Nét: %d/%d nét
                - Lực bút & Tốc độ: %s (Tốc độ: %s, Độ vững tay: %d%%)
                - Lỗi sai cụ thể từ thuật toán:
                %s
                Format phản hồi (ngắn gọn, ân cần, định dạng 3 mục rõ ràng):
                1. 🌸 [CẢM QUAN]: Khen ngợi 1 điểm tốt về lực bút, nhịp lia bút hoặc thế chữ.
                2. ✍️ [SỬA LỖI]: Hướng dẫn kỹ thuật dừng bút (tome), gập (hane), vuốt (harai) hoặc sửa hướng nét dựa đúng vào lỗi ở trên.
                3. 🎯 [ĐỘNG VIÊN]: 1 câu thành ngữ/châm ngôn tiếng Nhật ngắn gọn kèm dịch nghĩa.
                """,
                kanjiChar, hanViet, meaning,
                vr.getTotalScorePercent(), vr.getGrade() != null ? vr.getGrade() : "Đạt chuẩn",
                vr.getUserStrokes(), stdStrokes,
                sensoryFeel, speedText, stability,
                errorBullets.toString()
        );
    }

    private String buildFallbackSenseiNote(KanjiDictionary kanji, ScoreResult vr) {
        String kanjiChar = kanji != null ? kanji.getKanjiCharacter() : vr.getKanji();
        String sensory = vr.getSensoryAssessment() != null ? vr.getSensoryAssessment() : "Lực bút tương đối đều tay";

        return String.format(
                "🌸 **Đánh giá nét viết chữ \"%s\"**:\n\n" +
                "Chữ viết của bạn đã được thuật toán Topo Hình học Vector & Động học cảm quan đánh giá đạt **%d/100 điểm (%s)**.\n" +
                "- Cảm quan lực bút: **%s**\n" +
                "- Độ chuẩn hướng nét (Cosine Similarity): **%d%%**\n" +
                "- Độ cân đối khung chữ ô Mễ: **%d%%**\n" +
                "- Điểm số nét & trình tự bút thuận: **%d/25 điểm**\n\n" +
                "Hãy tiếp tục duy trì tốc độ lia bút đều tay, chú ý kỹ thuật dừng bút (tome) và vuốt nét (harai) để nét chữ ngày càng sắc nét!",
                kanjiChar, vr.getTotalScorePercent(), vr.getGrade() != null ? vr.getGrade() : "Đạt chuẩn",
                sensory,
                vr.getDirectionAccuracy(), vr.getProportionAccuracy(),
                vr.getStrokeCriterion() != null ? vr.getStrokeCriterion().score() : 20
        );
    }
}
