# QUY CHUẨN THIẾT KẾ ĐỒNG BỘ SHODO NEO-BRUTALIST (DESIGN RULES)
*(Quy tắc UI/UX Chuẩn Hóa Theo Mẫu `/handwriting` — Dành Cho AI Code Assistant & Toàn Bộ Frontend)*

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
<!-- [END IMMUTABLE CORE GUARDRAILS]                                        -->
<!-- ===================================================================== -->

---

## 1. NGUYÊN TẮC THIẾT KẾ ĐỒNG NHẤT (SHODO NEO-BRUTALIST SYSTEM)

Toàn bộ các màn hình học tập và hỗ trợ học viên (bao gồm 12 trang: `/kanji-dictionary`, `/handwriting`, `/shodo`, `/ai-writing`, `/grammar`, `/flashcards`, `/kanji-game`, `/kana`, `/match`, v.v.) phải tuân thủ nghiêm ngặt **hệ quy chuẩn đồng nhất trích xuất từ trang chuẩn `/handwriting`**.

### 1.1. Khóa Chặt Khung Trục Khổ Rộng Duy Nhất (Unified Shell Layout)
- **Kích thước khung chính:** Toàn bộ nội dung trang đặt trong thẻ `<main>` với kích thước cố định:
  ```html
  <main class="flex-1 overflow-y-auto p-4 sm:p-6 w-full max-w-6xl mx-auto">
  ```
- **Loại bỏ phân mảnh:** Tuyệt đối không sử dụng tùy tiện `max-w-4xl`, `max-w-5xl`, `max-w-7xl` hay `max-w-[1240px]` trên các trang học tập.
- **Loại bỏ khung `<PageHeader>` rườm rà:** Không chèn banner tĩnh chiếm diện tích hay khung tiêu đề cồng kềnh đầu trang; đưa trực tiếp thanh công cụ và nội dung tương tác vào tầm mắt người học.

### 1.2. Chuẩn Hóa Card & Khung Nội Dung (Card & Container Specs)
- **Card chính (`.shodo-card`, `.ls-card`):**
  - Nền trắng: `bg-white`
  - Viền mực: `border-2 border-[#0F172A]`
  - Bo góc chuẩn: `rounded-xl` (12px)
  - Bóng cứng: `shadow-[3px_3px_0px_0px_#0F172A]`
  - Class Tailwind: `bg-white border-2 border-[#0F172A] rounded-xl shadow-[3px_3px_0px_0px_#0F172A]`
- **Khung Canvas / Giấy Washi (`.shodo-canvas-box`):**
  - Nền giấy Washi: `bg-[#FAF8F2]`
  - Viền mực: `border-2 border-[#0F172A]`
  - Bo góc: `rounded-[8px]`
  - Bóng cứng: `shadow-[3px_3px_0px_0px_#0F172A]`
- **Thanh công cụ / Thanh tác vụ (`.shodo-toolbar`, `.ls-toolbar`):**
  - Nền trắng: `bg-white border border-[#0F172A] rounded-lg shadow-[2px_2px_0px_0px_#0F172A] p-2 sm:p-3`

---

## 2. HỆ THỐNG MÀU SẮC ĐỒNG BỘ (COLOR TOKENS & BUTTONS)

Tuyệt đối loại bỏ các màu đen thô (`#000000`, `#1E293B`), xanh lam cũ (`#006194`), xanh rêu (`#2A5A1A`), hay hồng sen (`#E0399A`). Bảng màu chuẩn mực là:

### 2.1. Nút Hành Động Chính (Primary Action CTA)
Áp dụng cho mọi nút kêu gọi hành động chính: *Tìm kiếm, Nạp chữ, Chấm & Sửa câu, Bắt đầu chơi, Lưu thẻ, Nộp bài, Tab Đang chọn*:
- **Màu nền:** Đỏ Hồng Trầm (Rose Wine) `bg-[#9F1239]`
- **Hover:** `hover:bg-[#BE185D]`
- **Màu chữ:** `text-white font-bold`
- **Viền:** `border border-[#0F172A]`
- **Bóng cứng:** `shadow-[2px_2px_0px_0px_#0F172A]`
- **Hiệu ứng bấm:** `active:translate-x-[1px] active:translate-y-[1px] active:shadow-none`
- **Class CSS:** `.btn-primary`, `.shodo-btn-primary`, `.ls-btn-primary`

### 2.2. Nút Phụ / Điểm Nhấn Nhẹ (Secondary Action / Cherry Tint)
Áp dụng cho các tính năng bổ trợ, nút lọc phụ, bộ thẻ cá nhân, huy hiệu nổi bật:
- **Màu nền:** Hồng Anh Đào nhạt `bg-[#FFE5EC]`
- **Hover:** `hover:bg-[#FCE7F3]`
- **Màu chữ:** Đỏ Rượu đậm `text-[#831843] font-semibold`
- **Viền:** `border border-[#FBCFE8]`
- **Bóng cứng:** `shadow-[1px_1px_0px_0px_#0F172A]`
- **Class CSS:** `.btn-secondary`, `.shodo-btn-secondary`, `.ls-btn-secondary`

### 2.3. Nút Viền Đơn / Tác Vụ Phụ (Neutral Outline Button)
Áp dụng cho nút Quay lại, Xóa nét vẽ, Đóng modal, Tùy chọn thứ cấp:
- **Màu nền:** `bg-white hover:bg-slate-50`
- **Màu chữ:** `text-[#0F172A] font-medium`
- **Viền:** `border border-[#0F172A]`
- **Bóng cứng:** `shadow-[1px_1px_0px_0px_#0F172A]`
- **Hiệu ứng bấm:** `active:translate-x-[1px] active:translate-y-[1px] active:shadow-none`
- **Class CSS:** `.btn-outline`, `.shodo-btn-outline`, `.ls-btn-outline`

### 2.4. Huy Hiệu Cấp Độ & Nhãn (Badges & Pills)
- **Huy hiệu JLPT / Sakura Pill:** `bg-[#FFE5EC] text-[#831843] border border-[#FBCFE8] px-2.5 py-0.5 rounded-full text-xs font-semibold`
- **Huy hiệu Trạng thái / Đã học:** `bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full text-xs font-medium`
- **Huy hiệu Trung tính:** `bg-slate-100 text-slate-700 border border-slate-200 px-2 py-0.5 rounded-full text-xs font-medium`

---

## 3. PHÔNG CHỮ & ĐỊNH DẠNG VĂN BẢN (TYPOGRAPHY HIERARCHY)

Tuyệt đối loại bỏ phông chữ viết tay `Itim` hoặc phông hệ thống `Segoe UI` gây lệch phong cách học thuật.

- **Văn bản chung / Giao diện người dùng (Body & Headings):**  
  `font-family: "Inter", "Plus Jakarta Sans", sans-serif;`  
  Rõ nét, hiện đại, hỗ trợ dấu tiếng Việt chuẩn và đạt độ tương phản cao.
- **Ký tự Kanji & Kana (Chữ Hán & Nét chữ Nhật Bản):**  
  `font-family: "Noto Serif JP", "Klee One", serif;`  
  Thể hiện chuẩn xác nét bút lông Shodo thanh đậm truyền thống.
- **Mã số / Số nét / Bảng thống kê:**  
  `font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;`

---

## 4. QUY TẮC BẮT BUỘC KHI THÊM MỚI TÍNH NĂNG / MÀN HÌNH (MANDATORY INHERITANCE RULE)

> ⚠️ **ĐẶC QUYỀN ÁP DỤNG CHO KHỐI "HỌC TẬP & RÈN LUYỆN":**  
> Mỗi khi phát triển hoặc cập nhật bất kỳ màn hình, thành phần (component), popup/modal nào thuộc khối **Học Tập & Rèn Luyện (Learning & Practice)**:
> 
> 1. **Bắt buộc đọc lại và tuân thủ tuyệt đối các quy tắc ở trên:**
>    - Nhúng file CSS dùng chung: `<link rel="stylesheet" href="/assets/css/learning-support-common.css">` ở đầu thẻ `<head>`.
>    - Khóa chặt chiều rộng: `<main class="flex-1 overflow-y-auto p-4 sm:p-6 w-full max-w-6xl mx-auto">`.
>    - Mọi Card phải có `border-2 border-[#0F172A] rounded-xl shadow-[3px_3px_0px_0px_#0F172A]`.
>    - Mọi nút bấm chính phải có màu Đỏ Hồng Trầm `bg-[#9F1239]` kèm bóng cứng `shadow-[2px_2px_0px_0px_#0F172A]`.
>    - Tuyệt đối tuân thủ nguyên tắc WCAG AAA chống chữ tàng hình, không bao giờ để chữ trắng trên nền sáng.
>    - Toàn bộ luồng dữ liệu phải là Zero-Reload: truyền ngầm JSON qua `apiClient.js` và cập nhật DOM động, kèm thông báo nổi Toast/Dialog.
> 
> 2. **Giới hạn phạm vi nghiêm ngặt:**  
>    Quy chuẩn Shodo Neo-Brutalist này áp dụng thống nhất cho toàn bộ các trang học tập (Kana Quiz, Bộ thẻ Flashcards, Chi tiết Flashcard, Luyện lật thẻ, Phòng luyện viết Shodo, Tra cứu Từ điển chữ Hán, Tra cứu viết tay AI, Game Mưa chữ Hán, Ngữ pháp & AI Sensei, Trợ lý học tập và các module mới).
