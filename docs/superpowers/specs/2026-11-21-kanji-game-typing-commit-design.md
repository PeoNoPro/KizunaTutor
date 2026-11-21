# Thiết Kế: Cải Tiến Cơ Chế Gõ Phím Bắn Chữ (Space / Enter Commit & IME Safe)

- **Ngày tạo**: 2026-11-21
- **Trạng thái**: Chờ phê duyệt (Pending Approval)
- **Mục tiêu**: Minigame "Mưa chữ Kanji" (`frontend/pages/kanji-game/`)

---

## 1. Bối cảnh & Mục tiêu (Intent & Goals)

### 1.1. Hiện trạng & Vấn đề
- Trò chơi đang dùng cơ chế "Bắn tức thì" (Instant Match on Input): vừa gõ đủ ký tự là hệ thống tự xóa ô gõ và bắn nổ chữ.
- **Xung đột bộ gõ (IME Buffer):** Khi người dùng bật Unikey/EVKey (tiếng Việt) hoặc Microsoft IME (tiếng Nhật), việc JavaScript tự ý gán `input.value = ''` giữa chừng làm đứt gãy phiên gõ (`composition`). Bộ gõ hệ điều hành sẽ đẩy ngược các ký tự cũ vào ô gõ ở phím tiếp theo, khiến chữ không xóa được.
- **Kẹt chữ khi gõ nhầm:** Nếu gõ thừa hoặc sai 1 ký tự, chuỗi sai nằm kẹt trong ô, người chơi mất quyền tự chủ và phải nhấn Backspace nhiều lần hoặc click chuột vào nút xóa.
- **Xung đột từ ngắn - từ dài:** Khi có 2 chữ rơi cùng lúc (ví dụ chữ có cách đọc `hi` và `hito`), người chơi định gõ `hito` thì vừa gõ `hi` đã bị bắn mất chữ ngắn và xóa mất từ đang gõ dở.

### 1.2. Mục tiêu (Goals)
1. **Cơ chế Space / Enter Commit:** Người chơi chủ động gõ từ và nhấn phím `Space` (Phím cách) hoặc `Enter` để thực hiện lệnh bắn (Shoot).
2. **IME Safe (An toàn với bộ gõ):** Theo dõi cờ `isComposing` qua các sự kiện `compositionstart` và `compositionend`. Chỉ thực hiện lệnh commit khi người dùng đã kết thúc phiên gõ IME (tránh việc gõ phím Space/Enter để chọn từ trong IME bị kích hoạt bắn nhầm).
3. **Auto-Clear on Miss (Tự dọn khi bắn hụt):** Nếu người chơi nhấn Space/Enter mà không trúng chữ nào: ô gõ rung đỏ nhẹ (`shake-error`), phát âm thanh buzz và tự động xóa sạch nội dung về rỗng ngay sau 120ms để người chơi gõ tiếp từ khác mà không cần ấn xóa từng phím.
4. **Targeting thời gian thực (Live Target Preview):** Trong khi đang gõ từng ký tự (chưa nhấn Space/Enter), chữ rơi tương ứng vẫn được làm nổi bật viền sáng Neon (`isTargeted = true`) để người chơi biết mình đang ngắm trúng mục tiêu nào.
5. **Cập nhật giao diện trực quan:** Đổi placeholder trong ô nhập liệu thành `"Nhập cách đọc rồi nhấn [Space] hoặc [Enter] để bắn 🚀"`.

### 1.3. Phi mục tiêu (Non-goals)
- Không thay đổi thuật toán tính điểm combo, độ khó hay tốc độ rơi của chữ.
- Không thay đổi các bảng cơ sở dữ liệu hay API backend.

---

## 2. Giải pháp kỹ thuật chi tiết

### 2.1. Quản lý trạng thái IME (`isComposing`)
```javascript
let isComposing = false;

inputField.addEventListener('compositionstart', () => {
  isComposing = true;
});

inputField.addEventListener('compositionend', () => {
  isComposing = false;
  // Cập nhật lại target hiển thị sau khi kết thúc gõ bộ gõ
  updateTargetKanji();
});
```

### 2.2. Xử lý sự kiện bàn phím (Keyboard Event)
- **Khi gõ từng phím (`input` event):**
  - Chỉ cập nhật hiển thị chữ đang nhắm tới (`updateTargetKanji()`) và thanh hiển thị gõ trực tiếp (`liveTypingEcho`).
  - **KHÔNG** tự động tiêu diệt chữ hay xóa ô khi chưa nhấn Space/Enter.
- **Khi nhấn `Space` hoặc `Enter` (`keydown` event):**
  - Nếu `isComposing === true` (đang trong phiên chọn chữ của IME): Cho phép IME xử lý bình thường, không kích hoạt bắn.
  - Nếu `isComposing === false`:
    - Gọi `e.preventDefault()` để tránh ký tự khoảng trắng thừa lọt vào ô.
    - Gọi `checkTypingMatch(true)` để bắn chữ.

### 2.3. Xử lý trong `checkTypingMatch(isCommit)`
- **Nếu Bắn Trúng:**
  - Tiêu diệt chữ rơi, phát âm thanh ding, tăng điểm combo.
  - Xóa sạch ô nhập: `inputField.value = ''`.
- **Nếu Bắn Hụt (khi `isCommit === true`):**
  - Rung đỏ cảnh báo `shake-error`.
  - Tự động xóa sạch ô nhập: `inputField.value = ''` (hoặc bôi đen toàn bộ `inputField.select()`).
  - Người chơi lập tức có thể gõ từ tiếp theo mà không bị kẹt phím.

---

## 3. Kế hoạch kiểm thử & Xác minh
1. **Kiểm thử gõ bình thường (Romaji / Tiếng Anh):**
   - Gõ `nichi` $\rightarrow$ Chữ 日 sáng đèn viền neon $\rightarrow$ Nhấn `Space` $\rightarrow$ Chữ nổ tung, ô gõ sạch về rỗng.
2. **Kiểm thử gõ sai (Typo):**
   - Gõ `abc` $\rightarrow$ Nhấn `Space` hoặc `Enter` $\rightarrow$ Ô rung đỏ, tự động xóa sạch về rỗng, người chơi gõ tiếp `hi` $\rightarrow$ Nhấn `Space` $\rightarrow$ Bắn trúng.
3. **Kiểm thử tương thích Unikey / EVKey:**
   - Bật Unikey tiếng Việt, gõ `nhat` $\rightarrow$ `nhật` $\rightarrow$ Nhấn `Enter`/`Space` $\rightarrow$ Bắn trúng chữ 日, không bị kẹt buffer hay sinh ra ký tự rác.
4. **Kiểm thử cú pháp & Regression test:**
   - Chạy `node --check` và bộ test frontend để đảm bảo an toàn 100%.
