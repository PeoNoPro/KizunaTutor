# Thiết Kế: Sửa Lỗi Đồng Bộ Lưu Trữ Flashcard Excel & Tương Thích Minigame Bắn Chữ

- **Ngày tạo**: 2026-11-21
- **Trạng thái**: Chờ phê duyệt (Pending Approval)
- **Tác giả / Module**: Learning Support - Flashcards & Minigames (`KizunaTutor`)

---

## 1. Bối cảnh & Mục tiêu (Intent & Goals)

### 1.1. Hiện trạng & Vấn đề
- Người học thêm chữ Hán / từ vựng vào "Bộ thẻ của tôi" bằng tính năng tra cứu từ điển thì lưu thành công vào Database.
- Tuy nhiên, khi nhập dữ liệu từ file Excel (ở trang chi tiết `/learner/deck-detail` hoặc modal chỉnh sửa `/learner/flashcards`):
  1. Frontend gọi `apiClient.addCards(id, cards)` tới `POST /api/learner/decks/{id}/cards`, nhưng Backend (`FlashcardDeckController.java`) **chưa có endpoint này** $\rightarrow$ Dữ liệu chỉ ghi tạm vào `localStorage`. Khi thoát ra hoặc F5, `getDeck(id)` kéo từ Database về ghi đè, làm mất sạch thẻ vừa nạp từ Excel.
  2. Ở modal danh sách bộ thẻ, import Excel chỉ đưa vào biến tạm `editingCards`, nếu người dùng không bấm nút "Lưu thay đổi" thì dữ liệu bị hủy.
  3. Định dạng thẻ từ Excel nếu thiếu cách đọc Kana (`reading`) sẽ khiến game "Bắn chữ" (Kanji Rain Game) không xác định được phím gõ chính xác để hạ gục từ rơi xuống.

### 1.2. Mục tiêu (Goals)
1. **Lưu trữ vĩnh viễn (Persistence):** Thẻ nạp từ Excel được ghi trực tiếp vào Database thông qua endpoint backend chính thức và đồng bộ với LocalStorage. Sau khi tải lại trang, đăng xuất/đăng nhập lại thẻ vẫn còn nguyên.
2. **Auto-save tiện lợi:** Nạp file Excel xong là tự động lưu ngay, có thông báo Toast rõ ràng, không bắt người dùng phải bấm thêm nút lưu phụ dễ quên.
3. **Đồng nhất dữ liệu (Unified Schema):** Không chia tách 2 thư mục/bảng riêng; dùng chung 1 mảng JSON chuẩn trong `learner_flashcard_decks`.
4. **Tương thích Minigame:** Parser Excel và `enrichFlashcardItem` tự động nhận diện từ vựng lẫn chữ Hán, tự động bóc tách/gán cách đọc Kana (`reading`) để game "Bắn chữ" (`kanji-game.js`) tương tác mượt mà.
5. **Template mẫu chuẩn:** Cập nhật file Excel mẫu tải về hỗ trợ cả chữ Hán lẫn từ vựng tiếng Nhật.

### 1.3. Phi mục tiêu (Non-goals)
- Không thay đổi cấu trúc bảng CSDL `learner_flashcard_decks` (giữ nguyên kiểu dữ liệu `cards_json TEXT`).
- Không bắt buộc người dùng phải gõ định dạng khắt khe (chấp nhận cả file 2 cột, 3 cột, đa cột).

---

## 2. Các phương án kiến trúc & Lựa chọn

### Phương án A: Chỉ vá ở Frontend (Gọi `PUT /api/learner/decks/{id}`)
- *Cách làm:* Frontend lấy toàn bộ mảng thẻ cũ, nối mảng mới vào rồi gọi `PUT` đè lên bộ thẻ.
- *Nhược điểm:* Nguy cơ ghi đè dữ liệu (race condition) nếu có nhiều tab cùng mở; Backend thiếu endpoint chuẩn theo đặc tả RESTful `POST /decks/{id}/cards`.

### Phương án B (Được chọn): Hoàn thiện toàn diện Backend + Frontend (2 giai đoạn tuần tự)
- **Giai đoạn 1: Sửa lỗi Đồng bộ & Bền vững dữ liệu (Persistence & API)**
  - Thêm endpoint `POST /api/learner/decks/{id}/cards` vào `FlashcardDeckController.java` (nhận danh sách thẻ con, nối vào `cards_json` hiện tại trong DB, kiểm tra quyền sở hữu `owner_key`, cập nhật `updated_at`).
  - Cập nhật `apiClient.addCards` để gửi dữ liệu chuẩn, xử lý đồng bộ mượt mà với LocalStorage.
  - Tự động gọi lưu Database ngay sau khi parse Excel thành công ở cả 2 màn hình (`flashcards.js` và `flashcards-detail.js`).
- **Giai đoạn 2: Nâng cấp Bộ chuẩn hóa Excel & Tương thích Minigame**
  - Cải tiến hàm `enrichFlashcardItem` trong `apiClient.js` để tự động suy luận và trích xuất cách đọc Kana từ từ vựng/nghĩa nếu người dùng nạp file 2 cột hoặc 3 cột.
  - Đồng bộ logic nạp file Excel ở `flashcards.js` và `flashcards-detail.js` theo bộ Parser thống nhất: nhận diện linh hoạt 2 cột, 3 cột, 4+ cột.
  - Cập nhật hàm tải file Excel mẫu `downloadSampleExcel` bao gồm cả Kanji và từ vựng mẫu (như 勉強, 桜, 日本語...).

---

## 3. Đặc tả kỹ thuật chi tiết

### 3.1. Backend API: `POST /api/learner/decks/{id}/cards`
- **Request Body:**
  ```json
  {
    "cards": [
      {
        "id": "card-1732170000000-abc12",
        "char": "勉強",
        "reading": "べんきょう",
        "meaning": "Học tập, việc học",
        "jlpt": "N5"
      }
    ]
  }
  ```
- **Xử lý:**
  1. Kiểm tra JWT token và lấy `ownerKey`.
  2. Tìm bộ thẻ theo `id`. Nếu không thấy trả về 404; nếu `owner_key` không khớp trả về 403.
  3. Parse `cards_json` hiện tại, nối thêm danh sách thẻ mới (tự sinh `id` nếu thẻ chưa có `id`).
  4. Update `cards_json` và `updated_at = NOW()`.
  5. Trả về `200 OK` với danh sách thẻ đã cập nhật.

### 3.2. Frontend: Auto-save & Feedback
- Khi người dùng chọn file Excel:
  1. Hiển thị thông báo Toast đang xử lý.
  2. Parse file thành danh sách thẻ đã chuẩn hóa bằng `enrichFlashcardItem`.
  3. Gọi ngay `apiClient.addCards(deckId, newCards)` để ghi vào Database.
  4. Cập nhật giao diện danh sách thẻ Zero-Reload.
  5. Báo Toast thành công: *"Đã lưu thành công X thẻ vào bộ nhớ vĩnh viễn!"*.

### 3.3. Tương thích Minigame Bắn chữ
- Cột mốc đọc:
  - Thẻ chữ Hán đơn lẻ (ví dụ: 日): có `onyomi` / `kunyomi` / `reading` $\rightarrow$ gõ được cả âm Hán hoặc Kana.
  - Thẻ từ vựng (ví dụ: 桜 - さくら, 勉強 - べんきょう): `char` = 勉強, `reading` = べんきょう $\rightarrow$ `kanji-game.js` tự động lấy `primaryReading` = べんきょう để người chơi gõ `benkyou` bắn chữ.

---

## 4. Kế hoạch xác minh (Verification Plan)
1. **Kiểm tra lưu trữ:**
   - Tạo bộ thẻ mới $\rightarrow$ Nhập file Excel $\rightarrow$ Thoát hẳn ra Dashboard / F5 tải lại trang $\rightarrow$ Kiểm tra thẻ vẫn còn nguyên đầy đủ.
2. **Kiểm tra đa dạng format Excel:**
   - Thử nghiệm file 2 cột (Từ vựng | Nghĩa có chứa kana).
   - Thử nghiệm file 3 cột (Từ vựng | Kana | Nghĩa tiếng Việt).
   - Thử nghiệm file mẫu đa cột (Kanji chuẩn).
3. **Kiểm tra Minigame Bắn chữ:**
   - Vào `/learner/kanji-game`, chọn nguồn từ bộ thẻ vừa nạp $\rightarrow$ Bắt đầu chơi $\rightarrow$ Các từ vựng/chữ Hán rơi xuống và gõ trúng bằng Kana/Romaji/Hán Việt thành công.
