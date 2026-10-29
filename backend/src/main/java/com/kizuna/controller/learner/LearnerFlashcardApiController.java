package com.kizuna.controller.learner;

import com.kizuna.dto.minigame.KanjiFolderSummaryDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.IKanjiFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller phục vụ các thao tác quản lý chữ Kanji trong Bộ Thẻ Flashcard cho Học Viên:
 * - POST /api/learner/flashcard-decks/{deckId}/kanji: Thêm chữ vào bộ thẻ
 * - DELETE /api/learner/flashcard-decks/{deckId}/kanji/{kanjiId}: Xóa chữ khỏi bộ thẻ
 * - POST /api/learner/flashcard-decks/{deckId}/duplicate: Nhân bản bộ thẻ
 * - POST /api/learner/flashcard-decks/{deckId}/merge: Gộp bộ thẻ
 */
@RestController
@RequestMapping("/api/learner/flashcard-decks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LearnerFlashcardApiController {

    private final IKanjiFolderService folderService;
    private final KanjiDictionaryRepository kanjiRepository;

    @PostMapping("/{deckId}/kanji")
    public ResponseEntity<Map<String, Object>> addKanjiToDeck(
            @PathVariable("deckId") UUID deckId,
            @RequestBody Map<String, Object> body) {
        UUID kanjiId = null;
        Object rawId = body.get("kanjiId");
        if (rawId != null && !rawId.toString().trim().isEmpty() 
                && !"null".equalsIgnoreCase(rawId.toString().trim()) 
                && !"undefined".equalsIgnoreCase(rawId.toString().trim())) {
            try {
                kanjiId = UUID.fromString(rawId.toString().trim());
            } catch (IllegalArgumentException ignored) {
                // Fallback nếu chuỗi không phải UUID hợp lệ
            }
        }

        // Nếu chưa có kanjiId hợp lệ, thử tra cứu theo character / kanjiChar
        String kanjiChar = null;
        if (kanjiId == null) {
            Object rawChar = body.get("character") != null ? body.get("character") : body.get("kanjiChar");
            if (rawChar != null && !rawChar.toString().trim().isEmpty()) {
                kanjiChar = rawChar.toString().trim();
                Optional<KanjiDictionary> kOpt = kanjiRepository.findByKanjiCharacter(kanjiChar);
                if (kOpt.isPresent()) {
                    kanjiId = kOpt.get().getKanjiId();
                }
            }
        } else {
            // Lấy character nếu có để phản hồi
            Optional<KanjiDictionary> kOpt = kanjiRepository.findById(kanjiId);
            if (kOpt.isPresent()) {
                kanjiChar = kOpt.get().getKanjiCharacter();
            }
        }

        if (kanjiId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, 
                    "message", "Không tìm thấy thông tin chữ Kanji hợp lệ trong từ điển để thêm vào bộ thẻ"
            ));
        }

        folderService.addKanjiToFolder(deckId, kanjiId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã thêm chữ Kanji vào bộ thẻ thành công");
        res.put("deckId", deckId);
        res.put("kanjiId", kanjiId);
        if (kanjiChar != null) {
            res.put("character", kanjiChar);
        }
        return ResponseEntity.ok(res);
    }

    @DeleteMapping("/{deckId}/kanji/{kanjiId}")
    public ResponseEntity<Map<String, Object>> removeKanjiFromDeck(
            @PathVariable("deckId") UUID deckId,
            @PathVariable("kanjiId") UUID kanjiId) {
        folderService.removeKanjiFromFolder(deckId, kanjiId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã xóa chữ Kanji khỏi bộ thẻ thành công");
        res.put("deckId", deckId);
        res.put("kanjiId", kanjiId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{deckId}/duplicate")
    public ResponseEntity<KanjiFolderSummaryDto> duplicateDeck(@PathVariable("deckId") UUID deckId) {
        KanjiFolderSummaryDto cloned = folderService.duplicateFolder(deckId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    @PostMapping("/{deckId}/merge")
    public ResponseEntity<KanjiFolderSummaryDto> mergeDecks(
            @PathVariable("deckId") UUID sourceDeckId,
            @RequestParam(required = false) UUID targetDeckId,
            @RequestBody(required = false) Map<String, Object> body) {
        UUID targetId = targetDeckId;
        if (targetId == null && body != null && body.get("targetDeckId") != null) {
            targetId = UUID.fromString(body.get("targetFolderId") != null ? body.get("targetFolderId").toString() : body.get("targetDeckId").toString());
        }
        if (targetId == null) {
            throw new IllegalArgumentException("Thiếu ID bộ thẻ đích cần gộp");
        }
        KanjiFolderSummaryDto merged = folderService.mergeFolders(sourceDeckId, targetId);
        return ResponseEntity.ok(merged);
    }
}
