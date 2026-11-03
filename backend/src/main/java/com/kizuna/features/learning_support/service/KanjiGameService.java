package com.kizuna.features.learning_support.service;

import com.kizuna.features.learning_support.dto.KanjiPlayDataDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;

import java.util.List;
import java.util.UUID;

public interface KanjiGameService {

    List<KanjiPlayDataDto> getPlayData(String jlpt, int limit);

    MinigameSessionResponse saveGameSession(UUID userId, MinigameSessionRequest request);

    List<MinigameSessionResponse> getUserSessions(UUID userId);

    List<MinigameSessionResponse> getLeaderboard();
}
