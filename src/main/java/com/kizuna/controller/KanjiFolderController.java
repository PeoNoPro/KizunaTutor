package com.kizuna.controller;

import com.kizuna.dto.minigame.*;
import com.kizuna.service.IKanjiFolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller quản lý Bộ thẻ / Thư mục Kanji của Người dùng.
 * Phục vụ các thao tác CRUD danh mục học tập và chuẩn bị dữ liệu cho Minigame.
 */
@RestController
@RequestMapping("/api/user/kanji-folders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class KanjiFolderController {

    private final IKanjiFolderService folderService;

    /**
     * 1. GET /api/user/kanji-folders: Lấy danh sách thư mục của người dùng kèm số lượng chữ trong mỗi thư mục.
     */
    @GetMapping
    public ResponseEntity<List<KanjiFolderSummaryDto>> getUserFolders(
            @RequestParam(required = false) UUID userId) {
        List<KanjiFolderSummaryDto> folders = folderService.getUserFolders(userId);
        return ResponseEntity.ok(folders);
    }

    /**
     * Chi tiết thư mục kèm danh sách chữ Kanji bên trong
     */
    @GetMapping("/{id}")
    public ResponseEntity<KanjiFolderDetailDto> getFolderDetail(@PathVariable("id") UUID folderId) {
        KanjiFolderDetailDto detail = folderService.getFolderDetail(folderId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 2. POST /api/user/kanji-folders: Tạo thư mục mới kèm danh sách kanjiIds ban đầu.
     */
    @PostMapping
    public ResponseEntity<KanjiFolderSummaryDto> createFolder(
            @RequestParam(required = false) UUID userId,
            @Valid @RequestBody CreateKanjiFolderRequest request) {
        KanjiFolderSummaryDto created = folderService.createFolder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 3. PUT /api/user/kanji-folders/{id}: Đổi tên thư mục, thêm/bớt chữ Kanji trong thư mục.
     */
    @PutMapping("/{id}")
    public ResponseEntity<KanjiFolderSummaryDto> updateFolder(
            @PathVariable("id") UUID folderId,
            @Valid @RequestBody UpdateKanjiFolderRequest request) {
        KanjiFolderSummaryDto updated = folderService.updateFolder(folderId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * 4. DELETE /api/user/kanji-folders/{id}: Xóa thư mục.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteFolder(@PathVariable("id") UUID folderId) {
        folderService.deleteFolder(folderId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã xóa thư mục Kanji thành công");
        res.put("folderId", folderId);
        return ResponseEntity.ok(res);
    }

    /**
     * 4b. POST /api/user/kanji-folders/{id}/items/{kanjiId}: Thêm 1 chữ Kanji vào bộ thẻ.
     */
    @PostMapping("/{id}/items/{kanjiId}")
    public ResponseEntity<Map<String, Object>> addKanjiToFolder(
            @PathVariable("id") UUID folderId,
            @PathVariable("kanjiId") UUID kanjiId) {
        folderService.addKanjiToFolder(folderId, kanjiId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã thêm chữ Kanji vào bộ thẻ thành công");
        res.put("folderId", folderId);
        res.put("kanjiId", kanjiId);
        return ResponseEntity.ok(res);
    }

    /**
     * 4b2. POST /api/user/kanji-folders/{id}/items: Thêm 1 chữ Kanji vào bộ thẻ qua Request Body
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<Map<String, Object>> addKanjiToFolderByBody(
            @PathVariable("id") UUID folderId,
            @RequestBody Map<String, Object> body) {
        Object rawId = body.get("kanjiId");
        if (rawId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu tham số kanjiId"));
        }
        UUID kanjiId = UUID.fromString(rawId.toString());
        folderService.addKanjiToFolder(folderId, kanjiId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã thêm chữ Kanji vào bộ thẻ thành công");
        res.put("folderId", folderId);
        res.put("kanjiId", kanjiId);
        return ResponseEntity.ok(res);
    }

    /**
     * 4d. POST /api/user/kanji-folders/{id}/add-random: Nạp ngẫu nhiên chữ Kanji theo JLPT (tự động loại trừ chữ đã có)
     */
    @PostMapping("/{id}/add-random")
    public ResponseEntity<AddRandomKanjiResponse> addRandomKanjis(
            @PathVariable("id") UUID folderId,
            @RequestBody(required = false) AddRandomKanjiRequest request) {
        AddRandomKanjiResponse response = folderService.addRandomKanjis(
                folderId,
                request != null ? request : new AddRandomKanjiRequest("N5", 10)
        );
        return ResponseEntity.ok(response);
    }

    /**
     * 4c. DELETE /api/user/kanji-folders/{id}/items/{kanjiId}: Xóa 1 chữ Kanji khỏi bộ thẻ.
     */
    @DeleteMapping("/{id}/items/{kanjiId}")
    public ResponseEntity<Map<String, Object>> removeKanjiFromFolder(
            @PathVariable("id") UUID folderId,
            @PathVariable("kanjiId") UUID kanjiId) {
        folderService.removeKanjiFromFolder(folderId, kanjiId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Đã xóa chữ Kanji khỏi bộ thẻ thành công");
        res.put("folderId", folderId);
        res.put("kanjiId", kanjiId);
        return ResponseEntity.ok(res);
    }

    /**
     * 5. GET /api/user/kanji-folders/{id}/play-data: Lấy danh sách chữ kèm cách đọc Hiragana (kunyomi/onyomi) chuẩn hóa để nạp vào game.
     */
    @GetMapping("/{id}/play-data")
    public ResponseEntity<List<KanjiPlayDataDto>> getFolderPlayData(@PathVariable("id") UUID folderId) {
        List<KanjiPlayDataDto> playData = folderService.getFolderPlayData(folderId);
        return ResponseEntity.ok(playData);
    }
}
