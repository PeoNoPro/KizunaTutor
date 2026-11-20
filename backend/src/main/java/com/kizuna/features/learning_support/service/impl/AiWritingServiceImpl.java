package com.kizuna.features.learning_support.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;
import com.kizuna.features.learning_support.dto.AiWritingErrorItemDto;
import com.kizuna.features.learning_support.dto.AiWritingVocabularyDto;
import com.kizuna.features.learning_support.dto.AiWritingPromptDto;
import com.kizuna.features.learning_support.service.AiWritingService;
import com.kizuna.features.admin.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiWritingServiceImpl implements AiWritingService {

    private final ObjectMapper objectMapper;
    /** Admin chọn nhà cung cấp, model và API key ở trang Cấu hình hệ thống (FE-11); không có thì dùng key trong cấu hình server. */
    private final SystemSettingService settings;

    @Override
    public AiWritingCheckResponse checkWriting(AiWritingCheckRequest request) {
        String originalText = request.getText() != null ? request.getText().trim() : "";
        String jlpt = (request.getJlptLevel() != null && !request.getJlptLevel().isBlank()) ? request.getJlptLevel() : "N5";
        String formality = (request.getFormality() != null && !request.getFormality().isBlank()) ? request.getFormality() : "POLITE";
        String targetPattern = request.getGrammarPattern();

        if (originalText.isEmpty()) {
            return AiWritingCheckResponse.builder()
                    .originalText("")
                    .correctedText("")
                    .score(0)
                    .ratingBadge("Chưa có nội dung")
                    .overallComment("Vui lòng nhập văn bản tiếng Nhật cần đánh giá.")
                    .errors(List.of())
                    .suggestedVocabulary(List.of())
                    .nativeAlternatives(List.of())
                    .build();
        }

        // 0. Phát hiện nhập linh tinh / chuỗi vô nghĩa -> cảnh báo ngay lập tức, điểm 0
        if (isGibberishOrNonsense(originalText)) {
            return buildGibberishWarningResponse(originalText);
        }

        // 1. Thử gọi AI theo nhà cung cấp, model và API key Admin đã chọn
        SystemSettingService.AiChoice ai = settings.aiChoice();
        if (ai.apiKey() != null && !ai.apiKey().isBlank()) {
            try {
                String prompt = buildPrompt(originalText, jlpt, formality, targetPattern);
                String jsonText = "GEMINI".equals(ai.provider())
                        ? callGemini(prompt, ai.model(), ai.apiKey())
                        : callOpenAiCompatible(prompt, ai.model(), ai.apiKey(), ai.baseUrl(), "OPENAI".equals(ai.provider()));
                if (jsonText != null && !jsonText.isBlank()) {
                    AiWritingCheckResponse result = objectMapper.readValue(stripFence(jsonText), AiWritingCheckResponse.class);
                    result.setOriginalText(originalText);
                    result.setProvider(ai.provider());
                    result.setModel(ai.model());
                    result.setFallback(false);

                    // Hàng rào kiểm duyệt bổ sung (Post-Validation Guardrails):
                    // Trường hợp người dùng không nhập tiếng Nhật nhưng AI vẫn chấm điểm quá cao
                    if (!containsJapanese(originalText)) {
                        if (result.getScore() > 20) {
                            result.setScore(15);
                        }
                        if (result.getRatingBadge() == null || !result.getRatingBadge().toLowerCase().contains("tiếng nhật")) {
                            result.setRatingBadge("Chưa đúng ngôn ngữ / Cần viết bằng tiếng Nhật");
                        }
                        String curComment = result.getOverallComment() != null ? result.getOverallComment() : "";
                        if (!curComment.toLowerCase().contains("tiếng nhật")) {
                            result.setOverallComment("Nhắc nhở: Bạn đang nhập văn bản không phải tiếng Nhật. Vui lòng luyện viết bằng chữ Hiragana, Katakana hoặc Kanji nhé! ✍️ " + curComment);
                        }
                    }

                    // Trường hợp văn bản gốc chứa ký tự lỗi/hỏng mã
                    if (hasMalformedCharacters(originalText)) {
                        boolean hasCharErr = result.getErrors() != null && result.getErrors().stream()
                                .anyMatch(e -> e.getErrorType() != null && e.getErrorType().toLowerCase().contains("ký tự"));
                        if (!hasCharErr) {
                            List<AiWritingErrorItemDto> errs = new ArrayList<>(result.getErrors() != null ? result.getErrors() : List.of());
                            errs.add(AiWritingErrorItemDto.builder()
                                    .errorType("Ký tự lỗi")
                                    .originalFragment("Ký tự lạ/lỗi mã")
                                    .correctedFragment("Ký tự chuẩn")
                                    .explanation("Văn bản chứa ký tự bất thường hoặc lỗi font/mã hóa. Vui lòng kiểm tra lại bộ gõ tiếng Nhật.")
                                    .build());
                            result.setErrors(errs);
                            result.setScore(Math.max(0, result.getScore() - 15));
                        }
                    }

                    return result;
                }
            } catch (org.springframework.web.client.HttpStatusCodeException ex) {
                String detail = SystemSettingService.providerError(ex.getResponseBodyAsString(), ai.apiKey());
                log.warn("Lỗi máy chủ AI {} model {} (mã {}): {}. Kích hoạt đánh giá dự phòng.",
                        ai.provider(), ai.model(), ex.getStatusCode().value(), detail.isBlank() ? ex.getMessage() : detail);
            } catch (Exception ex) {
                log.warn("Không thể gọi AI {} ({}), kích hoạt bộ đánh giá dự phòng: {}", ai.provider(), ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        // 2. Chế độ dự phòng thông minh (Graceful Fallback) đảm bảo trải nghiệm người dùng luôn thông suốt
        return generateFallbackEvaluation(originalText, jlpt, formality, targetPattern);
    }

    private String buildPrompt(String text, String jlpt, String formality, String targetPattern) {
        return """
            Bạn là Kizuna AI Sensei - chuyên gia ngôn ngữ tiếng Nhật và sư phạm giàu kinh nghiệm của nền tảng KizunaTutor.
            Nhiệm vụ của bạn là thẩm định, sửa lỗi và hướng dẫn học viên cải thiện câu văn hoặc đoạn văn tiếng Nhật.

            Cấp độ JLPT hướng đến: %s
            Phong cách mong muốn: %s (POLITE: lịch sự です/ます, PLAIN: thể ngắn thân mật 普通形, KEIGO: kính ngữ thương mại 敬語)
            Mẫu ngữ pháp mục tiêu (nếu có): %s

            Văn bản học viên nhập vào:
            "%s"

            QUY TẮC THẨM ĐỊNH NGHIÊM NGẶT (BẮT BUỘC TUÂN THỦ):
            1. PHÁT HIỆN VĂN BẢN KHÔNG PHẢI TIẾNG NHẬT (Tiếng Việt, Tiếng Anh, Romaji thuần túy, hoặc không chứa bất kỳ chữ Hiragana/Katakana/Kanji nào):
               - Tuyệt đối KHÔNG chấm điểm cao. Điểm số "score" chỉ được từ 0 đến 20 điểm.
               - "ratingBadge": BẮT BUỘC là "Chưa đúng ngôn ngữ / Cần viết bằng tiếng Nhật".
               - "overallComment": Nhắc nhở rõ ràng bằng tiếng Việt thân thiện: "Nhắc nhở: Bạn đang nhập văn bản không phải tiếng Nhật. Vui lòng luyện viết bằng chữ Hiragana, Katakana hoặc Kanji để Sensei có thể chấm điểm và sửa lỗi cho bạn nhé! ✍️".
               - "errors": Thêm mục lỗi với errorType: "Ngôn ngữ chưa phù hợp", originalFragment: văn bản gốc, correctedFragment: câu dịch tiếng Nhật chuẩn tương ứng, explanation: "Vui lòng nhập văn bản bằng tiếng Nhật (Hiragana, Katakana, Kanji).".
               - "correctedText": Dịch ý của học viên sang câu tiếng Nhật chuẩn tự nhiên theo cấp độ %s và phong cách %s.
               - "nativeAlternatives": Các cách diễn đạt tương ứng bằng tiếng Nhật tự nhiên.

            2. PHÁT HIỆN NHẬP LINH TINH / VÔ NGHĨA (Chuỗi gõ phím ngẫu nhiên, spam phím, dãy số vô nghĩa, ký tự lặp vô nghĩa, ví dụ: 'asdfgh', 'qwert', '12345678', 'aaaaaa', hoặc câu từ rời rạc hoàn toàn vô nghĩa):
               - BẮT BUỘC cho "score": 0 điểm. TUYỆT ĐỐI KHÔNG khen ngợi hay tự bịa ra ngữ nghĩa.
               - "ratingBadge": BẮT BUỘC là "Cảnh báo: Nội dung vô nghĩa".
               - "overallComment": Phát cảnh báo rõ ràng bằng tiếng Việt: "⚠️ Cảnh báo: Nội dung bạn nhập dường như là các ký tự ngẫu nhiên hoặc vô nghĩa, không tạo thành câu tiếng Nhật hoàn chỉnh. Vui lòng nhập một câu tiếng Nhật có nghĩa để Sensei hỗ trợ sửa bài nhé!".
               - "errors": Thêm mục lỗi với errorType: "Nội dung vô nghĩa", originalFragment: văn bản gốc, correctedFragment: "Hãy nhập câu tiếng Nhật có nghĩa", explanation: "Chuỗi ký tự không có ý nghĩa trong tiếng Nhật. Không thể thẩm định ngữ pháp.".
               - "correctedText": "",
               - "suggestedVocabulary": [],
               - "nativeAlternatives": []

            3. PHÁT HIỆN KÝ TỰ LỖI / LỖI CHÍNH TẢ / KÝ TỰ BẤT THƯỜNG (Ký tự sai bảng chữ cái, lẫn ký tự lạ hỏng mã, gõ nhầm chữ nhỏ/chữ lớn như とうきよう -> とうきょう, sai trợ từ は/わ, を/お, へ/え, chia thể động từ, tính từ):
               - Báo rõ từng lỗi cụ thể vào danh sách "errors" với:
                 + errorType: "Ký tự lỗi / Lỗi chính tả" (hoặc "Trợ từ", "Chia thể", "Ngữ pháp mục tiêu").
                 + originalFragment: đúng đoạn ký tự/từ bị lỗi trong câu gốc.
                 + correctedFragment: cách viết/ký tự chuẩn xác.
                 + explanation: giải thích chi tiết, dễ hiểu bằng tiếng Việt tại sao sai và sửa thế nào.
               - Bắt buộc trừ điểm tương ứng với từng lỗi (mỗi lỗi ký tự hoặc ngữ pháp trừ 10 - 25 điểm). Điểm số phản ánh đúng chất lượng thực tế của câu, không tự động cho điểm cao nếu câu còn nhiều lỗi.

            4. NẾU CÂU LÀ TIẾNG NHẬT HỢP LỆ VÀ CÓ NGHĨA:
               - Chấm điểm công tâm từ 0 - 100 dựa trên độ chính xác ngữ pháp, trợ từ, thể động từ, phong cách %s và mức độ hoàn thành mẫu ngữ pháp %s.
               - Sửa câu cho tự nhiên nhất có thể trong "correctedText".
               - Nhận xét và giải thích chi tiết bằng tiếng Việt.

            BẮT BUỘC TRẢ VỀ DUY NHẤT 1 ĐỐI TƯỢNG JSON với cấu trúc chính xác sau (không bọc trong markdown ```json, không kèm lời giải thích nào ngoài JSON):
            {
              "correctedText": "Câu tiếng Nhật đã được sửa chuẩn tự nhiên theo phong cách yêu cầu (hoặc rỗng nếu câu hoàn toàn vô nghĩa)",
              "score": 85,
              "ratingBadge": "Tự nhiên xuất sắc / Khá tốt - Cần trau chuốt / Cần cải thiện / Cảnh báo: Nội dung vô nghĩa / Chưa đúng ngôn ngữ",
              "overallComment": "Nhận xét tổng quan / Lời nhắc nhở / Lời cảnh báo bằng tiếng Việt theo đúng các quy tắc trên",
              "errors": [
                {
                  "errorType": "Ký tự lỗi / Lỗi chính tả / Trợ từ / Chia thì / Ngôn ngữ chưa phù hợp / Nội dung vô nghĩa",
                  "originalFragment": "phần sai trong câu gốc",
                  "correctedFragment": "phần đã được sửa",
                  "explanation": "Giải thích chi tiết dễ hiểu bằng tiếng Việt tại sao sai và tại sao nên sửa như vậy"
                }
              ],
              "suggestedVocabulary": [
                {
                  "word": "từ vựng mới hoặc collocations hay",
                  "reading": "cách đọc hiragana",
                  "meaning": "nghĩa tiếng Việt"
                }
              ],
              "nativeAlternatives": [
                "Cách diễn đạt tự nhiên khác 1",
                "Cách diễn đạt tự nhiên khác 2"
              ]
            }
            """.formatted(jlpt, formality, targetPattern != null ? targetPattern : "Tự do", text, jlpt, formality, formality, targetPattern != null ? targetPattern : "Tự do");
    }

    private static String stripFence(String jsonText) {
        if (jsonText == null) return "{}";
        jsonText = jsonText.trim();
        // Cắt bỏ thẻ suy luận <think>...</think> nếu có (từ DeepSeek-R1, reasoning models)
        if (jsonText.contains("</think>")) {
            jsonText = jsonText.substring(jsonText.indexOf("</think>") + 8).trim();
        }
        if (jsonText.startsWith("```json")) jsonText = jsonText.substring(7);
        if (jsonText.startsWith("```")) jsonText = jsonText.substring(3);
        if (jsonText.endsWith("```")) jsonText = jsonText.substring(0, jsonText.length() - 3);
        return jsonText.trim();
    }

    /** Model chỉ gồm ký tự an toàn (đã kiểm tra khi Admin lưu). Key gửi qua header để không lọt vào log lỗi có kèm URL. */
    private String callGemini(String prompt, String model, String apiKey) throws Exception {
        if (!model.matches("[A-Za-z0-9._:-]{1,80}")) return null;
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(part));
        Map<String, Object> genConfig = Map.of(
                "temperature", 0.3,
                "responseMimeType", "application/json"
        );
        Map<String, Object> reqBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", genConfig
        );

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(45000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode parts = objectMapper.readTree(response.getBody()).path("candidates").path(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) return parts.get(0).path("text").asText();
        }
        return null;
    }

    /** OpenAI và các dịch vụ theo chuẩn OpenAI (Groq, DeepSeek, OpenRouter, Ollama...) cùng gọi {baseUrl}/chat/completions. */
    private String callOpenAiCompatible(String prompt, String model, String apiKey, String baseUrl, boolean jsonMode) throws Exception {
        String base = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("model", model);
        reqBody.put("temperature", 0.3);
        reqBody.put("max_tokens", 3500); // Tăng max_tokens cho cả reasoning models và đoạn văn phân tích dài
        reqBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an expert Japanese teacher. Always output valid JSON without markdown formatting."),
                Map.of("role", "user", "content", prompt)
        ));
        // Không phải dịch vụ nào cũng hỗ trợ response_format nên chỉ bật cho OpenAI.
        if (jsonMode) reqBody.put("response_format", Map.of("type", "json_object"));

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(45000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<String> response = restTemplate.exchange(base + "/chat/completions", HttpMethod.POST, new HttpEntity<>(reqBody, headers), String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode content = objectMapper.readTree(response.getBody()).path("choices").path(0).path("message").path("content");
            if (!content.isMissingNode() && !content.asText().isBlank()) return content.asText();
        }
        return null;
    }

    private boolean isGrammarPatternSatisfied(String text, String targetPattern) {
        if (text == null || text.isBlank() || targetPattern == null || targetPattern.isBlank()) {
            return true;
        }

        // 1. Làm sạch các chú thích tiếng Anh/tiếng Việt trong ngoặc
        String cleanPattern = targetPattern
                .replaceAll("（[^）]*）", "")
                .replaceAll("\\([^)]*\\)", "")
                .replaceAll("\\[[^\\]]*\\]", "")
                .replace("〜", "")
                .replace("~", "")
                .trim();

        if (cleanPattern.isEmpty()) {
            return true;
        }

        // 2. Nhận diện cấu trúc tần suất: [Khoảng thời gian] + に + [Số lần]
        String lowerTarget = targetPattern.toLowerCase();
        if (lowerTarget.contains("tần suất") || lowerTarget.contains("frequency") || "に".equals(cleanPattern)) {
            if (text.matches(".*[一二三四五六七八九十百千\\d]+(日|週|週間|月|か月|ヶ月|年|時間|分)に[一二三四五六七八九十百千\\d]+(回|度|時間).*")
                    || (text.contains("に") && (text.contains("回") || text.contains("度")))) {
                return true;
            }
        }

        // 3. Tách các thành phần rời nhau (như 〜より〜のほうが)
        String[] parts = cleanPattern.split("[〜~\\s]+");
        boolean allMatched = true;
        for (String part : parts) {
            String p = part.trim();
            if (!p.isEmpty() && !text.contains(p)) {
                allMatched = false;
                break;
            }
        }
        if (allMatched && parts.length > 0) {
            return true;
        }

        return text.contains(cleanPattern);
    }

    public static boolean containsJapanese(String text) {
        if (text == null) return false;
        return text.codePoints().anyMatch(cp ->
                (cp >= 0x3040 && cp <= 0x309F) || // Hiragana
                (cp >= 0x30A0 && cp <= 0x30FF) || // Katakana
                (cp >= 0x4E00 && cp <= 0x9FAF)    // Kanji
        );
    }

    public static boolean hasMalformedCharacters(String text) {
        if (text == null) return false;
        return text.contains("\uFFFD") || text.matches(".*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F].*");
    }

    public static boolean isGibberishOrNonsense(String text) {
        if (text == null || text.isBlank()) return true;
        String trimmed = text.trim();

        // 1. Chỉ chứa số hoặc ký tự đặc biệt / dấu câu, không có chữ cái hay kana/kanji nào
        if (trimmed.matches("^[\\d\\s.,!?_\\-+/*=~@#$%^&()\\[\\]{}|<>:;'\"`~・…！？。、]+$")) {
            return true;
        }

        // 2. Ký tự lặp lại vô nghĩa 4 lần trở lên (ví dụ: aaaa, 1111, ああああ, .....)
        if (trimmed.matches("^(.)\\1{3,}$")) {
            return true;
        }

        // 3. Chuỗi ký tự ngẫu nhiên lặp lại ngắn (ví dụ: asdasdasd, abcabcabc)
        if (trimmed.matches("^(.{2,4})\\1{2,}$")) {
            return true;
        }

        // 4. Chuỗi Latin gõ phím ngẫu nhiên (keyboard smash không nguyên âm hoặc chuỗi phụ âm dày đặc)
        if (trimmed.matches("(?i)^[b-df-hj-np-tv-z]{5,}$")) {
            return true;
        }

        // 5. Các chuỗi gõ phím bừa theo hàng phím phổ biến
        String lower = trimmed.toLowerCase();
        if (lower.matches(".*(asdfgh|qwerty|zxcvbn|qazwsx|123456|poiuyt|lkjhgf).*")) {
            return true;
        }

        // 6. Gõ lặp hàng phím Hiragana cơ bản liên tiếp không có nghĩa ngữ pháp
        if (trimmed.matches("^(あいうえお|かきくけこ|さしすせそ|たちつてと|なにぬねの|はひふへほ|まみむめも|らりるれろ|アイウエオ)+$")) {
            return true;
        }

        return false;
    }

    private static AiWritingCheckResponse buildGibberishWarningResponse(String text) {
        return AiWritingCheckResponse.builder()
                .originalText(text)
                .correctedText("")
                .score(0)
                .ratingBadge("Cảnh báo: Nội dung vô nghĩa")
                .overallComment("⚠️ Cảnh báo: Nội dung bạn nhập dường như là các ký tự ngẫu nhiên hoặc vô nghĩa, không tạo thành câu tiếng Nhật hoàn chỉnh. Vui lòng nhập một câu tiếng Nhật có nghĩa để Sensei hỗ trợ sửa bài nhé!")
                .errors(List.of(
                        AiWritingErrorItemDto.builder()
                                .errorType("Nội dung vô nghĩa")
                                .originalFragment(text)
                                .correctedFragment("Hãy nhập câu tiếng Nhật có nghĩa")
                                .explanation("Chuỗi ký tự ngẫu nhiên hoặc không có ý nghĩa trong tiếng Nhật. Không thể thẩm định ngữ pháp.")
                                .build()
                ))
                .suggestedVocabulary(List.of())
                .nativeAlternatives(List.of())
                .provider("SYSTEM_GUARD")
                .model("GuardRail-v1")
                .fallback(false)
                .build();
    }

    private static AiWritingCheckResponse buildNonJapaneseReminderResponse(String text) {
        return AiWritingCheckResponse.builder()
                .originalText(text)
                .correctedText("Ví dụ câu tiếng Nhật: 私は日本語を勉強しています。")
                .score(15)
                .ratingBadge("Chưa đúng ngôn ngữ / Cần viết bằng tiếng Nhật")
                .overallComment("Nhắc nhở: Bạn đang nhập văn bản không phải tiếng Nhật. Vui lòng luyện viết bằng chữ Hiragana, Katakana hoặc Kanji để Sensei có thể chấm điểm và sửa lỗi cho bạn nhé! ✍️")
                .errors(List.of(
                        AiWritingErrorItemDto.builder()
                                .errorType("Ngôn ngữ chưa phù hợp")
                                .originalFragment(text)
                                .correctedFragment("Cần viết bằng chữ Hiragana / Katakana / Kanji")
                                .explanation("Văn bản bạn nhập không chứa ký tự tiếng Nhật. Hệ thống luyện viết KizunaTutor yêu cầu bài viết bằng tiếng Nhật.")
                                .build()
                ))
                .suggestedVocabulary(List.of(
                        AiWritingVocabularyDto.builder()
                                .word("日本語")
                                .reading("にほんご (nihongo)")
                                .meaning("Tiếng Nhật")
                                .build(),
                        AiWritingVocabularyDto.builder()
                                .word("勉強")
                                .reading("べんきょう (benkyou)")
                                .meaning("Học tập")
                                .build()
                ))
                .nativeAlternatives(List.of(
                        "私は日本語を勉強しています。(Tôi đang học tiếng Nhật)",
                        "日本語で書いてみましょう！(Hãy thử viết bằng tiếng Nhật nhé!)"
                ))
                .provider("SYSTEM_FALLBACK")
                .model("RuleEngine-v1")
                .fallback(true)
                .build();
    }

    private AiWritingCheckResponse generateFallbackEvaluation(String text, String jlpt, String formality, String targetPattern) {
        // 1. Kiểm tra nhập linh tinh / vô nghĩa
        if (isGibberishOrNonsense(text)) {
            return buildGibberishWarningResponse(text);
        }

        // 2. Kiểm tra văn bản không phải tiếng Nhật (tiếng Việt, tiếng Anh, Romaji, etc.)
        if (!containsJapanese(text)) {
            return buildNonJapaneseReminderResponse(text);
        }

        // 3. Thuật toán phân tích ngữ pháp và phát hiện lỗi ký tự khi mạng ngoài chưa khả dụng
        List<AiWritingErrorItemDto> errors = new ArrayList<>();
        List<AiWritingVocabularyDto> vocabList = new ArrayList<>();
        List<String> alternatives = new ArrayList<>();

        int score = 96;
        String ratingBadge = "Tự nhiên xuất sắc";
        String comment = "Tuyệt vời! Câu văn rất tự nhiên, từ vựng và ngữ pháp được áp dụng chính xác theo văn phong chuẩn Nhật.";
        String corrected = text;

        // 3.1. Kiểm tra ký tự lỗi / ký tự hỏng mã
        if (hasMalformedCharacters(text)) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Ký tự lỗi")
                    .originalFragment("Ký tự không hợp lệ")
                    .correctedFragment("Ký tự chuẩn")
                    .explanation("Văn bản chứa ký tự bất thường hoặc lỗi font/mã hóa. Vui lòng kiểm tra lại bộ gõ tiếng Nhật.")
                    .build());
            score -= 20;
        }

        // 3.2. Kiểm tra lỗi chính tả âm ghép / chữ nhỏ (ảo âm: ゃ, ゅ, ょ / xúc âm: っ)
        if (text.contains("とうきよう") || text.contains("しやしん") || text.contains("びよういん") || text.contains("きよう") || text.contains("がつこう") || text.contains("がこう")) {
            String typoWord = text.contains("とうきよう") ? "とうきよう" :
                              text.contains("しやしん") ? "しやしん" :
                              text.contains("びよういん") ? "びよういん" :
                              text.contains("きよう") ? "きよう" : "がつこう";
            String fixedWord = typoWord.replace("とうきよう", "とうきょう")
                                       .replace("しやしん", "しゃしん")
                                       .replace("びよういん", "びょういん")
                                       .replace("きよう", "きょう")
                                       .replace("がつこう", "がっこう")
                                       .replace("がこう", "がっこう");
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Ký tự lỗi / Lỗi chính tả")
                    .originalFragment(typoWord)
                    .correctedFragment(fixedWord)
                    .explanation("Lỗi chính tả âm ghép/xúc âm: Từ này cần dùng chữ nhỏ (ゃ, ゅ, ょ, っ) thay vì chữ kích thước thông thường.")
                    .build());
            corrected = corrected.replace(typoWord, fixedWord);
            score -= 15;
        }

        // 3.3. Kiểm tra trợ từ は (wa) viết nhầm thành わ
        if (text.contains("私わ") || text.contains("わたしわ") || text.contains("彼わ") || text.contains("彼女わ")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Trợ từ")
                    .originalFragment("わ")
                    .correctedFragment("は (phát âm: wa)")
                    .explanation("Trợ từ chỉ chủ đề/chủ ngữ trong tiếng Nhật luôn được viết bằng chữ Hiragana は (ha), không dùng わ.")
                    .build());
            corrected = corrected.replace("私わ", "私は").replace("わたしわ", "わたしは").replace("彼わ", "彼は").replace("彼女わ", "彼女は");
            score -= 10;
        }

        // 3.4. Kiểm tra trợ từ を bị viết nhầm thành お
        if (text.matches(".*[\\u3040-\\u309F\\u4E00-\\u9FAF]+お(食べる|飲む|見る|する|買う|読む|聞く|待つ).*")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Ký tự lỗi / Trợ từ")
                    .originalFragment("お")
                    .correctedFragment("を (phát âm: o/wo)")
                    .explanation("Trợ từ chỉ tân ngữ/đối tượng của hành động luôn được viết bằng chữ Hiragana を, không dùng お.")
                    .build());
            corrected = corrected.replaceAll("([\\u3040-\\u309F\\u4E00-\\u9FAF]+)お(食べる|飲む|見る|する|買う|読む|聞く|待つ)", "$1を$2");
            score -= 10;
        }

        // 3.5. Kiểm tra trợ từ へ (e) bị viết nhầm thành え
        if (text.matches(".*(学校|日本|東京|家|病院)え.*")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Ký tự lỗi / Trợ từ")
                    .originalFragment("え")
                    .correctedFragment("へ (phát âm: e)")
                    .explanation("Trợ từ chỉ phương hướng di chuyển luôn được viết bằng chữ Hiragana へ (phát âm: e), không dùng え.")
                    .build());
            corrected = corrected.replaceAll("(学校|日本|東京|家|病院)え", "$1へ");
            score -= 10;
        }

        // 3.6. Kiểm tra động từ nguyên mẫu đi trực tiếp với でした
        if (text.contains("食べるでした") || text.contains("行くでした") || text.contains("飲むでした") || text.matches(".*[るうつぬぶむくぐす]でした.*")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Chia thể động từ")
                    .originalFragment("Động từ nguyên mẫu + でした")
                    .correctedFragment("〜ました (Lịch sự) hoặc 〜た (Thể thường)")
                    .explanation("Động từ thể từ điển không thể kết hợp trực tiếp với 'でした'. Dạng quá khứ đúng là đuôi 〜ました (ví dụ: 食べました, 行きました).")
                    .build());
            corrected = corrected.replace("食べるでした", "食べました")
                                 .replace("行くでした", "行きました")
                                 .replace("飲むでした", "飲みました");
            score -= 15;
        }

        // 3.7. Kiểm tra tính từ đuôi い đi kèm với だ
        if (text.matches(".*[い]だ.*") && !text.contains("からだ")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Ngữ pháp tính từ")
                    .originalFragment("Tính từ đuôi い + だ")
                    .correctedFragment("Tính từ đuôi い (bỏ だ)")
                    .explanation("Trong tiếng Nhật thể thường, tính từ đuôi い không đi kèm với だ (ví dụ: おいしいだ là sai, chỉ cần nói おいしい hoặc おいしかったです).")
                    .build());
            score -= 10;
        }

        // 3.8. Kiểm tra sắc thái câu Lịch sự (POLITE)
        if ("POLITE".equalsIgnoreCase(formality)) {
            String trimmed = corrected.replaceAll("[。！？\\s]+$", "");
            if (trimmed.endsWith("だ") || trimmed.endsWith("る") || trimmed.endsWith("た") || trimmed.endsWith("ない")) {
                if (!trimmed.endsWith("です") && !trimmed.endsWith("ます") && !trimmed.endsWith("でした") && !trimmed.endsWith("ました") && !trimmed.endsWith("でしょうか") && !trimmed.endsWith("ください")) {
                    errors.add(AiWritingErrorItemDto.builder()
                            .errorType("Sắc thái câu")
                            .originalFragment("Đuôi câu thể thường")
                            .correctedFragment("Đuôi câu thể lịch sự (です/ます)")
                            .explanation("Bạn đang chọn phong cách Lịch sự. Hãy đổi đuôi câu sang dạng lịch sự (です/ます) để câu văn trang nhã hơn.")
                            .build());
                    score -= 10;
                }
            }
        }

        // 3.9. Kiểm tra mẫu ngữ pháp mục tiêu
        if (targetPattern != null && !targetPattern.isBlank()) {
            if (!isGrammarPatternSatisfied(text, targetPattern)) {
                errors.add(AiWritingErrorItemDto.builder()
                        .errorType("Ngữ pháp mục tiêu")
                        .originalFragment("Chưa thấy mẫu ngữ pháp")
                        .correctedFragment(targetPattern)
                        .explanation("Câu viết của bạn chưa lồng ghép rõ mẫu ngữ pháp mục tiêu: " + targetPattern + ". Hãy thử áp dụng công thức của mẫu này vào câu nhé.")
                        .build());
                score -= 15;
            }
        }

        score = Math.max(0, Math.min(100, score));

        if (!errors.isEmpty()) {
            if (score >= 80) {
                ratingBadge = "Khá tốt - Cần trau chuốt";
                comment = "Câu viết tương đối mượt mà. Hãy tham khảo phần lưu ý bên dưới để câu văn đạt mức hoàn hảo nhất nhé!";
            } else if (score >= 60) {
                ratingBadge = "Cần cải thiện";
                comment = "Cố lên bạn nhé! Hãy xem kỹ các điểm lưu ý về trợ từ hoặc thể động từ ở bảng bên dưới để đặt câu chuẩn xác hơn.";
            } else {
                ratingBadge = "Cần sửa lại ký tự & ngữ pháp";
                comment = "Câu văn có nhiều lỗi về ký tự hoặc ngữ pháp cơ bản. Hãy xem kỹ bảng sửa lỗi chi tiết bên dưới để hoàn thiện câu nhé!";
            }
        }

        // Gợi ý từ vựng hữu ích
        vocabList.add(AiWritingVocabularyDto.builder()
                .word("表現")
                .reading("ひょうげん (hyougen)")
                .meaning("Cách diễn đạt, biểu đạt")
                .build());
        vocabList.add(AiWritingVocabularyDto.builder()
                .word("自然な日本語")
                .reading("しぜんなにほんご (shizen na nihongo)")
                .meaning("Tiếng Nhật tự nhiên như người bản xứ")
                .build());

        alternatives.add(corrected);

        return AiWritingCheckResponse.builder()
                .originalText(text)
                .correctedText(corrected)
                .score(score)
                .ratingBadge(ratingBadge)
                .overallComment(comment)
                .errors(errors)
                .suggestedVocabulary(vocabList)
                .nativeAlternatives(alternatives)
                .provider("SYSTEM_FALLBACK")
                .model("RuleEngine-v1")
                .fallback(true)
                .build();
    }

    @Override
    public List<AiWritingPromptDto> getGrammarChallengePrompts(String jlptLevel) {
        String level = (jlptLevel != null && !jlptLevel.isBlank()) ? jlptLevel.toUpperCase() : "N5";
        List<AiWritingPromptDto> list = new ArrayList<>();

        if ("N5".equals(level)) {
            list.add(AiWritingPromptDto.builder()
                    .id("N5_01")
                    .level("N5")
                    .title("Yêu cầu lịch sự")
                    .grammarPattern("〜てください")
                    .meaning("Hãy / Xin vui lòng làm V (Yêu cầu lịch sự)")
                    .promptHint("Viết câu nhờ ai đó làm gì (ví dụ: Hãy đợi một chút, Hãy ăn thử món này).")
                    .sampleKeywords(List.of("待つ (chờ)", "食べる (ăn)", "書く (viết)"))
                    .build());
            list.add(AiWritingPromptDto.builder()
                    .id("N5_02")
                    .level("N5")
                    .title("Cấm đoán")
                    .grammarPattern("〜てはいけません")
                    .meaning("Không được phép làm V (Cấm đoán)")
                    .promptHint("Viết câu nhắc nhở quy định (ví dụ: Không được chụp ảnh ở đây, Không được hút thuốc).")
                    .sampleKeywords(List.of("写真を撮る (chụp ảnh)", "吸う (hút)", "入る (vào)"))
                    .build());
            list.add(AiWritingPromptDto.builder()
                    .id("N5_03")
                    .level("N5")
                    .title("Kinh nghiệm quá khứ")
                    .grammarPattern("〜たことがあります")
                    .meaning("Đã từng làm V (Kinh nghiệm trong quá khứ)")
                    .promptHint("Chia sẻ trải nghiệm của bạn (ví dụ: Đã từng leo núi Phú Sĩ, Đã từng ăn sushi).")
                    .sampleKeywords(List.of("富士山 (núi Phú Sĩ)", "寿司 (sushi)", "日本 (Nhật Bản)"))
                    .build());
        } else if ("N4".equals(level)) {
            list.add(AiWritingPromptDto.builder()
                    .id("N4_01")
                    .level("N4")
                    .title("Lời khuyên")
                    .grammarPattern("〜たほうがいい / 〜ないほうがいい")
                    .meaning("Nên làm V / Không nên làm V (Lời khuyên)")
                    .promptHint("Đưa ra lời khuyên cho bạn bè khi họ bị ốm hoặc chuẩn bị đi du lịch.")
                    .sampleKeywords(List.of("病院 (bệnh viện)", "薬 (thuốc)", "早く寝る (ngủ sớm)"))
                    .build());
            list.add(AiWritingPromptDto.builder()
                    .id("N4_02")
                    .level("N4")
                    .title("Dự đoán không chắc chắn")
                    .grammarPattern("〜かもしれません")
                    .meaning("Có lẽ, có thể là... (Dự đoán không chắc chắn)")
                    .promptHint("Dự đoán thời tiết hoặc khả năng xảy ra sự việc (ví dụ: Chiều nay trời có thể mưa).")
                    .sampleKeywords(List.of("雨 (mưa)", "午後 (buổi chiều)", "遅れる (đến muộn)"))
                    .build());
        } else {
            list.add(AiWritingPromptDto.builder()
                    .id("N3_01")
                    .level("N3")
                    .title("Thói quen cố gắng")
                    .grammarPattern("〜ようにしている")
                    .meaning("Cố gắng làm / Giữ thói quen làm V")
                    .promptHint("Viết về một thói quen tốt hàng ngày bạn đang cố duy trì.")
                    .sampleKeywords(List.of("運動 (vận động)", "野菜 (rau củ)", "毎日 (mỗi ngày)"))
                    .build());
            list.add(AiWritingPromptDto.builder()
                    .id("N3_02")
                    .level("N3")
                    .title("Không thể từ chối")
                    .grammarPattern("〜わけにはいかない")
                    .meaning("Không thể làm V (vì lý do đạo đức, tâm lý, xã hội)")
                    .promptHint("Viết về việc không thể bỏ dở hoặc không thể từ chối.")
                    .sampleKeywords(List.of("約束 (lời hứa)", "諦める (từ bỏ)", "休む (nghỉ)"))
                    .build());
        }

        return list;
    }
}
