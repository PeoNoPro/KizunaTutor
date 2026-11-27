# KizunaTutor Design System (DESIGN.md)

<!-- ===================================================================== -->
<!-- [IMMUTABLE CORE GUARDRAILS] DO NOT MODIFY, OVERWRITE OR DELETE BELOW  -->
<!-- KHỐI QUY TẮC BẤT BIẾN: NGHIÊM CẤM TỰ Ý SỬA ĐỔI HOẶC XÓA BỎ DƯỚI MỌI HÌNH THỨC -->
<!-- ===================================================================== -->

> ⛔ **CẢNH BÁO BẢO VỆ HỆ THỐNG (STRICT NON-NEGOTIABLE GUARDRAILS):**  
> Đây là 3 nguyên tắc nền tảng tối thượng của hệ thống KizunaTutor. Mọi tác vụ lập trình (Backend & Frontend) và cập nhật tài liệu BẮT BUỘC phải tuân thủ nghiêm ngặt:
>
> 1. **KHÔNG BAO GIỜ HIỂN THỊ NỘI DUNG BACKEND LÊN FRONTEND (ZERO BACKEND LEAKS):**
>    - Tuyệt đối cấm để lộ các mã nội bộ, tên enum, tên Exception, mã HTTP raw hay cấu trúc dữ liệu Backend ra giao diện (ví dụ: `ROLE_ADMIN`, `ROLE_LEARNER`, `BadCredentialsException`, `NullPointerException`, `500 INTERNAL_SERVER_ERROR`, `user_id`, `is_active`).
>    - Toàn bộ thông báo, nhãn, badge và trạng thái gửi tới người dùng BẮT BUỘC phải được chuyển hóa 100% sang ngôn ngữ người dùng tự nhiên (Human-centric / User-friendly Vietnamese).
>
> 2. **KHÔNG BAO GIỜ VIẾT IN HOA TOÀN BỘ TRÊN GIAO DIỆN (NO ALL-CAPS UI TEXT):**
>    - Tuyệt đối cấm sử dụng chữ IN HOA TOÀN BỘ (ALL-CAPS) cho các dòng thông báo, tiêu đề Modal, Alert, Toast, Empty States hay nội dung giải thích (ví dụ: cấm `LỖI ĐĂNG NHẬP!`, `ĐĂNG KÝ THÀNH CÔNG!`, `KHÔNG TÌM THẤY DỮ LIỆU!`).
>    - Chỉ áp dụng viết hoa chữ cái đầu câu (Sentence case) hoặc chữ cái đầu mỗi từ chính (Title Case cho nhãn nút/tiêu đề ngắn). Chữ in hoa chỉ được dùng duy nhất cho các từ viết tắt chuyên môn đã định nghĩa (ví dụ: `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`).
>
> 3. **KHÔNG BAO GIỜ CẬP NHẬT DỮ LIỆU ẢO ĐỂ HIỂN THỊ HỆ THỐNG (NO MOCK / FAKE DATA INJECTION):**
>    - Tuyệt đối không tự ý chèn dữ liệu ảo (hardcoded mock data, dummy JSON, dữ liệu bịa đặt) vào mã nguồn hoặc cơ sở dữ liệu để đối phó hiển thị khi chưa có API thực tế.
>    - Mọi dữ liệu hiển thị trên màn hình của 6 Actor (Admin, Manager, CS Staff, Academic Officer, Tutor, Learner) BẮT BUỘC phải được kết nối từ Endpoint API thực tế hoặc hiển thị màn hình rỗng chuẩn (Empty State) nếu cơ sở dữ liệu chưa có dữ liệu.

<!-- ===================================================================== -->
<!-- [END IMMUTABLE CORE GUARDRAILS] - SAFE TO APPEND EXTENSIONS BELOW     -->
<!-- ===================================================================== -->

> Built upon **pbakaus/impeccable** and **Leonxlnx/taste-skill** standards.
> Anti-slop, pedagogical, culturally rooted in modern Japanese aesthetics.

---

## 1. Brand Identity & Aesthetic Direction

- **Concept**: *Wabi-Sabi meets Modern EdTech* (Giản dị, tinh tế, học thuật nhưng thân thiện và ấm áp).
- **Voice & Tone**: Chu đáo (Omoiyari - 思いやり), Đáng tin cậy, Khích lệ học viên tiến bộ mỗi ngày.
- **Visual Personality**: Sạch sẽ, nhiều khoảng thở (Ma - 間), phân cấp thông tin rõ ràng, nét chữ Kanji nổi bật trên lưới chuẩn.

---

## 2. Color Palette (Design Tokens)

Bảng màu được trích xuất từ văn hóa truyền thống Nhật Bản kết hợp hệ quy chuẩn Shodo Neo-Brutalist:

| Tên Token | Mã Hex | Ý nghĩa văn hóa / Ứng dụng |
| :--- | :--- | :--- |
| `--color-primary` / `--color-btn-primary` | `#9F1239` | **Đỏ Hồng Trầm (Rose Wine)**: Màu hành động chính khối học tập, nút CTA, nạp chữ, tìm kiếm, tab đang chọn. |
| `--color-primary-hover` | `#BE185D` | Hiệu ứng hover cho nút bấm chính khối học tập. |
| `--color-secondary-cherry` | `#FFE5EC` | **Hồng Anh Đào (Cherry Tint)**: Nền nút phụ, tab thẻ cá nhân, badge JLPT, vùng chọn nhẹ. |
| `--color-secondary-border` | `#FBCFE8` | Đường viền cho nút và badge Cherry. |
| `--color-secondary-text`   | `#831843` | Chữ trên nền hồng cherry, tạo độ tương phản cao đạt chuẩn WCAG AAA. |
| `--color-ink-border`       | `#0F172A` | **Màu Mực Đen (Ink Border)**: Viền mực 2px cho Card/Canvas, 1px cho Button/Toolbar. |
| `--color-accent-matcha`   | `#88A064` | **Trà Xanh (Matcha)**: Trạng thái thành công, bài tập đạt chuẩn, thanh tiến độ. |
| `--color-alert-akane`     | `#C84B31` | **Đỏ son (Akane)**: Nét vẽ sai, cảnh báo vi phạm hạn mức, nút hủy. |
| `--color-bg-washi`        | `#FAF8F2` | **Giấy Washi**: Nền chính bàn vẽ Canvas Shodo, khung luyện viết. |
| `--color-surface`         | `#FFFFFF` | Nền thẻ bài học (Card), bảng từ điển, modal popup. |
| `--color-text-sumi`       | `#0F172A` | **Mực Tàu (Sumi)**: Màu chữ chính, độ tương phản cao. |
| `--color-text-muted`      | `#64748B` | Chú thích Furigana, Romaji, ngày tạo, số nét. |

---

## 3. Typography Hierarchy

- **Latin Font (Body & Headings)**: `"Inter"`, `"Plus Jakarta Sans"` hoặc system fonts (`-apple-system, BlinkMacSystemFont, "Segoe UI"`). Tuyệt đối không dùng phông viết tay `Itim` làm tiêu đề gây lệch chuẩn học thuật.
- **Japanese Kanji & Kana Font**: `"Noto Sans JP"`, `"Zen Kaku Gothic New"`, `"Yu Gothic"`, `sans-serif`.
- **Kanji Practice Font**: `"Noto Serif JP"`, `"Klee One"` (cho phong cách nét bút lông truyền thống).

### Tỷ lệ cỡ chữ:
- **Display (Kanji Focus)**: `48px` - `64px` (Hiển thị nét chữ Hán tập viết trên khung Canvas).
- **H1 (Page Title)**: `32px`, font-weight `700`, line-height `1.2`.
- **H2 (Section Header)**: `24px`, font-weight `600`, line-height `1.3`.
- **H3 (Card Title)**: `18px`, font-weight `600`, line-height `1.4`.
- **Body**: `15px`, font-weight `400`, line-height `1.6`.
- **Furigana / Caption**: `12px`, font-weight `500`.

---

## 4. Kanji Practice Grid Specs (Lưới luyện viết)

- **Kích thước chuẩn**: `109px x 109px` (khớp với viewBox chuẩn của KanjiVG `0 0 109 109`).
- **Lưới phụ trợ**:
  - Khung viền ngoài: `border: 2px solid #0F172A`.
  - Trục chữ thập (thập tự giá) và đường chéo mờ chia 4 ô hoặc 8 ô: `stroke: #E2E8F0; stroke-dasharray: 4, 4`.
- **Màu nét chữ**:
  - Nét mẫu: `#0F172A` (độ dày nét `3px`).
  - Nét đang vẽ: `#C84B31` hoặc `#88A064` (khi vẽ đúng).
  - Thứ tự số thứ tự nét: `#808080`, cỡ chữ `8px`.

---

## 5. Anti-Slop Principles (Nguyên tắc chống rập khuôn AI)

Tuân thủ nghiêm ngặt theo **Leonxlnx/taste-skill** & **pbakaus/impeccable**:
1. ❌ **Không dùng dải màu tím AI tím lịm (purple gradients)** hoặc bóng đổ neon mờ ảo không có chủ đích.
2. ❌ **Không dùng layout 3 thẻ bài (three identical cards)** đồng dạng nhàm chán.
3. ✅ **Tập trung vào trải nghiệm sư phạm**: Lộ trình bài học rõ ràng, bảng từ điển dễ tra cứu, thẻ gia sư làm nổi bật chứng chỉ N1/N2/Sư phạm.
4. ✅ **Khoảng cách (Spacing)**: Sử dụng hệ số 8pt (`8px`, `16px`, `24px`, `32px`, `48px`).
5. ✅ **Border Radius**: Card dùng `rounded-xl` (12px), Canvas dùng `rounded-[8px]`, Button dùng `rounded-lg` (8px).

---

## 6. Bản Địa Hóa 100% Tiếng Việt & Không Rò Rỉ Mã Kỹ Thuật (Human-First UI)

1. **Ngôn ngữ hiển thị**: Toàn bộ giao diện (tiêu đề, nút bấm, nhãn, cảnh báo, thống kê, hộp thoại modal) **phải 100% bằng Tiếng Việt**.
2. **Không để lộ mã nguồn & cơ sở dữ liệu**: Tuyệt đối không để lọt các từ ngữ kỹ thuật như ID, UUID, entity, session, request, response, DTO, camelCase, snake_case lên màn hình học viên.
3. **Từ ngữ thân thiện sư phạm**:
   - Thay "Deadline" bằng "Vạch ranh giới nguy hiểm".
   - Thay "Game Over" bằng "Kết thúc ván đấu (Chưa vượt qua)".
   - Thay "You Win" bằng "Chiến thắng xuất sắc!".
   - Thay "Accuracy" bằng "Độ chính xác".
   - Thay "Score" bằng "Điểm số tích lũy".
   - Thay "Combo" bằng "Chuỗi gõ liên hoàn".

---

## QUY CHUẨN HIỂN THỊ VĂN BẢN TRÊN GIAO DIỆN (UI COPYWRITING STANDARDS)

### 1. Bảng chuyển ngữ thuật ngữ Backend sang Giao diện
Tất cả các phản hồi từ API hoặc xử lý giao diện phải ánh xạ theo bảng sau trước khi hiển thị:

| Mã Backend / Kỹ thuật | Hiển thị chuẩn trên UI | Cấm hiển thị |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | Quản trị viên | ROLE_ADMIN, Admin role |
| `ROLE_MANAGER` | Quản lý vận hành | ROLE_MANAGER |
| `ROLE_CS_STAFF` | Chăm sóc khách hàng | ROLE_CS_STAFF, CS Staff |
| `ROLE_ACADEMIC_OFFICER` | Cán bộ học vụ | ROLE_ACADEMIC_OFFICER |
| `ROLE_TUTOR` | Gia sư | ROLE_TUTOR, Tutor role |
| `ROLE_LEARNER` / `ROLE_STUDENT` | Học viên | ROLE_LEARNER, STUDENT |
| `BadCredentialsException` | Email hoặc mật khẩu không chính xác | Bad credentials |
| `UserAlreadyExistsException` | Email này đã được đăng ký trong hệ thống | User already exists |
| `InternalServerError` | Hệ thống đang bận, vui lòng thử lại sau ít phút | 500 Server Error |
| `NullPointerException` | Đã có lỗi xảy ra trong quá trình xử lý | Null pointer exception |

### 2. Quy chuẩn viết hoa (Casing Rules)
- **Thông báo Toast / Alert:** Dùng Sentence case. 
  - Đúng: `✅ Đăng ký tài khoản thành công. Vui lòng kiểm tra email để lấy mã OTP.`
  - Sai: `✅ ĐĂNG KÝ TÀI KHOẢN THÀNH CÔNG!`
- **Tiêu đề Modal / Tiêu đề Card:** Dùng Title Case hoặc Sentence case trang nhã.
  - Đúng: `Xác thực mã OTP`, `Khởi tạo tài khoản nhân viên`
  - Sai: `XÁC THỰC MÃ OTP`, `KHỞI TẠO TÀI KHOẢN NHÂN VIÊN`
- **Thông báo lỗi (Error messages):** Giọng văn trung tính, lịch sự, không dùng dấu chấm than dồn dập (`!!!`).
  - Đúng: `Mã OTP không chính xác. Bạn còn 3 lần thử lại.`
  - Sai: `LỖI! MÃ OTP SAI! HÃY NHẬP LẠI NGAY!`
- **Ngoại lệ:** Chỉ viết hoa toàn bộ đối với các từ viết tắt chuyên môn / chuẩn quốc tế đã được quy ước (như `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `JWT`).

---

## 7. QUY CHUẨN ĐỒNG NHẤT SHODO NEO-BRUTALIST (LEARNING & PRACTICE PAGES)

*(Quy chuẩn thiết kế chuẩn mực trích xuất trực tiếp từ trang mẫu `/handwriting` và áp dụng thống nhất cho toàn bộ 12 trang khối Học Tập & Rèn Luyện)*

### 1. Khóa Chặt Khổ Rộng Trục Trang (Unified Container Shell):
- Toàn bộ các trang học tập được gom trong thẻ `<main>` duy nhất:
  ```html
  <main class="flex-1 overflow-y-auto p-4 sm:p-6 w-full max-w-6xl mx-auto">
  ```
- **Tuyệt đối cấm phân mảnh:** Không dùng các khổ `max-w-4xl`, `max-w-5xl`, `max-w-7xl` hoặc `max-w-[1240px]` trên các trang học tập.
- **Loại bỏ khung `<PageHeader>` rườm rà:** Không chèn banner hay khối tiêu đề cồng kềnh đầu trang gây chiếm diện tích; đưa ngay thanh công cụ và giao diện luyện tập vào tầm nhìn.

### 2. Ngôn Ngữ Hình Khối & Bóng Đổ (Visual Language & Hard Shadow):
- **Bo góc chuẩn:**
  - Thẻ nội dung chính (`.shodo-card`, `.ls-card`): `rounded-xl` (12px).
  - Khung Canvas / Bàn luyện viết: `rounded-[8px]`.
  - Thanh công cụ (Toolbar) / Nút bấm: `rounded-lg` (8px) hoặc `rounded-[6px]`.
- **Viền mực (Ink Border):**
  - Thẻ và khung lớn: `border-2 border-[#0F172A]`.
  - Nút bấm, thanh toolbar, ô input: `border border-[#0F172A]`.
- **Đổ bóng khối cứng (Hard Ink Shadow):**
  - Card chính & Canvas: `shadow-[3px_3px_0px_0px_#0F172A]`.
  - Nút bấm chính & Toolbar: `shadow-[2px_2px_0px_0px_#0F172A]`.
  - Nút phụ & Huy hiệu: `shadow-[1px_1px_0px_0px_#0F172A]`.
- **Hiệu ứng tương tác (Micro-interactions):**
  - Hover: Tăng tương phản nhẹ nhàng (`hover:bg-[#BE185D]`, `hover:bg-slate-50`).
  - Active (Bấm nút): `active:translate-x-[1px] active:translate-y-[1px] active:shadow-none`.

### 3. Bảng Color Tokens Thống Nhất:
- **Nền trang chính:** `bg-[#F8FAFC]` (hoặc `#FCFBF7`).
- **Nền thẻ nội dung:** `bg-white` (bề mặt thẻ sạch sẽ, tương phản cao).
- **Nền bàn vẽ Canvas / Giấy Washi:** `bg-[#FAF8F2]`.
- **Nút hành động chính (Primary Action CTA - Tìm kiếm, Nạp chữ, Chấm bài, Bắt đầu):**  
  `bg-[#9F1239]` (Đỏ Hồng Trầm / Rose Wine), hover `bg-[#BE185D]`, chữ `text-white font-bold`, viền `border border-[#0F172A]`, bóng `shadow-[2px_2px_0px_0px_#0F172A]`.
- **Nút phụ / Điểm nhấn nhẹ (Cherry Tint - Bộ thẻ cá nhân, Bộ lọc, Tab phụ):**  
  `bg-[#FFE5EC]`, hover `bg-[#FCE7F3]`, chữ `text-[#831843]`, viền `border border-[#FBCFE8]`, bóng `shadow-[1px_1px_0px_0px_#0F172A]`.
- **Nút viền mảnh trung tính (Neutral Outline - Quay lại, Xóa nét, Đóng popup):**  
  `bg-white`, hover `bg-slate-50`, chữ `text-[#0F172A]`, viền `border border-[#0F172A]`, bóng `shadow-[1px_1px_0px_0px_#0F172A]`.
- **Huy hiệu JLPT / Sakura Pill:**  
  `bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8] px-2.5 py-0.5 rounded-full text-xs font-semibold`.

### 4. Quy Chuẩn Phông Chữ (Typography Scale):
- **Tuyệt đối cấm sử dụng phông viết tay `Itim` hoặc `Segoe UI`** làm tiêu đề trên các trang học tập vì gây xung đột giao diện và mất vẻ học thuật.
- **Văn bản chung, tiêu đề UI, nội dung bài học:** `"Inter"`, `"Plus Jakarta Sans"`, sans-serif.
- **Ký tự Kanji & Kana nét bút:** `"Noto Serif JP"`, `"Klee One"`, serif.
- **Số liệu / Mã / Phím tắt:** Monospace.

---

## 📦 QUY CHUẨN THỰC THI CHO KHỐI "HỖ TRỢ HỌC TẬP" (LEARNING SUPPORT ARCHITECTURE)

### 1. BẮT BUỘC NHÚNG CSS DÙNG CHUNG (MANDATORY INCLUSION)
Tất cả các màn hình thuộc phân hệ Học Tập & Rèn Luyện của Học viên:
- **Tra cứu Từ Điển Chữ Hán:** `/kanji-dictionary` (hoặc `/learner/dictionary`)
- **Tra cứu Viết tay AI:** `/handwriting` (hoặc `/handwriting-search`)
- **Phòng luyện viết Shodo:** `/shodo` (hoặc `/shodo-practice`, `/learner/practice-canvas`)
- **Chấm & Sửa câu bằng AI:** `/ai-writing`
- **Ngữ pháp & AI Sensei:** `/grammar` (hoặc `/learner/grammar`)
- **Bộ thẻ Flashcard:** `/flashcards`, `/flashcards-detail`, `/flashcards-study`, `/flashcards-match`
- **Game Mưa Chữ Hán:** `/kanji-game`
- **Đấu trường Kana Quiz:** `/kana-quiz`

**BẮT BUỘC** phải import file CSS dùng chung ở thẻ `<head>`:
```html
<link rel="stylesheet" href="/assets/css/learning-support-common.css">
```

### 2. ÁNH XẠ NGUYÊN TẮC THIẾT KẾ (DESIGN RULE MAPPING)

| Thành phần UI | Class chuẩn sử dụng | Mô tả & Quy tắc bắt buộc |
| :--- | :--- | :--- |
| **Nền trang chính** | `<body class="bg-[#F8FAFC]">` | Sử dụng màu `#F8FAFC` sạch sẽ, êm mắt. |
| **Khung trục trang** | `<main class="max-w-6xl mx-auto p-4 sm:p-6">` | Khóa chặt trục `max-w-6xl`, căn giữa toàn trang. |
| **Thẻ bài học chính** | `.shodo-card` / `.ls-card` | Nền trắng, viền mực `border-2 border-[#0F172A]`, bo góc `rounded-xl`, bóng `shadow-[3px_3px_0px_0px_#0F172A]`. |
| **Khung vẽ Canvas** | `.shodo-canvas-box` | Nền Washi `#FAF8F2`, viền `border-2 border-[#0F172A]`, bo góc `rounded-[8px]`, bóng `shadow-[3px_3px_0px_0px_#0F172A]`. |
| **Nút bấm chính (Action)** | `.btn-primary` / `.shodo-btn-primary` / `.ls-btn-primary` | Nền Đỏ Hồng Trầm `bg-[#9F1239]`, chữ trắng rõ nét, viền `border border-[#0F172A]`, bóng `shadow-[2px_2px_0px_0px_#0F172A]`. |
| **Nút phụ / Điểm nhấn** | `.btn-secondary` / `.shodo-btn-secondary` / `.ls-btn-secondary` | Nền `bg-[#FFE5EC]`, chữ `text-[#831843]`, viền `border border-[#FBCFE8]`, bóng `shadow-[1px_1px_0px_0px_#0F172A]`. |
| **Nút viền mảnh / Quay lại** | `.btn-outline` / `.shodo-btn-outline` / `.ls-btn-outline` | Nền trắng, viền `border border-[#0F172A]`, chữ `#0F172A`, bóng `shadow-[1px_1px_0px_0px_#0F172A]`. |
| **Tab / Nút đang chọn** | `.is-active` / `.ls-btn-active` | Nền Đỏ Hồng Trầm `bg-[#9F1239] text-white`, viền `#0F172A`, bóng `shadow-[2px_2px_0px_0px_#0F172A]`. |
| **Thanh điều hướng / Tác vụ**| `.shodo-toolbar` / `.ls-toolbar` | Đặt ở đầu trang chi tiết, bọc cụm nút bấm và bộ lọc, viền `#0F172A`, bóng `shadow-[2px_2px_0px_0px_#0F172A]`. |
| **Bộ lọc dạng Pill** | `.ls-pill` | Bo tròn viên thuốc `rounded-full`, nền `#FFE5EC` text `#831843` khi chọn tab JLPT. |
| **Ô nhập liệu** | `.ls-input` / `.shodo-input` | Viền mực `border border-[#0F172A]`, bo góc `rounded-lg`, focus viền nổi bật. |
| **Huy hiệu JLPT** | `.ls-badge.ls-badge-jlpt` | Nền hồng anh đào `#FFE5EC`, chữ `#831843`, viền `#FBCFE8`. |

### 3. QUY TẮC ĐỘ TƯƠNG PHẢN & NỘI DUNG (WCAG AAA SAFETY)
- **Tuyệt đối chống chữ tàng hình:** Tất cả văn bản (giải nghĩa tiếng Việt, câu ví dụ, âm Hán Việt) bắt buộc nhận màu mực đậm `#0F172A` hoặc `#334155`. Không bao giờ áp dụng màu chữ trắng (`#FFFFFF`) lên các vùng thẻ có nền sáng.
- **Xóa bỏ thành phần dư thừa:** Tuyệt đối không để dải breadcrumb lặp lại (`school Không gian học viên...`) hoặc banner từ điển tĩnh xuất hiện làm choán diện tích trên các trang học tập.
- **Luồng dữ liệu (Zero-Reload):** Toàn bộ thao tác chuyển tab bài học, lật thẻ Flashcard, nộp nét vẽ Shodo phải truyền ngầm bằng gói tin JSON qua `apiClient.js` và cập nhật DOM động, không reload trang.

### 4. NGUYÊN TẮC BẮT BUỘC KHI THÊM MỚI TÍNH NĂNG / MÀN HÌNH (MANDATORY INHERITANCE RULE)
> ⚠️ **ĐẶC QUYỀN ÁP DỤNG RIÊNG CHO KHỐI "HỌC TẬP & RÈN LUYỆN":**  
> Mỗi khi phát triển hoặc thêm mới bất kỳ màn hình, thành phần (component), popup/modal hay tính năng nào thuộc khối **Học Tập & Rèn Luyện (Learning & Practice)** của Học viên:
> 
> 1. **Bắt buộc đọc lại và tuân thủ tuyệt đối các quy tắc ở trên:**
>    - Nhúng file CSS dùng chung: `<link rel="stylesheet" href="/assets/css/learning-support-common.css">` ở đầu thẻ `<head>`.
>    - Khóa chặt chiều rộng: `<main class="flex-1 overflow-y-auto p-4 sm:p-6 w-full max-w-6xl mx-auto">`.
>    - Sử dụng đúng hệ thống Design Tokens, thẻ bài học `.shodo-card` / `.ls-card`, cụm nút bấm (`.btn-primary`, `.btn-secondary`, `.btn-outline`), thanh tác vụ `.shodo-toolbar`, ô nhập liệu và huy hiệu `.ls-badge`.
>    - Tuyệt đối tuân thủ nguyên tắc WCAG AAA chống chữ tàng hình (`#0F172A`, `#334155`), không bao giờ để chữ trắng trên nền sáng.
>    - Toàn bộ luồng dữ liệu phải là Zero-Reload: truyền ngầm JSON qua `apiClient.js` và cập nhật DOM động, kèm thông báo nổi Toast/Dialog.
> 
> 2. **Giới hạn phạm vi nghiêm ngặt:**  
>    Quy định này **chỉ áp dụng duy nhất cho các phần trong khối Học Tập & Rèn Luyện** (bao gồm: Kana Quiz, Bộ thẻ Flashcards, Chi tiết Flashcard, Luyện lật thẻ, Phòng luyện viết Shodo, Tra cứu Từ điển chữ Hán, Tra cứu viết tay AI, Game Mưa chữ Hán, Ngữ pháp & AI Sensei, Trợ lý học tập và các module học tập mới phát triển thêm sau này). Tuyệt đối **không áp dụng** sang các phân hệ ngoài khối này (như Admin, Manager, Gia sư, CSKH hay các trang Portal công khai).
