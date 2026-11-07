package com.kizuna.features.learning_support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.admin.service.SystemSettingService;
import org.mockito.Mockito;
import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;
import com.kizuna.features.learning_support.dto.AiWritingPromptDto;
import com.kizuna.features.learning_support.service.impl.AiWritingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiWritingServiceTest {

    private AiWritingServiceImpl aiWritingService;

    @BeforeEach
    void setUp() {
        // Cấu hình AI giờ đọc từ trang Cấu hình (FE-11); test không có API key nên luôn chạy bộ chấm dự phòng.
        SystemSettingService settings = Mockito.mock(SystemSettingService.class);
        Mockito.when(settings.aiChoice()).thenReturn(new SystemSettingService.AiChoice("GEMINI", "gemini-2.5-flash", "", ""));
        aiWritingService = new AiWritingServiceImpl(new ObjectMapper(), settings);
    }

    @Test
    @DisplayName("Kiểm tra bộ đánh giá câu tiếng Nhật dự phòng nhận diện lỗi trợ từ chính xác")
    void testFallbackEvaluation_detectsParticleError() {
        AiWritingCheckRequest request = AiWritingCheckRequest.builder()
                .text("私わ昨日寿司を食べました。")
                .jlptLevel("N5")
                .formality("POLITE")
                .build();

        AiWritingCheckResponse response = aiWritingService.checkWriting(request);

        assertNotNull(response);
        assertNotNull(response.getCorrectedText());
        assertTrue(response.getScore() > 0 && response.getScore() <= 100);
        assertFalse(response.getErrors().isEmpty(), "Phải phát hiện lỗi trợ từ '私わ'");
        assertEquals("Trợ từ", response.getErrors().get(0).getErrorType());
        assertFalse(response.getSuggestedVocabulary().isEmpty(), "Phải có gợi ý từ vựng");
    }

    @Test
    @DisplayName("Lấy danh sách mẫu câu gợi ý theo cấp độ JLPT")
    void testGetGrammarPrompts() {
        List<AiWritingPromptDto> promptsN5 = aiWritingService.getGrammarChallengePrompts("N5");
        assertNotNull(promptsN5);
        assertFalse(promptsN5.isEmpty());
        assertEquals("N5", promptsN5.get(0).getLevel());
        assertNotNull(promptsN5.get(0).getGrammarPattern());

        List<AiWritingPromptDto> promptsN4 = aiWritingService.getGrammarChallengePrompts("N4");
        assertNotNull(promptsN4);
        assertFalse(promptsN4.isEmpty());
        assertEquals("N4", promptsN4.get(0).getLevel());
    }
}
