package com.kizuna.features.learning_support.service.impl;

import com.kizuna.core.entity.User;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.learning_support.dto.KanjiPlayDataDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;
import com.kizuna.features.learning_support.dto.MistakeItemDto;
import com.kizuna.features.learning_support.entity.KanjiDictionary;
import com.kizuna.features.learning_support.entity.MinigameMistakeLog;
import com.kizuna.features.learning_support.entity.MinigameSession;
import com.kizuna.features.learning_support.repository.KanjiDictionaryRepository;
import com.kizuna.features.learning_support.repository.MinigameMistakeLogRepository;
import com.kizuna.features.learning_support.repository.MinigameSessionRepository;
import com.kizuna.features.learning_support.service.KanjiGameService;
import com.kizuna.features.learning_support.util.KanjiReadingNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KanjiGameServiceImpl implements KanjiGameService {

    private final KanjiDictionaryRepository kanjiRepository;
    private final MinigameSessionRepository sessionRepository;
    private final MinigameMistakeLogRepository mistakeLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<KanjiPlayDataDto> getPlayData(String jlpt, int limit) {
        int max = (limit > 0 && limit <= 100) ? limit : 30;
        List<KanjiDictionary> kanjis = kanjiRepository.findRandomPlayableKanji(jlpt, max);

        return kanjis.stream().map(this::mapToPlayDataDto).toList();
    }

    private KanjiPlayDataDto mapToPlayDataDto(KanjiDictionary k) {
        String kun = k.getKunyomi();
        String on = k.getOnyomi();
        String hv = k.getHanViet();

        List<String> readings = KanjiReadingNormalizer.extractNormalizedReadings(kun, on, hv);
        String primary = KanjiReadingNormalizer.determinePrimaryReading(kun, on);

        if (primary.isEmpty() && !readings.isEmpty()) {
            primary = readings.get(0);
        }

        return KanjiPlayDataDto.builder()
                .kanjiId(k.getKanjiId())
                .character(k.getKanjiCharacter())
                .hanViet(hv != null ? hv : "HÁN TỰ")
                .meaning(k.getMeaning() != null ? k.getMeaning() : "")
                .jlptLevel(k.getJlptLevel() != null ? k.getJlptLevel().name() : "N5")
                .strokeCount(k.getStrokeCount())
                .onyomi(k.getOnyomi())
                .kunyomi(k.getKunyomi())
                .primaryReading(primary)
                .readings(readings)
                .build();
    }

    @Override
    @Transactional
    public MinigameSessionResponse saveGameSession(UUID userId, MinigameSessionRequest request) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        String folderName = "Tự do (Toàn bộ Hán tự)";

        MinigameSession session = MinigameSession.builder()
                .user(user)
                .folder(null)
                .folderName(folderName)
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0)
                .targetDurationSeconds(request.getTargetDurationSeconds())
                .wordsTypedCount(request.getWordsTypedCount() != null ? request.getWordsTypedCount() : 0)
                .totalMistakes(request.getTotalMistakes() != null ? request.getTotalMistakes() : 0)
                .accuracy(request.getAccuracy() != null ? request.getAccuracy() : 100.0)
                .score(request.getScore() != null ? request.getScore() : 0)
                .isWon(Boolean.TRUE.equals(request.getIsWon()))
                .failedKanjiId(request.getFailedKanjiId())
                .failedKanjiCharacter(request.getFailedKanjiCharacter())
                .build();

        session = sessionRepository.save(session);

        List<MistakeItemDto> savedMistakes = new ArrayList<>();
        if (request.getMistakes() != null && !request.getMistakes().isEmpty()) {
            for (MistakeItemDto m : request.getMistakes()) {
                MinigameMistakeLog logEntity = MinigameMistakeLog.builder()
                        .session(session)
                        .kanjiId(m.getKanjiId())
                        .kanjiCharacter(m.getKanjiCharacter() != null ? m.getKanjiCharacter() : "?")
                        .reading(m.getReading())
                        .mistakeCount(m.getMistakeCount() != null ? m.getMistakeCount() : 1)
                        .typedInput(m.getTypedInput())
                        .build();
                mistakeLogRepository.save(logEntity);
                savedMistakes.add(m);
            }
        }

        String titleRank = calculateTitleRank(session.getWordsTypedCount(), session.getAccuracy(), session.getIsWon());

        return MinigameSessionResponse.builder()
                .sessionId(session.getSessionId())
                .folderId(null)
                .folderName(folderName)
                .durationSeconds(session.getDurationSeconds())
                .targetDurationSeconds(session.getTargetDurationSeconds())
                .wordsTypedCount(session.getWordsTypedCount())
                .totalMistakes(session.getTotalMistakes())
                .accuracy(session.getAccuracy())
                .score(session.getScore())
                .isWon(session.getIsWon())
                .failedKanjiId(session.getFailedKanjiId())
                .failedKanjiCharacter(session.getFailedKanjiCharacter())
                .playedAt(session.getPlayedAt())
                .titleRank(titleRank)
                .topMistakes(savedMistakes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MinigameSessionResponse> getUserSessions(UUID userId) {
        List<MinigameSession> list;
        if (userId != null) {
            list = sessionRepository.findByUserUserIdOrderByPlayedAtDesc(userId);
        } else {
            list = sessionRepository.findTop20ByOrderByPlayedAtDesc();
        }

        return list.stream().map(this::mapToSessionResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MinigameSessionResponse> getLeaderboard() {
        List<MinigameSession> list = sessionRepository.findTop10ByOrderByScoreDesc();
        return list.stream().map(this::mapToSessionResponse).toList();
    }

    private MinigameSessionResponse mapToSessionResponse(MinigameSession s) {
        List<MinigameMistakeLog> mistakeLogs = mistakeLogRepository.findBySessionSessionIdOrderByMistakeCountDesc(s.getSessionId());
        List<MistakeItemDto> mistakes = mistakeLogs.stream().map(m -> MistakeItemDto.builder()
                .kanjiId(m.getKanjiId())
                .kanjiCharacter(m.getKanjiCharacter())
                .reading(m.getReading())
                .mistakeCount(m.getMistakeCount())
                .typedInput(m.getTypedInput())
                .build()).toList();

        return MinigameSessionResponse.builder()
                .sessionId(s.getSessionId())
                .folderId(null)
                .folderName(s.getFolderName())
                .durationSeconds(s.getDurationSeconds())
                .targetDurationSeconds(s.getTargetDurationSeconds())
                .wordsTypedCount(s.getWordsTypedCount())
                .totalMistakes(s.getTotalMistakes())
                .accuracy(s.getAccuracy())
                .score(s.getScore())
                .isWon(s.getIsWon())
                .failedKanjiId(s.getFailedKanjiId())
                .failedKanjiCharacter(s.getFailedKanjiCharacter())
                .playedAt(s.getPlayedAt())
                .titleRank(calculateTitleRank(s.getWordsTypedCount(), s.getAccuracy(), s.getIsWon()))
                .topMistakes(mistakes)
                .build();
    }

    private String calculateTitleRank(int words, double accuracy, boolean won) {
        if (!won) {
            return "Chiến Binh Kiên Trì 🛡️";
        }
        if (words >= 45 && accuracy >= 95.0) {
            return "Bậc Thầy Chữ Hán 👑";
        } else if (words >= 30 && accuracy >= 90.0) {
            return "Cao Thủ Gõ Phím ⚡";
        } else if (words >= 15 && accuracy >= 80.0) {
            return "Học Giả Tiến Bộ 📖";
        } else {
            return "Tân Binh Luyện Chữ 🌱";
        }
    }
}
