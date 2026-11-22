# Kanji Game Typing Commit & Auto-Clear Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển đổi cơ chế gõ phím trong minigame "Mưa chữ Kanji" từ bắn tức thì sang chủ động bắn bằng phím `Space` hoặc `Enter`, hỗ trợ an toàn bộ gõ tiếng Việt / tiếng Nhật (IME Safe) và tự động xóa sạch ô nhập khi bắn hụt.

**Architecture:** Sử dụng mô hình commit theo lệnh (Command-driven input): sự kiện `input` chỉ cập nhật live target preview; sự kiện `keydown` phím `Space` / `Enter` (khi `isComposing === false`) kích hoạt lệnh bắn trong `checkTypingMatch`. Bắn trúng hay bắn hụt đều tự động làm sạch ô gõ để duy trì nhịp gõ 10 ngón liên tục.

**Tech Stack:** JavaScript (ES6+), HTML5 Canvas, Node.js test runner (`node:test`).

**Spec:** [docs/superpowers/specs/2026-11-21-kanji-game-typing-commit-design.md](file:///c:/Users/Vuh26/Desktop/hm/KizunaTutor/docs/superpowers/specs/2026-11-21-kanji-game-typing-commit-design.md)

## Global Constraints
- Không làm thay đổi hệ số tính điểm, combo hay tốc độ rơi của các chữ Hán.
- Phải tương thích hoàn toàn với các bộ gõ tiếng Việt (Unikey/EVKey) và IME tiếng Nhật trên Windows/Mac.
- Không gây ảnh hưởng đến các màn hình và trò chơi khác trong khối Learning Support.

## Review Focus
1. Người chơi gõ bộ gõ tiếng Nhật hoặc Unikey: Phím Enter/Space dùng để chọn từ trong IME không được kích hoạt bắn nhầm khi `isComposing === true`.
2. Người chơi gõ sai và bấm Space/Enter: Ô rung đỏ cảnh báo và tự động xóa sạch về rỗng, con trỏ vẫn nằm sẵn sàng trong ô để gõ từ mới.
3. Người chơi gõ đúng và bấm Space/Enter: Chữ rơi phát nổ, cộng điểm, ô tự động xóa về rỗng ngay lập tức.
4. Gõ khoảng trắng liên tiếp hoặc bấm Space khi ô rỗng: Bỏ qua an toàn, không kích hoạt tính lỗi (không trừ điểm/combo).
5. Live target highlighting: Khi đang gõ dở từng ký tự, chữ rơi tương ứng vẫn được làm sáng đèn Neon đỏ để người chơi nhận biết mục tiêu.

---

### Task 1: Quản lý IME và Tách biệt Live Preview vs Commit Bắn

**Files:**
- Modify: `frontend/pages/kanji-game/kanji-game.js`

**Interfaces:**
- `isComposing`: cờ boolean theo dõi trạng thái bộ gõ IME.
- `input` event: chỉ gọi `updateTargetKanji()` và cập nhật `liveTypingEcho`.
- `keydown` event: bắt phím `Enter` và `' '` (Space) khi `!isComposing` để commit.

- [ ] **Step 1: Khai báo biến cờ `isComposing` và gắn sự kiện IME**
  ```javascript
  let isComposing = false;
  inputField?.addEventListener('compositionstart', () => {
    isComposing = true;
  });
  inputField?.addEventListener('compositionend', () => {
    isComposing = false;
    updateTargetKanji();
  });
  ```

- [ ] **Step 2: Cải tiến `input` listener để chỉ cập nhật live preview**
  Xóa bỏ việc tự động gọi bắn tức thì trong `input` listener. Chỉ cập nhật `updateTargetKanji()` và `liveTypingEcho`.

- [ ] **Step 3: Cải tiến `keydown` listener cho phím `Space` và `Enter`**
  ```javascript
  inputField?.addEventListener('keydown', (e) => {
    if ((e.key === 'Enter' || e.key === ' ') && !isComposing) {
      e.preventDefault();
      const val = (inputField.value || '').trim();
      if (val.length > 0) {
        checkTypingMatch(true);
      }
    }
  });
  ```

- [ ] **Step 4: Kiểm tra cú pháp bằng `node --check`**
  Run: `node --check frontend/pages/kanji-game/kanji-game.js`
  Expected: Clean exit code 0.

- [ ] **Step 5: Commit**
  ```bash
  git add frontend/pages/kanji-game/kanji-game.js
  git commit -m "feat(kanji-game): add IME composition safety and space/enter commit listener"
  ```

---

### Task 2: Cải Tiến `checkTypingMatch` Với Auto-Clear on Miss

**Files:**
- Modify: `frontend/pages/kanji-game/kanji-game.js`

**Interfaces:**
- `checkTypingMatch(isCommit)`:
  - Nếu `!isCommit`: chỉ thực hiện preview target.
  - Nếu `isCommit === true`:
    - Tìm chữ khớp thấp nhất (`lowestMatchIndex`).
    - Nếu khớp: nổ chữ, cộng combo/điểm, xóa ô gõ (`inputField.value = ''`).
    - Nếu không khớp: rung đỏ `shake-error`, ghi nhận lỗi, tự động xóa sạch ô gõ (`inputField.value = ''`) và reset target.

- [ ] **Step 1: Cập nhật hàm `checkTypingMatch`**
  - Đảm bảo khi `isCommit === false`: chỉ tìm target để viền sáng, không nổ chữ và không xóa ô.
  - Khi `isCommit === true` và tìm thấy chữ khớp: nổ chữ và xóa trắng ô nhập ngay lập tức.
  - Khi `isCommit === true` và không tìm thấy chữ khớp: rung đỏ, phát âm thanh lỗi, tự động xóa trắng ô nhập ngay lập tức để người chơi gõ tiếp từ khác.

- [ ] **Step 2: Đảm bảo reset trạng thái `isTargeted` an toàn**
  Sau khi bắn trúng hoặc trượt, đặt lại `isTargeted = false` cho các chữ còn lại và reset `liveTypingEcho`.

- [ ] **Step 3: Kiểm tra cú pháp bằng `node --check`**
  Run: `node --check frontend/pages/kanji-game/kanji-game.js`
  Expected: Clean exit code 0.

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/pages/kanji-game/kanji-game.js
  git commit -m "feat(kanji-game): implement auto-clear on hit and miss in checkTypingMatch"
  ```

---

### Task 3: Cập Nhật Placeholder Giao Diện & Viết Unit Test Kiểm Thử

**Files:**
- Modify: `frontend/pages/kanji-game/index.html`
- Create: `frontend/tests/kanji-game-typing.test.mjs`

- [ ] **Step 1: Cập nhật placeholder trong `frontend/pages/kanji-game/index.html`**
  Đổi placeholder của `#kanjiGameInput` thành:
  `placeholder="Nhập cách đọc rồi nhấn [Space] hoặc [Enter] để bắn 🚀"`

- [ ] **Step 2: Viết test case trong `frontend/tests/kanji-game-typing.test.mjs`**
  Kiểm tra:
  - Hàm `romajiToHiragana` và `normalizeInput` chuẩn hóa đúng các chuỗi gõ từ Space/Enter.
  - Phím space trailing được làm sạch an toàn.
  - Logic phân biệt khi commit vs không commit.

- [ ] **Step 3: Chạy test suite xác nhận thành công**
  Run: `node --test frontend/tests/kanji-game-typing.test.mjs`
  Run: `node --test frontend/tests/flashcard-helpers.test.mjs`
  Expected: PASS 100%.

- [ ] **Step 4: Commit**
  ```bash
  git add frontend/pages/kanji-game/index.html frontend/tests/kanji-game-typing.test.mjs
  git commit -m "feat(kanji-game): update input placeholder and add typing test suite"
  ```
