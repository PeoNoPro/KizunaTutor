package com.kizuna.features.learning_support.service.impl;

import com.kizuna.features.learning_support.dto.MistakeItemDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;
import com.kizuna.core.entity.*;
import com.kizuna.features.classroom.entity.*;
import com.kizuna.features.learning_support.entity.*;
import com.kizuna.features.payment.entity.*;
import com.kizuna.features.progress.entity.*;
import com.kizuna.features.verification.entity.*;
import com.kizuna.features.tutor_search.entity.*;
import com.kizuna.features.ticket.entity.*;
import com.kizuna.features.notification.entity.*;
import com.kizuna.features.booking.entity.*;
import com.kizuna.features.admin.entity.*;
import com.kizuna.features.classroom.repository.KanjiFolderRepository;
import com.kizuna.features.learning_support.repository.MinigameMistakeLogRepository;
import com.kizuna.features.learning_support.repository.MinigameSessionRepository;
import com.kizuna.core.repository.UserRepository;
import com.kizuna.features.learning_support.service.IMinigameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinigameServiceImpl implements IMinigameService {

    private final MinigameSessionRepository sessionRepository;
    private final MinigameMistakeLogRepository mistakeLogRepository;
    private final KanjiFolderRepository folderRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MinigameSessionResponse saveGameSession(UUID userId, MinigameSessionRequest request) {
        User user = resolveOrCreateUser(userId);

        KanjiFolder folder = null;
        String folderName = "Mặc định (Toàn bộ Kanji)";
        if (request.getFolderId() != null) {
            folder = folderRepository.findById(request.getFolderId()).orElse(null);
            if (folder != null) {
                folderName = folder.getFolderName();
            }
        }

        MinigameSession session = MinigameSession.builder()
                .user(user)
                .folder(folder)
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
                        .kanjiCharacter(m.getKanjiCharacter())
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
                .folderId(folder != null ? folder.getFolderId() : null)
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

        return list.stream().map(s -> {
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
                    .folderId(s.getFolder() != null ? s.getFolder().getFolderId() : null)
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
        }).toList();
    }

    private String calculateTitleRank(int words, double accuracy, boolean won) {
        if (!won) {
            return "Chiến Binh Kiên Trì 🛡️";
        }
        if (words >= 45 && accuracy >= 95.0) {
            return "Bậc Thầy Chữ Hán 🏆";
        } else if (words >= 30 && accuracy >= 90.0) {
            return "Cao Thủ Gõ Phím ⚔️";
        } else if (words >= 15 && accuracy >= 80.0) {
            return "Học Giả Tiến Bộ 📜";
        } else {
            return "Tân Binh Luyện Chữ 🌸";
        }
    }

    private User resolveOrCreateUser(UUID userId) {
        if (userId != null) {
            return userRepository.findById(userId).orElseGet(this::getOrCreateFallbackUser);
        }
        return getOrCreateFallbackUser();
    }

    private User getOrCreateFallbackUser() {
        return userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User guest = User.builder().build();
            return userRepository.save(guest);
        });
    }
}
