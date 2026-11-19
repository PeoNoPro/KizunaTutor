# KỊCH BẢN KIỂM THỬ TỰ ĐỘNG GIAO DIỆN (E2E PLAYWRIGHT RUNBOOK)
**Dự án KizunaTutor – Module Tài Khoản & Hỗ Trợ Học Tập**

> Tài liệu này chuẩn hóa quy trình "Bấm thử app" (User Journey & E2E Testing) dành cho Playwright và kiểm thử thủ công, đồng bộ với các quy tắc bất biến trong `.agents/AGENTS.md` và `frontend/FRONTEND_RULES.md`.

---

## 1. Chuẩn bị môi trường trước khi bấm thử

1. **Khởi chạy Frontend Dev Server:**
   ```powershell
   # Chạy bằng PowerShell tích hợp sẵn (không phụ thuộc Node)
   cd frontend
   .\start-server.ps1 -Port 3000
   ```
   *Frontend sẵn sàng tại:* `http://localhost:3000`

2. **Khởi chạy Backend (Spring Boot API):**
   ```powershell
   ./mvnw.cmd spring-boot:run
   ```
   *API sẵn sàng tại:* `http://localhost:8085`

---

## 2. Kịch bản E2E 1: Module Tài khoản & Phân quyền (Authentication)

### Bước 1.1: Đăng nhập & Điều hướng vai trò
- **URL mục tiêu:** `http://localhost:3000/login`
- **Hành động:**
  1. Nhập `email`: `learner@kizuna.vn`, `password`: `Password123@`.
  2. Bấm nút `Đăng nhập`.
- **Kiểm tra kỳ vọng (Assertion):**
  - Chuyển hướng chính xác đến trang chủ học viên: `/pages/home/index.html`.
  - Cookie `access_token` được gán vào trình duyệt (HttpOnly, Path `/`).
  - Header hiển thị tên và avatar của học viên.

### Bước 1.2: Đăng ký tài khoản mới & Xác thực OTP
- **URL mục tiêu:** `http://localhost:3000/register`
- **Hành động:**
  1. Điền Họ tên, Email mới, Mật khẩu, chọn vai trò `Học viên`, mục tiêu `N3`.
  2. Bấm `Tiếp tục`.
  3. Màn hình chuyển sang bước nhập OTP (6 ô nhập số).
  4. Điền mã OTP 6 số hợp lệ.
- **Kiểm tra kỳ vọng (Assertion):**
  - Tự động đăng nhập và đưa người dùng về trang chủ.

---

## 3. Kịch bản E2E 2: Module Hỗ trợ học tập (Learning Support)

### Bước 2.1: Truy cập Hub Hỗ trợ học tập
- **URL mục tiêu:** `http://localhost:3000/learning-support`
- **Kiểm tra kỳ vọng (Assertion):**
  - Hiển thị đầy đủ các thẻ chức năng: Tra cứu Kanji, Tra viết tay, Bộ thẻ Flashcard, Game Kanji, Luyện thư pháp Shodo AI, AI Đọc hiểu.

### Bước 2.2: Tra cứu Kanji & Nét vẽ động KanjiVG
- **URL mục tiêu:** `http://localhost:3000/kanji-dictionary`
- **Hành động:**
  1. Nhập vào ô tìm kiếm: chữ `日` hoặc `学`.
  2. Bấm `Tra cứu` (hoặc Enter).
- **Kiểm tra kỳ vọng (Assertion):**
  - Khung thông tin hiển thị đúng: Âm Hán Việt (`NHẬT`), Onyomi (`ニチ, ジツ`), Kunyomi (`ひ, -び`).
  - Trình phát thứ tự nét vẽ `KanjiStrokePlayer` nạp thành công SVG từ `/kanjivg/` và phát hoạt ảnh mượt mà.

### Bước 2.3: Học Bộ thẻ Flashcards 3D & Voicevox
- **URL mục tiêu:** `http://localhost:3000/flashcards`
- **Hành động:**
  1. Chọn một bộ thẻ có sẵn (ví dụ: Bộ thẻ mẫu N5).
  2. Bấm `Học thẻ` $\rightarrow$ Chuyển sang `/flashcards-study`.
  3. Bấm vào thẻ để lật 3D (xem mặt sau: nghĩa, ví dụ).
  4. Bấm biểu tượng loa $\rightarrow$ Kích hoạt phát âm Voicevox.
- **Kiểm tra kỳ vọng (Assertion):**
  - Hiệu ứng 3D lật mượt mà không lỗi console.
  - Âm thanh phát hoặc hiển thị trạng thái phát âm thân thiện.

### Bước 2.4: Trò chơi Kanji Rain (Mưa Hán Tự)
- **URL mục tiêu:** `http://localhost:3000/kanji-game`
- **Hành động:**
  1. Chọn cấp độ `N5`, bấm `Bắt đầu chơi`.
  2. Ký tự Kanji rơi từ trên xuống; gõ đúng Romaji/Kana vào ô input.
  3. Kết thúc phiên chơi (hoặc hết thời gian) $\rightarrow$ Xem bảng điểm tổng kết và danh hiệu Title Rank.
- **Kiểm tra kỳ vọng (Assertion):**
  - Trận đấu diễn ra trơn tru, đếm đúng số chữ đã gõ và độ chính xác (Accuracy %).

### Bước 2.5: Luyện viết thư pháp Shodo cùng AI
- **URL mục tiêu:** `http://localhost:3000/shodo-practice`
- **Hành động:**
  1. Chọn chữ mẫu `一` hoặc `人`.
  2. Dùng chuột/cảm ứng vẽ nét theo khung ô Mễ trên Canvas.
  3. Bấm `Chấm điểm cùng AI`.
- **Kiểm tra kỳ vọng (Assertion):**
  - Vector Kinematics đo đạc số nét, hướng nét và độ tương đồng Cosine.
  - Phản hồi nhận xét sư phạm hiển thị chuẩn tiếng Việt, không bị tràn màn hình.

### Bước 2.6: Trợ lý Đọc hiểu & Phân tích Ngữ pháp (AI Reading)
- **URL mục tiêu:** `http://localhost:3000/ai-reading`
- **Hành động:**
  1. Dán văn bản tiếng Nhật: `日本語が上手になるように、毎朝勉強しています。`.
  2. Bấm `Phân tích văn bản`.
- **Kiểm tra kỳ vọng (Assertion):**
  - Đoạn văn hiển thị Furigana Ruby phía trên từng chữ Hán.
  - Danh sách cấu trúc ngữ pháp (ví dụ: `〜ように`) và từ vựng trọng tâm hiển thị ở cột bên phải.

---

## 4. Mẫu kiểm thử Playwright tự động tương ứng

```javascript
// frontend/tests/e2e/learning-support.spec.js
import { test, expect } from '@playwright/test';

test.describe('KizunaTutor E2E - Learning Support & Auth', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000/login');
    // Đăng nhập giả lập hoặc đăng nhập thật
    await page.fill('input[type="email"]', 'learner@kizuna.vn');
    await page.fill('input[type="password"]', 'Password123@');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/.*home.*/);
  });

  test('Kiểm tra Tra từ điển Kanji & Nét vẽ KanjiVG', async ({ page }) => {
    await page.goto('http://localhost:3000/kanji-dictionary');
    await page.fill('#searchInput', '日');
    await page.click('#searchBtn');
    await expect(page.locator('#hanVietText')).toContainText('NHẬT');
    await expect(page.locator('#strokeSvgContainer svg')).toBeVisible();
  });

  test('Kiểm tra Trò chơi Kanji Rain', async ({ page }) => {
    await page.goto('http://localhost:3000/kanji-game');
    await expect(page.locator('#btnStartFromLobby')).toBeVisible();
  });
});
```
