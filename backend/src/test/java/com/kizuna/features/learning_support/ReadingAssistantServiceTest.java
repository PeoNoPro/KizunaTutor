package com.kizuna.features.learning_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.service.SystemSettingService;
import com.kizuna.features.learning_support.dto.ReadingAnalysisRequest;
import com.kizuna.features.learning_support.dto.ReadingAnalysisResponse;
import com.kizuna.features.learning_support.service.impl.ReadingAssistantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ReadingAssistantServiceTest {

    private ReadingAssistantServiceImpl readingService;

    @BeforeEach
    void setUp() {
        SystemSettingService settings = Mockito.mock(SystemSettingService.class);
        Mockito.when(settings.aiChoice()).thenReturn(new SystemSettingService.AiChoice("GEMINI", "gemini-3.8-flash", "", ""));
        readingService = new ReadingAssistantServiceImpl(new ObjectMapper(), settings);
    }

    @Test
    @DisplayName("Kiểm tra bóc tách đoạn văn tiếng Nhật N3 hoạt động chuẩn xác với bộ dự phòng")
    void testAnalyzeReading_FallbackN3() {
        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text("日本語が上手になるように、毎朝NHKのニュースを聞くことにしています。雨が降っても散歩に行きます。")
                .targetJlpt("N3")
                .includeFurigana(true)
                .build();

        ReadingAnalysisResponse response = readingService.analyzeReading(request);

        assertNotNull(response);
        assertEquals("N3", response.getEstimatedLevel());
        assertNotNull(response.getOverallSummary());
        assertTrue(response.getRubyHtml().contains("<ruby>"));
        assertFalse(response.getDetectedGrammars().isEmpty(), "Phải phát hiện ít nhất một cấu trúc ngữ pháp");
        assertFalse(response.getVocabularies().isEmpty(), "Phải có danh sách từ vựng trọng tâm");
        assertFalse(response.getSentenceTranslations().isEmpty(), "Phải có bản dịch tham khảo");

        // Kiểm tra xem mẫu 〜ことにしている hoặc 〜ても có được nhận diện không
        boolean hasKotoniShiteiru = response.getDetectedGrammars().stream()
                .anyMatch(g -> g.getPattern().contains("ことにしている") || g.getPattern().contains("ても"));
        assertTrue(hasKotoniShiteiru, "Phải nhận diện được mẫu ngữ pháp 〜ことにしている hoặc 〜ても");
    }

    @Test
    @DisplayName("Kiểm tra bóc tách đoạn văn câu chuyện tình bạn Tsang với bộ bóc tách dự phòng")
    void testAnalyzeReading_TsangFriendshipText() {
        String tsangText = "「人生最大の贈り物は友情で, 私はもう手に入れました!」 ツァン君は私の同じ大学です。 彼は背が高くて、ちょっとスリムで目が明るくて、かっこい人です。 初めて出会った相手は建築大学の入学手続きをした時でした。彼は私を声を掛けました。 それから、毎日一緒に大学に行ったり、ハノイの様々な素晴らしい観光地に行ったりしました。春夏秋冬のいい記念を作って来ました。 そばにいるとき、彼はいつでも笑います。私にツァン君の笑みは一番素晴らしものです。彼は我慢強いや強気を教えてくれました。 今まで、私たちはお二人のこと知っているのは10年間です。これからもずっといい友達になりたいです。 ツァン君に：「これからは初めてのように手伝ってくれたいです」。";

        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text(tsangText)
                .targetJlpt("ALL")
                .includeFurigana(true)
                .build();

        ReadingAnalysisResponse response = readingService.analyzeReading(request);

        assertNotNull(response);
        assertEquals("N3", response.getEstimatedLevel());
        assertTrue(response.getRubyHtml().contains("<ruby>"));
        assertEquals(6, response.getDetectedGrammars().size(), "Phải phát hiện đủ 6 cấu trúc ngữ pháp");
        assertTrue(response.getVocabularies().size() >= 14, "Phải có ít nhất 14 từ vựng trọng tâm");
        assertNotNull(response.getVocabularies().get(0).getId(), "Từ vựng phải có ID");
        assertNotNull(response.getVocabularies().get(0).getSurfaceText(), "Từ vựng phải có surfaceText để highlight");
        assertNotNull(response.getDetectedGrammars().get(0).getId(), "Ngữ pháp phải có ID");
    }

    @Test
    @DisplayName("Kiểm tra bóc tách đoạn văn chuyến đi chơi Tokyo và Asakusa với bộ bóc tách dự phòng")
    void testAnalyzeReading_TokyoAsakusaText() {
        String tokyoText = "先週の日曜日、私は友達と東京へ行きました。朝9時に駅で友達に会いました。それから、電車で浅草へ行きました。浅草は人がたくさんいましたが、とてもにぎやかで面白かったです。私たちは大きいお寺を見たり、写真を撮ったりしました。昼ごはんに美味しいラーメンを食べました。午後、デパートで家族にお土産を買いました。少し疲れましたが、とても楽しかったです。また友達と東京へ行きたいです。";

        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text(tokyoText)
                .targetJlpt("ALL")
                .includeFurigana(true)
                .build();

        ReadingAnalysisResponse response = readingService.analyzeReading(request);

        assertNotNull(response);
        assertEquals("N5", response.getEstimatedLevel());
        assertTrue(response.getRubyHtml().contains("<ruby>"));
        assertTrue(response.getRubyHtml().contains("<ruby>東京<rt>とうきょう</rt></ruby>"));
        assertTrue(response.getRubyHtml().contains("<ruby>浅草<rt>あさくさ</rt></ruby>"));
        assertFalse(response.getDetectedGrammars().isEmpty(), "Phải phát hiện các cấu trúc ngữ pháp như 〜たり〜たり, 〜たい");
        assertFalse(response.getVocabularies().isEmpty(), "Phải có danh sách từ vựng trọng tâm xuất hiện trong bài");

        // Kiểm tra xem các từ vựng xuất hiện trong bài có surfaceText chuẩn không
        boolean hasTokyo = response.getVocabularies().stream().anyMatch(v -> "東京".equals(v.getSurfaceText()));
        boolean hasAsakusa = response.getVocabularies().stream().anyMatch(v -> "浅草".equals(v.getSurfaceText()));
        boolean hasRamen = response.getVocabularies().stream().anyMatch(v -> "ラーメン".equals(v.getSurfaceText()));
        boolean hasOmiyage = response.getVocabularies().stream().anyMatch(v -> "お土産".equals(v.getSurfaceText()));

        assertTrue(hasTokyo, "Phải có từ vựng 東京");
        assertTrue(hasAsakusa, "Phải có từ vựng 浅草");
        assertTrue(hasRamen, "Phải có từ vựng ラーメン");
        assertTrue(hasOmiyage, "Phải có từ vựng お土産");
    }

    @Test
    @DisplayName("Kiểm tra xử lý văn bản rỗng không bị ném exception")
    void testAnalyzeReading_EmptyText() {
        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text("   ")
                .targetJlpt("ALL")
                .build();

        ReadingAnalysisResponse response = readingService.analyzeReading(request);

        assertNotNull(response);
        assertEquals("", response.getOriginalText());
        assertTrue(response.getDetectedGrammars().isEmpty());
        assertTrue(response.getVocabularies().isEmpty());
    }

    @Test
    @DisplayName("Kiểm tra bóc tách trực tiếp bằng Google Gemini Online AI với API key mới")
    void testAnalyzeReading_LiveGeminiOnline() {
        String liveKey = System.getenv("GEMINI_API_KEY");
        if (liveKey == null || liveKey.isBlank()) {
            return; // Bỏ qua live test khi không có biến môi trường GEMINI_API_KEY để bảo mật
        }
        SystemSettingService liveSettings = Mockito.mock(SystemSettingService.class);
        Mockito.when(liveSettings.aiChoice()).thenReturn(new SystemSettingService.AiChoice("GEMINI", "gemini-flash-lite-latest", liveKey, ""));
        ReadingAssistantServiceImpl liveService = new ReadingAssistantServiceImpl(new ObjectMapper(), liveSettings);

        String tokyoText = "先週の日曜日、私は友達と東京へ行きました。朝9時に駅で友達に会いました。それから、電車で浅草へ行きました。浅草は人がたくさんいましたが、とてもにぎやかで面白かったです。私たちは大きいお寺を見たり、写真を撮ったりしました。昼ごはんに美味しいラーメンを食べました。午後、デパートで家族にお土産を買いました。少し疲れましたが、とても楽しかったです。また友達と東京へ行きたいです。";

        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text(tokyoText)
                .targetJlpt("ALL")
                .includeFurigana(true)
                .build();

        ReadingAnalysisResponse response = liveService.analyzeReading(request);

        assertNotNull(response);
        System.out.println("=== LIVE GEMINI AI RESPONSE ===");
        System.out.println("Level: " + response.getEstimatedLevel());
        System.out.println("Summary: " + response.getOverallSummary());
        System.out.println("Vocab count: " + (response.getVocabularies() != null ? response.getVocabularies().size() : 0));
        System.out.println("Grammar count: " + (response.getDetectedGrammars() != null ? response.getDetectedGrammars().size() : 0));
        System.out.println("Sentences count: " + (response.getSentenceTranslations() != null ? response.getSentenceTranslations().size() : 0));
        System.out.println("Ruby sample: " + (response.getRubyHtml() != null && response.getRubyHtml().length() > 100 ? response.getRubyHtml().substring(0, 100) : response.getRubyHtml()));

        assertNotNull(response.getRubyHtml());
        assertTrue(response.getRubyHtml().contains("<ruby>"));
        assertNotNull(response.getVocabularies());
        assertFalse(response.getVocabularies().isEmpty());
        assertNotNull(response.getDetectedGrammars());
        assertFalse(response.getDetectedGrammars().isEmpty());
    }

    @Test
    @DisplayName("Kiểm tra bộ nhớ đệm (Cache HIT) phản hồi tức thì dưới 50ms khi phân tích lại cùng văn bản")
    void testAnalyzeReading_CacheHitInstantResponse() {
        ReadingAnalysisRequest request = ReadingAnalysisRequest.builder()
                .text("日本語の勉強はとても面白いです。毎日新しい言葉を覚えます。")
                .targetJlpt("N4")
                .includeFurigana(true)
                .build();

        // Lần 1: Gọi phân tích
        ReadingAnalysisResponse first = readingService.analyzeReading(request);
        assertNotNull(first);

        // Lần 2: Gọi lại cùng văn bản -> Phải trả về từ Cache HIT ngay lập tức
        long start = System.nanoTime();
        ReadingAnalysisResponse second = readingService.analyzeReading(request);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertNotNull(second);
        assertSame(first, second, "Lần gọi thứ 2 phải trả về đúng đối tượng từ cache");
        assertTrue(elapsedMs < 50, "Cache HIT phải phản hồi trong thời gian < 50ms, thực tế: " + elapsedMs + "ms");
    }
}
