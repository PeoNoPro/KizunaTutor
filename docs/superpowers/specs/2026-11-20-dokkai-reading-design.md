# Design Document: Phân hệ Luyện Đọc Hiểu Dokkai AI & Tối Ưu Menu Sidebar

**Ngày tạo:** 2026-11-20  
**Tác giả:** Antigravity AI Assistant & KizunaTutor Core Team  
**Trạng thái:** Đã phê duyệt (Approved by User - 2026-11-20)  
**Tài liệu tham chiếu:** Góp ý chuyên môn từ Giảng viên hướng dẫn về tính năng `http://localhost:3000/reading`

---

## 1. Bối cảnh & Mục đích (Intent & The Why)

### 1.1. Vấn đề thực tế
Học viên và Giảng viên phản ánh tính năng tại URL `http://localhost:3000/reading` (tên hiển thị cũ: "Luyện Đọc Hiểu & Bóc Tách Ngữ Pháp AI") hiện tại chỉ là một công cụ phân tích từ vựng, ngữ pháp, furigana và dịch câu (Text Parser / Dictionary Lookup) giống như Mazii hoặc Watanoc. Nó hoàn toàn thiếu vắng bản chất cốt lõi của **Đọc hiểu (読解 - Dokkai)** theo chuẩn sư phạm tiếng Nhật và kỳ thi JLPT.

### 1.2. Mục tiêu cải tiến
1. **Chuyển hóa thành phân hệ Đọc hiểu Dokkai AI thực thụ:**
   * Cho phép học viên dán bất kỳ đoạn văn nào (hoặc chọn bài mẫu), AI sẽ tự động sinh bộ câu hỏi Đọc hiểu Dokkai chuẩn format JLPT (Ý chính 要旨, Lý do 理由, Từ chỉ định 指示語, Quan điểm tác giả 筆者の主張).
   * **Môi trường thi thử trực quan (Split-view Reader):** Học viên theo dõi bài đọc "sạch" (có bật/tắt Furigana, không có highlight từ điển gây lộ bài) và làm bài trắc nghiệm song song.
   * **AI Dokkai Sensei:** Chấm điểm tức thì, giải thích chi tiết tại sao đáp án đúng, phân tích bẫy (tại sao các đáp án còn lại sai) và highlight trực tiếp dẫn chứng (Evidence Citation) ngay trong bài đọc.
   * Giữ lại toàn bộ công cụ bóc tách chuyên sâu (Từ vựng, Ngữ pháp, Dịch từng câu, Phát âm VOICEVOX) làm các tab củng cố kiến thức, học viên có thể tự do tra cứu bất kỳ lúc nào.
2. **Chuẩn hóa danh mục Sidebar:**
   * Đổi tên trên Sidebar học viên thành **"Đọc hiểu"** (ngắn gọn, chuẩn xác, không rườm rà).

---

## 2. Mục tiêu, Phi mục tiêu & Ranh giới (Goals, Non-Goals & Anti-Goals)

### 2.1. Goals
* **G1:** Mở rộng backend `ReadingAssistantServiceImpl.java` để Gemini Flash tự động sinh mảng `comprehensionQuestions` (1 - 3 câu hỏi trắc nghiệm 4 lựa chọn tùy độ dài bài đọc).
* **G2:** Mỗi câu hỏi bao gồm: Cấp độ, Loại câu hỏi, Nội dung câu hỏi (kèm dịch nếu sơ cấp N5/N4), 4 phương án lựa chọn, Đáp án đúng (`correctOptionIndex`), Câu dẫn chứng (`evidenceQuote`), và Lời giải thích phân tích bẫy (`explanation`).
* **G3:** Xây dựng giao diện làm bài Đọc hiểu tại Tab 1 ("🎯 Thử thách Đọc hiểu Dokkai"):
  * Thiết kế **Split-View 2 cột trên Desktop**: Cột trái ghim bài đọc sạch (Sticky Passage Reader) với công tắc Bật/Tắt Furigana; cột phải là danh sách câu hỏi trắc nghiệm A, B, C, D. Trên Mobile: Hiển thị bài đọc ở trên, câu hỏi ở dưới.
  * Nút "Nộp bài & Chấm điểm AI".
  * Bảng kết quả sư phạm: Hiển thị đúng/sai, số câu đạt được, phân tích cặn kẽ từng câu, nút "🔍 Xem dẫn chứng trong bài" (tự động chớp sáng màu vàng câu văn chứng minh trong khung bài đọc sạch).
* **G4:** Điều hướng mở tự do linh hoạt (Phương án A): Mặc định mở Tab 1 sau khi phân tích để khuyến khích học viên làm bài trước, đồng thời cho phép tự do click chuyển sang các tab Từ vựng, Ngữ pháp, Bản dịch bất cứ lúc nào.
* **G5:** Xử lý Fallback/Ngoại tuyến: Nếu AI mất kết nối hoặc bài đọc tự do không sinh được câu hỏi, hiển thị thẻ thông báo thân thiện tại Tab 1 kèm nút chuyển sang Tab 2 (Bóc tách).
* **G6:** Cập nhật `learner-layout.js` đổi tên mục menu sidebar thành "Đọc hiểu".

### 2.2. Non-Goals
* Không biến phân hệ này thành bài thi JLPT toàn diện tính điểm vào hồ sơ thi thử chung (chức năng đó đã thuộc về module `practice`). Trang này là công cụ luyện tập linh hoạt cho bất kỳ bài đọc nào mà học viên muốn tìm hiểu.
* Không thay đổi luồng hoạt động của VOICEVOX hoặc việc thêm Flashcard vào bộ thẻ.

### 2.3. Anti-Goals
* Không loại bỏ các tính năng bóc tách từ vựng, ngữ pháp hiện có.
* Không ép buộc làm bài cứng nhắc (không khóa cưỡng bức): Tôn trọng quyền tự do tra cứu của học viên.

---

## 3. Các quyết định kiến trúc đã thống nhất (Decisions Made)

1. **Bố cục Tab 1 (Thử thách Đọc hiểu):**
   * Sử dụng bố cục 2 cột responsive:
     * Desktop (`lg` trở lên): Cột trái (50%) là Khung bài đọc tiếng Nhật chuẩn mực (sạch sẽ, chữ to rõ, line-height thoáng đãng, hỗ trợ thẻ `<ruby>` Furigana có nút bật/tắt). Cột phải (50%) là các card câu hỏi trắc nghiệm và khối chấm điểm AI.
     * Mobile: Xếp chồng (bài đọc ở trên, câu hỏi ở dưới).
2. **Cơ chế chuyển Tab:**
   * **Phương án A (Mở tự do):** Tab 1 là tab active mặc định khi có kết quả. Các tab 2 (Bài đọc tương tác), 3 (Từ vựng), 4 (Ngữ pháp), 5 (Bản dịch) luôn mở để học viên tự do tra cứu.
3. **Cơ chế Xem dẫn chứng (Evidence Citation):**
   * Khi học viên bấm nút *"🔍 Xem dẫn chứng trong bài"* ở câu hỏi bất kỳ sau khi chấm điểm, câu văn tương ứng trong khung bài đọc sạch ở cột bên trái sẽ được highlight nền vàng nhấp nháy (`animate-pulse` / viền nổi bật), giúp học viên đối chiếu ngay lập tức mà không phải chuyển đổi tab.
4. **Xử lý Offline / Fallback:**
   * Cho 5 bài đọc mẫu (N5, N4, N3, Tokyo, Tsang): Bộ `generateFallbackReadingAnalysis` sinh sẵn các câu hỏi Dokkai mẫu chuẩn xác.
   * Với bài đọc tự do khi không có kết nối AI: Hiển thị card thông báo rỗng kèm nút *"Xem phân tích từ vựng & ngữ pháp ngay"* dẫn sang Tab 2.
5. **Cập nhật Menu Sidebar:**
   * Đổi `title` của item `ai-reading` từ `'Đọc hiểu Dokkai AI'` thành `'Đọc hiểu'`.

---

## 4. Chi tiết Thiết kế & Đặc tả Kỹ thuật (Technical Specification)

### 4.1. Backend (Java Spring Boot)

#### 4.1.1. DTO mới: `ReadingComprehensionQuestionDto.java`
```java
package com.kizuna.features.learning_support.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingComprehensionQuestionDto {
    private String id;                 // ví dụ: "q_0", "q_1"
    private String questionType;       // "MAIN_IDEA", "REASON", "DETAIL", "REFERENCE"
    private String question;           // Nội dung câu hỏi tiếng Nhật (kèm tiếng Việt)
    private List<String> options;      // 4 lựa chọn trắc nghiệm
    private Integer correctOptionIndex;// 0, 1, 2, 3
    private String evidenceQuote;      // Đoạn trích chính xác trong văn bản chứng minh đáp án
    private String explanation;        // Giải thích sư phạm tại sao đúng và tại sao các phương án khác sai
}
```

#### 4.1.2. Mở rộng `ReadingAnalysisResponse.java`
Bổ sung trường:
```java
private List<ReadingComprehensionQuestionDto> comprehensionQuestions;
```

#### 4.1.3. Cải tiến Prompt AI (`ReadingAssistantServiceImpl.java`)
Trong prompt gửi tới Gemini Flash, bổ sung cấu trúc JSON `comprehensionQuestions`:
* 1 - 3 câu hỏi tùy theo độ dài văn bản.
* Đảm bảo `evidenceQuote` trích dẫn chính xác 100% văn bản để frontend tìm kiếm vị trí câu.
* Giải thích chi tiết nguyên nhân đúng/sai.

---

### 4.2. Frontend (`frontend/pages/ai-reading/` & `learner-layout.js`)

#### 4.2.1. Cấu trúc Tabs mới tại Màn hình Workspace:
1. **Tab 1: 🎯 Thử thách Đọc hiểu Dokkai (Mặc định)**
   * Khung Split-view 2 cột:
     * Cột trái: Khung bài đọc sạch (`cleanReadingBody`) kèm thanh công cụ Bật/Tắt Furigana.
     * Cột phải: Bộ câu hỏi trắc nghiệm, các lựa chọn A, B, C, D; Nút "Nộp bài & Chấm điểm AI".
     * Khi đã nộp bài: Hiển thị tỷ lệ trả lời đúng (ví dụ: 2/2 câu đúng), đổi màu các phương án đã chọn (Xanh cho đáp án đúng, Đỏ cho đáp án sai), hiển thị khối giải thích sư phạm kèm nút "🔍 Xem dẫn chứng trong bài".
2. **Tab 2: 📖 Bài đọc tương tác (Zen Reader)**
   * Giữ nguyên tính năng tra từ tương tác, Furigana on/off, phát âm VOICEVOX.
3. **Tab 3: 🗂️ Sổ tay Từ vựng & Hán tự**
   * Giữ nguyên lưới thẻ từ vựng và nút lưu Flashcard 1 chạm.
4. **Tab 4: ⛩️ Phân tích Cấu trúc Ngữ pháp**
   * Giữ nguyên danh sách các điểm ngữ pháp phát hiện trong bài.
5. **Tab 5: 🌐 Bản dịch song ngữ**
   * Giữ nguyên bản đối chiếu tiếng Việt từng câu.

#### 4.2.2. Đổi tên Menu Sidebar (`learner-layout.js`):
* Cập nhật `title` của item `ai-reading`:
  * Cũ: `'Đọc hiểu Dokkai AI'`
  * Mới: `'Đọc hiểu'`

---

## 5. Tiêu chí Nghiệm thu (Acceptance Criteria)
* [ ] Sidebar hiển thị tên mục menu là **"Đọc hiểu"**.
* [ ] Khi phân tích một bài đọc bất kỳ (hoặc bài mẫu), hệ thống trả về bộ câu hỏi Dokkai (1-3 câu hỏi phù hợp với bài).
* [ ] Tab 1 hiển thị bài đọc sạch bên trái và câu hỏi bên phải trên màn hình Desktop; bài đọc ở trên và câu hỏi ở dưới trên Mobile.
* [ ] Học viên làm bài trắc nghiệm, bấm "Nộp bài" nhận được kết quả chấm điểm tức thì.
* [ ] Mỗi câu hỏi có lời giải thích chi tiết, phân tích rõ ràng vì sao đáp án đúng và vì sao phương án khác sai.
* [ ] Khi click "Xem dẫn chứng trong bài", câu văn trong khung bài đọc sạch được highlight nổi bật ngay lập tức.
* [ ] Học viên có thể tự do chuyển sang các Tab Từ vựng, Ngữ pháp, Dịch câu, Furigana và VOICEVOX bất cứ lúc nào.
