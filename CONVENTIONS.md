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

---

## 3. Quy Chuẩn Khung Cố Định & Bố Cục Thẻ (Layout Containment)
- **Container Cố Định:** Mọi Card / Box hiển thị dữ liệu dạng danh sách hoặc lưới (Deck Grid, Kanji Cards) phải khai báo khung cố định (`h-[360px]`, `min-h-[...]`, `max-h-[...]`).
- **Nội dung con độc lập:** Vùng chứa chips hoặc empty states phải có kích thước cố định (`h-[155px]`) và thuộc tính cuộn nội bộ (`overflow-y-auto`).
- **Menu tùy chọn nổi:** Menu 3 chấm [⋮] phải luôn sử dụng `position: absolute; z-index: 50;` và phần tử bọc nút phải có `shrink-0 w-8 h-8`, tuyệt đối không gây dịch chuyển hay co dãn thẻ cha khi kích hoạt mở menu.

---

## 4. Quy Chuẩn Phản Hồi Giao Diện & Thông Báo Toàn Cục (UI Feedback & Notification Conventions)
- **Tuyệt đối cấm `window.alert()` và `window.confirm()`:** Mọi tương tác phản hồi trong mã JavaScript bắt buộc sử dụng `KizunaToast` hoặc `KizunaDialog`.
- **Cấm Inline Alert Box trong luồng tài liệu:** Không tạo các phần tử thông báo tĩnh như `<div id="alertBox">` nằm xen kẽ giữa các công cụ tìm kiếm và lưới nội dung gây giật nảy layout (Layout Shift) khi trạng thái thay đổi.
- **Sử dụng API chuẩn của `KizunaToast` (hoặc alias `showToast`):**
  - Thông báo thành công: `showToast.success('Nội dung thông báo');`
  - Thông báo cảnh báo: `showToast.warning('Nội dung cảnh báo');`
  - Thông báo xóa / nguy hiểm: `showToast.danger('Nội dung đã xóa');`
  - Thông báo thông tin: `showToast.info('Nội dung ghi chú');`
- **Nguyên tắc phân định tương tác:**
  - Sử dụng `KizunaToast` cho toàn bộ các thông báo trạng thái thụ động (lưu thành công, xóa bộ thẻ, khởi tạo dữ liệu, sao chép đường dẫn, lỗi kết nối nhẹ). Toast tự động ẩn sau 3.5 giây và không chặn click/scroll của người dùng.
  - Sử dụng `KizunaDialog` (`KizunaDialog.show()`, `KizunaDialog.confirm()`) chỉ khi cần sự can thiệp có ý thức của người dùng (xác nhận xóa vĩnh viễn, chấm điểm nét chữ AI, nộp bài kiểm tra).

---

## 5. Quy Chuẩn Điều Khiển Ô Nhập Liệu & Toolbar (Input & Toolbar Conventions)
- **Ô nhập số nét (Stroke Count):** Luôn dùng độ rộng cố định `w-20` (80px), placeholder ngắn gọn `"Số nét"`, căn giữa (`text-center`) để không làm chiếm diện tích các bộ lọc Bộ thủ và Cấp độ JLPT.
- **Thanh công cụ Shodo Canvas:** Tối giản các nút bấm, không tạo các nút trùng lặp chức năng (như "Vẽ Lại"). Nút phát hướng dẫn tự đổi nhãn/trạng thái thành "Phát Lại" khi animation hoàn tất.


