package com.kizuna.features.learning_support;

import com.kizuna.features.learning_support.controller.ReadingAssistantController;
import com.kizuna.features.learning_support.dto.ReadingAnalysisRequest;
import com.kizuna.features.learning_support.dto.ReadingAnalysisResponse;
import com.kizuna.features.learning_support.dto.ReadingGrammarItemDto;
import com.kizuna.features.learning_support.dto.ReadingVocabularyItemDto;
import com.kizuna.features.learning_support.service.ReadingAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Reading Assistant Controller Unit Tests")
class ReadingAssistantControllerTest {

    private ReadingAssistantService readingService;
    private ReadingAssistantController controller;

    @BeforeEach
    void setUp() {
        readingService = Mockito.mock(ReadingAssistantService.class);
        controller = new ReadingAssistantController(readingService);
    }

    @Test
    @DisplayName("UTCID01 (Normal): Phân tích văn bản tiếng Nhật hợp lệ trả về HTTP 200 và kết quả phân tích đầy đủ")
    void analyzeReading_happyPath_shouldReturnOkWithAnalysis() {
        ReadingAnalysisRequest req = ReadingAnalysisRequest.builder()
                .text("毎朝日本語を勉強しています。")
                .targetJlpt("N4")
                .includeFurigana(true)
                .build();

        ReadingAnalysisResponse mockResponse = ReadingAnalysisResponse.builder()
                .originalText(req.getText())
                .estimatedLevel("N4")
                .rubyHtml("<ruby>毎朝<rt>まいあさ</rt></ruby>日本語を勉強しています。")
                .overallSummary("Nói về thói quen học tiếng Nhật mỗi buổi sáng.")
                .detectedGrammars(List.of(
                        ReadingGrammarItemDto.builder()
                                .pattern("〜ている")
                                .meaning("Diễn tả hành động đang diễn ra hoặc thói quen")
                                .jlptLevel("N5")
                                .build()
                ))
                .vocabularies(List.of(
                        ReadingVocabularyItemDto.builder()
                                .kanji("毎朝")
                                .reading("まいあさ")
                                .hanViet("MỖI TRIỀU")
                                .meaning("Mỗi sáng")
                                .jlpt("N5")
                                .build()
                ))
                .build();

        when(readingService.analyzeReading(any(ReadingAnalysisRequest.class))).thenReturn(mockResponse);

        ResponseEntity<ReadingAnalysisResponse> resp = controller.analyzeReading(req);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("N4", resp.getBody().getEstimatedLevel());
        assertFalse(resp.getBody().getDetectedGrammars().isEmpty());
        assertFalse(resp.getBody().getVocabularies().isEmpty());
        verify(readingService, times(1)).analyzeReading(any(ReadingAnalysisRequest.class));
    }

    @Test
    @DisplayName("UTCID02 (Abnormal): Văn bản rỗng trả về HTTP 200 với danh sách phân tích rỗng an toàn")
    void analyzeReading_emptyText_shouldReturnEmptyAnalysisSafely() {
        ReadingAnalysisRequest req = ReadingAnalysisRequest.builder()
                .text("")
                .targetJlpt("ALL")
                .build();

        ReadingAnalysisResponse emptyResponse = ReadingAnalysisResponse.builder()
                .originalText("")
                .estimatedLevel("ALL")
                .rubyHtml("")
                .detectedGrammars(List.of())
                .vocabularies(List.of())
                .build();

        when(readingService.analyzeReading(any(ReadingAnalysisRequest.class))).thenReturn(emptyResponse);

        ResponseEntity<ReadingAnalysisResponse> resp = controller.analyzeReading(req);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertTrue(resp.getBody().getDetectedGrammars().isEmpty());
        assertTrue(resp.getBody().getVocabularies().isEmpty());
    }

    @Test
    @DisplayName("UTCID03 (Boundary): Không chọn cấp độ JLPT cụ thể (targetJlpt = null) -> Controller vẫn xử lý bình thường")
    void analyzeReading_nullTargetJlpt_shouldHandleWithoutError() {
        ReadingAnalysisRequest req = ReadingAnalysisRequest.builder()
                .text("こんにちは")
                .targetJlpt(null)
                .build();

        ReadingAnalysisResponse fallbackResponse = ReadingAnalysisResponse.builder()
                .originalText("こんにちは")
                .estimatedLevel("N5")
                .build();

        when(readingService.analyzeReading(any(ReadingAnalysisRequest.class))).thenReturn(fallbackResponse);

        ResponseEntity<ReadingAnalysisResponse> resp = controller.analyzeReading(req);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("N5", resp.getBody().getEstimatedLevel());
    }

    @Test
    @DisplayName("UTCID04 (Boundary): Văn bản null (req.getText() = null) -> Controller ghi nhận text length = 0 và xử lý an toàn")
    void analyzeReading_nullText_shouldHandleSafelyWithoutNpe() {
        ReadingAnalysisRequest req = ReadingAnalysisRequest.builder()
                .text(null)
                .targetJlpt("N5")
                .build();

        ReadingAnalysisResponse fallbackResponse = ReadingAnalysisResponse.builder()
                .originalText("")
                .estimatedLevel("N5")
                .rubyHtml("")
                .detectedGrammars(List.of())
                .vocabularies(List.of())
                .build();

        when(readingService.analyzeReading(any(ReadingAnalysisRequest.class))).thenReturn(fallbackResponse);

        ResponseEntity<ReadingAnalysisResponse> resp = controller.analyzeReading(req);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        assertEquals("N5", resp.getBody().getEstimatedLevel());
        verify(readingService, times(1)).analyzeReading(any(ReadingAnalysisRequest.class));
    }
}
