# KizunaTutor Code & Naming Conventions (CONVENTIONS.md)

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

---

## 1. Quy Ước Đặt Tên (Naming Conventions)

### 1.1. Java Backend
- **Package:** Toàn bộ chữ thường, theo domain: `com.kizuna.controller.*`, `com.kizuna.service.*`, `com.kizuna.repository.*`, `com.kizuna.entity.*`, `com.kizuna.dto.*`.
- **Classes & Interfaces:** PascalCase (ví dụ: `LearnerController`, `TutorRepository`, `LearningSpaceService`).
- **Methods & Variables:** camelCase (ví dụ: `findActiveTutors()`, `populateLearnerShell()`, `hourlyRate`).
- **Constants & Enum Items:** UPPER_SNAKE_CASE (ví dụ: `ROLE_LEARNER`, `ACTIVE_PAID`, `APPROVED`).

### 1.2. HTML, Thymeleaf & CSS
- **File view:** kebab-case trong thư mục phân hệ (ví dụ: `templates/learner/find-tutor.html`, `templates/tutor/kanji-library.html`).
- **CSS Classes:** Kebab-case theo Tailwind và custom design tokens (`card-nhai`, `btn-nhai`, `badge-nhai`, `shadow-retro`).
- **HTML IDs:** camelCase (ví dụ: `practiceBoard`, `learnerSearchInput`, `modalTrialBooking`).
- **HTML5 Data Attributes:** kebab-case (ví dụ: `data-tutor-id`, `data-tutor-name`).

---

## 2. Quy Chuẩn Viết Hoa Chuỗi Giao Diện (Casing Directives)
- **Toast & Thông Báo:** Sentence case (chỉ viết hoa chữ đầu câu).
  - *Đúng:* `Đăng ký học thử thành công. Gia sư sẽ sớm phản hồi cho bạn.`
  - *Sai:* `ĐĂNG KÝ HỌC THỬ THÀNH CÔNG!`
- **Tiêu đề khối & Nút hành động:** Title Case hoặc Sentence case.
  - *Đúng:* `Tìm Gia Sư Hợp Gu`, `Cầm Bút Luyện Nét`, `Xem Hồ Sơ`
  - *Sai:* `TÌM GIA SƯ HỢP GU`, `CẦM BÚT LUYỆN NÉT`
