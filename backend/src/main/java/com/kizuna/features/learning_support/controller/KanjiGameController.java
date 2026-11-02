package com.kizuna.features.learning_support.controller;

import com.kizuna.features.learning_support.dto.KanjiPlayDataDto;
import com.kizuna.features.learning_support.dto.MinigameSessionRequest;
import com.kizuna.features.learning_support.dto.MinigameSessionResponse;
import com.kizuna.features.learning_support.service.KanjiGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/learner/minigame", "/api/minigame"})
@RequiredArgsConstructor
public class KanjiGameController {

    private final KanjiGameService gameService;

    /**
     * Lấy dữ liệu chữ Hán kèm các cách đọc Hiragana, Romaji, Hán Việt đã chuẩn hóa để nạp vào bàn cờ game.
     */
    @GetMapping("/play-data")
    public ResponseEntity<List<KanjiPlayDataDto>> getPlayData(
            @RequestParam(required = false) String jlpt,
            @RequestParam(defaultValue = "30") int limit) {
        List<KanjiPlayDataDto> data = gameService.getPlayData(jlpt, limit);
        return ResponseEntity.ok(data);
    }

    /**
     * Lưu kết quả phiên chơi minigame, ghi nhận điểm số, độ chính xác và thống kê từ sai.
     */
    @PostMapping("/sessions")
    public ResponseEntity<MinigameSessionResponse> saveSession(
            @RequestParam(required = false) UUID userId,
            @Valid @RequestBody MinigameSessionRequest request) {
        MinigameSessionResponse response = gameService.saveGameSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lấy lịch sử các phiên chơi gần đây.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<MinigameSessionResponse>> getUserSessions(
            @RequestParam(required = false) UUID userId) {
        List<MinigameSessionResponse> sessions = gameService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Bảng xếp hạng điểm cao minigame.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<List<MinigameSessionResponse>> getLeaderboard() {
        List<MinigameSessionResponse> leaderboard = gameService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }
}
