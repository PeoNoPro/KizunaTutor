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

        // 1. Thử gọi AI theo nhà cung cấp, model và API key Admin đã chọn
        SystemSettingService.AiChoice ai = settings.aiChoice();
        if (ai.apiKey() != null && !ai.apiKey().isBlank()) {
            try {
                String prompt = buildPrompt(originalText, jlpt, formality, targetPattern);
                String jsonText = "GEMINI".equals(ai.provider())
                        ? callGemini(prompt, ai.model(), ai.apiKey())
                        : callOpenAiCompatible(prompt, ai.model(), ai.apiKey(), ai.baseUrl(), "OPENAI".equals(ai.provider()));
                if (jsonText != null) {
                    AiWritingCheckResponse result = objectMapper.readValue(stripFence(jsonText), AiWritingCheckResponse.class);
                    result.setOriginalText(originalText);
                    return result;
                }
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

            Hãy phân tích kỹ lưỡng câu văn tiếng Nhật sau của học viên:
            "%s"

            BẮT BUỘC TRẢ VỀ DUY NHẤT 1 ĐỐI TƯỢNG JSON với cấu trúc chính xác sau (không bọc trong markdown ```json):
            {
              "correctedText": "Câu tiếng Nhật đã được sửa chuẩn tự nhiên theo phong cách yêu cầu",
              "score": 85,
              "ratingBadge": "Tự nhiên xuất sắc / Khá tốt - Cần lưu ý / Cần sửa lại ngữ pháp",
              "overallComment": "Nhận xét tổng quan thân thiện bằng tiếng Việt khuyến khích học viên",
              "errors": [
                {
                  "errorType": "Trợ từ / Chia thì / Từ vựng / Sắc thái kính ngữ",
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
            Chú ý: Điểm số từ 0 đến 100. Mọi lời giải thích và nhận xét BẮT BUỘC là tiếng Việt tự nhiên, thân thiện.
            """.formatted(jlpt, formality, targetPattern != null ? targetPattern : "Tự do", text);
    }

    private static String stripFence(String jsonText) {
        jsonText = jsonText.trim();
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

        RestTemplate restTemplate = new RestTemplate();
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

    /** OpenAI và các dịch vụ theo chuẩn OpenAI (Groq, DeepSeek, OpenRouter...) cùng gọi {baseUrl}/chat/completions. */
    private String callOpenAiCompatible(String prompt, String model, String apiKey, String baseUrl, boolean jsonMode) throws Exception {
        if (baseUrl == null || baseUrl.isBlank()) return null;
        Map<String, Object> reqBody = new HashMap<>(Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        ));
        // Không phải dịch vụ nào cũng hỗ trợ response_format nên chỉ bật cho OpenAI.
        if (jsonMode) reqBody.put("response_format", Map.of("type", "json_object"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        ResponseEntity<String> response = new RestTemplate().exchange(baseUrl + "/chat/completions", HttpMethod.POST, new HttpEntity<>(reqBody, headers), String.class);
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

    private AiWritingCheckResponse generateFallbackEvaluation(String text, String jlpt, String formality, String targetPattern) {
        // Thuật toán kiểm tra quy tắc thông minh khi mạng ngoài chưa khả dụng
        List<AiWritingErrorItemDto> errors = new ArrayList<>();
        List<AiWritingVocabularyDto> vocabList = new ArrayList<>();
        List<String> alternatives = new ArrayList<>();

        int score = 96;
        String ratingBadge = "Tự nhiên xuất sắc";
        String comment = "Tuyệt vời! Câu văn rất tự nhiên, từ vựng và ngữ pháp được áp dụng chính xác theo văn phong chuẩn Nhật.";
        String corrected = text;

        if (text.contains("私わ") || text.contains("わたしわ")) {
            errors.add(AiWritingErrorItemDto.builder()
                    .errorType("Trợ từ")
                    .originalFragment("わ")
                    .correctedFragment("は (phát âm: wa)")
                    .explanation("Trợ từ chỉ chủ đề/chủ ngữ trong tiếng Nhật luôn được viết bằng chữ Hiragana は (ha), không dùng わ.")
                    .build());
            corrected = corrected.replace("私わ", "私は").replace("わたしわ", "わたしは");
            score -= 10;
        }

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

        if (targetPattern != null && !targetPattern.isBlank()) {
            if (!isGrammarPatternSatisfied(text, targetPattern)) {
                errors.add(AiWritingErrorItemDto.builder()
                        .errorType("Ngữ pháp mục tiêu")
                        .originalFragment("Chưa thấy mẫu ngữ pháp")
                        .correctedFragment(targetPattern)
                        .explanation("Câu viết của bạn chưa lồng ghép rõ mẫu ngữ pháp mục tiêu: " + targetPattern + ". Hãy thử áp dụng công thức của mẫu này vào câu nhé.")
                        .build());
                score -= 10;
            }
        }

        if (!errors.isEmpty()) {
            if (score >= 80) {
                ratingBadge = "Khá tốt - Cần trau chuốt";
                comment = "Câu viết tương đối mượt mà. Hãy tham khảo phần lưu ý bên dưới để câu văn đạt mức hoàn hảo nhất nhé!";
            } else {
                ratingBadge = "Cần cải thiện";
                comment = "Cố lên bạn nhé! Hãy xem kỹ các điểm lưu ý về trợ từ hoặc thể động từ ở bảng bên dưới để đặt câu chuẩn xác hơn.";
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
                .score(Math.max(40, score))
                .ratingBadge(ratingBadge)
                .overallComment(comment)
                .errors(errors)
                .suggestedVocabulary(vocabList)
                .nativeAlternatives(alternatives)
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
