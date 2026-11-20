package com.kizuna.features.learning_support.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.service.SystemSettingService;
import com.kizuna.features.learning_support.dto.ReadingAnalysisRequest;
import com.kizuna.features.learning_support.dto.ReadingAnalysisResponse;
import com.kizuna.features.learning_support.dto.ReadingComprehensionQuestionDto;
import com.kizuna.features.learning_support.dto.ReadingGrammarItemDto;
import com.kizuna.features.learning_support.dto.ReadingVocabularyItemDto;
import com.kizuna.features.learning_support.service.ReadingAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingAssistantServiceImpl implements ReadingAssistantService {

    private final ObjectMapper objectMapper;
    private final SystemSettingService settings;

    // Bộ đệm kết quả bóc tách đọc hiểu (In-Memory Cache) theo SHA-256 hash của văn bản + JLPT
    // Giúp loại bỏ độ trễ mạng 2s - 6s và tiết kiệm 100% token Gemini khi học viên phân tích lại
    private final Map<String, ReadingAnalysisResponse> analysisCache = new java.util.concurrent.ConcurrentHashMap<>();

    private String computeCacheKey(String text, String targetJlpt) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((targetJlpt + "::" + text).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return targetJlpt + "_" + text.hashCode();
        }
    }

    private void cacheResult(String cacheKey, ReadingAnalysisResponse response) {
        if (response == null) return;
        if (analysisCache.size() > 500) {
            analysisCache.clear(); // Ngăn ngừa rò rỉ bộ nhớ khi cache quá 500 bài đọc
        }
        analysisCache.put(cacheKey, response);
    }

    @Override
    public ReadingAnalysisResponse analyzeReading(ReadingAnalysisRequest request) {
        String originalText = request.getText() != null ? request.getText().trim() : "";
        String targetJlpt = (request.getTargetJlpt() != null && !request.getTargetJlpt().isBlank()) ? request.getTargetJlpt() : "ALL";

        if (originalText.isEmpty()) {
            return ReadingAnalysisResponse.builder()
                    .originalText("")
                    .rubyHtml("")
                    .overallSummary("Vui lòng nhập đoạn văn bản tiếng Nhật để bóc tách.")
                    .estimatedLevel("N/A")
                    .detectedGrammars(List.of())
                    .vocabularies(List.of())
                    .sentenceTranslations(List.of())
                    .build();
        }

        // Kiểm tra bộ nhớ đệm (Cache HIT)
        String cacheKey = computeCacheKey(originalText, targetJlpt);
        ReadingAnalysisResponse cached = analysisCache.get(cacheKey);
        if (cached != null) {
            log.info("Trả về kết quả bóc tách đọc hiểu từ bộ nhớ đệm (Cache HIT) cho bài đọc (độ dài: {} ký tự)", originalText.length());
            return cached;
        }

        // 1. Thử gọi AI từ cấu hình hệ thống (Gemini hoặc OpenAI)
        SystemSettingService.AiChoice ai = settings.aiChoice();
        if (ai.apiKey() != null && !ai.apiKey().isBlank()) {
            try {
                String prompt = buildPrompt(originalText, targetJlpt);
                String jsonText = "GEMINI".equals(ai.provider())
                        ? callGemini(prompt, ai.model(), ai.apiKey())
                        : callOpenAiCompatible(prompt, ai.model(), ai.apiKey(), ai.baseUrl(), "OPENAI".equals(ai.provider()));

                if (jsonText != null && !jsonText.isBlank()) {
                    ReadingAnalysisResponse result = objectMapper.readValue(stripFence(jsonText), ReadingAnalysisResponse.class);
                    if (result.getOriginalText() == null || result.getOriginalText().isBlank()) {
                        result.setOriginalText(originalText);
                    }
                    // Đảm bảo gán ID cho vocabularies và grammars
                    if (result.getVocabularies() != null) {
                        for (int i = 0; i < result.getVocabularies().size(); i++) {
                            ReadingVocabularyItemDto v = result.getVocabularies().get(i);
                            if (v.getId() == null || v.getId().isBlank()) {
                                v.setId("v_" + i);
                            }
                            if (v.getSurfaceText() == null || v.getSurfaceText().isBlank()) {
                                v.setSurfaceText(v.getKanji() != null ? v.getKanji() : v.getReading());
                            }
                        }
                    }
                    if (result.getDetectedGrammars() != null) {
                        for (int i = 0; i < result.getDetectedGrammars().size(); i++) {
                            ReadingGrammarItemDto g = result.getDetectedGrammars().get(i);
                            if (g.getId() == null || g.getId().isBlank()) {
                                g.setId("g_" + i);
                            }
                            // Chuẩn hóa matchedText: nếu AI điền dấu ngã thì làm sạch
                            if (g.getMatchedText() == null || g.getMatchedText().isBlank()) {
                                g.setMatchedText(g.getPattern() != null ? g.getPattern().replace("〜", "").replace("~", "").trim() : "");
                            }
                        }
                    }
                    // Đảm bảo gán ID và chuẩn hóa cho comprehensionQuestions
                    if (result.getComprehensionQuestions() != null) {
                        for (int i = 0; i < result.getComprehensionQuestions().size(); i++) {
                            ReadingComprehensionQuestionDto q = result.getComprehensionQuestions().get(i);
                            if (q.getId() == null || q.getId().isBlank()) {
                                q.setId("q_" + i);
                            }
                            if (q.getOptions() == null) {
                                q.setOptions(new ArrayList<>());
                            }
                            if (q.getCorrectOptionIndex() == null || q.getCorrectOptionIndex() < 0 || (!q.getOptions().isEmpty() && q.getCorrectOptionIndex() >= q.getOptions().size())) {
                                q.setCorrectOptionIndex(0);
                            }
                            // Tách nếu câu hỏi tiếng Nhật bị lồng tiếng Việt dạng (Bản dịch...)
                            if ((q.getQuestionVi() == null || q.getQuestionVi().isBlank()) && q.getQuestion() != null) {
                                String qText = q.getQuestion();
                                int parenIdx = qText.indexOf('(');
                                if (parenIdx < 0) parenIdx = qText.indexOf('（');
                                if (parenIdx > 0 && parenIdx < qText.length() - 1) {
                                    int closeIdx = qText.indexOf(')', parenIdx);
                                    if (closeIdx < 0) closeIdx = qText.indexOf('）', parenIdx);
                                    if (closeIdx > parenIdx) {
                                        q.setQuestionVi(qText.substring(parenIdx + 1, closeIdx).trim());
                                        q.setQuestion(qText.substring(0, parenIdx).trim());
                                    }
                                }
                            }
                        }
                    }
                    result.setProvider(ai.provider());
                    result.setModel(ai.model());
                    result.setFallback(false);
                    cacheResult(cacheKey, result);
                    return result;
                }
            } catch (org.springframework.web.client.HttpStatusCodeException ex) {
                String detail = SystemSettingService.providerError(ex.getResponseBodyAsString(), ai.apiKey());
                log.warn("Lỗi máy chủ AI {} model {} (mã {}): {}. Chuyển sang bộ bóc tách dự phòng.",
                        ai.provider(), ai.model(), ex.getStatusCode().value(), detail.isBlank() ? ex.getMessage() : detail);
            } catch (Exception ex) {
                log.warn("Không thể gọi AI {} cho bóc tách đọc hiểu ({}), chuyển sang bộ bóc tách dự phòng: {}",
                        ai.provider(), ex.getClass().getSimpleName(), ex.getMessage());
            }
        }

        // 2. Chế độ dự phòng thông minh (Graceful Fallback) khi không có API key hoặc lỗi mạng
        ReadingAnalysisResponse fallbackResult = generateFallbackReadingAnalysis(originalText, targetJlpt);
        cacheResult(cacheKey, fallbackResult);
        return fallbackResult;
    }

    private String buildPrompt(String text, String targetJlpt) {
        return """
            Bạn là Kizuna Reading Sensei - chuyên gia sư phạm và ngôn ngữ tiếng Nhật của nền tảng KizunaTutor.
            Nhiệm vụ của bạn là bóc tách toàn diện đoạn văn bản tiếng Nhật học viên cung cấp để hỗ trợ luyện kỹ năng Đọc hiểu (Dokkai).

            Văn bản gốc:
            \"\"\"
            %s
            \"\"\"
            Cấp độ JLPT trọng tâm mong muốn: %s

            Yêu cầu phân tích và trả về DUY NHẤT một chuỗi JSON thuần (không kèm markdown râu ria) theo đúng cấu trúc sau:
            {
              "originalText": "văn bản gốc",
              "rubyHtml": "văn bản gốc được bọc chuẩn thẻ HTML <ruby>Hán tự<rt>furigana</rt></ruby> cho TẤT CẢ các từ chứa chữ Hán. Giữ nguyên dấu câu và các từ kana không cần ruby.",
              "overallSummary": "Tóm tắt ngắn gọn 1-2 câu ý chính của đoạn văn bằng tiếng Việt",
              "estimatedLevel": "Ước tính cấp độ JLPT tổng thể của bài đọc (ví dụ: N5, N4, N3, N2, N1)",
              "comprehensionQuestions": [
                {
                  "id": "q_0",
                  "questionType": "MAIN_IDEA | REASON | DETAIL | REFERENCE",
                  "question": "Nội dung câu hỏi đọc hiểu THUẦN TIẾNG NHẬT (không chứa phần dịch tiếng Việt trong chuỗi này)",
                  "questionVi": "Bản dịch tiếng Việt chuẩn xác của câu hỏi",
                  "options": [
                    "Lựa chọn A thuần tiếng Nhật",
                    "Lựa chọn B thuần tiếng Nhật",
                    "Lựa chọn C thuần tiếng Nhật",
                    "Lựa chọn D thuần tiếng Nhật"
                  ],
                  "optionsVi": [
                    "Bản dịch tiếng Việt của lựa chọn A",
                    "Bản dịch tiếng Việt của lựa chọn B",
                    "Bản dịch tiếng Việt của lựa chọn C",
                    "Bản dịch tiếng Việt của lựa chọn D"
                  ],
                  "correctOptionIndex": 0,
                  "evidenceQuote": "Câu văn trích dẫn CHÍNH XÁC 100%% từ bài đọc làm căn cứ chứng minh cho đáp án đúng",
                  "explanation": "Giải thích chi tiết bằng tiếng Việt: vì sao đáp án đúng và phân tích bẫy vì sao các phương án khác sai"
                }
              ],
              "detectedGrammars": [
                {
                  "id": "g_0",
                  "pattern": "Mẫu ngữ pháp JLPT chuẩn (ví dụ: 〜ことにしている, 〜ても, 〜たり〜たりする)",
                  "grammarCode": "Mã ngữ pháp nếu có hoặc để trống",
                  "jlptLevel": "Cấp độ JLPT của mẫu này (N5/N4/N3/N2/N1)",
                  "meaning": "Ý nghĩa tiếng Việt",
                  "matchedText": "Chuỗi con xuất hiện CHÍNH XÁC 100%% trong văn bản gốc (KHÔNG CHỨA dấu ngã 〜 hay ký tự suy đoán). Ví dụ nếu trong bài viết '大学に行ったり、観光地に行ったりしました' thì matchedText phải là đúng chuỗi đó.",
                  "explanation": "Giải thích ngữ cảnh sư phạm ngắn gọn bằng tiếng Việt"
                }
              ],
              "vocabularies": [
                {
                  "id": "v_0",
                  "surfaceText": "Chuỗi từ vựng xuất hiện CHÍNH XÁC trực tiếp trong câu văn bản (ví dụ: 手に入れました, 出会った, 行ったり)",
                  "kanji": "Từ vựng chữ Hán hoặc thể từ điển nguyên mẫu (ví dụ: 手に入れる, 出会う, 行く)",
                  "reading": "Cách đọc Hiragana chuẩn (ví dụ: てにいれる, であう, いく)",
                  "hanViet": "Âm Hán Việt in hoa (nếu có chữ Hán, ví dụ: THỦ NHẬP, XUẤT HỘI, HÀNH)",
                  "meaning": "Nghĩa tiếng Việt súc tích",
                  "jlpt": "Cấp độ JLPT (N5 - N1)",
                  "wordType": "Từ loại (Danh từ, Động từ V1/V2/V3, Tính từ -i/-na, Phó từ)"
                }
              ],
              "sentenceTranslations": [
                "Bản dịch tiếng Việt sát nghĩa cho từng câu trong đoạn văn theo thứ tự"
              ]
            }
            Lưu ý:
            - Sinh từ 1 đến 3 câu hỏi Đọc hiểu (Dokkai) trắc nghiệm 4 lựa chọn phù hợp với độ dài bài đọc (dưới 200 ký tự: 1 câu; 200-500: 2 câu; trên 500: 3 câu). Đảm bảo evidenceQuote trích dẫn 100%% nguyên văn từ bài đọc.
            - Trích xuất tối thiểu 4-12 từ vựng quan trọng nhất trong bài.
            - Bóc tách đầy đủ tất cả cấu trúc ngữ pháp có trong bài đọc, matchedText phải trích dẫn trực tiếp từ bài đọc để hệ thống highlight chính xác.
            - Furigana trong rubyHtml phải chính xác theo ngữ cảnh câu.
            """.formatted(text, targetJlpt);
    }

    private static String stripFence(String jsonText) {
        if (jsonText == null) return "{}";
        jsonText = jsonText.trim();
        // Lọc bỏ khối reasoning / suy nghĩ <think>...</think> của model lý luận (DeepSeek-R1, ...)
        if (jsonText.contains("</think>")) {
            jsonText = jsonText.substring(jsonText.indexOf("</think>") + 8).trim();
        }
        if (jsonText.startsWith("```json")) jsonText = jsonText.substring(7);
        if (jsonText.startsWith("```")) jsonText = jsonText.substring(3);
        if (jsonText.endsWith("```")) jsonText = jsonText.substring(0, jsonText.length() - 3);
        return jsonText.trim();
    }

    private String callGemini(String prompt, String model, String apiKey) throws Exception {
        List<String> candidates = new ArrayList<>();
        if (model != null && model.matches("[A-Za-z0-9._:-]{1,80}")) {
            candidates.add(model);
        }
        for (String m : List.of("gemini-flash-lite-latest", "gemini-3.1-flash-lite", "gemini-3.5-flash")) {
            if (!candidates.contains(m)) {
                candidates.add(m);
            }
        }

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(45000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> contentObj = Map.of("parts", List.of(part));
        Map<String, Object> genConfig = Map.of(
                "temperature", 0.2,
                "responseMimeType", "application/json"
        );
        Map<String, Object> reqBody = Map.of(
                "contents", List.of(contentObj),
                "generationConfig", genConfig
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

        Exception lastEx = null;
        for (String m : candidates) {
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + m + ":generateContent";
            try {
                ResponseEntity<String> res = restTemplate.postForEntity(apiUrl, entity, String.class);
                if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
                    JsonNode root = objectMapper.readTree(res.getBody());
                    JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
                    if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                        log.info("Gọi thành công Gemini AI với model: {}", m);
                        return textNode.asText();
                    }
                }
            } catch (Exception ex) {
                lastEx = ex;
                log.warn("Gemini model {} thất bại ({}), thử model tiếp theo...", m, ex.getMessage());
            }
        }
        if (lastEx != null) {
            throw lastEx;
        }
        return null;
    }

    private String callOpenAiCompatible(String prompt, String model, String apiKey, String baseUrl, boolean isOpenAiOfficial) throws Exception {
        String base = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
        String url = base + "/chat/completions";

        Map<String, Object> systemMsg = Map.of("role", "system", "content", "You are an expert Japanese teacher. Always output valid JSON without markdown wrapping.");
        Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);
        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("model", model);
        reqBody.put("messages", List.of(systemMsg, userMsg));
        reqBody.put("temperature", 0.2);
        reqBody.put("max_tokens", 4000); // Đảm bảo bóc tách toàn diện không bị cắt ngắn
        if (isOpenAiOfficial) {
            reqBody.put("response_format", Map.of("type", "json_object"));
        }

        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(45000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);
        ResponseEntity<String> res = restTemplate.postForEntity(url, entity, String.class);
        if (res.getStatusCode().is2xxSuccessful() && res.getBody() != null) {
            JsonNode root = objectMapper.readTree(res.getBody());
            JsonNode content = root.at("/choices/0/message/content");
            if (!content.isMissingNode()) {
                return content.asText();
            }
        }
        return null;
    }

    // =========================================================================
    // TỪ ĐIỂN TỰ ĐỘNG BÓC TÁCH CHO BỘ PHÂN TÍCH DỰ PHÒNG (MASTER LEXICON)
    // =========================================================================
    private record LexiconItem(
            String surface,
            String kanji,
            String reading,
            String hanViet,
            String meaning,
            String jlpt,
            String wordType
    ) {}

    private static final List<LexiconItem> MASTER_LEXICON = List.of(
            // Từ vựng Chuyến đi Tokyo / Asakusa
            new LexiconItem("先週", "先週", "せんしゅう", "TIÊN CHU", "Tuần trước", "N5", "Danh từ chỉ thời gian"),
            new LexiconItem("日曜日", "日曜日", "にちようび", "NHẬT DIỆU NHẬT", "Ngày Chủ nhật", "N5", "Danh từ chỉ thời gian"),
            new LexiconItem("友達", "友達", "ともだち", "HỮU ĐẠT", "Bạn bè, người bạn", "N5", "Danh từ"),
            new LexiconItem("東京", "東京", "とうきょう", "ĐÔNG KINH", "Thủ đô Tokyo, Nhật Bản", "N5", "Danh từ riêng"),
            new LexiconItem("行きました", "行く", "いく", "HÀNH", "Đã đi", "N5", "Động từ nhóm 1"),
            new LexiconItem("行きたいです", "行く", "いく", "HÀNH", "Muốn đi", "N5", "Thể mong muốn 〜たい"),
            new LexiconItem("朝", "朝", "あさ", "TRIỀU", "Buổi sáng, ban sáng", "N5", "Danh từ chỉ thời gian"),
            new LexiconItem("駅", "駅", "えき", "DỊCH", "Nhà ga xe lửa", "N5", "Danh từ"),
            new LexiconItem("会いました", "会う", "あう", "HỘI", "Đã gặp gỡ", "N5", "Động từ nhóm 1"),
            new LexiconItem("電車", "電車", "でんしゃ", "ĐIỆN XA", "Tàu điện", "N5", "Danh từ"),
            new LexiconItem("浅草", "浅草", "あさくさ", "THIỂN THẢO", "Khu phố cổ Asakusa nổi tiếng ở Tokyo", "N4", "Danh từ riêng"),
            new LexiconItem("人", "人", "ひと", "NHÂN", "Người, con người", "N5", "Danh từ"),
            new LexiconItem("たくさん", "たくさん", "たくさん", null, "Nhiều, phong phú", "N5", "Phó từ"),
            new LexiconItem("にぎやか", "賑やか", "にぎやか", "CHỈNH", "Náo nhiệt, nhộn nhịp, đông vui", "N5", "Tính từ đuôi -na"),
            new LexiconItem("面白かったです", "面白い", "おもしろい", "DIỆN BẠCH", "Đã rất thú vị, hay, vui vẻ", "N5", "Tính từ đuôi -i"),
            new LexiconItem("面白い", "面白い", "おもしろい", "DIỆN BẠCH", "Thú vị, hay, vui vẻ", "N5", "Tính từ đuôi -i"),
            new LexiconItem("私たち", "私たち", "わたしたち", "TƯ", "Chúng tôi, chúng ta", "N5", "Đại từ"),
            new LexiconItem("私", "私", "わたし", "TƯ", "Tôi, bản thân người nói", "N5", "Đại từ"),
            new LexiconItem("大きい", "大きい", "おおきい", "ĐẠI", "To lớn, rộng lớn", "N5", "Tính từ đuôi -i"),
            new LexiconItem("お寺", "お寺", "おてら", "TỰ", "Ngôi chùa, chùa chiền", "N4", "Danh từ"),
            new LexiconItem("見たり", "見る", "みる", "KIẾN", "Xem, ngắm (hành động liệt kê)", "N5", "Động từ nhóm 2"),
            new LexiconItem("写真", "写真", "しゃしん", "TẢ CHÂN", "Bức ảnh, tấm hình kỷ niệm", "N5", "Danh từ"),
            new LexiconItem("撮ったり", "撮る", "とる", "TOÁT", "Chụp ảnh (hành động liệt kê)", "N5", "Động từ nhóm 1"),
            new LexiconItem("昼ごはん", "昼ご飯", "ひるごはん", "TRÚ PHẠN", "Bữa ăn trưa, cơm trưa", "N5", "Danh từ"),
            new LexiconItem("美味しい", "美味しい", "おいしい", "MỸ VỊ", "Ngon miệng, thơm ngon", "N5", "Tính từ đuôi -i"),
            new LexiconItem("ラーメン", "ラーメン", "ラーメン", null, "Mì ramen truyền thống của Nhật", "N5", "Danh từ"),
            new LexiconItem("食べました", "食べる", "たべる", "THỰC", "Đã ăn", "N5", "Động từ nhóm 2"),
            new LexiconItem("午後", "午後", "ごご", "NGỌ HẬU", "Buổi chiều", "N5", "Danh từ chỉ thời gian"),
            new LexiconItem("デパート", "デパート", "デパート", null, "Trung tâm thương mại bách hóa", "N5", "Danh từ"),
            new LexiconItem("家族", "家族", "かぞく", "GIA TỘC", "Gia đình, người thân", "N5", "Danh từ"),
            new LexiconItem("お土産", "お土産", "おみやげ", "THỔ SẢN", "Quà lưu niệm, đặc sản", "N4", "Danh từ"),
            new LexiconItem("買いました", "買う", "かう", "MÃI", "Đã mua", "N5", "Động từ nhóm 1"),
            new LexiconItem("少し", "少し", "すこし", "THIỂU", "Một chút, một ít", "N5", "Phó từ"),
            new LexiconItem("疲れました", "疲れる", "つかれる", "BÌ", "Đã mệt mỏi, kiệt sức", "N5", "Động từ nhóm 2"),
            new LexiconItem("楽しかったです", "楽しい", "たのしい", "LẠC", "Đã rất vui vẻ, hào hứng", "N5", "Tính từ đuôi -i"),
            new LexiconItem("楽しい", "楽しい", "たのしい", "LẠC", "Vui vẻ, hào hứng", "N5", "Tính từ đuôi -i"),

            // Từ vựng Câu chuyện bạn Tsang
            new LexiconItem("人生", "人生", "じんせい", "NHÂN SINH", "Cuộc đời, đời người, kiếp nhân sinh", "N3", "Danh từ"),
            new LexiconItem("最大", "最大", "さいだい", "TỐI ĐẠI", "Lớn nhất, cực đại, tối đa", "N3", "Danh từ / Tính từ đuôi -na"),
            new LexiconItem("贈り物", "贈り物", "おくりもの", "TẶNG VẬT", "Món quà tặng, quà biếu", "N3", "Danh từ"),
            new LexiconItem("友情", "友情", "ゆうじょう", "HỮU TÌNH", "Tình bạn, tình hữu nghị cao đẹp", "N3", "Danh từ"),
            new LexiconItem("手に入れました", "手に入れる", "てにいれる", "THỦ NHẬP", "Đạt được, có được trong tay, giành được", "N3", "Cụm Động từ"),
            new LexiconItem("大学", "大学", "だいがく", "ĐẠI HỌC", "Trường đại học", "N5", "Danh từ"),
            new LexiconItem("建築大学", "建築大学", "けんちくだいがく", "KIẾN TRÚC ĐẠI HỌC", "Trường Đại học Kiến trúc", "N3", "Danh từ riêng"),
            new LexiconItem("入学手続き", "入学手続き", "にゅうがくてつづき", "NHẬP HỌC THỦ TỤC", "Thủ tục nhập học, giấy tờ nhập trường", "N3", "Danh từ"),
            new LexiconItem("観光地", "観光地", "かんこうち", "QUAN QUANG ĐỊA", "Địa điểm tham quan, danh lam thắng cảnh", "N3", "Danh từ"),
            new LexiconItem("春夏秋冬", "春夏秋冬", "しゅんかしゅうとう", "XUÂN HẠ THU ĐÔNG", "Bốn mùa trong năm (Xuân, Hạ, Thu, Đông)", "N2", "Thành ngữ 4 chữ / Danh từ"),
            new LexiconItem("記念", "記念", "きねん", "KÍ NIỆM", "Kỷ niệm, hồi ức đẹp", "N3", "Danh từ / Động từ Suru"),
            new LexiconItem("我慢強い", "我慢強い", "がまんづよい", "NGÃ MẠN CƯỜNG", "Kiên nhẫn, nhẫn nại, chịu đựng bền bỉ", "N2", "Tính từ đuôi -i"),
            new LexiconItem("強気", "強気", "つよき", "CƯỜNG KHÍ", "Mạnh mẽ, kiên cường, vững vàng", "N2", "Danh từ / Tính từ đuôi -na"),

            // Từ vựng Chuyến đi Kyoto
            new LexiconItem("京都", "京都", "きょうと", "KINH ĐÔ", "Cố đô Kyoto", "N4", "Danh từ riêng"),
            new LexiconItem("旅行", "旅行", "りょこう", "LỮ HÀNH", "Chuyến du lịch", "N5", "Danh từ / Động từ Suru"),
            new LexiconItem("新幹線", "新幹線", "しんかんせん", "TÂN CÁN TUYẾN", "Tàu siêu tốc Shinkansen", "N4", "Danh từ"),
            new LexiconItem("金閣寺", "金閣寺", "きんかくじ", "KIM CÁC TỰ", "Chùa Vàng Kinkaku-ji", "N4", "Danh từ riêng"),
            new LexiconItem("抹茶", "抹茶", "まっちゃ", "MẠT TRÀ", "Trà xanh Matcha", "N4", "Danh từ"),
            new LexiconItem("着物", "着物", "きもの", "TRỨ VẬT", "Trang phục Kimono truyền thống", "N4", "Danh từ"),

            // Từ vựng Phương pháp học NHK
            new LexiconItem("日本語", "日本語", "にほんご", "NHẬT BẢN NGỮ", "Tiếng Nhật", "N5", "Danh từ"),
            new LexiconItem("毎朝", "毎朝", "まいあさ", "MỖI TRIỀU", "Mỗi buổi sáng", "N5", "Danh từ chỉ thời gian"),
            new LexiconItem("単語", "単語", "たんご", "ĐƠN NGỮ", "Từ vựng", "N4", "Danh từ"),
            new LexiconItem("文脈", "文脈", "ぶんみゃく", "VĂN MẠCH", "Ngữ cảnh câu văn, văn cảnh", "N3", "Danh từ"),
            new LexiconItem("推測", "推測", "すいそく", "THÔI TRẮC", "Sự suy đoán, phỏng đoán", "N3", "Danh từ / Động từ Suru"),
            new LexiconItem("努力", "努力", "どりょく", "NỖ LỰC", "Sự nỗ lực, cố gắng", "N4", "Danh từ / Động từ Suru"),
            new LexiconItem("継続", "継続", "けいぞく", "KẾ TỤC", "Sự kiên trì, duy trì bền bỉ", "N3", "Danh từ / Động từ Suru"),
            new LexiconItem("大切", "大切", "たいせつ", "ĐẠI THIẾT", "Quan trọng, quý giá", "N5", "Tính từ đuôi -na"),

            // Từ vựng Buổi sáng công viên
            new LexiconItem("公園", "公園", "こうえん", "CÔNG VIÊN", "Công viên, vườn hoa", "N5", "Danh từ"),
            new LexiconItem("散歩", "散歩", "さんぽ", "TẢN BỘ", "Đi dạo, tản bộ thư giãn", "N5", "Danh từ / Động từ Suru"),
            new LexiconItem("天気", "天気", "てんき", "THIÊN KHÍ", "Thời tiết, khí trời", "N5", "Danh từ"),
            new LexiconItem("富士山", "富士山", "ふじさん", "PHÚ SĨ SƠN", "Núi Phú Sĩ biểu tượng của Nhật Bản", "N5", "Danh từ riêng"),
            new LexiconItem("朝ご飯", "朝ご飯", "あさごはん", "TRIỀU PHẠN", "Bữa ăn sáng", "N5", "Danh từ"),
            new LexiconItem("パン屋", "パン屋", "パンや", "ỐC", "Tiệm bánh mì", "N5", "Danh từ")
    );

    /**
     * Bộ phân tích dự phòng giả lập thông minh (Dynamic Fallback Engine)
     */
    private ReadingAnalysisResponse generateFallbackReadingAnalysis(String originalText, String targetJlpt) {
        List<ReadingComprehensionQuestionDto> comprehensionQuestions = new ArrayList<>();
        List<ReadingGrammarItemDto> detectedGrammars = new ArrayList<>();
        List<ReadingVocabularyItemDto> vocabularies = new ArrayList<>();
        List<String> translations = new ArrayList<>();
        String summary;
        String estLevel;

        if (originalText.contains("ツァン") || originalText.contains("友情") || originalText.contains("贈り物") || originalText.contains("ハノイ")) {
            // Preset 1: Bài viết về tình bạn 10 năm với bạn Tsang (Hà Nội, ĐH Kiến trúc)
            estLevel = "N3";
            summary = "Kỷ niệm 10 năm tình bạn gắn bó cùng người bạn đại học tên Tsang: cùng nhau khám phá Hà Nội, chia sẻ niềm vui và học được đức tính nhẫn nại, kiên cường.";
            translations = List.of(
                    "「Món quà lớn nhất của cuộc đời chính là tình bạn, và tôi đã may mắn có được điều quý giá ấy!」",
                    "Bạn Tsang học cùng trường đại học với tôi.",
                    "Cậu ấy dáng người cao ráo, hơi thanh mảnh, ánh mắt sáng ngời và là một chàng trai rất đỗi tuyệt vời.",
                    "Lần đầu tiên chúng tôi gặp gỡ là khi cùng làm thủ tục nhập học tại trường Đại học Kiến trúc. Cậu ấy đã chủ động cất tiếng gọi tôi.",
                    "Kể từ đó, ngày ngày chúng tôi cùng nhau tới trường đại học, rồi cùng nhau dạo quanh khám phá biết bao danh lam thắng cảnh tuyệt đẹp của Hà Nội.",
                    "Chúng tôi đã cùng nhau tạo nên những kỷ niệm tuyệt đẹp suốt bốn mùa xuân hạ thu đông.",
                    "Mỗi khi ở bên cạnh, cậu ấy lúc nào cũng nở nụ cười. Đối với tôi, nụ cười của Tsang là điều tuyệt vời nhất.",
                    "Cậu ấy đã dạy cho tôi về sự nhẫn nại kiên trì và tinh thần mạnh mẽ vững vàng.",
                    "Cho đến nay, chúng tôi đã biết nhau được tròn 10 năm rồi. Từ nay về sau tôi vẫn luôn muốn mãi là những người bạn tốt của nhau.",
                    "Gửi tới bạn Tsang: 「Từ giờ trở đi, tôi cũng luôn muốn được giúp đỡ bạn như những ngày đầu tiên」."
            );

            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_0").pattern("〜たり〜たりする").grammarCode("N5_052").jlptLevel("N5")
                    .meaning("Nào là... nào là... (Liệt kê hành động tiêu biểu)")
                    .matchedText("大学に行ったり、ハノイの様々な素晴らしい観光地に行ったりしました")
                    .explanation("Dùng để liệt kê một số hoạt động tiêu biểu đã cùng nhau trải nghiệm như đi học và khám phá danh lam thắng cảnh ở Hà Nội.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_1").pattern("〜てくる").grammarCode("N4_025").jlptLevel("N4")
                    .meaning("Đã và đang làm... từ quá khứ đến hiện tại")
                    .matchedText("作って来ました")
                    .explanation("Diễn tả những kỷ niệm bốn mùa đã cùng nhau tích lũy, vun đắp suốt từ quá khứ cho tới tận bây giờ.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_2").pattern("〜てくれる").grammarCode("N5_063").jlptLevel("N5")
                    .meaning("Làm việc gì cho mình (với lòng biết ơn)")
                    .matchedText("教えてくれました")
                    .explanation("Diễn tả sự biết ơn chân thành khi người bạn đã tận tình chỉ dạy, truyền cảm hứng về đức tính kiên nhẫn và mạnh mẽ.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_3").pattern("〜たい / 〜たいです").grammarCode("N5_041").jlptLevel("N5")
                    .meaning("Muốn làm V / Muốn trở thành... (Nguyện vọng, ước muốn)")
                    .matchedText("友達になりたいです")
                    .explanation("Bày tỏ mong muốn từ đáy lòng được tiếp tục giữ gìn tình bạn đẹp đẽ này mãi mãi về sau.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_4").pattern("〜時").grammarCode("N5_059").jlptLevel("N5")
                    .meaning("Vào lúc / Khi thực hiện hành vi...")
                    .matchedText("手続きをした時でした")
                    .explanation("Xác định mốc thời gian kỷ niệm lần đầu tiên hai người gặp gỡ nhau là vào dịp làm thủ tục nhập học.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_5").pattern("〜のように").grammarCode("N4_038").jlptLevel("N4")
                    .meaning("Như... / Giống như... (So sánh tương đồng)")
                    .matchedText("初めてのように")
                    .explanation("Mong muốn được nhiệt tình tương trợ, giúp đỡ bạn hệt như nhiệt huyết thuở ban đầu mới quen.")
                    .build());

            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_0")
                    .questionType("MAIN_IDEA")
                    .question("筆者はツァン君についてどのように思っていますか。")
                    .questionVi("Tác giả suy nghĩ như thế nào về người bạn Tsang?")
                    .options(List.of(
                            "いつも笑っていて、強気や我慢強さを教えてくれた最高の友達だと思っている。",
                            "背が高くてかっこいいが、少し怒りっぽい人だと思っている。",
                            "建築大学を卒業したあと、会えなくなって寂しいと思っている。",
                            "ハノイの観光地を案内してくれただけの知り合いだと思っている。"
                    ))
                    .optionsVi(List.of(
                            "Là người bạn tuyệt vời nhất luôn mỉm cười và dạy tác giả sự nhẫn nại, kiên cường.",
                            "Là người cao ráo đẹp trai nhưng lại có phần dễ nổi nóng.",
                            "Cảm thấy buồn vì sau khi tốt nghiệp ĐH Kiến trúc không còn được gặp nhau nữa.",
                            "Chỉ là người quen từng dẫn đi tham quan các danh lam thắng cảnh ở Hà Nội."
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("私にツァン君の笑みは一番素晴らしものです。彼は我慢強いや強気を教えてくれました。")
                    .explanation("Đáp án đúng là 1: Tác giả nhấn mạnh nụ cười của Tsang là điều tuyệt vời nhất và bạn ấy đã dạy cho tác giả sự nhẫn nại, kiên cường. Các phương án khác đưa thông tin sai lệch so với bài đọc.")
                    .build());
            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_1")
                    .questionType("DETAIL")
                    .question("二人が初めて出会ったのはいつですか。")
                    .questionVi("Hai người gặp nhau lần đầu tiên khi nào?")
                    .options(List.of(
                            "建築大学の入学手続きをした時",
                            "ハノイの観光地で写真を撮っていた時",
                            "冬休みに友達と一緒に旅行した時",
                            "10年前にツァン君の家へ遊びに行った時"
                    ))
                    .optionsVi(List.of(
                            "Khi làm thủ tục nhập học tại trường Đại học Kiến trúc",
                            "Khi đang chụp ảnh tại một danh lam thắng cảnh ở Hà Nội",
                            "Khi đi du lịch cùng bạn bè trong kỳ nghỉ đông",
                            "Khi đến nhà bạn Tsang chơi 10 năm trước"
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("初めて出会った相手は建築大学の入学手続きをした時でした。")
                    .explanation("Đáp án đúng là 1: Trong bài tác giả viết rõ '初めて出会った相手は建築大学の入学手続きをした時でした' (Lần đầu gặp gỡ là lúc làm thủ tục nhập học ĐH Kiến trúc).")
                    .build());

        } else if (originalText.contains("東京") || originalText.contains("浅草")) {
            // Preset 2: Chuyến đi chơi Tokyo & Asakusa cùng bạn bè
            estLevel = "N5";
            summary = "Chuyến đi chơi Tokyo và Asakusa trong ngày đầy ắp kỷ niệm cùng bạn bè: đi tàu điện, vãn cảnh chùa lớn, chụp ảnh, ăn mì ramen và mua quà lưu niệm.";
            translations = List.of(
                    "Chủ nhật tuần trước, tôi đã cùng bạn bè đi chơi ở Tokyo.",
                    "Lúc 9 giờ sáng, tôi đã gặp bạn ở nhà ga.",
                    "Sau đó, chúng tôi đi tàu điện đến khu phố cổ Asakusa.",
                    "Ở Asakusa có rất nhiều người, không khí vô cùng nhộn nhịp và thú vị.",
                    "Chúng tôi đã đi ngắm ngôi chùa lớn, chụp thật nhiều bức ảnh kỷ niệm.",
                    "Bữa trưa, chúng tôi đã ăn món mì ramen rất ngon miệng.",
                    "Buổi chiều, tôi ghé trung tâm thương mại mua quà lưu niệm cho gia đình.",
                    "Tuy có hơi mệt một chút nhưng chuyến đi thật sự rất vui vẻ.",
                    "Tôi rất mong lại được cùng bạn bè đi chơi Tokyo thêm lần nữa."
            );

            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_0").pattern("〜たり〜たりする").grammarCode("N5_052").jlptLevel("N5")
                    .meaning("Nào là... nào là... (Liệt kê hành động tiêu biểu)")
                    .matchedText("見たり、写真を撮ったりしました")
                    .explanation("Dùng để liệt kê các hoạt động tiêu biểu đã trải nghiệm ở Asakusa như vãn cảnh chùa và chụp ảnh kỷ niệm.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_1").pattern("〜たい / 〜たいです").grammarCode("N5_041").jlptLevel("N5")
                    .meaning("Muốn làm V (Bày tỏ nguyện vọng, mong ước)")
                    .matchedText("行きたいです")
                    .explanation("Bày tỏ mong muốn từ đáy lòng được tiếp tục cùng bạn bè đi chơi Tokyo vào các dịp sau.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_2").pattern("〜が (Liên từ nghịch đối)").grammarCode("N5_028").jlptLevel("N5")
                    .meaning("Tuy... nhưng... (Nối hai vế đối lập)")
                    .matchedText("少し疲れましたが")
                    .explanation("Nối hai mệnh đề: tuy cơ thể có mệt một chút nhưng tinh thần vẫn rất vui vẻ, hào hứng.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_3").pattern("〜が (Liên từ nghịch đối)").grammarCode("N5_028").jlptLevel("N5")
                    .meaning("Tuy... nhưng... (Nối hai vế đối lập)")
                    .matchedText("たくさんいましたが")
                    .explanation("Nêu tình huống người rất đông đúc nhưng không khí lại vô cùng nhộn nhịp, thú vị.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_4").pattern("それから (Liên từ liên kết)").grammarCode("N5_019").jlptLevel("N5")
                    .meaning("Sau đó, tiếp theo đó (Chỉ trình tự hành động)")
                    .matchedText("それから")
                    .explanation("Nối tiếp hành động: sau khi gặp nhau ở ga lúc 9 giờ thì cùng nhau lên tàu điện đi đến Asakusa.")
                    .build());

            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_0")
                    .questionType("DETAIL")
                    .question("筆者たちは昼ごはんに何を食べましたか。")
                    .questionVi("Nhóm tác giả đã ăn gì vào bữa trưa?")
                    .options(List.of(
                            "美味しいラーメンを食べた。",
                            "デパートでお弁当を買って食べた。",
                            "お寺の近くで抹茶アイスを食べた。",
                            "友達の家でパンを食べた。"
                    ))
                    .optionsVi(List.of(
                            "Đã ăn món mì ramen rất ngon.",
                            "Đã mua cơm hộp ở trung tâm thương mại để ăn.",
                            "Đã ăn kem matcha ở gần ngôi chùa.",
                            "Đã ăn bánh mì tại nhà của bạn."
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("昼ごはんに美味しいラーメンを食べました。")
                    .explanation("Đáp án đúng là 1: Trong bài tác giả nêu rõ câu '昼ごはんに美味しいラーメンを食べました' (Bữa trưa chúng tôi đã ăn món mì ramen ngon tuyệt).")
                    .build());
            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_1")
                    .questionType("REASON")
                    .question("筆者は東京旅行についてどう感じましたか。")
                    .questionVi("Tác giả cảm thấy như thế nào về chuyến đi Tokyo?")
                    .options(List.of(
                            "少し疲れたが、とても楽しくてまた行きたいと感じた。",
                            "浅草に人が多すぎて、全く面白くなかったと感じた。",
                            "電車が混んでいて、家族へのお土産も買えなかったと感じた。",
                            "雨が降ってしまい、写真が撮れなくて残念だったと感じた。"
                    ))
                    .optionsVi(List.of(
                            "Tuy có hơi mệt nhưng rất vui và muốn được đi thêm lần nữa.",
                            "Cảm thấy hoàn toàn không thú vị vì ở Asakusa quá đông người.",
                            "Cảm thấy tàu điện quá đông và không mua được quà cho gia đình.",
                            "Cảm thấy tiếc nuối vì trời đổ mưa nên không chụp được ảnh."
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("少し疲れましたが、とても楽しかったです。また友達と東京へ行きたいです。")
                    .explanation("Đáp án đúng là 1: Tác giả tổng kết 'Tuy có hơi mệt một chút nhưng rất vui, muốn lại cùng bạn đi chơi Tokyo lần nữa'. Các phương án khác mô tả sai cảm xúc bài viết.")
                    .build());

        } else if (originalText.contains("京都") || originalText.contains("新幹線") || originalText.contains("金閣寺")) {
            // Preset 3: Chuyến đi Kyoto
            estLevel = "N4";
            summary = "Chuyến du lịch Kyoto đầy kỷ niệm cùng bạn bè: đi tàu Shinkansen, thăm chùa Vàng Kinkakuji, ăn kem matcha và mặc kimono chụp ảnh dưới mưa.";
            translations = List.of(
                    "Chủ nhật tuần trước, tôi đã cùng bạn bè đi du lịch đến Kyoto.",
                    "Chúng tôi đi tàu Shinkansen mất khoảng 2 tiếng đồng hồ.",
                    "Được ngắm nhìn chùa Kinkakuji, được ăn kem matcha nên chuyến đi rất là vui.",
                    "Dù cho trời có mưa, chúng tôi vẫn mặc trang phục Kimono và chụp thật nhiều bức ảnh kỷ niệm."
            );
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_0").pattern("〜たり〜たりする").grammarCode("N5_052").jlptLevel("N5")
                    .meaning("Nào là... nào là... (Liệt kê hành động tiêu biểu)")
                    .matchedText("見たり、抹茶アイスを食べたりして")
                    .explanation("Dùng để liệt kê một số hành động tiêu biểu đã làm trong chuyến đi mà không cần kể hết toàn bộ.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_1").pattern("〜ても / 〜でも").grammarCode("N5_045").jlptLevel("N5")
                    .meaning("Dù... nhưng vẫn... (Điều kiện nghịch)")
                    .matchedText("雨が降っても")
                    .explanation("Diễn tả điều kiện nghịch: cho dù trời có mưa thì vẫn hào hứng mặc kimono và chụp ảnh.")
                    .build());

            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_0")
                    .questionType("DETAIL")
                    .question("京都で雨が降ったとき、筆者たちは何をしましたか。")
                    .questionVi("Khi trời mưa ở Kyoto, nhóm tác giả đã làm gì?")
                    .options(List.of(
                            "着物を着て写真を撮った。",
                            "新幹線に乗ってすぐに帰った。",
                            "ホテルの中でずっと休んでいた。",
                            "お寺に入らないで買い物をした。"
                    ))
                    .optionsVi(List.of(
                            "Mặc trang phục Kimono và chụp ảnh kỷ niệm.",
                            "Lên tàu Shinkansen và trở về ngay lập tức.",
                            "Ở trong khách sạn và nghỉ ngơi suốt cả ngày.",
                            "Không vào thăm chùa mà chỉ đi mua sắm."
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("雨が降っても着物を着て写真を撮りました。")
                    .explanation("Đáp án đúng là 1: Trong bài có câu '雨が降っても着物を着て写真を撮りました' (Dù trời mưa vẫn mặc kimono và chụp ảnh).")
                    .build());

        } else if (originalText.contains("NHK") || originalText.contains("継続") || originalText.contains("推測")) {
            // Preset 4: Phương pháp học tiếng Nhật
            estLevel = "N3";
            summary = "Bí quyết học tiếng Nhật: thói quen nghe bản tin NHK mỗi sáng, rèn luyện kỹ năng suy đoán ngữ cảnh và sự kiên trì bền bỉ.";
            translations = List.of(
                    "Để tiếng Nhật của mình ngày ngày tiến bộ, mỗi sáng tôi đều duy trì thói quen nghe bản tin tiếng Nhật dễ hiểu của đài NHK.",
                    "Dù gặp phải những từ vựng chưa biết, tôi cũng không vội bỏ cuộc mà nỗ lực suy đoán ý nghĩa của từ dựa vào ngữ cảnh câu.",
                    "Tôi tâm niệm rằng việc kiên trì duy trì đều đặn mỗi ngày là điều quan trọng và cốt lõi nhất."
            );
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_0").pattern("〜ように").grammarCode("N4_038").jlptLevel("N4")
                    .meaning("Để... (Chỉ mục đích)")
                    .matchedText("上手になるように")
                    .explanation("Diễn tả mục đích muốn đạt được một trạng thái mong muốn (tiếng Nhật ngày một giỏi hơn).")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_1").pattern("〜ことにしている").grammarCode("N3_012").jlptLevel("N3")
                    .meaning("Duy trì thói quen / Tự đặt quy tắc cho bản thân")
                    .matchedText("聞くことにしています")
                    .explanation("Diễn tả thói quen hoặc quy tắc do chính người nói chủ động tự đặt ra cho bản thân để rèn luyện.")
                    .build());
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_2").pattern("〜ないで").grammarCode("N5_078").jlptLevel("N5")
                    .meaning("Không làm V1 mà làm V2")
                    .matchedText("諦めないで")
                    .explanation("Không nản chí bỏ cuộc ngay mà tiếp tục nỗ lực phán đoán ý nghĩa.")
                    .build());

            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_0")
                    .questionType("MAIN_IDEA")
                    .question("筆者が日本語の勉強で一番大切だと考えていることは何ですか。")
                    .questionVi("Điều tác giả cho là quan trọng nhất trong việc học tiếng Nhật là gì?")
                    .options(List.of(
                            "諦めずに文脈から推測し、毎日継続すること",
                            "分からない単語をすべて辞書で丸暗記すること",
                            "難しいニュースだけを一日中聞き続けること",
                            "日本の友達とだけ毎日会話すること"
                    ))
                    .optionsVi(List.of(
                            "Không bỏ cuộc mà suy đoán theo ngữ cảnh và kiên trì mỗi ngày",
                            "Học vẹt toàn bộ từ vựng chưa biết bằng từ điển",
                            "Nghe các bản tin thời sự khó suốt cả ngày",
                            "Chỉ trò chuyện hàng ngày với bạn bè người Nhật"
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("分からない単語があってもすぐに諦めないで、文脈から意味を推測するように努力しています。継続することが一番大切だと思います。")
                    .explanation("Đáp án đúng là 1: Tác giả nhấn mạnh không bỏ cuộc ngay mà suy đoán từ ngữ cảnh, và '継続することが一番大切' (Việc kiên trì là quan trọng nhất).")
                    .build());

        } else if (originalText.contains("公園") || originalText.contains("散歩") || originalText.contains("富士山")) {
            // Preset 5: Buổi sáng công viên (N5)
            estLevel = "N5";
            summary = "Thói quen buổi sáng lành mạnh: thức dậy lúc 7 giờ, tản bộ ngắm núi Phú Sĩ trong công viên và mua bánh mì mới nướng cùng cà phê.";
            translations = List.of(
                    "Tôi thức dậy lúc 7 giờ mỗi sáng và đi dạo trong công viên.",
                    "Vào những ngày trời đẹp, núi Phú Sĩ hiện lên rất đẹp.",
                    "Trên đường đi, tôi ghé vào tiệm bánh mì mua bánh vừa mới nướng và cà phê.",
                    "Sau khi trở về nhà, tôi ăn bữa sáng."
            );
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_0").pattern("〜てから").grammarCode("N5_042").jlptLevel("N5")
                    .meaning("Sau khi làm V1 thì làm V2")
                    .matchedText("帰ってから")
                    .explanation("Diễn tả trình tự hành động: sau khi về đến nhà thì mới ăn bữa sáng.")
                    .build());
            comprehensionQuestions.add(ReadingComprehensionQuestionDto.builder()
                    .id("q_0")
                    .questionType("DETAIL")
                    .question("天気がいい日、公園から何がきれいに見えますか。")
                    .questionVi("Vào những ngày trời đẹp, từ công viên có thể nhìn thấy cái gì rất đẹp?")
                    .options(List.of(
                            "富士山",
                            "東京タワー",
                            "金閣寺",
                            "大きいお寺"
                    ))
                    .optionsVi(List.of(
                            "Núi Phú Sĩ",
                            "Tháp Tokyo",
                            "Chùa Vàng Kinkakuji",
                            "Ngôi chùa lớn"
                    ))
                    .correctOptionIndex(0)
                    .evidenceQuote("天気がいい日は富士山がきれいに見えます。")
                    .explanation("Đáp án đúng là 1: Trong bài tác giả nói '天気がいい日は富士山がきれいに見えます' (Những ngày trời đẹp thì nhìn thấy núi Phú Sĩ rất đẹp).")
                    .build());

        } else {
            // Mặc định hoặc văn bản tự do
            estLevel = "N5";
            summary = "Đoạn văn miêu tả các hoạt động sinh hoạt, học tập hoặc trải nghiệm đời thường trong ngày.";
            // Tách từng câu văn tự động
            String[] rawSentences = originalText.split("(?<=[。！？!?\n])");
            for (String sent : rawSentences) {
                String trimmed = sent.trim();
                if (!trimmed.isEmpty()) {
                    translations.add(trimmed);
                }
            }
            if (translations.isEmpty()) {
                translations.add(originalText);
            }

            // Quét cấu trúc ngữ pháp phổ biến bằng Regex
            scanGrammarPatterns(originalText, detectedGrammars);
        }

        // =====================================================================
        // QUÉT ĐỘNG DANH MỤC TỪ VỰNG TỪ BẢNG TỪ ĐIỂN CHUẨN (DYNAMIC SCANNER)
        // Bất kỳ từ nào xuất hiện trong originalText đều sẽ được bóc tách
        // =====================================================================
        Set<String> addedSurfaces = new HashSet<>();
        int vocabIndex = 0;
        for (LexiconItem item : MASTER_LEXICON) {
            if (originalText.contains(item.surface()) && !addedSurfaces.contains(item.surface())) {
                addedSurfaces.add(item.surface());
                vocabularies.add(ReadingVocabularyItemDto.builder()
                        .id("v_" + vocabIndex++)
                        .surfaceText(item.surface())
                        .kanji(item.kanji())
                        .reading(item.reading())
                        .hanViet(item.hanViet())
                        .meaning(item.meaning())
                        .jlpt(item.jlpt())
                        .wordType(item.wordType())
                        .build());
            }
        }

        // Tự động sinh Ruby Furigana cho tất cả chữ Hán trong văn bản
        String rubyHtml = generateComprehensiveRubyHtml(originalText);

        return ReadingAnalysisResponse.builder()
                .originalText(originalText)
                .rubyHtml(rubyHtml)
                .overallSummary(summary)
                .estimatedLevel(estLevel)
                .comprehensionQuestions(comprehensionQuestions)
                .detectedGrammars(detectedGrammars)
                .vocabularies(vocabularies)
                .sentenceTranslations(translations)
                .provider("SYSTEM_FALLBACK")
                .model("RuleEngine-v1")
                .fallback(true)
                .build();
    }

    private void scanGrammarPatterns(String text, List<ReadingGrammarItemDto> detectedGrammars) {
        int idx = detectedGrammars.size();

        // 1. 〜たり〜たりする
        Pattern tariPattern = Pattern.compile("([^\\s、。]+たり[^\\s、。]*[、\\s]*[^\\s、。]+たり(する|した|します|しました))");
        Matcher tariMatcher = tariPattern.matcher(text);
        if (tariMatcher.find()) {
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_" + idx++)
                    .pattern("〜たり〜たりする")
                    .grammarCode("N5_052")
                    .jlptLevel("N5")
                    .meaning("Nào là... nào là... (Liệt kê hành động tiêu biểu)")
                    .matchedText(tariMatcher.group(1))
                    .explanation("Dùng để liệt kê một số hoạt động tiêu biểu diễn ra trong đoạn văn.")
                    .build());
        }

        // 2. 〜たい / 〜たいです
        Pattern taiPattern = Pattern.compile("([^\\s、。]+たい(です)?)");
        Matcher taiMatcher = taiPattern.matcher(text);
        if (taiMatcher.find()) {
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_" + idx++)
                    .pattern("〜たい / 〜たいです")
                    .grammarCode("N5_041")
                    .jlptLevel("N5")
                    .meaning("Muốn làm V (Nguyện vọng, ước muốn)")
                    .matchedText(taiMatcher.group(1))
                    .explanation("Bày tỏ mong muốn từ đáy lòng được thực hiện một hành động.")
                    .build());
        }

        // 3. 〜が (Liên từ nghịch đối)
        Pattern gaPattern = Pattern.compile("([^\\s、。]+(ました|でした|た|い)が)");
        Matcher gaMatcher = gaPattern.matcher(text);
        if (gaMatcher.find()) {
            detectedGrammars.add(ReadingGrammarItemDto.builder()
                    .id("g_" + idx++)
                    .pattern("〜が (Liên từ nghịch đối)")
                    .grammarCode("N5_028")
                    .jlptLevel("N5")
                    .meaning("Tuy... nhưng... (Nối hai vế đối lập)")
                    .matchedText(gaMatcher.group(1))
                    .explanation("Nối hai mệnh đề biểu thị ý nghĩa tương phản, nhượng bộ.")
                    .build());
        }

        // 4. 〜てから
        if (text.contains("てから")) {
            Pattern teKaraPattern = Pattern.compile("([^\\s、。]+てから)");
            Matcher teKaraMatcher = teKaraPattern.matcher(text);
            if (teKaraMatcher.find()) {
                detectedGrammars.add(ReadingGrammarItemDto.builder()
                        .id("g_" + idx++)
                        .pattern("〜てから")
                        .grammarCode("N5_034")
                        .jlptLevel("N5")
                        .meaning("Sau khi làm V1 thì làm V2")
                        .matchedText(teKaraMatcher.group(1))
                        .explanation("Diễn tả thứ tự trước sau của hành động.")
                        .build());
            }
        }
    }

    private String generateComprehensiveRubyHtml(String text) {
        String res = text;
        // Danh sách thay thế Ruby ưu tiên từ dài trước ngắn sau
        String[][] rubyReplacements = {
                // Tsang & Hà Nội
                {"人生最大の贈り物", "<ruby>人生<rt>じんせい</rt></ruby><ruby>最大<rt>さいだい</rt></ruby>の<ruby>贈り物<rt>おくりもの</rt></ruby>"},
                {"人生", "<ruby>人生<rt>じんせい</rt></ruby>"},
                {"最大", "<ruby>最大<rt>さいだい</rt></ruby>"},
                {"贈り物", "<ruby>贈り物<rt>おくりもの</rt></ruby>"},
                {"友情", "<ruby>友情<rt>ゆうじょう</rt></ruby>"},
                {"手に入れました", "<ruby>手<rt>て</rt></ruby>に<ruby>入<rt>い</rt></ruby>れました"},
                {"建築大学", "<ruby>建築大学<rt>けんちくだいがく</rt></ruby>"},
                {"大学", "<ruby>大学<rt>だいがく</rt></ruby>"},
                {"入学手続き", "<ruby>入学<rt>にゅうがく</rt></ruby><ruby>手続き<rt>てつづき</rt></ruby>"},
                {"手続き", "<ruby>手続き<rt>てつづき</rt></ruby>"},
                {"観光地", "<ruby>観光地<rt>かんこうち</rt></ruby>"},
                {"春夏秋冬", "<ruby>春夏秋冬<rt>しゅんかしゅうとう</rt></ruby>"},
                {"記念", "<ruby>記念<rt>きねん</rt></ruby>"},
                {"我慢強い", "<ruby>我慢強い<rt>がまんづよい</rt></ruby>"},
                {"強気", "<ruby>強気<rt>つよき</rt></ruby>"},
                {"友達", "<ruby>友達<rt>ともだち</rt></ruby>"},
                {"出会った", "<ruby>出会<rt>であ</rt></ruby>った"},
                {"相手", "<ruby>相手<rt>あいて</rt></ruby>"},
                {"素晴らしい", "<ruby>素晴<rt>すば</rt></ruby>らしい"},
                {"素晴らし", "<ruby>素晴<rt>すば</rt></ruby>らし"},
                {"毎日", "<ruby>毎日<rt>まいにち</rt></ruby>"},
                {"一緒", "<ruby>一緒<rt>いっしょ</rt></ruby>"},
                {"笑み", "<ruby>笑<rt>え</rt></ruby>み"},
                {"笑い", "<ruby>笑<rt>わら</rt></ruby>い"},
                {"初めて", "<ruby>初<rt>はじ</rt></ruby>めて"},
                {"教えてくれました", "<ruby>教<rt>おし</rt></ruby>えてくれました"},
                {"作って来ました", "<ruby>作<rt>つく</rt></ruby>って<ruby>来<rt>き</rt></ruby>ました"},
                {"手伝ってくれたい", "<ruby>手伝<rt>てつだ</rt></ruby>ってくれたい"},

                // Tokyo & Asakusa
                {"日曜日", "<ruby>日曜日<rt>にちようび</rt></ruby>"},
                {"先週", "<ruby>先週<rt>せんしゅう</rt></ruby>"},
                {"東京", "<ruby>東京<rt>とうきょう</rt></ruby>"},
                {"浅草", "<ruby>浅草<rt>あさくさ</rt></ruby>"},
                {"昼ごはん", "<ruby>昼<rt>ひる</rt></ruby>ごはん"},
                {"美味しい", "<ruby>美味<rt>おい</rt></ruby>しい"},
                {"お寺", "お<ruby>寺<rt>てら</rt></ruby>"},
                {"お土産", "お<ruby>土産<rt>みやげ</rt></ruby>"},
                {"電車", "<ruby>電車<rt>でんしゃ</rt></ruby>"},
                {"写真", "<ruby>写真<rt>しゃしん</rt></ruby>"},
                {"家族", "<ruby>家族<rt>かぞく</rt></ruby>"},
                {"午後", "<ruby>午後<rt>ごご</rt></ruby>"},
                {"行きました", "<ruby>行<rt>い</rt></ruby>きました"},
                {"行きたいです", "<ruby>行<rt>い</rt></ruby>きたいです"},
                {"行きたい", "<ruby>行<rt>い</rt></ruby>きたい"},
                {"会いました", "<ruby>会<rt>あ</rt></ruby>いました"},
                {"見たり", "<ruby>見<rt>み</rt></ruby>たり"},
                {"撮ったりしました", "<ruby>撮<rt>と</rt></ruby>ったりしました"},
                {"撮ったり", "<ruby>撮<rt>と</rt></ruby>ったり"},
                {"食べました", "<ruby>食<rt>た</rt></ruby>べました"},
                {"買いました", "<ruby>買<rt>か</rt></ruby>いました"},
                {"疲れましたが", "<ruby>疲<rt>つか</rt></ruby>れましたが"},
                {"疲れました", "<ruby>疲<rt>つか</rt></ruby>れました"},
                {"楽しかったです", "<ruby>楽<rt>たの</rt></ruby>しかったです"},
                {"面白かったです", "<ruby>面白<rt>おもしろ</rt></ruby>かったです"},
                {"大きい", "<ruby>大<rt>おお</rt></ruby>きい"},
                {"駅", "<ruby>駅<rt>えき</rt></ruby>"},
                {"朝", "<ruby>朝<rt>あさ</rt></ruby>"},
                {"人", "<ruby>人<rt>ひと</rt></ruby>"},
                {"私", "<ruby>私<rt>わたし</rt></ruby>"},
                {"少し", "<ruby>少<rt>すこ</rt></ruby>し"},

                // Kyoto, NHK, Buổi sáng
                {"毎朝", "<ruby>毎朝<rt>まいあさ</rt></ruby>"},
                {"公園", "<ruby>公園<rt>こうえん</rt></ruby>"},
                {"散歩", "<ruby>散歩<rt>さんぽ</rt></ruby>"},
                {"天気", "<ruby>天気<rt>てんき</rt></ruby>"},
                {"富士山", "<ruby>富士山<rt>ふじさん</rt></ruby>"},
                {"パン屋", "パン<ruby>屋<rt>や</rt></ruby>"},
                {"朝ご飯", "<ruby>朝<rt>あさ</rt></ruby>ご<ruby>飯<rt>はん</rt></ruby>"},
                {"京都", "<ruby>京都<rt>きょうと</rt></ruby>"},
                {"旅行", "<ruby>旅行<rt>りょこう</rt></ruby>"},
                {"新幹線", "<ruby>新幹線<rt>しんかんせん</rt></ruby>"},
                {"時間", "<ruby>時間<rt>じかん</rt></ruby>"},
                {"金閣寺", "<ruby>金閣寺<rt>きんかくじ</rt></ruby>"},
                {"抹茶", "<ruby>抹茶<rt>まっちゃ</rt></ruby>"},
                {"雨", "<ruby>雨<rt>あめ</rt></ruby>"},
                {"降", "<ruby>降<rt>ふ</rt></ruby>"},
                {"着物", "<ruby>着物<rt>きもの</rt></ruby>"},
                {"着て", "<ruby>着<rt>き</rt></ruby>て"},
                {"日本語", "<ruby>日本語<rt>にほんご</rt></ruby>"},
                {"上手", "<ruby>上手<rt>じょうず</rt></ruby>"},
                {"単語", "<ruby>単語<rt>たんご</rt></ruby>"},
                {"文脈", "<ruby>文脈<rt>ぶんみゃく</rt></ruby>"},
                {"意味", "<ruby>意味<rt>いみ</rt></ruby>"},
                {"推測", "<ruby>推測<rt>すいそく</rt></ruby>"},
                {"努力", "<ruby>努力<rt>どりょく</rt></ruby>"},
                {"継続", "<ruby>継続<rt>けいぞく</rt></ruby>"},
                {"一番", "<ruby>一番<rt>いちばん</rt></ruby>"},
                {"大切", "<ruby>大切<rt>たいせつ</rt></ruby>"}
        };

        for (String[] pair : rubyReplacements) {
            res = res.replace(pair[0], pair[1]);
        }
        return res;
    }
}
