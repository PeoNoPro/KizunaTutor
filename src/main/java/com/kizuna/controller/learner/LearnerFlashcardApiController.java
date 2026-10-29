package com.kizuna.controller.learner;

import com.kizuna.dto.minigame.CreateKanjiFolderRequest;
import com.kizuna.dto.minigame.KanjiFolderDetailDto;
import com.kizuna.dto.minigame.KanjiFolderSummaryDto;
import com.kizuna.entity.KanjiDictionary;
import com.kizuna.repository.KanjiDictionaryRepository;
import com.kizuna.service.IKanjiFolderService;
import com.kizuna.service.kanji.HandwritingSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller phục vụ các thao tác quản lý Bộ Thẻ Flashcard cho Học Viên:
 * - GET    /api/learner/decks: Lấy danh sách bộ thẻ kèm chữ
 * - POST   /api/learner/decks: Tạo bộ thẻ mới
 * - GET    /api/learner/decks/{deckId}: Lấy chi tiết bộ thẻ
 * - POST   /api/learner/decks/{deckId}/cards (hoặc /kanji): Thêm chữ vào bộ thẻ
 * - DELETE /api/learner/decks/{deckId}/cards/{kanjiId}: Xóa chữ khỏi bộ thẻ
 * - DELETE /api/learner/decks/{deckId}: Xóa bộ thẻ
 * - POST   /api/learner/decks/{deckId}/duplicate: Nhân bản bộ thẻ
 * - POST   /api/learner/decks/{deckId}/merge: Gộp bộ thẻ
 */
@RestController
@RequestMapping({"/api/learner/flashcard-decks", "/api/learner/decks"})
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LearnerFlashcardApiController {

    private final IKanjiFolderService folderService;
    private final KanjiDictionaryRepository kanjiRepository;
    private final HandwritingSearchService handwritingSearchService;

    /**
     * 1. GET danh sách toàn bộ bộ thẻ học tập của Học Viên
     */
    @GetMapping
    public ResponseEntity<List<KanjiFolderSummaryDto>> getDecks(@RequestParam(required = false) UUID userId) {
        List<KanjiFolderSummaryDto> folders = folderService.getUserFolders(userId);
        return ResponseEntity.ok(folders);
    }

    /**
     * 2. GET chi tiết một bộ thẻ kèm danh sách các thẻ chữ
     */
    @GetMapping("/{deckId}")
    public ResponseEntity<KanjiFolderDetailDto> getDeckDetail(@PathVariable("deckId") UUID deckId) {
        KanjiFolderDetailDto detail = folderService.getFolderDetail(deckId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 3. POST tạo bộ thẻ mới
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDeck(
            @RequestParam(required = false) UUID userId,
            @RequestBody(required = false) Map<String, Object> body) {
        String name = "Bộ Thẻ Cá Nhân";
        String description = "Tạo từ bảng vẽ Shodo";
        String color = "#15803D";

        if (body != null) {
            if (body.get("deckName") != null && !body.get("deckName").toString().isBlank()) {
                name = body.get("deckName").toString().trim();
            } else if (body.get("folderName") != null && !body.get("folderName").toString().isBlank()) {
                name = body.get("folderName").toString().trim();
            } else if (body.get("name") != null && !body.get("name").toString().isBlank()) {
                name = body.get("name").toString().trim();
            }

            if (body.get("description") != null && !body.get("description").toString().isBlank()) {
                description = body.get("description").toString().trim();
            }
            if (body.get("color") != null && !body.get("color").toString().isBlank()) {
                color = body.get("color").toString().trim();
            }
        }

        CreateKanjiFolderRequest req = new CreateKanjiFolderRequest();
        req.setFolderName(name);
        req.setDescription(description);
        req.setColor(color);

        KanjiFolderSummaryDto created = folderService.createFolder(userId, req);

        Map<String, Object> res = new HashMap<>();
        res.put("id", created.getFolderId());
        res.put("folderId", created.getFolderId());
        res.put("deckId", created.getFolderId());
        res.put("deckName", created.getFolderName());
        res.put("folderName", created.getFolderName());
        res.put("description", created.getDescription());
        res.put("kanjiCount", created.getKanjiCount());
        res.put("color", created.getColor());
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /**
     * 4. POST thêm chữ Kanji vào bộ thẻ (hỗ trợ cả /{deckId}/kanji và /{deckId}/cards)
     */
    @PostMapping(path = {"/{deckId}/kanji", "/{deckId}/cards"})
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
            }
        }

        String kanjiChar = null;
        Object rawChar = body.get("character") != null ? body.get("character") : body.get("kanjiChar");
        if (rawChar != null && !rawChar.toString().trim().isEmpty()) {
            kanjiChar = rawChar.toString().trim();
        }

        if (kanjiId != null) {
            Optional<KanjiDictionary> kOpt = kanjiRepository.findById(kanjiId);
            if (kOpt.isPresent() && kanjiChar == null) {
                kanjiChar = kOpt.get().getKanjiCharacter();
            }
        }

        if (kanjiId == null && kanjiChar != null) {
            Optional<KanjiDictionary> kOpt = kanjiRepository.findByKanjiCharacter(kanjiChar);
            if (kOpt.isPresent()) {
                kanjiId = kOpt.get().getKanjiId();
            } else {
                // Tự động tìm kiếm dữ liệu offline hoặc tạo mới bản ghi KanjiDictionary
                var offlineOpt = handwritingSearchService.getOfflineKanji(kanjiChar);
                String hanViet = body.get("hanViet") != null ? body.get("hanViet").toString()
                        : (offlineOpt.map(HandwritingSearchService.OfflineKanji::han_viet).orElse("HÁN TỰ"));
                String meaning = body.get("meaning") != null ? body.get("meaning").toString()
                        : (offlineOpt.map(HandwritingSearchService.OfflineKanji::meaning).orElse(""));
                String onyomi = body.get("onyomi") != null ? body.get("onyomi").toString()
                        : (offlineOpt.map(HandwritingSearchService.OfflineKanji::onyomi).orElse("-"));
                String kunyomi = body.get("kunyomi") != null ? body.get("kunyomi").toString()
                        : (offlineOpt.map(HandwritingSearchService.OfflineKanji::kunyomi).orElse("-"));
                Short strokeCount = (short) 4;
                if (body.get("strokeCount") != null) {
                    try {
                        strokeCount = Short.valueOf(body.get("strokeCount").toString().trim());
                    } catch (Exception ignored) {}
                }

                String singleChar = kanjiChar.substring(0, 1);
                String hex = String.format("%04x", (int) singleChar.charAt(0));
                KanjiDictionary newKanji = KanjiDictionary.builder()
                        .kanjiCharacter(singleChar)
                        .unicodeHex(hex)
                        .hanViet(hanViet)
                        .meaning(meaning)
                        .onyomi(onyomi)
                        .kunyomi(kunyomi)
                        .strokeCount(strokeCount)
                        .radical(singleChar)
                        .build();
                newKanji = kanjiRepository.save(newKanji);
                kanjiId = newKanji.getKanjiId();
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
        res.put("message", "Đã thêm chữ Kanji [" + (kanjiChar != null ? kanjiChar : "") + "] vào bộ thẻ thành công");
        res.put("deckId", deckId);
        res.put("id", deckId);
        res.put("kanjiId", kanjiId);
        if (kanjiChar != null) {
            res.put("character", kanjiChar);
            res.put("kanjiChar", kanjiChar);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * 5. DELETE xóa chữ Kanji khỏi bộ thẻ (hỗ trợ cả /{deckId}/kanji/{kanjiId} và /{deckId}/cards/{kanjiId})
     */
    @DeleteMapping(path = {"/{deckId}/kanji/{kanjiId}", "/{deckId}/cards/{kanjiId}"})
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

    /**
     * 6. DELETE xóa nguyên một bộ thẻ
     */
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Map<String, Object>> deleteDeck(@PathVariable("deckId") UUID deckId) {
        folderService.deleteFolder(deckId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã xóa bộ thẻ thành công");
        res.put("deckId", deckId);
        return ResponseEntity.ok(res);
    }

    /**
     * 7. POST nhân bản bộ thẻ
     */
    @PostMapping("/{deckId}/duplicate")
    public ResponseEntity<KanjiFolderSummaryDto> duplicateDeck(@PathVariable("deckId") UUID deckId) {
        KanjiFolderSummaryDto cloned = folderService.duplicateFolder(deckId);
        return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
    }

    /**
     * 8. POST gộp bộ thẻ
     */
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
