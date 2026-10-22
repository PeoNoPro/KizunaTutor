package com.kizuna.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.kanji.ScoreResult;
import com.kizuna.entity.KanjiDictionary;
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
 * Service tích hợp Trí tuệ Nhân tạo (Google Gemini 1.5 Flash / OpenAI)
 * đóng vai trò Shodo Sensei (Thư pháp gia tiếng Nhật) phân tích nét vẽ người học.
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
     * Phân tích và sinh lời nhận xét thư pháp từ AI Sensei
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
                return callOpenAiSensei(kanji, vectorResult, effectiveKey, base64ImageData);
            } else {
                return callGeminiSensei(kanji, vectorResult, effectiveKey, base64ImageData);
            }
        } catch (Exception e) {
            log.warn("[Kizuna Sensei] Lỗi khi gọi AI API ({}: {}). Sử dụng nhận xét kỹ thuật.", provider, e.getMessage());
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
     * Gọi Google Gemini API (tự động thử các model khả dụng: gemini-3.8-flash, gemini-flash-latest)
     */
    private String callGeminiSensei(
            KanjiDictionary kanji,
            ScoreResult vectorResult,
            String apiKey,
            String base64ImageData) throws Exception {

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

        List<Map<String, Object>> partsList = new ArrayList<>();
        partsList.add(Map.of("text", prompt));

        // Nếu client gửi kèm ảnh vẽ Canvas Base64, thêm vào parts để Gemini Vision phân tích
        if (base64ImageData != null && base64ImageData.contains(",")) {
            try {
                String cleanBase64 = base64ImageData.substring(base64ImageData.indexOf(",") + 1).trim();
                partsList.add(Map.of(
                        "inlineData", Map.of(
                                "mimeType", "image/png",
                                "data", cleanBase64
                        )
                ));
            } catch (Exception ex) {
                log.debug("Không thể trích xuất base64 image: {}", ex.getMessage());
            }
        }

        Map<String, Object> reqBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", partsList
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.4,
                        "maxOutputTokens", 600
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
                        .timeout(Duration.ofSeconds(15))
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
     * Gọi OpenAI GPT-4o-mini API
     */
    private String callOpenAiSensei(
            KanjiDictionary kanji,
            ScoreResult vectorResult,
            String apiKey,
            String base64ImageData) throws Exception {

        String prompt = buildPrompt(kanji, vectorResult);
        String url = "https://api.openai.com/v1/chat/completions";

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "Bạn là Kizuna Sensei - Chuyên gia Thư pháp Nhật Bản kiêm Giảng viên Kanji hàng đầu tại học viện KizunaTutor."));

        if (base64ImageData != null && base64ImageData.startsWith("data:image")) {
            messages.add(Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url", "image_url", Map.of("url", base64ImageData))
                    )
            ));
        } else {
            messages.add(Map.of("role", "user", "content", prompt));
        }

        Map<String, Object> reqBody = Map.of(
                "model", "gpt-4o-mini",
                "messages", messages,
                "temperature", 0.4,
                "max_tokens", 600
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

    private String buildPrompt(KanjiDictionary kanji, ScoreResult vr) {
        String kanjiChar = kanji != null ? kanji.getKanjiCharacter() : vr.getKanji();
        String hanViet = kanji != null && kanji.getHanViet() != null ? kanji.getHanViet() : "—";
        String meaning = kanji != null && kanji.getMeaning() != null ? kanji.getMeaning() : "—";
        int stdStrokes = kanji != null && kanji.getStrokeCount() != null ? kanji.getStrokeCount() : vr.getStandardStrokes();

        StringBuilder deductionsSummary = new StringBuilder();
        if (vr.getDeductions() != null && !vr.getDeductions().isEmpty()) {
            for (ScoreResult.ScoreDeduction d : vr.getDeductions()) {
                deductionsSummary.append(String.format("- %s (%dđ): %s\n", d.title(), d.penalty(), d.description()));
            }
        } else {
            deductionsSummary.append("- Không có lỗi trừ điểm đáng kể, nét viết rất chuẩn mực.\n");
        }

        return String.format("""
                Bạn là Kizuna Sensei - Chuyên gia Thư pháp Nhật Bản (Shodo) kiêm Giảng viên Kanji cao cấp tại KizunaTutor.
                Nhiệm vụ của bạn là nhận xét và hướng dẫn học viên vừa tự tay viết chữ Kanji "%s" trên bảng vẽ tương tác.

                Thông tin chữ Hán:
                - Chữ: %s
                - Âm Hán Việt: %s
                - Nghĩa: %s
                - Số nét chuẩn: %d nét

                Dữ liệu phân tích kỹ thuật hình học nét viết của học viên:
                - Số nét học viên đã viết: %d nét (chuẩn: %d nét)
                - Tổng điểm kỹ thuật: %d/100 (%s)
                - Độ chính xác số nét & bút thuận: %d/25 (%d%%)
                - Độ cân đối khung chữ & trọng tâm ô Mễ: %d/35 (%d%%)
                - Độ chuẩn hướng đi nét & góc nghiêng (Cosine Similarity): %d/40 (%d%%)
                - Chi tiết các điểm cần sửa:
                %s

                Yêu cầu phản hồi:
                Hãy viết lời nhận xét mang phong thái một Sensei người Nhật tận tụy, ân cần, giàu chuyên môn và khích lệ người học:
                1. 🌸 [CẢM QUAN & NÉT VẼ]: Khen ngợi điểm tốt (lực bút, độ liền mạch, cố gắng của người học) và nêu ấn tượng tổng thể về chữ viết.
                2. ✍️ [KỸ THUẬT & GỢI Ý CHUYÊN SÂU]: Chỉ dẫn cụ thể cách hạ bút (tome), nét gập (hane/ore), hoặc độ vươn (harai) để chữ thanh thoát, chuẩn thư pháp hơn dựa trên dữ liệu trên.
                3. 🎯 [LỜI ĐỘNG VIÊN / CHÂM NGÔN]: 1 câu thành ngữ/châm ngôn tiếng Nhật ngắn gọn kèm dịch nghĩa tiếng Việt khích lệ học viên tiếp tục rèn chữ (ví dụ: 「継続は力なり」- Kiên trì là sức mạnh).
                Trình bày ngắn gọn, súc tích (khoảng 100-160 từ), chia đoạn rõ ràng.
                """,
                kanjiChar, kanjiChar, hanViet, meaning, stdStrokes,
                vr.getUserStrokes(), stdStrokes,
                vr.getTotalScorePercent(), vr.getGrade() != null ? vr.getGrade() : "Đạt chuẩn",
                vr.getStrokeCriterion() != null ? vr.getStrokeCriterion().score() : 20, vr.getOrderAccuracy(),
                vr.getBalanceCriterion() != null ? vr.getBalanceCriterion().score() : 30, vr.getProportionAccuracy(),
                vr.getTrajectoryCriterion() != null ? vr.getTrajectoryCriterion().score() : 35, vr.getDirectionAccuracy(),
                deductionsSummary.toString()
        );
    }

    private String buildFallbackSenseiNote(KanjiDictionary kanji, ScoreResult vr) {
        String kanjiChar = kanji != null ? kanji.getKanjiCharacter() : vr.getKanji();
        return String.format(
                "🌸 **Đánh giá nét viết chữ \"%s\"**:\n\n" +
                "Chữ viết của bạn đã được thuật toán Topo Hình học Vector đánh giá đạt **%d/100 điểm (%s)**.\n" +
                "- Độ chuẩn hướng nét (Cosine Similarity): **%d%%**\n" +
                "- Độ cân đối khung chữ ô Mễ: **%d%%**\n" +
                "- Điểm số nét & trình tự bút thuận: **%d/25 điểm**\n\n" +
                "Hãy tiếp tục duy trì tốc độ lia bút đều tay để nét chữ ngày càng chuẩn mực và uyển chuyển hơn!",
                kanjiChar, vr.getTotalScorePercent(), vr.getGrade() != null ? vr.getGrade() : "Đạt chuẩn",
                vr.getDirectionAccuracy(), vr.getProportionAccuracy(),
                vr.getStrokeCriterion() != null ? vr.getStrokeCriterion().score() : 20
        );
    }
}
