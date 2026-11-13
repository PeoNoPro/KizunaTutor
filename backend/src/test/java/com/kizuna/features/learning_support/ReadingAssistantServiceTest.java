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
}
