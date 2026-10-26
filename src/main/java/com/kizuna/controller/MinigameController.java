package com.kizuna.controller;

import com.kizuna.dto.minigame.MinigameSessionRequest;
import com.kizuna.dto.minigame.MinigameSessionResponse;
import com.kizuna.service.IMinigameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller xử lý Game Analytics và lưu tiến độ cho Minigame Kanji Rain Typing.
 */
@RestController
@RequestMapping("/api/user/minigame")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MinigameController {

    private final IMinigameService minigameService;

    /**
     * POST /api/user/minigame/sessions: Lưu kết quả phiên chơi
     * Gồm: thời gian chơi, số từ gõ đúng, độ chính xác (Accuracy), chữ bị thua và danh sách các từ bị gõ sai nhiều nhất.
     */
    @PostMapping("/sessions")
    public ResponseEntity<MinigameSessionResponse> saveSession(
            @RequestParam(required = false) UUID userId,
            @Valid @RequestBody MinigameSessionRequest request) {
        MinigameSessionResponse response = minigameService.saveGameSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/user/minigame/sessions: Lấy lịch sử các phiên chơi của người dùng
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<MinigameSessionResponse>> getUserSessions(
            @RequestParam(required = false) UUID userId) {
        List<MinigameSessionResponse> list = minigameService.getUserSessions(userId);
        return ResponseEntity.ok(list);
    }
}
