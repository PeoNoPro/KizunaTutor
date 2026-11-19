package com.kizuna.features.learning_support;

import com.kizuna.features.learning_support.controller.KanjiGameController;
import com.kizuna.features.learning_support.dto.KanjiPlayDataDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;
import com.kizuna.features.learning_support.dto.MistakeItemDto;
import com.kizuna.features.learning_support.service.KanjiGameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Kanji Game Controller Unit Tests")
class KanjiGameControllerTest {

    private KanjiGameService gameService;
    private KanjiGameController controller;

    @BeforeEach
    void setUp() {
        gameService = Mockito.mock(KanjiGameService.class);
        controller = new KanjiGameController(gameService);
    }

    @Test
    @DisplayName("UTCID01 (Normal): Lấy dữ liệu chơi game với JLPT = N5, limit = 20 -> Trả về HTTP 200 và danh sách thẻ")
    void getPlayData_validJlptAndLimit_shouldReturn200AndList() {
        List<KanjiPlayDataDto> mockData = List.of(
                KanjiPlayDataDto.builder()
                        .character("日")
                        .hanViet("NHẬT")
                        .meaning("mặt trời, ngày")
                        .primaryReading("ひ")
                        .readings(List.of("ひ", "nichi"))
                        .jlptLevel("N5")
                        .build()
        );

        when(gameService.getPlayData("N5", 20)).thenReturn(mockData);

        ResponseEntity<List<KanjiPlayDataDto>> resp = controller.getPlayData("N5", 20);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals("日", resp.getBody().get(0).getCharacter());
        verify(gameService, times(1)).getPlayData("N5", 20);
    }

    @Test
    @DisplayName("UTCID02 (Boundary): Lấy dữ liệu với tham số mặc định (jlpt = null, limit = 30) -> Trả về HTTP 200")
    void getPlayData_defaultParams_shouldReturn200AndList() {
        when(gameService.getPlayData(null, 30)).thenReturn(List.of());

        ResponseEntity<List<KanjiPlayDataDto>> resp = controller.getPlayData(null, 30);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(gameService, times(1)).getPlayData(null, 30);
    }

    @Test
    @DisplayName("UTCID03 (Normal): Lưu kết quả phiên chơi game có userId -> Trả về HTTP 201 CREATED")
    void saveSession_withUserId_shouldReturn201Created() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        MinigameSessionRequest req = MinigameSessionRequest.builder()
                .durationSeconds(60)
                .wordsTypedCount(10)
                .totalMistakes(1)
                .accuracy(90.0)
                .score(150)
                .isWon(true)
                .mistakes(List.of(
                        MistakeItemDto.builder()
                                .kanjiCharacter("水")
                                .reading("みず")
                                .typedInput("みす")
                                .mistakeCount(1)
                                .build()
                ))
                .build();

        MinigameSessionResponse mockResponse = MinigameSessionResponse.builder()
                .sessionId(sessionId)
                .score(150)
                .accuracy(90.0)
                .durationSeconds(60)
                .wordsTypedCount(10)
                .totalMistakes(1)
                .isWon(true)
                .playedAt(OffsetDateTime.now())
                .build();

        when(gameService.saveGameSession(eq(userId), any(MinigameSessionRequest.class))).thenReturn(mockResponse);

        ResponseEntity<MinigameSessionResponse> resp = controller.saveSession(userId, req);

        assertNotNull(resp);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(sessionId, resp.getBody().getSessionId());
        assertEquals(150, resp.getBody().getScore());
        verify(gameService, times(1)).saveGameSession(eq(userId), any(MinigameSessionRequest.class));
    }

    @Test
    @DisplayName("UTCID04 (Boundary): Lưu kết quả phiên chơi khách vãng lai (userId = null) -> Trả về HTTP 201 CREATED an toàn")
    void saveSession_nullUserId_shouldReturn201CreatedSafely() {
        UUID sessionId = UUID.randomUUID();

        MinigameSessionRequest req = MinigameSessionRequest.builder()
                .durationSeconds(45)
                .wordsTypedCount(15)
                .totalMistakes(0)
                .accuracy(100.0)
                .score(200)
                .isWon(true)
                .mistakes(List.of())
                .build();

        MinigameSessionResponse mockResponse = MinigameSessionResponse.builder()
                .sessionId(sessionId)
                .score(200)
                .accuracy(100.0)
                .durationSeconds(45)
                .wordsTypedCount(15)
                .totalMistakes(0)
                .isWon(true)
                .playedAt(OffsetDateTime.now())
                .build();

        when(gameService.saveGameSession(isNull(), any(MinigameSessionRequest.class))).thenReturn(mockResponse);

        ResponseEntity<MinigameSessionResponse> resp = controller.saveSession(null, req);

        assertNotNull(resp);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertEquals(200, resp.getBody().getScore());
        verify(gameService, times(1)).saveGameSession(isNull(), any(MinigameSessionRequest.class));
    }

    @Test
    @DisplayName("UTCID05 (Normal): Lấy lịch sử phiên chơi của user theo userId -> Trả về HTTP 200 và danh sách phiên")
    void getUserSessions_withUserId_shouldReturn200AndList() {
        UUID userId = UUID.randomUUID();
        List<MinigameSessionResponse> mockList = List.of(
                MinigameSessionResponse.builder()
                        .sessionId(UUID.randomUUID())
                        .score(100)
                        .accuracy(85.0)
                        .playedAt(OffsetDateTime.now())
                        .build()
        );

        when(gameService.getUserSessions(userId)).thenReturn(mockList);

        ResponseEntity<List<MinigameSessionResponse>> resp = controller.getUserSessions(userId);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(100, resp.getBody().get(0).getScore());
        verify(gameService, times(1)).getUserSessions(userId);
    }

    @Test
    @DisplayName("UTCID06 (Boundary): Lấy lịch sử phiên chơi khi userId = null -> Trả về HTTP 200 an toàn")
    void getUserSessions_nullUserId_shouldReturn200Safely() {
        when(gameService.getUserSessions(null)).thenReturn(List.of());

        ResponseEntity<List<MinigameSessionResponse>> resp = controller.getUserSessions(null);

        assertNotNull(resp);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(gameService, times(1)).getUserSessions(null);
    }
}
