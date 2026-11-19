package com.kizuna.features.learning_support;

import com.kizuna.core.entity.User;
import com.kizuna.core.entity.UserAccountStatus;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.learning_support.dto.KanjiPlayDataDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;
import com.kizuna.features.learning_support.dto.MistakeItemDto;
import com.kizuna.features.learning_support.entity.JlptLevel;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.entity.MinigameSession;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.learning_support.repository.MinigameMistakeLogRepository;
import com.kizuna.features.learning_support.repository.MinigameSessionRepository;
import com.kizuna.features.learning_support.service.impl.KanjiGameServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Kanji Game Service Unit Tests (3 Test Cases)")
class KanjiGameServiceTest {

    private KanjiDictionaryRepository kanjiRepository;
    private MinigameSessionRepository sessionRepository;
    private MinigameMistakeLogRepository mistakeLogRepository;
    private UserRepository userRepository;
    private KanjiGameServiceImpl gameService;

    @BeforeEach
    void setUp() {
        kanjiRepository = mock(KanjiDictionaryRepository.class);
        sessionRepository = mock(MinigameSessionRepository.class);
        mistakeLogRepository = mock(MinigameMistakeLogRepository.class);
        userRepository = mock(UserRepository.class);

        gameService = new KanjiGameServiceImpl(
                kanjiRepository,
                sessionRepository,
                mistakeLogRepository,
                userRepository
        );
    }

    private KanjiDictionary createMockKanji(String character, String hv, String on, String kun, JlptLevel level) {
        return KanjiDictionary.builder()
                .kanjiId(UUID.randomUUID())
                .kanjiCharacter(character)
                .unicodeHex("U+" + Integer.toHexString(character.charAt(0)).toUpperCase())
                .hanViet(hv)
                .onyomi(on)
                .kunyomi(kun)
                .meaning("Nghĩa của " + character)
                .jlptLevel(level)
                .strokeCount((short) 4)
                .build();
    }

    @Test
    @DisplayName("UTCID01 (Normal): Lấy dữ liệu chơi Kanji Rain cho cấp độ N5 với limit 20")
    void getPlayData_whenValidJlptAndLimit_shouldReturnKanjiListWithReadings() {
        List<KanjiDictionary> mockKanjis = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            mockKanjis.add(createMockKanji("日", "NHẬT", "ニチ, ジツ", "ひ, -び", JlptLevel.N5));
        }

        when(kanjiRepository.findRandomPlayableKanji("N5", 20)).thenReturn(mockKanjis);

        List<KanjiPlayDataDto> result = gameService.getPlayData("N5", 20);

        assertNotNull(result);
        assertEquals(20, result.size());
        KanjiPlayDataDto first = result.get(0);
        assertEquals("日", first.getCharacter());
        assertEquals("NHẬT", first.getHanViet());
        assertNotNull(first.getReadings());
        assertFalse(first.getReadings().isEmpty());
        verify(kanjiRepository, times(1)).findRandomPlayableKanji("N5", 20);
    }

    @Test
    @DisplayName("UTCID02 (Normal): Lưu kết quả phiên chơi game, tính độ chính xác và lưu danh sách lỗi")
    void saveGameSession_whenValidRequest_shouldSaveSessionAndMistakes() {
        UUID userId = UUID.randomUUID();
        User mockUser = User.builder()
                .userId(userId)
                .email("gamer@kizuna.vn")
                .fullName("Gamer Pro")
                .accountStatus(UserAccountStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(sessionRepository.save(any(MinigameSession.class))).thenAnswer(inv -> {
            MinigameSession s = inv.getArgument(0);
            s.setSessionId(UUID.randomUUID());
            return s;
        });

        MinigameSessionRequest request = MinigameSessionRequest.builder()
                .durationSeconds(45)
                .targetDurationSeconds(60)
                .wordsTypedCount(18)
                .totalMistakes(2)
                .accuracy(90.0)
                .score(180)
                .isWon(true)
                .mistakes(List.of(
                        MistakeItemDto.builder()
                                .kanjiCharacter("月")
                                .reading("つき")
                                .mistakeCount(1)
                                .typedInput("tsuki")
                                .build()
                ))
                .build();

        MinigameSessionResponse response = gameService.saveGameSession(userId, request);

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertEquals(45, response.getDurationSeconds());
        assertEquals(18, response.getWordsTypedCount());
        assertEquals(90.0, response.getAccuracy());
        assertEquals(180, response.getScore());
        assertTrue(response.getIsWon());

        verify(sessionRepository, times(1)).save(any(MinigameSession.class));
        verify(mistakeLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("UTCID03 (Boundary): Tham số limit <= 0 hoặc âm -> Tự động kẹp giá trị về tối thiểu 10 phần tử")
    void getPlayData_whenLimitZeroOrNegative_shouldClampToDefaultMinimum10() {
        List<KanjiDictionary> mockKanjis = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockKanjis.add(createMockKanji("本", "BẢN", "ホン", "もと", JlptLevel.N5));
        }

        when(kanjiRepository.findRandomPlayableKanji("N5", 10)).thenReturn(mockKanjis);

        List<KanjiPlayDataDto> resultZero = gameService.getPlayData("N5", 0);
        assertNotNull(resultZero);
        assertEquals(10, resultZero.size());

        List<KanjiPlayDataDto> resultNegative = gameService.getPlayData("N5", -5);
        assertNotNull(resultNegative);
        assertEquals(10, resultNegative.size());

        verify(kanjiRepository, times(2)).findRandomPlayableKanji("N5", 10);
    }
}
