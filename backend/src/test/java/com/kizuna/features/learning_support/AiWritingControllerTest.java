package com.kizuna.features.learning_support;

import com.kizuna.features.learning_support.controller.AiWritingController;
import com.kizuna.features.learning_support.dto.AiWritingCheckRequest;
import com.kizuna.features.learning_support.dto.AiWritingCheckResponse;
import com.kizuna.features.learning_support.dto.AiWritingPromptDto;
import com.kizuna.features.learning_support.service.AiWritingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AI Writing Controller Unit Tests")
class AiWritingControllerTest {

    private AiWritingService aiWritingService;
    private AiWritingController controller;

    @BeforeEach
    void setUp() {
        aiWritingService = Mockito.mock(AiWritingService.class);
        controller = new AiWritingController(aiWritingService);
    }

    @Test
    @DisplayName("UTCID01 (Normal): Kiểm tra bài viết tiếng Nhật hợp lệ -> Trả về HTTP 200 và kết quả phân tích sửa câu")
    void checkWriting_validText_shouldReturnOkWithFeedback() {
        AiWritingCheckRequest req = AiWritingCheckRequest.builder()
                .text("わたしは学校に行きます。")
                .jlptLevel("N5")
                .formality("POLITE")
                .build();

        AiWritingCheckResponse mockResponse = AiWritingCheckResponse.builder()
                .originalText(req.getText())
                .correctedText("私は学校へ行きます。")
                .score(95)
                .overallComment("Câu diễn đạt rất tự nhiên.")
                .errors(List.of())
                .build();

        when(aiWritingService.checkWriting(any(AiWritingCheckRequest.class))).thenReturn(mockResponse);

        ResponseEntity<AiWritingCheckResponse> resp = controller.checkWriting(req);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(95, resp.getBody().getScore());
        assertEquals("私は学校へ行きます。", resp.getBody().getCorrectedText());
        verify(aiWritingService, times(1)).checkWriting(any(AiWritingCheckRequest.class));
    }

    @Test
    @DisplayName("UTCID02 (Boundary): Kiểm tra bài viết có text = null -> Controller ghi nhận length = 0 và xử lý an toàn")
    void checkWriting_nullText_shouldHandleSafelyWithoutNpe() {
        AiWritingCheckRequest req = AiWritingCheckRequest.builder()
                .text(null)
                .jlptLevel("N5")
                .build();

        AiWritingCheckResponse emptyResponse = AiWritingCheckResponse.builder()
                .originalText("")
                .score(0)
                .overallComment("Vui lòng nhập nội dung bài viết.")
                .errors(List.of())
                .build();

        when(aiWritingService.checkWriting(any(AiWritingCheckRequest.class))).thenReturn(emptyResponse);

        ResponseEntity<AiWritingCheckResponse> resp = controller.checkWriting(req);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(0, resp.getBody().getScore());
        verify(aiWritingService, times(1)).checkWriting(any(AiWritingCheckRequest.class));
    }

    @Test
    @DisplayName("UTCID03 (Normal): Lấy danh sách đề bài gợi ý luyện viết với level = N4 -> Trả về HTTP 200 và danh sách prompt")
    void getPrompts_withSpecificLevel_shouldReturnOkAndPrompts() {
        List<AiWritingPromptDto> mockPrompts = List.of(
                AiWritingPromptDto.builder()
                        .id("P-N4-01")
                        .title("Viết về kỳ nghỉ hè")
                        .grammarPattern("〜たことがある, 〜たり〜たりする")
                        .level("N4")
                        .build()
        );

        when(aiWritingService.getGrammarChallengePrompts("N4")).thenReturn(mockPrompts);

        ResponseEntity<List<AiWritingPromptDto>> resp = controller.getPrompts("N4");

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals("P-N4-01", resp.getBody().get(0).getId());
        verify(aiWritingService, times(1)).getGrammarChallengePrompts("N4");
    }

    @Test
    @DisplayName("UTCID04 (Boundary): Lấy danh sách đề bài với level mặc định N5 -> Trả về HTTP 200")
    void getPrompts_defaultLevel_shouldReturnOkAndPrompts() {
        when(aiWritingService.getGrammarChallengePrompts("N5")).thenReturn(List.of());

        ResponseEntity<List<AiWritingPromptDto>> resp = controller.getPrompts("N5");

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(aiWritingService, times(1)).getGrammarChallengePrompts("N5");
    }
}
