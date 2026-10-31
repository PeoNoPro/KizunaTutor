package com.kizuna.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.dto.grammar.AiGenerateSentenceRequest;
import com.kizuna.dto.grammar.AiGenerateSentenceResponse;
import com.kizuna.dto.grammar.GrammarExampleDto;
import com.kizuna.dto.grammar.GrammarRuleDto;
import com.kizuna.service.IGrammarService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarServiceImpl implements IGrammarService {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    @Value("${app.ai.gemini-api-key:}")
    private String configuredGeminiKey;

    @Value("${app.ai.gemini-model:gemini-2.5-flash}")
    private String configuredGeminiModel;

    // Bộ nhớ đệm danh sách ngữ pháp theo Level (N5 -> N1)
    private final Map<String, List<GrammarRuleDto>> grammarCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadGrammarFromOpenJLPT();
    }

    private void loadGrammarFromOpenJLPT() {
        String[] levels = {"n5", "n4", "n3", "n2", "n1"};
        for (String lvl : levels) {
            String upper = lvl.toUpperCase();
            try {
                // Ưu tiên đọc từ data/openjlpt/data/json/grammar/{lvl}.json
                File file = new File("data/openjlpt/data/json/grammar/" + lvl + ".json");
                if (file.exists()) {
                    try (InputStream fis = new FileInputStream(file)) {
                        readRulesFromStream(fis, upper);
                    }
                } else {
                    ClassPathResource res = new ClassPathResource("data/openjlpt/data/json/grammar/" + lvl + ".json");
                    if (res.exists()) {
                        try (InputStream cis = res.getInputStream()) {
                            readRulesFromStream(cis, upper);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[GrammarService] Không thể nạp ngữ pháp {}: {}", lvl, e.getMessage());
            }
        }
    }

    private void readRulesFromStream(InputStream is, String upper) throws Exception {
        List<Map<String, Object>> rawList = objectMapper.readValue(is, new TypeReference<>() {});
        List<GrammarRuleDto> rules = new ArrayList<>();
        for (Map<String, Object> map : rawList) {
            String id = (String) map.get("id");
            String rawPattern = (String) map.get("pattern");
            String rawRomaji = (String) map.get("romaji");
            String pattern = cleanGrammarPattern(rawPattern);
            String romaji = cleanGrammarRomaji(rawRomaji);
            String level = (String) map.get("level");
            String rawMeaning = (String) map.get("meaning");
            String rawFormation = (String) map.get("formation");
            String rawNotes = (String) map.get("notes");

            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) map.get("tags");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exRaw = (List<Map<String, Object>>) map.get("examples");
            List<GrammarExampleDto> examples = new ArrayList<>();
            if (exRaw != null) {
                for (Map<String, Object> exMap : exRaw) {
                    String exJa = (String) exMap.get("ja");
                    String exEn = (String) exMap.get("en");
                    examples.add(GrammarExampleDto.builder()
                            .ja(exJa)
                            .furigana((String) exMap.get("furigana"))
                            .en(exEn)
                            .vn(com.kizuna.util.GrammarVietnameseDictionary.translateExample(exJa, exEn))
                            .build());
                }
            }

            rules.add(GrammarRuleDto.builder()
                    .id(id)
                    .pattern(pattern)
                    .romaji(romaji)
                    .level(level != null ? level : upper)
                    .meaning(rawMeaning)
                    .meaningVn(com.kizuna.util.GrammarVietnameseDictionary.translateMeaning(id, pattern, rawMeaning, upper))
                    .formation(com.kizuna.util.GrammarVietnameseDictionary.translateFormation(rawFormation))
                    .notes(com.kizuna.util.GrammarVietnameseDictionary.translateNotes(id, pattern, rawNotes, upper))
                    .tags(tags != null ? tags : List.of())
                    .examples(examples)
                    .build());
        }
        grammarCache.put(upper, rules);
        log.info("[GrammarService] Đã nạp thành công {} mẫu ngữ pháp cấp độ {} (100% tiếng Việt).", rules.size(), upper);
    }

    @Override
    public Map<String, Long> getLevelCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String lvl : List.of("N5", "N4", "N3", "N2", "N1")) {
            counts.put(lvl, (long) grammarCache.getOrDefault(lvl, List.of()).size());
        }
        return counts;
    }

    @Override
    public Page<GrammarRuleDto> getGrammarRules(String level, String keyword, Pageable pageable) {
        String lvl = (level != null && !level.isBlank()) ? level.trim().toUpperCase() : "N5";
        List<GrammarRuleDto> list = grammarCache.getOrDefault(lvl, List.of());

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            list = list.stream().filter(g ->
                    (g.getPattern() != null && g.getPattern().toLowerCase().contains(kw)) ||
                    (g.getRomaji() != null && g.getRomaji().toLowerCase().contains(kw)) ||
                    (g.getMeaning() != null && g.getMeaning().toLowerCase().contains(kw)) ||
                    (g.getFormation() != null && g.getFormation().toLowerCase().contains(kw)) ||
                    (g.getMeaningVn() != null && g.getMeaningVn().toLowerCase().contains(kw))
            ).toList();
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<GrammarRuleDto> sub = (start <= end && start < list.size()) ? list.subList(start, end) : List.of();
        return new PageImpl<>(sub, pageable, list.size());
    }

    @Override
    public GrammarRuleDto getGrammarDetail(String id) {
        if (id == null) return null;
        for (List<GrammarRuleDto> rules : grammarCache.values()) {
            for (GrammarRuleDto r : rules) {
                if (id.equalsIgnoreCase(r.getId())) return r;
            }
        }
        return null;
    }

    @Override
    public File exportGrammarExcel() {
        String outputPath = "C:\\Users\\Vuh26\\Desktop\\KizunaTutor\\Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx";
        File file = new File(outputPath);
        if (file.exists() && file.length() > 50000) {
            log.info("[GrammarService] Sử dụng file Excel ngữ pháp sẵn có: {}", outputPath);
            return file;
        }
        return com.kizuna.util.GrammarExcelGenerator.generateExcel(outputPath);
    }

    @Override
    public AiGenerateSentenceResponse generateAiSentence(AiGenerateSentenceRequest req) {
        String pattern = req.getPattern() != null ? req.getPattern() : "〜てください";
        String context = req.getContext() != null ? req.getContext() : "Trong nhà hàng / Quán ăn";
        String level = req.getLevel() != null ? req.getLevel() : "N5";

        String apiKey = resolveApiKey(req.getApiKey());
        if (apiKey == null || apiKey.isBlank()) {
            // Fallback thông minh nếu chưa có key AI
            return buildFallbackAiResponse(pattern, context, level);
        }

        try {
            return callGeminiGrammarSensei(pattern, context, level, apiKey);
        } catch (Throwable t) {
            log.warn("[AiGrammar] Không thể gọi Gemini API: {}. Chuyển sang phản hồi thông minh.", t.getMessage());
            return buildFallbackAiResponse(pattern, context, level);
        }
    }

    private String resolveApiKey(String clientKey) {
        if (clientKey != null && !clientKey.isBlank()) return clientKey.trim();
        if (configuredGeminiKey != null && !configuredGeminiKey.isBlank() && !configuredGeminiKey.startsWith("AQ.")) {
            return configuredGeminiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank() && !envKey.startsWith("AQ.")) {
            return envKey.trim();
        }
        return null;
    }

    private AiGenerateSentenceResponse callGeminiGrammarSensei(String pattern, String context, String level, String apiKey) throws Exception {
        String prompt = """
            Bạn là Kizuna Sensei - Chuyên gia giảng dạy tiếng Nhật bản ngữ cho người Việt.
            YÊU CẦU BẮT BUỘC TỐI THƯỢNG:
            Hãy đặt DUY NHẤT 1 câu tiếng Nhật tự nhiên BẮT BUỘC SỬ DỤNG ĐÚNG MẪU NGỮ PHÁP: [%s] (cấp độ %s) trong đúng ngữ cảnh: [%s].
            TUYỆT ĐỐI KHÔNG ĐƯỢC THAY BẰNG MẪU KHÁC (câu tiếng Nhật bắt buộc phải xuất hiện cấu trúc ngữ pháp %s).
            Yêu cầu trả về DUY NHẤT một chuỗi JSON chuẩn (không dùng markdown code fence ```json) theo định dạng:
            {
              "japaneseSentence": "câu tiếng Nhật chuẩn chứa đúng mẫu %s",
              "furiganaHtml": "câu tiếng Nhật có gắn thẻ <ruby>chữ Hán<rt>furigana</rt></ruby> chuẩn xác",
              "vietnameseMeaning": "bản dịch tiếng Việt tự nhiên và chuẩn sắc thái",
              "nuanceNote": "1-2 câu giải thích vì sao dùng mẫu câu này trong hoàn cảnh này và mẹo thực tế",
              "kanjiCharacters": ["danh", "sách", "chữ", "Hán", "có", "trong", "câu"],
              "keyVocabularies": ["từ vựng 1 kèm nghĩa", "từ vựng 2 kèm nghĩa"]
            }
            """.formatted(pattern, level, context, pattern, pattern);

        Map<String, Object> reqBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 500
                )
        );

        String model = configuredGeminiModel;
        if (model == null || model.isBlank() || model.contains("3.8")) {
            model = "gemini-1.5-flash";
        }
        String jsonPayload = objectMapper.writeValueAsString(reqBody);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (!textNode.isMissingNode()) {
                String rawText = textNode.asText().trim();
                if (rawText.startsWith("```")) {
                    rawText = rawText.replaceAll("^```json", "").replaceAll("^```", "").replaceAll("```$", "").trim();
                }
                try {
                    JsonNode resJson = objectMapper.readTree(rawText);

                    List<String> kanjis = new ArrayList<>();
                    if (resJson.has("kanjiCharacters")) {
                        resJson.get("kanjiCharacters").forEach(k -> kanjis.add(k.asText()));
                    }
                    List<String> vocabs = new ArrayList<>();
                    if (resJson.has("keyVocabularies")) {
                        resJson.get("keyVocabularies").forEach(v -> vocabs.add(v.asText()));
                    }

                    return AiGenerateSentenceResponse.builder()
                            .pattern(pattern)
                            .context(context)
                            .japaneseSentence(resJson.path("japaneseSentence").asText())
                            .furiganaHtml(resJson.path("furiganaHtml").asText())
                            .vietnameseMeaning(resJson.path("vietnameseMeaning").asText())
                            .nuanceNote(resJson.path("nuanceNote").asText())
                            .kanjiCharacters(kanjis)
                            .keyVocabularies(vocabs)
                            .build();
                } catch (Exception parseEx) {
                    log.warn("Lỗi parse JSON kết quả từ AI: {}", parseEx.getMessage());
                }
            }
        } else {
            log.warn("Gemini API HTTP Error code: {}, body: {}", response.statusCode(), response.body());
        }
        return buildFallbackAiResponse(pattern, context, level);
    }

    private GrammarRuleDto findRuleByPatternOrId(String patternOrId) {
        if (patternOrId == null || patternOrId.isBlank()) return null;
        String clean = patternOrId.replace("〜", "").replace("~", "").trim().toLowerCase();
        for (List<GrammarRuleDto> rules : grammarCache.values()) {
            for (GrammarRuleDto r : rules) {
                if (patternOrId.equalsIgnoreCase(r.getId()) || patternOrId.equalsIgnoreCase(r.getPattern())) {
                    return r;
                }
                String rClean = r.getPattern() != null ? r.getPattern().replace("〜", "").replace("~", "").trim().toLowerCase() : "";
                if (!clean.isEmpty() && rClean.equals(clean)) {
                    return r;
                }
            }
        }
        return null;
    }

    private AiGenerateSentenceResponse buildFallbackAiResponse(String pattern, String context, String level) {
        String clean = (pattern != null ? pattern : "").replace("〜", "").replace("~", "").trim();
        String ctxLower = (context != null ? context : "").toLowerCase();

        // 1. Kiểm tra ma trận ngữ cảnh cho các cấu trúc thông dụng
        AiGenerateSentenceResponse contextualMatch = matchContextualSentence(pattern, clean, context, ctxLower);
        if (contextualMatch != null) {
            return contextualMatch;
        }

        // 2. Tìm trong kho ngữ pháp đã nạp từ OpenJLPT
        GrammarRuleDto matchedRule = findRuleByPatternOrId(pattern);
        if (matchedRule != null && matchedRule.getExamples() != null && !matchedRule.getExamples().isEmpty()) {
            GrammarExampleDto ex = matchedRule.getExamples().get(0);
            String ja = ex.getJa();
            String ruby = furiganaToRuby(ex.getFurigana() != null ? ex.getFurigana() : ex.getJa());
            String vn = ex.getVn() != null && !ex.getVn().isBlank() ? ex.getVn() : matchedRule.getMeaningVn();
            String note = "Trong ngữ cảnh " + context + ", mẫu ngữ pháp [" + matchedRule.getPattern() + "] diễn đạt ý nghĩa: "
                    + matchedRule.getMeaningVn() + ". Cấu trúc ghép từ: " + matchedRule.getFormation() + ".";
            List<String> kanjis = extractKanjis(ja);
            List<String> vocabs = List.of(
                    matchedRule.getPattern() + " - " + matchedRule.getMeaningVn(),
                    context + " (Tình huống giao tiếp)"
            );

            return AiGenerateSentenceResponse.builder()
                    .pattern(pattern)
                    .context(context)
                    .japaneseSentence(ja)
                    .furiganaHtml(ruby)
                    .vietnameseMeaning(vn)
                    .nuanceNote(note)
                    .kanjiCharacters(kanjis)
                    .keyVocabularies(vocabs)
                    .build();
        }

        // 3. Dự phòng cấu trúc câu tự nhiên đúng ngữ pháp
        String defJa = "これは" + clean + "です。";
        String defRuby = "これは<ruby>" + clean + "<rt>" + clean + "</rt></ruby>です。";
        String defVn = "Mẫu câu sử dụng ngữ pháp " + pattern + " trong hoàn cảnh " + context + ".";
        String defNote = "Cấu trúc " + pattern + " được áp dụng linh hoạt trong ngữ cảnh " + context + ".";

        return AiGenerateSentenceResponse.builder()
                .pattern(pattern)
                .context(context)
                .japaneseSentence(defJa)
                .furiganaHtml(defRuby)
                .vietnameseMeaning(defVn)
                .nuanceNote(defNote)
                .kanjiCharacters(extractKanjis(defJa))
                .keyVocabularies(List.of(pattern, context))
                .build();
    }

    private AiGenerateSentenceResponse matchContextualSentence(String pattern, String clean, String context, String ctx) {
        boolean isFood = ctx.contains("quán") || ctx.contains("ăn") || ctx.contains("nhà hàng") || ctx.contains("thực đơn");
        boolean isOffice = ctx.contains("công sở") || ctx.contains("văn phòng") || ctx.contains("họp") || ctx.contains("việc") || ctx.contains("công ty");
        boolean isTrain = ctx.contains("ga") || ctx.contains("tàu") || ctx.contains("đường") || ctx.contains("xe");
        boolean isShop = ctx.contains("mua") || ctx.contains("sắm") || ctx.contains("tiệm") || ctx.contains("siêu thị");

        if (clean.equals("は") || clean.startsWith("は")) {
            if (isFood) {
                return makeAiResp(pattern, context, "おすすめの料理は天ぷらです。", "おすすめの<ruby>料理<rt>りょうり</rt></ruby>は<ruby>天<rt>てん</rt></ruby>ぷらです。",
                        "Món ăn đặc sản của quán hôm nay là món tempura.",
                        "Trong quán ăn, dùng [〜は] để nêu chủ đề chính là món ăn đặc sắc của quán, vị ngữ giới thiệu món tempura.",
                        List.of("料", "理", "天"), List.of("料理 (りょうり) - Món ăn", "天ぷら (てんぷら) - Món Tempura"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "明日の会議は午後2時からです。", "<ruby>明日<rt>あした</rt></ruby>の<ruby>会議<rt>かいぎ</rt></ruby>は<ruby>午後<rt>ごご</rt></ruby>2<ruby>時<rt>じ</rt></ruby>からです。",
                        "Cuộc họp ngày mai bắt đầu từ lúc 2 giờ chiều.",
                        "Trong môi trường công sở, dùng [〜は] để xác định cuộc họp làm chủ đề chính cần thông báo mốc thời gian.",
                        List.of("明", "日", "会", "議", "午", "後", "時"), List.of("会議 (かいぎ) - Cuộc họp", "午後 (ごご) - Buổi chiều"));
            } else if (isTrain) {
                return makeAiResp(pattern, context, "次の電車は10時発です。", "<ruby>次<rt>つぎ</rt></ruby>の<ruby>電車<rt>でんしゃ</rt></ruby>は10<ruby>時<rt>じ</rt></ruby><ruby>発<rt>はつ</rt></ruby>です。",
                        "Chuyến tàu điện tiếp theo sẽ khởi hành vào lúc 10 giờ.",
                        "Ở ga tàu, [〜は] giúp hành khách và nhân viên nhà ga xác định đối tượng cần tra cứu giờ tàu chạy.",
                        List.of("次", "電", "車", "時", "発"), List.of("電車 (でんしゃ) - Tàu điện", "発 (はつ) - Khởi hành"));
            } else {
                return makeAiResp(pattern, context, "このシャツは綿100%です。", "このシャツは<ruby>綿<rt>めん</rt></ruby>100%です。",
                        "Chiếc áo sơ mi này được làm từ chất liệu 100% cotton.",
                        "Khi mua sắm, dùng [〜は] để hỏi han hoặc giới thiệu chất liệu sản phẩm.",
                        List.of("綿"), List.of("綿 (めん) - Sợi bông / Cotton"));
            }
        }

        if (clean.equals("が") || clean.startsWith("が（")) {
            if (isFood) {
                return makeAiResp(pattern, context, "注文したラーメンが来ました。", "<ruby>注文<rt>ちゅうもん</rt></ruby>したラーメンが<ruby>来<rt>き</rt></ruby>ました。",
                        "Tô mì ramen tôi đặt đã được mang ra rồi.",
                        "Trong quán ăn, trợ từ [〜が] nhấn mạnh đối tượng vừa xuất hiện là tô mì ramen mà bạn đã gọi.",
                        List.of("注", "文", "来"), List.of("注文 (ちゅうもん) - Gọi món / Đặt hàng", "来る (くる) - Đến"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "田中さんが資料を準備しています。", "<ruby>田中<rt>たなか</rt></ruby>さんが<ruby>資料<rt>しりょう</rt></ruby>を<ruby>準備<rt>じゅんび</rt></ruby>しています。",
                        "Anh Tanaka đang phụ trách chuẩn bị tài liệu.",
                        "Tại văn phòng, [〜が] nhấn mạnh chính anh Tanaka là người thực hiện hành động chuẩn bị.",
                        List.of("田", "中", "資", "料", "準", "備"), List.of("資料 (しりょう) - Tài liệu", "準備 (じゅんび) - Chuẩn bị"));
            } else {
                return makeAiResp(pattern, context, "電車が遅れています。", "<ruby>電車<rt>でんしゃ</rt></ruby>が<ruby>遅<rt>おく</rt></ruby>れています。",
                        "Chuyến tàu điện đang bị đến trễ giờ.",
                        "Trợ từ [〜が] nhấn mạnh thông tin mới phát sinh: tàu điện đang bị chậm trễ.",
                        List.of("電", "車", "遅"), List.of("電車 (でんしゃ) - Tàu điện", "遅れる (おくれる) - Đến trễ"));
            }
        }

        if (clean.equals("を")) {
            if (isFood) {
                return makeAiResp(pattern, context, "温かいお茶を飲みます。", "<ruby>温<rt>あたた</rt></ruby>かいお<ruby>茶<rt>ちゃ</rt></ruby>を<ruby>飲<rt>の</rt></ruby>みます。",
                        "Tôi uống một tách trà ấm nóng.",
                        "Trợ từ [〜を] chỉ trực tiếp thức uống trà ấm chịu tác động của hành động uống.",
                        List.of("温", "茶", "飲"), List.of("お茶 (おちゃ) - Trà", "飲む (のむ) - Uống"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "報告書を作成します。", "<ruby>報告書<rt>ほうこくしょ</rt></ruby>を<ruby>作成<rt>さくせい</rt></ruby>します。",
                        "Tôi sẽ tiến hành soạn thảo bản báo cáo công việc.",
                        "Tại nơi làm việc, [〜を] chỉ đối tượng của hành động soạn thảo là văn bản báo cáo.",
                        List.of("報", "告", "書", "作", "成"), List.of("報告書 (ほうこくしょ) - Bản báo cáo", "作成 (さくせい) - Soạn thảo"));
            } else {
                return makeAiResp(pattern, context, "駅で切符を買います。", "<ruby>駅<rt>えき</rt></ruby>で<ruby>切符<rt>きっぷ</rt></ruby>を<ruby>買<rt>か</rt></ruby>います。",
                        "Tôi mua vé tàu tại nhà ga.",
                        "Trợ từ [〜を] chỉ trực tiếp tấm vé tàu được mua.",
                        List.of("駅", "切", "符", "買"), List.of("駅 (えき) - Nhà ga", "切符 (きっぷ) - Vé tàu"));
            }
        }

        if (clean.equals("てください")) {
            if (isFood) {
                return makeAiResp(pattern, context, "メニューを見せてください。", "メニューを<ruby>見<rt>み</rt></ruby>せてください。",
                        "Xin vui lòng cho tôi xem thực đơn ạ.",
                        "Tại nhà hàng, mẫu [〜てください] là cách yêu cầu lịch sự khi muốn nhân viên phục vụ đưa thực đơn.",
                        List.of("見"), List.of("見せる (みせる) - Cho xem", "メニュー - Thực đơn"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "この書類を確認してください。", "この<ruby>書類<rt>しょるい</rt></ruby>を<ruby>確認<rt>かくにん</rt></ruby>してください。",
                        "Xin vui lòng kiểm tra lại tài liệu này giúp tôi ạ.",
                        "Mẫu [〜てください] dùng rất phổ biến khi nhờ đồng nghiệp hoặc cấp dưới rà soát văn bản.",
                        List.of("書", "類", "確", "認"), List.of("書類 (しょるい) - Tài liệu", "確認 (かくにん) - Kiểm tra"));
            } else {
                return makeAiResp(pattern, context, "駅への道を教えてください。", "<ruby>駅<rt>えき</rt></ruby>への<ruby>道<rt>みち</rt></ruby>を<ruby>教<rt>おし</rt></ruby>えてください。",
                        "Làm ơn chỉ đường tới nhà ga giúp tôi với ạ.",
                        "Dùng [〜てください] khi nhờ người đi đường chỉ dẫn phương hướng một cách lịch sự.",
                        List.of("駅", "道", "教"), List.of("駅 (えき) - Nhà ga", "道 (みち) - Con đường", "教える (おしえる) - Chỉ dẫn"));
            }
        }

        if (clean.equals("ないでください")) {
            if (isFood) {
                return makeAiResp(pattern, context, "わさびを入れないでください。", "わさびを<ruby>入<rt>い</rt></ruby>れないでください。",
                        "Xin đừng cho mù tạt wasabi vào món ăn giúp tôi ạ.",
                        "Dùng [〜ないでください] khi dặn đầu bếp hoặc bồi bàn tránh dùng gia vị bạn không ăn được.",
                        List.of("入"), List.of("入れる (いれる) - Cho vào"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "このファイルを削除しないでください。", "このファイルを<ruby>削除<rt>さくじょ</rt></ruby>しないでください。",
                        "Xin vui lòng đừng xóa tệp dữ liệu quan trọng này.",
                        "Dùng [〜ないでください] để nhắc nhở đồng nghiệp không thao tác nhầm trên dữ liệu chung.",
                        List.of("削", "除"), List.of("削除 (さくじょ) - Xóa bỏ"));
            } else {
                return makeAiResp(pattern, context, "黄色い線の外側に出ないでください。", "<ruby>黄色<rt>きいろ</rt></ruby>い<ruby>線<rt>せん</rt></ruby>の<ruby>外側<rt>そとがわ</rt></ruby>に<ruby>出<rt>で</rt></ruby>ないでください。",
                        "Xin quý khách vui lòng không bước ra ngoài vạch kẻ màu vàng.",
                        "Đây là thông báo an toàn quen thuộc tại các sân ga Nhật Bản nhắc nhở hành khách giữ khoảng cách.",
                        List.of("黄", "色", "線", "外", "側", "出"), List.of("黄色 (きいろ) - Màu vàng", "線 (せん) - Đường kẻ"));
            }
        }

        if (clean.equals("てもいい") || clean.contains("てもいい")) {
            if (isFood) {
                return makeAiResp(pattern, context, "ここで写真を撮ってもいいですか。", "ここで<ruby>写真<rt>しゃしん</rt></ruby>を<ruby>撮<rt>と</rt></ruby>ってもいいですか。",
                        "Tôi có thể chụp ảnh món ăn ở đây được không ạ?",
                        "Dùng [〜てもいいですか] để xin phép chủ quán hoặc nhân viên trước khi chụp ảnh không gian quán.",
                        List.of("写", "真", "撮"), List.of("写真 (しゃしん) - Bức ảnh", "撮る (とる) - Chụp ảnh"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "このパソコンを使ってもいいですか。", "このパソコンを<ruby>使<rt>つか</rt></ruby>ってもいいですか。",
                        "Tôi có thể sử dụng chiếc máy tính này được không ạ?",
                        "Cách hỏi xin phép mượn thiết bị làm việc của văn phòng một cách lễ phép.",
                        List.of("使"), List.of("使う (つかう) - Sử dụng"));
            } else if (isShop) {
                return makeAiResp(pattern, context, "この服を試着してもいいですか。", "この<ruby>服<rt>ふく</rt></ruby>を<ruby>試着<rt>しちゃく</rt></ruby>してもいいですか。",
                        "Tôi có thể mặc thử bộ quần áo này được không ạ?",
                        "Dùng [〜てもいいですか] khi muốn vào phòng thử đồ tại cửa hàng thời trang.",
                        List.of("服", "試", "着"), List.of("服 (ふく) - Quần áo", "試着 (しちゃく) - Mặc thử"));
            } else {
                return makeAiResp(pattern, context, "この席に座ってもいいですか。", "この<ruby>席<rt>せき</rt></ruby>に<ruby>座<rt>すわ</rt></ruby>ってもいいですか。",
                        "Tôi có thể ngồi vào ghế này được không ạ?",
                        "Cách hỏi xin phép ngồi ghế trống trên tàu điện hoặc nơi công cộng rất lịch sự.",
                        List.of("席", "座"), List.of("席 (せき) - Chỗ ngồi", "座る (すわる) - Ngồi"));
            }
        }

        if (clean.equals("たい") || clean.contains("たい")) {
            if (isFood) {
                return makeAiResp(pattern, context, "おいしい寿司を食べたいです。", "おいしい<ruby>寿司<rt>すし</rt></ruby>を<ruby>食<rt>た</rt></ruby>べたいです。",
                        "Tôi muốn thưởng thức món sushi thơm ngon.",
                        "Dùng [〜たい] thể hiện nguyện vọng ẩm thực của bản thân khi đi ăn nhà hàng.",
                        List.of("寿", "司", "食"), List.of("寿司 (すし) - Món sushi", "食べる (たべる) - Ăn"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "プロジェクトについて相談したいです。", "プロジェクトについて<ruby>相談<rt>そうだん</rt></ruby>したいです。",
                        "Tôi muốn được trao đổi ý kiến về dự án này.",
                        "Dùng [〜たい] thể hiện mong muốn được thảo luận công việc với cấp trên hoặc đồng đội.",
                        List.of("相", "談"), List.of("相談 (そうだん) - Thảo luận / Trao đổi"));
            } else {
                return makeAiResp(pattern, context, "新幹線に乗りたいです。", "<ruby>新幹線<rt>しんかんせん</rt></ruby>に<ruby>乗<rt>の</rt></ruby>りたいです。",
                        "Tôi muốn đi trải nghiệm tàu siêu tốc Shinkansen.",
                        "Thể hiện mong muốn đi lại bằng phương tiện đường sắt hiện đại của Nhật Bản.",
                        List.of("新", "幹", "線", "乗"), List.of("新幹線 (しんかんせん) - Tàu Shinkansen", "乗る (のる) - Đi / Lên tàu"));
            }
        }

        if (clean.contains("たことがある")) {
            if (isFood) {
                return makeAiResp(pattern, context, "本場のラーメンを食べたことがあります。", "<ruby>本場<rt>ほんば</rt></ruby>のラーメンを<ruby>食<rt>た</rt></ruby>べたことがあります。",
                        "Tôi đã từng ăn món mì ramen chuẩn vị tại Nhật rồi.",
                        "Dùng [〜たことがある] để chia sẻ trải nghiệm thưởng thức ẩm thực trong quá khứ.",
                        List.of("本", "場", "食"), List.of("本場 (ほんば) - Chuẩn vị / Bản địa", "食べる (たべる) - Ăn"));
            } else if (isOffice) {
                return makeAiResp(pattern, context, "日本企業で働いたことがあります。", "<ruby>日本<rt>にほん</rt></ruby><ruby>企業<rt>きぎょう</rt></ruby>で<ruby>働<rt>はたら</rt></ruby>いたことがあります。",
                        "Tôi đã từng có kinh nghiệm làm việc tại doanh nghiệp Nhật Bản.",
                        "Dùng [〜たことがある] khi phỏng vấn hoặc giới thiệu quá trình kinh nghiệm làm việc.",
                        List.of("日", "本", "企", "業", "働"), List.of("企業 (きぎょう) - Doanh nghiệp", "働く (はたらく) - Làm việc"));
            } else {
                return makeAiResp(pattern, context, "夜行列車に乗ったことがあります。", "<ruby>夜行列車<rt>やこうれっしゃ</rt></ruby>に<ruby>乗<rt>の</rt></ruby>ったことがあります。",
                        "Tôi đã từng đi chuyến tàu hỏa đêm.",
                        "Kể về trải nghiệm du lịch di chuyển bằng phương tiện giao thông đường sắt.",
                        List.of("夜", "行", "列", "車", "乗"), List.of("夜行列車 (やこうれっしゃ) - Tàu đêm", "乗る (のる) - Đi tàu"));
            }
        }

        if (clean.contains("ことができる")) {
            if (isFood) {
                return makeAiResp(pattern, context, "この店で電子マネーを使うことができます。", "この<ruby>店<rt>みせ</rt></ruby>で<ruby>電子<rt>でんし</rt></ruby>マネーを<ruby>使<rt>つか</rt></ruby>うことができます。",
                        "Ở quán này có thể thanh toán bằng ví điện tử.",
                        "Dùng [〜ことができる] để thông báo về phương thức thanh toán có thể áp dụng.",
                        List.of("店", "電", "子", "使"), List.of("電子マネー (でんしまねー) - Ví điện tử", "使う (つかう) - Sử dụng"));
            } else {
                return makeAiResp(pattern, context, "駅前で自転車を借りることができます。", "<ruby>駅前<rt>えきまえ</rt></ruby>で<ruby>自転車<rt>じてんしゃ</rt></ruby>を<ruby>借<rt>か</rt></ruby>りることができます。",
                        "Có thể thuê xe đạp công cộng ở ngay trước nhà ga.",
                        "Chỉ khả năng thực hiện dịch vụ công cộng thuận tiện cho hành khách.",
                        List.of("駅", "前", "自", "転", "車", "借"), List.of("自転車 (じてんしゃ) - Xe đạp", "借りる (かりる) - Thuê / Mượn"));
            }
        }

        if (clean.contains("わけにはいかない")) {
            if (isOffice) {
                return makeAiResp(pattern, context, "重要な会議ですから、遅刻するわけにはいかない。", "<ruby>重要<rt>じゅうよう</rt></ruby>な<ruby>会議<rt>かいぎ</rt></ruby>ですから、<ruby>遅刻<rt>ちこく</rt></ruby>するわけにはいかない。",
                        "Vì là cuộc họp quan trọng nên tôi tuyệt đối không thể đến trễ được.",
                        "Mẫu [〜わけにはいかない] nhấn mạnh việc không thể làm gì đó vì lý do trách nhiệm và đạo đức nghề nghiệp.",
                        List.of("重", "要", "会", "議", "遅", "刻"), List.of("重要 (じゅうよう) - Quan trọng", "遅刻 (ちこく) - Đi trễ"));
            } else {
                return makeAiResp(pattern, context, "終電に乗り遅れるわけにはいかない。", "<ruby>終電<rt>しゅうでん</rt></ruby>に<ruby>乗<rt>の</rt></ruby>り<ruby>遅<rt>おく</rt></ruby>れるわけにはいかない。",
                        "Tôi tuyệt đối không thể để lỡ chuyến tàu cuối cùng được.",
                        "Thể hiện sự quyết tâm buộc phải bắt kịp chuyến tàu cuối cùng về nhà.",
                        List.of("終", "電", "乗", "遅"), List.of("終電 (しゅうでん) - Chuyến tàu cuối cùng", "乗り遅れる - Lỡ tàu"));
            }
        }

        return null;
    }

    private AiGenerateSentenceResponse makeAiResp(String pattern, String context, String ja, String ruby, String vn, String note, List<String> kanjis, List<String> vocabs) {
        return AiGenerateSentenceResponse.builder()
                .pattern(pattern)
                .context(context)
                .japaneseSentence(ja)
                .furiganaHtml(ruby)
                .vietnameseMeaning(vn)
                .nuanceNote(note)
                .kanjiCharacters(kanjis)
                .keyVocabularies(vocabs)
                .build();
    }

    private String furiganaToRuby(String text) {
        if (text == null) return "";
        return text.replaceAll("\\{([^|]+)\\|([^}]+)\\}", "<ruby>$1<rt>$2</rt></ruby>");
    }

    private List<String> extractKanjis(String text) {
        if (text == null) return List.of();
        List<String> list = new ArrayList<>();
        Set<Character> seen = new HashSet<>();
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9faf') {
                if (seen.add(c)) {
                    list.add(String.valueOf(c));
                }
            }
        }
        return list;
    }

    private String cleanGrammarPattern(String pattern) {
        if (pattern == null) return "";
        return pattern.replaceAll("（[a-zA-Z\\s/_\\-]+）|\\([a-zA-Z\\s/_\\-]+\\)", "").trim();
    }

    private String cleanGrammarRomaji(String romaji) {
        if (romaji == null) return "";
        return romaji.replaceAll("（[a-zA-Z\\s/_\\-]+）|\\([a-zA-Z\\s/_\\-]+\\)", "").trim();
    }
}
