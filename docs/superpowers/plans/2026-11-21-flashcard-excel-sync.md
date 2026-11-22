# Flashcard Excel Persistence & Game Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khắc phục triệt để lỗi mất dữ liệu khi nạp thẻ Flashcard từ Excel (cả ở trang danh sách và trang chi tiết), tự động lưu vào Database và LocalStorage, đồng thời nâng cấp bộ chuẩn hóa thẻ để minigame "Bắn chữ" (Kanji Rain Game) nhận diện đầy đủ cách đọc Kana cho cả chữ Hán lẫn từ vựng.

**Architecture:** Bổ sung endpoint RESTful `POST /api/learner/decks/{id}/cards` tại Spring Boot backend để cập nhật mảng thẻ con vào trường `cards_json` của bộ thẻ với kiểm tra quyền `owner_key`. Tại frontend, cải tiến `enrichFlashcardItem` trong `apiClient.js` làm Adapter trung tâm chuẩn hóa linh hoạt dữ liệu 2 cột, 3 cột và đa cột; tích hợp luồng Auto-save ngay khi tải file Excel xong.

**Tech Stack:** Java 17, Spring Boot 3, JdbcTemplate, Jackson ObjectMapper, Vanilla JavaScript (ES Modules), HTML5 Canvas (Kanji Game), Node.js test runner (`node:test`).

**Spec:** [docs/superpowers/specs/2026-11-21-flashcard-excel-sync-design.md](file:///c:/Users/Vuh26/Desktop/hm/KizunaTutor/docs/superpowers/specs/2026-11-21-flashcard-excel-sync-design.md)

## Global Constraints
- Backend: Endpoint phải kiểm tra xác thực JWT và bảo mật quyền sở hữu `owner_key` theo chuẩn PR #20 (ném 401 khi chưa đăng nhập, 403 khi thao tác bộ thẻ của người khác, 404 khi không tìm thấy bộ thẻ).
- Frontend: Luồng Zero-Reload qua `apiClient.js`, hiển thị thông báo qua `KizunaToast`, đồng bộ tức thời cả REST API và `kizuna_flashcard_decks` trong `localStorage`.
- Thẻ học: Luôn chuẩn hóa có các trường cơ bản `{ id, char, reading, meaning, jlpt, createdAt }` để tương thích với tất cả các chế độ học tập (Lật thẻ, Bắn chữ, Nối từ, In ấn).

## Review Focus
1. Nạp file Excel khi chưa đăng nhập hoặc token hết hạn: Không để crash giao diện, hiển thị cảnh báo Toast và fallback an toàn sang `localStorage`.
2. Thẻ Excel chỉ có 2 cột (Từ vựng | Nghĩa) không có cột Kana riêng: Tự động trích xuất Kana nếu nằm trong phần giải nghĩa hoặc gán fallback hợp lý để game Bắn chữ không bị treo.
3. Người dùng nạp Excel liên tục nhiều lần: Danh sách thẻ phải được nối tiếp (append), không bị ghi đè làm mất các thẻ đã nạp trước đó.
4. Nạp file Excel có dòng trống hoặc tiêu đề lặp: Bỏ qua dòng trống và dòng tiêu đề, chỉ nạp dữ liệu hợp lệ.
5. Bộ thẻ có sẵn thẻ tra cứu rồi nạp thêm thẻ Excel: Cả hai loại thẻ cùng hiển thị hòa hợp trên trang chi tiết và trong game Bắn chữ.

---

### Task 1: Backend Endpoint `POST /api/learner/decks/{id}/cards`

**Files:**
- Modify: `backend/src/main/java/com/kizuna/features/learning_support/controller/FlashcardDeckController.java`
- Test: `backend/src/test/java/com/kizuna/features/learning_support/FlashcardDeckControllerTest.java`

**Interfaces:**
- Produces: `POST /api/learner/decks/{id}/cards` -> trả về `{ "id": id, "title": title, "cards": [...] }`

- [ ] **Step 1: Viết test cho endpoint `POST /api/learner/decks/{id}/cards`**
  Thêm test case vào `FlashcardDeckControllerTest.java`:
  - `addCards_success_shouldAppendCardsAndUpdateDb()`: Giả lập bộ thẻ có 1 thẻ, gọi thêm 2 thẻ mới -> kết quả bộ thẻ có 3 thẻ, gọi `jdbcTemplate.update` lưu `cards_json`.
  - `addCards_unauthenticated_shouldThrow401()`: Không truyền JWT token -> ném lỗi 401.
  - `addCards_otherOwner_shouldThrow403()`: Truy cập bộ thẻ của tài khoản khác -> ném lỗi 403.
  - `addCards_notFound_shouldThrow404()`: Bộ thẻ không tồn tại -> ném lỗi 404.

- [ ] **Step 2: Chạy test để xác nhận test thất bại (do method chưa tồn tại)**
  Run: `./mvnw test -Dtest=FlashcardDeckControllerTest -pl backend`
  Expected: FAIL hoặc Compile Error do method chưa được định nghĩa trong controller.

- [ ] **Step 3: Cài đặt method `addCards` trong `FlashcardDeckController.java`**
  ```java
  @PostMapping("/{id}/cards")
  public ResponseEntity<Map<String, Object>> addCards(
          @PathVariable String id,
          @RequestBody Map<String, Object> body,
          @RequestHeader(value = "Authorization", required = false) String authHeader,
          @CookieValue(value = "access_token", required = false) String cookieToken) {
      String ownerKey = authenticatedUserHelper.requireAuthenticatedEmail(authHeader, cookieToken);
      ensureTable();

      Map<String, Object> existingRow = findDeckRowById(id)
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bộ thẻ: " + id));
      assertOwner(existingRow, ownerKey, "Bạn không có quyền thêm thẻ vào bộ thẻ của tài khoản khác.");

      List<Map<String, Object>> existingCards = parseCardsJson((String) existingRow.get("cards_json"));
      Object cardsObj = body.get("cards");
      List<Map<String, Object>> newCards = new ArrayList<>();
      if (cardsObj instanceof List<?> list) {
          for (Object item : list) {
              if (item instanceof Map<?, ?> map) {
                  Map<String, Object> cardMap = new LinkedHashMap<>((Map<String, Object>) map);
                  if (cardMap.get("id") == null || String.valueOf(cardMap.get("id")).isBlank()) {
                      cardMap.put("id", "card-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 5));
                  }
                  newCards.add(cardMap);
              }
          }
      }

      existingCards.addAll(newCards);
      String updatedCardsJson = toCardsJson(existingCards);

      jdbcTemplate.update(
              "UPDATE learner_flashcard_decks SET cards_json = ?, updated_at = NOW() WHERE id = ? AND owner_key = ?",
              updatedCardsJson, id, ownerKey
      );

      Map<String, Object> result = mapRowToDeck(existingRow);
      result.put("cards", existingCards);
      result.put("updatedAt", OffsetDateTime.now().toString());
      return ResponseEntity.ok(result);
  }
  ```

- [ ] **Step 4: Chạy test xác nhận thành công**
  Run: `./mvnw test -Dtest=FlashcardDeckControllerTest -pl backend`
  Expected: PASS toàn bộ test suite.

- [ ] **Step 5: Commit**
  ```bash
  git add backend/src/main/java/com/kizuna/features/learning_support/controller/FlashcardDeckController.java backend/src/test/java/com/kizuna/features/learning_support/FlashcardDeckControllerTest.java
  git commit -m "feat(flashcards): add POST /api/learner/decks/{id}/cards endpoint with owner guard"
  ```

---

### Task 2: Nâng Cấp Chuẩn Hóa Thẻ `enrichFlashcardItem` & Parser Đa Cột

**Files:**
- Modify: `frontend/js/apiClient.js`
- Create: `frontend/tests/flashcard-helpers.test.mjs`

**Interfaces:**
- Consumes: Raw card data từ Excel hoặc form nhập nhanh
- Produces: `enrichFlashcardItem(card: object) -> object` luôn đảm bảo có `char`, `reading`, `meaning`, `hanViet`, `jlpt`.

- [ ] **Step 1: Viết test cho `enrichFlashcardItem`**
  Tạo `frontend/tests/flashcard-helpers.test.mjs`:
  - Test case thẻ 2 cột có kana trong nghĩa: `{ char: '勉強', meaning: 'べんきょう, Học tập' }` -> trích xuất `reading: 'べんきょう'`.
  - Test case thẻ 2 cột thuần nghĩa: `{ char: '日', meaning: 'Mặt trời, ngày' }` -> không crash, gán `meaning`, fallback reading rỗng hoặc trích xuất phù hợp.
  - Test case thẻ 3 cột: `{ char: '猫', reading: 'ねこ', meaning: 'Con mèo' }` -> giữ nguyên `reading: 'ねこ'`.
  - Test case thẻ đa cột Kanji: `{ char: '月', onyomi: 'ゲツ, ガツ', kunyomi: 'つき', meaning: 'Mặt trăng' }` -> trích xuất reading ưu tiên kana.

- [ ] **Step 2: Chạy test xác nhận thất bại trước khi cập nhật**
  Run: `node --test frontend/tests/flashcard-helpers.test.mjs`

- [ ] **Step 3: Cải tiến `enrichFlashcardItem` trong `frontend/js/apiClient.js`**
  Bổ sung logic nhận diện từ vựng tiếng Nhật:
  - Nếu `char` chứa Kana thuần (ví dụ: こんにちは) -> gán luôn `reading = char`.
  - Nếu `char` chứa Kanji và `back` chứa chuỗi Kana (như `べんきょう`, `[さくら]`) -> trích xuất Kana đầu tiên làm `reading`.
  - Chuẩn hóa các trường `front`, `char`, `word` về `char`; `meaning`, `back`, `definition` về `meaning`.

- [ ] **Step 4: Chạy lại test xác nhận thành công**
  Run: `node --test frontend/tests/flashcard-helpers.test.mjs`
  Expected: PASS

- [ ] **Step 5: Commit**
  ```bash
  git add frontend/js/apiClient.js frontend/tests/flashcard-helpers.test.mjs
  git commit -m "feat(flashcards): improve enrichFlashcardItem reading extraction and add unit tests"
  ```

---

### Task 3: Tinh Chỉnh `apiClient.addCards` & Cơ Chế Đồng Bộ 2 Chiều

**Files:**
- Modify: `frontend/js/apiClient.js`

**Interfaces:**
- Consumes: `apiClient.addCards(id, cards)`
- Produces: Gửi `POST /api/learner/decks/{id}/cards` với token JWT; nếu thành công cập nhật `localStorage`; nếu offline fallback lưu `localStorage`.

- [ ] **Step 1: Cập nhật hàm `addCards` trong `frontend/js/apiClient.js`**
  - Đảm bảo header có `Authorization: Bearer <token>` thông qua hàm `getDeckHeaders(true)`.
  - Chuẩn hóa danh sách thẻ mới qua `enrichFlashcardItem` trước khi gửi lên API.
  - Khi API trả về danh sách `cards` đã cập nhật từ server, cập nhật ngay vào `localStorage` tương ứng của bộ thẻ đó.
  - Bổ sung fallback nếu server trả lỗi: vẫn lưu vào `localStorage` và cảnh báo người dùng.

- [ ] **Step 2: Cập nhật hàm `getDeck(id)` trong `frontend/js/apiClient.js`**
  - Khi tải chi tiết bộ thẻ: Nếu API trả về bộ thẻ từ Database, hợp nhất thông minh (Smart Merge) với các thẻ chưa kịp đồng bộ trong `localStorage` (nếu có thẻ cục bộ mới hơn).

- [ ] **Step 3: Chạy test kiểm thử tích hợp frontend**
  Run: `node --test frontend/tests/flashcard-helpers.test.mjs`
  Expected: PASS

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/js/apiClient.js
  git commit -m "fix(flashcards): robust API sync and smart merge in apiClient.addCards"
  ```

---

### Task 4: Tích Hợp Auto-Save khi Import Excel & Cập Nhật Mẫu Tải Xuống

**Files:**
- Modify: `frontend/pages/flashcards-detail/flashcards-detail.js`
- Modify: `frontend/pages/flashcards/flashcards.js`

**Interfaces:**
- Event listener cho input file Excel (`handleExcelImport` / `handleExcelUpload`)

- [ ] **Step 1: Cập nhật `handleExcelImport` trong `flashcards-detail.js`**
  - Sau khi parse các dòng Excel thành `newCards`:
  - Gọi ngay `await apiClient.addCards(currentDeckId, newCards)` để lưu Database ngay lập tức.
  - Cập nhật biến `currentCards` và gọi `renderCardsList()`.
  - Hiển thị Toast thành công: `KizunaToast.success(\`Đã lưu vĩnh viễn \${newCards.length} thẻ từ file Excel!\`)`.
  - Đóng modal nạp thẻ tự động.

- [ ] **Step 2: Cập nhật `handleExcelUpload` trong `flashcards.js`**
  - Khi người dùng nạp file Excel trong Modal Chỉnh Sửa Bộ Thẻ:
  - Tự động gọi `apiClient.addCards(activeEditingDeck.id, newCards)` ngay khi chọn file xong.
  - Cập nhật cả `activeEditingDeck.cards` và hiển thị danh sách thẻ.
  - Báo Toast: `KizunaToast.success(\`Đã đọc và lưu vĩnh viễn \${newCards.length} thẻ vào bộ thẻ!\`)`.

- [ ] **Step 3: Cập nhật hàm `downloadSampleExcel` ở cả 2 trang**
  - Mẫu Excel cung cấp các ví dụ thực tế gồm:
    1. Chữ Hán đơn lẻ (日, 月, 火...)
    2. Từ vựng ghép 2 chữ Hán kèm Kana (勉強 - べんきょう, 日本 - にほん, 富士山 - ふじさん)
    3. Từ vựng thuần Kana (こんにちは, ありがとう)
  - Hỗ trợ tiêu đề cột rõ ràng: `Mặt trước (Chữ Hán / Từ vựng)`, `Cách đọc Kana (Furigana)`, `Nghĩa tiếng Việt`, `Cấp độ JLPT`, `Ví dụ`.

- [ ] **Step 4: Kiểm tra lint và cú pháp**
  Run: `node --check frontend/pages/flashcards-detail/flashcards-detail.js`
  Run: `node --check frontend/pages/flashcards/flashcards.js`
  Expected: Không có lỗi cú pháp.

- [ ] **Step 5: Commit**
  ```bash
  git add frontend/pages/flashcards-detail/flashcards-detail.js frontend/pages/flashcards/flashcards.js
  git commit -m "feat(flashcards): auto-save on Excel import and enrich sample template"
  ```

---

### Task 5: Kiểm Thử Tương Thích Minigame "Bắn Chữ" & Nghiệm Thu End-to-End

**Files:**
- Test: `frontend/pages/kanji-game/kanji-game.js`
- Test: `frontend/tests/flashcard-helpers.test.mjs`

- [ ] **Step 1: Bổ sung test case kiểm tra hàm chuyển đổi thẻ sang game entry**
  Trong `frontend/tests/flashcard-helpers.test.mjs`, kiểm tra các đối tượng thẻ nạp từ Excel khi chuyển qua hàm `mapDeckCardToGameEntry` của game Bắn chữ:
  - Thẻ chữ Hán: sinh ra `primaryReading` và `readings` chứa âm On/Kun/Hán-Việt.
  - Thẻ từ vựng (ví dụ: `勉強` / `べんきょう`): sinh ra `primaryReading = 'べんきょう'`, `readings` chứa cả `benkyou`.
  - Thẻ thuần Kana (ví dụ: `さくら`): sinh ra `primaryReading = 'さくら'`, `readings` chứa cả `sakura`.

- [ ] **Step 2: Chạy toàn bộ test suite frontend và backend**
  Run: `node --test frontend/tests/flashcard-helpers.test.mjs`
  Run: `./mvnw test -Dtest=FlashcardDeckControllerTest,Pr20SecurityRegressionTest -pl backend`
  Expected: PASS 100%.

- [ ] **Step 3: Commit hoàn tất**
  ```bash
  git add frontend/tests/flashcard-helpers.test.mjs
  git commit -m "test(flashcards): verify minigame reading mappings and end-to-end compatibility"
  ```
