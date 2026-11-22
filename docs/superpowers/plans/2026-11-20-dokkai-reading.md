# Luyện Đọc Hiểu Dokkai AI & Tối Ưu Menu Sidebar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển hóa trang `/reading` thành phân hệ Luyện Đọc hiểu Dokkai AI chuẩn sư phạm JLPT với tính năng sinh câu hỏi trắc nghiệm, AI phân tích bẫy, highlight dẫn chứng song song bài đọc sạch, đồng thời đổi tên sidebar thành "Đọc hiểu".

**Architecture:** Bổ sung DTO và mở rộng prompt AI trên Spring Boot (`ReadingAssistantServiceImpl`) để sinh cấu trúc câu hỏi đọc hiểu kèm dẫn chứng và phân tích bẫy. Frontend xây dựng giao diện Split-view 2 cột trên Desktop (bài đọc sạch bên trái, câu hỏi Dokkai và phân tích bên phải) với cơ chế mở tự do các tab bóc tách từ vựng/ngữ pháp và cập nhật `learner-layout.js`.

**Tech Stack:** Java 21, Spring Boot, Jackson, Tailwind CSS, Vanilla JS ES Modules, Material Symbols.

**Spec:** `docs/superpowers/specs/2026-11-20-dokkai-reading-design.md`

## Global Constraints

- Backend: Java Spring Boot trong `com.kizuna.features.learning_support`.
- Cấu trúc phản hồi JSON tương thích ngược, không phá vỡ các trường hiện tại (`rubyHtml`, `vocabularies`, `detectedGrammars`, `sentenceTranslations`).
- Giao diện: Thiết kế theo hệ thống thiết kế Shodo/Kizuna (Tailwind CSS, bảng màu Slate/Rose `#9F1239` / `#831843`, viền `#E2E8F0` / `#0F172A`).
- Sidebar: Đổi tên hiển thị chính xác thành "Đọc hiểu".

## Review Focus

1. **Khả năng hiển thị khi AI không trả về câu hỏi (Fallback/Empty)**: Khi `comprehensionQuestions` rỗng (ví dụ offline bài đọc tự do), Tab 1 phải hiển thị thông báo rỗng thân thiện kèm nút bấm chuyển sang Tab 2, không gây lỗi giao diện.
2. **Khớp dẫn chứng `evidenceQuote` trong bài đọc sạch**: Xử lý trường hợp chuỗi dẫn chứng có dấu câu hoặc khoảng trắng lệch nhẹ; dùng thuật toán tìm kiếm chuỗi con an toàn để highlight vàng nhấp nháy.
3. **Bật/Tắt Furigana trên bài đọc sạch của Tab 1**: Hoạt động độc lập và đồng bộ với trạng thái `isFuriganaHidden`.
4. **Không xung đột với các tab tra cứu hiện có**: Click vào Tab 2, 3, 4, 5 vẫn tra cứu được từ vựng, ngữ pháp, dịch câu bình thường ngay cả trước hoặc sau khi làm bài.
5. **Hiển thị responsive trên thiết bị di động**: Trên màn hình nhỏ (`< 1024px`), bài đọc sạch nằm phía trên, danh sách câu hỏi trắc nghiệm cuộn mượt mà ở phía dưới.

---

### Task 1: Backend DTO & Response Update

**Files:**
- Create: `backend/src/main/java/com/kizuna/features/learning_support/dto/ReadingComprehensionQuestionDto.java`
- Modify: `backend/src/main/java/com/kizuna/features/learning_support/dto/ReadingAnalysisResponse.java`

**Interfaces:**
- Produces: `ReadingComprehensionQuestionDto` chứa `id`, `questionType`, `question`, `options`, `correctOptionIndex`, `evidenceQuote`, `explanation`.
- Produces: `ReadingAnalysisResponse.getComprehensionQuestions()` trả về `List<ReadingComprehensionQuestionDto>`.

- [ ] **Step 1: Tạo class DTO `ReadingComprehensionQuestionDto.java`**
  Khai báo đầy đủ annotations Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` và các thuộc tính theo Spec.

- [ ] **Step 2: Cập nhật `ReadingAnalysisResponse.java`**
  Thêm trường `private List<ReadingComprehensionQuestionDto> comprehensionQuestions;` vào `ReadingAnalysisResponse`.

- [ ] **Step 3: Biên dịch kiểm tra tính tương thích**
  Chạy lệnh `mvn test-compile` hoặc kiểm tra build backend để xác nhận không có lỗi cú pháp.

- [ ] **Step 4: Commit**
  `git commit -m "feat(backend): add ReadingComprehensionQuestionDto and update ReadingAnalysisResponse"`

---

### Task 2: Cập nhật Prompt AI & Bộ Fallback Engine

**Files:**
- Modify: `backend/src/main/java/com/kizuna/features/learning_support/service/impl/ReadingAssistantServiceImpl.java`

**Interfaces:**
- Consumes: `ReadingComprehensionQuestionDto`, `ReadingAnalysisResponse`.
- Produces: `ReadingAssistantService.analyzeReading(request)` trả về `ReadingAnalysisResponse` chứa danh sách câu hỏi Dokkai.

- [ ] **Step 1: Cập nhật `buildPrompt` trong `ReadingAssistantServiceImpl.java`**
  Bổ sung chỉ thị cho AI sinh mảng `comprehensionQuestions`:
  - 1-3 câu hỏi tùy theo độ dài văn bản.
  - Các dạng câu hỏi: Ý chính (`MAIN_IDEA`), Lý do (`REASON`), Chi tiết (`DETAIL`), Từ chỉ định (`REFERENCE`).
  - Mỗi câu hỏi gồm 4 lựa chọn, chỉ số đáp án đúng (0-3), câu trích dẫn dẫn chứng 100% nguyên văn `evidenceQuote`, và giải thích phân tích bẫy `explanation`.

- [ ] **Step 2: Cập nhật `generateFallbackReadingAnalysis`**
  Thêm dữ liệu câu hỏi mẫu đọc hiểu chất lượng cao cho 5 bài preset có sẵn:
  - `tokyo`: Câu hỏi về địa điểm ăn trưa hoặc phương tiện đi lại.
  - `n5`: Câu hỏi về thói quen buổi sáng hoặc việc nhìn thấy núi Phú Sĩ.
  - `n4`: Câu hỏi về chuyến đi Kyoto và trải nghiệm mặc kimono.
  - `n3`: Câu hỏi về bí quyết học tiếng Nhật qua tin tức NHK.
  - `tsang`: Câu hỏi về ý chính của tình bạn 10 năm giữa hai nhân vật.

- [ ] **Step 3: Chuẩn hóa dữ liệu trả về khi gọi AI**
  Đảm bảo gán `id` tự động (`q_0`, `q_1`, ...) nếu AI để trống, và kiểm tra biên cho `correctOptionIndex` (nằm trong khoảng 0 đến options.size() - 1).

- [ ] **Step 4: Commit**
  `git commit -m "feat(backend): update AI prompt and fallback engine for dokkai questions"`

---

### Task 3: Đổi tên Menu Sidebar thành "Đọc hiểu"

**Files:**
- Modify: `frontend/components/learner-layout/learner-layout.js`

**Interfaces:**
- Menu item `ai-reading` hiển thị nhãn "Đọc hiểu" trên sidebar.

- [ ] **Step 1: Cập nhật tiêu đề menu trong `learner-layout.js`**
  Đổi `title: 'Đọc hiểu Dokkai AI'` thành `title: 'Đọc hiểu'` tại mục khai báo menu `ai-reading`.
  Đổi `'ai-reading'` và `'reading'` trong map `DEFAULT_PAGE_TITLES` thành `title: 'Đọc hiểu'`.

- [ ] **Step 2: Kiểm tra trang `ai-reading/index.html`**
  Đảm bảo hàm `initLearnerShell` gọi:
  ```javascript
  pageTitle: 'Luyện Đọc Hiểu AI 📖',
  badge: 'Trợ lý đọc hiểu Dokkai'
  ```

- [ ] **Step 3: Commit**
  `git commit -m "feat(frontend): rename sidebar reading menu item to Đọc hiểu"`

---

### Task 4: Cập nhật Giao diện HTML & CSS (Tab 1 Split-View)

**Files:**
- Modify: `frontend/pages/ai-reading/index.html`

**Interfaces:**
- Giao diện có Tab 1 mới: `tabPanelDokkaiChallenge`.
- Cột trái: Khung bài đọc sạch `#cleanReadingPassageBody` với nút toggle Furigana `#btnToggleCleanFurigana`.
- Cột phải: `#dokkaiQuestionsContainer`, nút nộp bài `#btnSubmitDokkai`, hộp điểm số `#dokkaiScoreSummary`.

- [ ] **Step 1: Cập nhật thanh chuyển Tab `#readingTabGroup`**
  Đổi thứ tự và nhãn các tab:
  1. Tab 1: `data-tab="challenge"`: "🎯 Thử thách Đọc hiểu (<span id="countDokkaiQuestions">0</span>)" (Mặc định).
  2. Tab 2: `data-tab="reader"`: "📖 Bài đọc tương tác".
  3. Tab 3: `data-tab="vocab"`: "🗂️ Từ vựng & Hán tự (<span id="countVocabs">0</span>)".
  4. Tab 4: `data-tab="grammar"`: "⛩️ Cấu trúc Ngữ pháp (<span id="countGrammars">0</span>)".
  5. Tab 5: `data-tab="translation"`: "🌐 Bản dịch song ngữ".

- [ ] **Step 2: Thêm Tab Panel `tabPanelDokkaiChallenge`**
  Xây dựng layout Split-view 2 cột (`grid grid-cols-1 lg:grid-cols-12 gap-4`):
  - Cột 1 (lg:col-span-6): Card chứa bài đọc sạch, header có badge cấp độ và nút Bật/Tắt Furigana. Khung chữ rộng rãi `#cleanReadingPassageBody`.
  - Cột 2 (lg:col-span-6): Card chứa danh sách câu hỏi trắc nghiệm `#dokkaiQuestionsContainer`, nút nộp bài `#btnSubmitDokkai`, và hộp kết quả `#dokkaiScoreSummary` (ẩn ban đầu).

- [ ] **Step 3: Thêm CSS highlight dẫn chứng (Evidence citation highlight)**
  Thêm lớp CSS `.evidence-highlight` với animation chớp sáng nền vàng/viền cam nổi bật khi học viên bấm nút xem dẫn chứng.

- [ ] **Step 4: Commit**
  `git commit -m "feat(frontend): add split-view dokkai challenge tab and evidence highlight styles"`

---

### Task 5: Triển khai Logic Tương Tác & Chấm Điểm Đọc Hiểu

**Files:**
- Modify: `frontend/pages/ai-reading/ai-reading.js`

**Interfaces:**
- Consumes: `data.comprehensionQuestions`, `data.rubyHtml`, `data.originalText`.
- Produces: Render bài đọc sạch, tương tác chọn đáp án, tính điểm đúng/sai, hiển thị phân tích bẫy, highlight dẫn chứng.

- [ ] **Step 1: Quản lý State cho phần Đọc hiểu**
  Khai báo biến state:
  `let dokkaiAnswers = {};` (lưu đáp án học viên chọn cho từng câu `q.id`)
  `let dokkaiSubmitted = false;`

- [ ] **Step 2: Hàm `renderDokkaiChallenge(questions, rubyHtml, originalText)`**
  - Render nội dung bài đọc sạch vào `#cleanReadingPassageBody` sử dụng `rubyHtml`.
  - Nếu `questions` rỗng: Hiển thị Empty State với nút *"Chuyển sang xem bóc tách từ vựng & ngữ pháp"*.
  - Nếu có `questions`: Cập nhật badge số lượng câu hỏi và render các card câu hỏi trắc nghiệm (mỗi card gồm: Số câu, loại câu hỏi badge, nội dung câu hỏi, 4 lựa chọn A, B, C, D dạng custom radio cards).

- [ ] **Step 3: Xử lý sự kiện chọn đáp án và Nộp bài `#btnSubmitDokkai`**
  - Khi học viên chọn radio, cập nhật `dokkaiAnswers[qId] = optionIndex`.
  - Khi click nộp bài: Kiểm tra xem đã chọn đủ câu hỏi chưa. Nếu chưa chọn đủ, hiển thị toast nhắc nhở nhẹ nhàng nhưng vẫn cho phép nộp nếu muốn.
  - Tính điểm số: Đếm số câu đúng (`dokkaiAnswers[qId] === q.correctOptionIndex`).
  - Hiển thị card điểm số `#dokkaiScoreSummary` với lời nhận xét sư phạm.
  - Đổi màu các phương án lựa chọn: Viền xanh lá + icon check cho đáp án đúng; Viền đỏ + icon close cho đáp án sai mà học viên chọn.
  - Hiển thị khối `explanation` bên dưới câu hỏi.
  - Render nút *"🔍 Xem dẫn chứng trong bài"*.

- [ ] **Step 4: Xử lý sự kiện "Xem dẫn chứng trong bài"**
  - Khi click nút xem dẫn chứng: Lấy `evidenceQuote`.
  - Tìm kiếm đoạn text trong `#cleanReadingPassageBody`.
  - Bọc tạm thời bằng `<mark class="evidence-highlight">` và cuộn mượt (`scrollIntoView`) đến vị trí câu dẫn chứng ở cột bên trái.
  - Sau 3 giây, tự động làm dịu hiệu ứng chớp sáng.

- [ ] **Step 5: Xử lý chuyển tab và đồng bộ Furigana**
  - Đảm bảo nút Bật/Tắt Furigana ở cột bài đọc sạch hoạt động mượt mà.
  - Cho phép học viên chuyển sang các Tab 2, 3, 4, 5 bất cứ lúc nào.

- [ ] **Step 6: Commit**
  `git commit -m "feat(frontend): implement dokkai interactive answering, grading, and evidence citation"`

---

### Task 6: Kiểm Thử Toàn Diện & Nghiệm Thu (Acceptance Verification)

**Files:**
- Toàn bộ các files đã cập nhật.

- [ ] **Step 1: Kiểm tra Sidebar**
  Mở giao diện kiểm tra menu item hiển thị đúng "Đọc hiểu", URL điều hướng `/reading`.

- [ ] **Step 2: Kiểm thử với các bài đọc mẫu**
  Thử lần lượt 5 bài mẫu (`Tokyo`, `N5`, `N4`, `N3`, `Tsang`):
  - Bấm phân tích -> Hệ thống tự động mở Tab 1 "Thử thách Đọc hiểu".
  - Kiểm tra giao diện 2 cột trên Desktop (bài đọc sạch bên trái, câu hỏi bên phải).
  - Thử chọn đáp án, bấm "Nộp bài & Chấm điểm AI".
  - Xác nhận kết quả hiển thị đúng/sai rõ ràng, có lời giải thích phân tích bẫy.
  - Bấm "Xem dẫn chứng trong bài" -> Kiểm tra câu văn bên cột trái được highlight chuẩn xác.

- [ ] **Step 3: Kiểm thử chuyển đổi sang các tab khác**
  Chuyển sang Tab 2 (Bài đọc tương tác tra từ), Tab 3 (Từ vựng), Tab 4 (Ngữ pháp), Tab 5 (Bản dịch) -> Xác nhận hoạt động trơn tru 100%.

- [ ] **Step 4: Commit hoàn tất**
  `git commit -m "chore: verify dokkai reading workflow and acceptance criteria"`
