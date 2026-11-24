# Pure Dokkai Studio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển đổi phân hệ Đọc hiểu (`/reading`) thành Pure Dokkai Studio tinh gọn: loại bỏ bóc tách từ vựng & ngữ pháp; tập trung 100% vào việc sinh câu hỏi đọc hiểu Dokkai trắc nghiệm 4 lựa chọn, chấm điểm, dẫn chứng và dịch nghĩa theo bố cục trên - dưới (văn bản ở trên, câu hỏi ở dưới).

**Architecture:** 
- Backend: Cắt giảm prompt AI trong `ReadingAssistantServiceImpl.java` để loại bỏ trích xuất từ vựng và ngữ pháp, tập trung 100% vào Dokkai questions, ruby furigana và bản dịch câu. Giảm `max_tokens` từ 4000 xuống 2000 giúp phản hồi nhanh trong 2-3s và tiết kiệm 70% token.
- Frontend: Xóa bỏ hệ thống 5 tab phức tạp trong `index.html` và `ai-reading.js`. Thiết lập giao diện duy nhất theo trình tự trên - dưới: Phần trên là Khung bài đọc tiếng Nhật (Furigana, Nghe audio, Dịch tham khảo), Phần dưới là Danh sách câu hỏi trắc nghiệm Dokkai kèm chấm điểm AI và nút "Xem dẫn chứng trong bài" cuộn mượt lên trên.

**Tech Stack:** Java 17, Spring Boot 3, TailwindCSS, Vanilla JavaScript ES6+, Jackson, Groq API (Qwen 2.5 / Llama 3.3).

**Spec:** [docs/superpowers/specs/2026-11-20-dokkai-reading-design.md](file:///c:/Users/Vuh26/Desktop/hm/KizunaTutor/docs/superpowers/specs/2026-11-20-dokkai-reading-design.md) & Quyết định người dùng 2026-11-24.

## Global Constraints

- Không làm ảnh hưởng đến các API khác của hệ thống.
- Giữ nguyên cấu trúc dữ liệu của `comprehensionQuestions` (`id`, `questionType`, `question`, `questionVi`, `options`, `optionsVi`, `correctOptionIndex`, `evidenceQuote`, `explanation`).
- Giữ tương thích ngược với fallback nếu AI ngoại tuyến (5 bài mẫu preset vẫn hiển thị trọn vẹn câu hỏi trắc nghiệm).
- Đảm bảo toàn bộ unit test hiện có pass 100%.

## Review Focus

1. Prompt AI tinh gọn trả về đúng JSON thuần và được Jackson parse mượt mà mà không có ngoại lệ.
2. Nút "Xem dẫn chứng trong bài" cuộn mượt (`scrollIntoView`) lên câu văn tương ứng trong khung bài đọc ở phía trên và chớp sáng màu vàng.
3. Chức năng nộp bài & chấm điểm AI so sánh chuẩn xác và hiển thị đầy đủ thẻ điểm + phân tích đúng/sai.
4. Bật/Tắt Furigana và Bật/Tắt Dịch tiếng Việt tham khảo hoạt động trơn tru.
5. Không còn code thừa (Dead Code / Unused Event Listeners) của các tab cũ.

---

### Task 1: Tinh gọn Backend AI Prompt & Logic trong `ReadingAssistantServiceImpl.java`

**Files:**
- Modify: `backend/src/main/java/com/kizuna/features/learning_support/service/impl/ReadingAssistantServiceImpl.java`
- Test: `backend/src/test/java/com/kizuna/features/learning_support/ReadingAssistantServiceTest.java`

- [ ] **Step 1: Cập nhật hàm `buildPrompt`**
  Loại bỏ yêu cầu AI trích xuất `vocabularies` và `detectedGrammars`. Tập trung yêu cầu vào:
  - `originalText`, `rubyHtml`, `overallSummary`, `estimatedLevel`
  - `comprehensionQuestions`: 1 - 3 câu hỏi Dokkai 4 lựa chọn có `questionType`, `question`, `questionVi`, `options`, `optionsVi`, `correctOptionIndex`, `evidenceQuote`, `explanation`.
  - `sentenceTranslations`: Dịch câu tiếng Việt.

- [ ] **Step 2: Đặt `max_tokens = 2000` trong `callOpenAiCompatible`**
  Giảm trần token để tối ưu hóa thời gian sinh và tránh dính Rate Limit TPM của nhà cung cấp.

- [ ] **Step 3: Cập nhật `ReadingAssistantServiceTest`**
  Chạy `.\mvnw.cmd test -Dtest=ReadingAssistantServiceTest` để đảm bảo logic phân tích và bộ đệm cache hoạt động chính xác 100%.

---

### Task 2: Tinh gọn Giao diện HTML trong `frontend/pages/ai-reading/index.html`

**Files:**
- Modify: `frontend/pages/ai-reading/index.html`

- [ ] **Step 1: Loại bỏ thanh chuyển Tab (`readingTabGroup`)**
  Xóa bỏ thanh chứa 5 nút tab (`Thử thách Đọc hiểu`, `Bài đọc tương tác`, `Từ vựng`, `Ngữ pháp`, `Bản dịch`).

- [ ] **Step 2: Loại bỏ các khung nội dung tab cũ**
  Xóa bỏ `tabPanelReader`, `tabPanelVocab`, `tabPanelGrammar`, `tabPanelTranslation` và các modal Flashcard không dùng đến trên trang này.

- [ ] **Step 3: Chuẩn hóa bố cục Trên - Dưới cho màn hình Workspace**
  - **Phần Trên**: Khung đoạn văn đọc hiểu (`cleanReadingPassageBody`) to rõ, kèm nút Bật/Tắt Furigana, nút Nghe đọc VOICEVOX và nút Bật/Tắt Bản dịch tham khảo tiếng Việt.
  - **Phần Dưới**: Khung câu hỏi đọc hiểu (`dokkaiQuestionsContainer`), bảng tổng kết điểm (`dokkaiScoreSummary`), nút nộp bài và các nút xem dẫn chứng.

---

### Task 3: Tinh gọn & Chuẩn hóa Logic JavaScript trong `frontend/pages/ai-reading/ai-reading.js`

**Files:**
- Modify: `frontend/pages/ai-reading/ai-reading.js`

- [ ] **Step 1: Dọn dẹp các biến DOM và Event Listener của các tab cũ**
  Xóa bỏ logic click token bóc tách từ vựng/ngữ pháp (Watanoc tooltips), modal Flashcard deck, grid từ vựng và cards ngữ pháp.

- [ ] **Step 2: Thêm tính năng Cuộn mượt (Smooth Scroll) cho nút "Xem dẫn chứng trong bài"**
  Khi người dùng bấm "🔍 Xem dẫn chứng trong bài" ở bất kỳ câu hỏi nào:
  - Gọi `highlightEvidenceInCleanPassage(evidenceQuote)`.
  - Thực hiện `evidenceEl.scrollIntoView({ behavior: 'smooth', block: 'center' })` để tự động đưa tầm nhìn người dùng lên đúng vị trí câu văn ở phần trên.

- [ ] **Step 3: Thêm nút Xem/Ẩn bản dịch tiếng Việt tham khảo toàn bài**
  Cho phép học viên tự do xem bản dịch của bài đọc khi cần đối chiếu sau khi làm bài.

---

### Task 4: Kiểm thử Tích hợp & Nghiệm thu Toàn diện (E2E Verification)

**Files:**
- Test: Gọi trực tiếp API `/api/learner/reading/analyze` với văn bản kiểm thử của người dùng.
- Manual check: Mở giao diện `http://localhost:3000/reading` trên trình duyệt.

- [ ] **Step 1: Chạy toàn bộ test suite Maven**
  Kiểm tra `.\mvnw.cmd test -Dtest=ReadingAssistantServiceTest` pass 100%.

- [ ] **Step 2: Thử nghiệm phân tích bài đọc kiểm thử của người dùng**
  Văn bản: *"私の国には、美しい自然がたくさんあります。特に、山や海、川などの景色がとてもきれいです..."*
  - Kiểm tra thời gian sinh < 4s.
  - Kiểm tra sinh đủ câu hỏi trắc nghiệm A/B/C/D.
  - Kiểm tra nộp bài, chấm điểm, chớp sáng dẫn chứng và cuộn mượt lên trên.
