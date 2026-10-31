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

## 3. Quy Chuẩn Khung Cố Định & Bố Cục Thẻ (Layout Containment & Zero Layout Shift)
- **Cố định cấu trúc khung Master Shell 3 phân khu (Fixed Header, Sidebar, Footer Shell):**
  - Thẻ `<html>` và `<body>` BẮT BUỘC khai báo `h-screen overflow-hidden flex flex-col`.
  - Khung bao ngoài BẮT BUỘC là `flex-1 flex h-screen w-screen overflow-hidden`.
  - **Sidebar (Trái):** Cố định tuyệt đối `h-full shrink-0 border-r border-black overflow-y-auto`. Tuyệt đối không co dãn hay cuộn theo trang.
  - **Khu vực làm việc Workspace (Phải):** `flex-1 flex flex-col min-w-0 h-full overflow-hidden`.
  - **Header (Trên cùng Workspace):** Cố định tuyệt đối `h-14 shrink-0 border-b border-black z-20` (hoặc `md:hidden` cho phân hệ Học viên). Không bao giờ bị trượt mất khi cuộn chuột.
  - **Loại bỏ Breadcrumbs & Header Thừa Ở Phân Hệ Học Viên (Zero Redundant Header on Desktop):**
    - Phân hệ Học viên (Learner) sử dụng Header thống nhất `<header th:replace="~{learner/layout/header :: header}"></header>` (`md:hidden h-11`).
    - Trên màn hình máy tính (Desktop `md:` trở lên), tuyệt đối KHÔNG dựng thêm thanh header độc lập hiển thị Breadcrumbs (`[Bàn học tập] / ...`) hay các nút dàn ngang (`[Xuất file Excel]`, `[Mở bộ thẻ]`) gây lãng phí diện tích hiển thị. Toàn bộ không gian phía trên dành trọn vẹn cho nội dung học tập. Mọi điều hướng đã có Sidebar bên trái đảm nhiệm.
    - Trên các phân hệ khác (Admin, Tutor, CS Staff, Manager, Academic Officer), thanh Header chỉ dành cho các nút tiện ích và thao tác nhanh, tuyệt đối cấm hiển thị chuỗi Breadcrumb dài dòng.
  - **Main Content (Giữa Workspace):** VÙNG CUỘN DUY NHẤT của trang `flex-1 overflow-y-auto scrollbar-thin`. Mọi thao tác lăn chuột chỉ diễn ra bên trong vùng này, bảo đảm không bao giờ làm dịch chuyển hay hỏng khung Header, Sidebar và Footer.
  - **Footer (Dưới cùng Workspace):** Cố định tuyệt đối `h-8 shrink-0 border-t border-black z-20`. Luôn ghim cố định ở đáy màn hình hiển thị trạng thái hệ thống.
- **Ngăn chặn triệt để Layout Shift:** Khi tải dữ liệu qua API, khi chuyển trang hoặc khi số lượng bản ghi thay đổi, kích thước của khung bao ngoài từng phần tuyệt đối không được giật nảy, phình to hay co rút gây xô lệch giao diện người dùng.
- **Thẻ Card & Lưới dữ liệu:** Mọi Card / Box hiển thị dữ liệu dạng danh sách hoặc lưới (Deck Grid, Kanji Cards, Course Cards) phải khai báo khung cố định đồng nhất (ví dụ: `h-[360px] max-h-[360px]`).
- **Nội dung con độc lập:** Vùng chứa chips, danh sách từ vựng, câu ví dụ hoặc empty states bên trong thẻ phải có kích thước cố định (`h-[155px] max-h-[155px]`) và thuộc tính cuộn nội bộ (`overflow-y-auto`).
- **Menu tùy chọn nổi tách rời (Decoupled Floating Menus):** Menu 3 chấm [⋮], popup menu phải luôn sử dụng `position: absolute; z-index: 50;` và phần tử bọc nút phải có `shrink-0 w-8 h-8`, tuyệt đối không gây dịch chuyển, co dãn hay chèn ép thẻ cha và các phần tử lân cận khi kích hoạt mở menu.

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

---

## 6. Quy Chuẩn Bộ Lọc Dữ Liệu (Filter Controls Convention: Bảng Chọn Dropdown Thay Vì Nút Bấm Trực Tiếp)
- **Bắt buộc hiển thị dạng Bảng Chọn (Select / Dropdown Picker):**
  - Toàn bộ các bộ lọc dữ liệu (Lọc theo cấp độ JLPT, theo chủ đề từ vựng, theo loại từ, theo trạng thái, theo gia sư, theo thời gian) **BẮT BUỘC hiển thị dưới dạng Bảng Chọn (Dropdown Select hoặc Popover Menu)**.
  - Ô chọn hiển thị giá trị hiện tại kèm mũi tên xổ xuống (`▼` hoặc chevron icon) và mở ra danh sách tùy chọn khi được nhấp chọn.
- **Tuyệt đối cấm hiển thị hàng loạt Nút Bấm (`<button>`) trực tiếp:**
  - Nghiêm cấm dàn trải một hàng dài các nút bấm bộ lọc trực tiếp (ví dụ: cấm đặt 5 nút `[Tất cả] [N5] [N4] [N3] [N2] [N1]` dàn ngang hay hàng chục nút chủ đề nằm lộ thiên trên giao diện).
  - Lý do: Việc dàn nút trực tiếp làm tràn màn hình, vỡ layout khi co dãn trên các độ phân giải khác nhau (Mobile/Tablet/Desktop), và gây rối mắt người học.
- **Quy cách Styling Bảng Chọn chuẩn Retro-Brutalist:**
  - Phần tử `<select>` hoặc Custom Dropdown Trigger: `rounded-[5px] border border-black bg-white px-3 py-1.5 text-xs font-itim shadow-[1.5px_1.5px_0px_0px_#0F172A] outline-none cursor-pointer focus:ring-1 focus:ring-black`.
  - Nhãn bộ lọc (Filter Label): Sentence case, font-bold `text-[11px] font-meta text-black flex items-center gap-1`.

---

## 7. Quy Chuẩn Kiến Trúc Tách Biệt Frontend & Backend (Decoupled Client-Server & REST API Architecture)
- **Tách rời hoàn toàn Frontend và Backend (Decoupled Architecture):**
  - Frontend và Backend là hai phân hệ độc lập, chạy trên các tiến trình/cổng riêng biệt (ví dụ: Frontend trên cổng riêng hoặc SPA client độc lập, Backend API Server trên cổng riêng).
  - Không gắn chặt mã nguồn giao diện vào cơ chế dựng giao diện phía máy chủ (Server-Side Rendering tightly coupled) gây phụ thuộc lẫn nhau.
- **Giao tiếp độc quyền qua Chuẩn RESTful API:**
  - Client và Server giao tiếp 100% qua chuẩn JSON RESTful API với phương thức HTTP chuẩn mực (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`).
  - Toàn bộ yêu cầu bảo mật, xác thực danh tính người dùng và phân quyền phải được truyền qua HTTP Header (Bearer Token / JWT Token), không dùng session state bí mật buộc chặt vào view rendering.
- **Phân định trách nhiệm rõ ràng (Separation of Concerns):**
  - **Backend:** Đóng vai trò Data & Logic Engine thuần túy — chịu trách nhiệm quản lý CSDL (PostgreSQL), nghiệp vụ xác thực, phân quyền (RBAC), tính toán AI và cung cấp API JSON chuẩn hóa.
  - **Frontend:** Đóng vai trò Presentation & Interaction Client thuần túy — chịu trách nhiệm render giao diện, quản lý trạng thái hiển thị (UI State), xử lý tương tác người dùng, và gọi dữ liệu qua API client (Fetch/Axios). Cấm nhúng logic nghiệp vụ backend hoặc mã SQL/Entity trực tiếp vào giao diện.

---

## 8. Quy Chuẩn Xử Lý Bộ Lọc Bất Đồng Bộ & Chống Lỗi Rỗng Dữ Liệu (Async Safe Filter & Anti-Flicker Architecture)
- **Cơ chế Hủy Request Cũ (AbortController Pattern):** Mọi thao tác đổi bộ lọc (cấp độ JLPT, danh mục chủ đề, phân trang, từ khóa tìm kiếm) BẮT BUỘC sử dụng `AbortController` để hủy ngay lập tức request đang chạy dở dang trước đó nếu người dùng click/chuyển bộ lọc liên tục. Bắt lỗi `if (err.name === 'AbortError') return;` để không bao giờ ghi đè mảng rỗng hoặc kích hoạt nhầm Empty State.
- **Bộ Nhớ Đệm Tạm Thời (In-Memory Cache):** Duy trì `memoryCache = new Map()` lưu trữ kết quả truy vấn theo khóa `${filterKey}_p${page}_kw${keyword}`. Khi người dùng click qua lại giữa các bộ lọc, lấy trực tiếp từ cache để render ngay trong 0ms, loại bỏ triệt để hiện tượng giật lag, chớp nháy hoặc mất dữ liệu tạm thời.
- **Tự Động Reset Từ Khóa Tìm Kiếm Khi Đổi Filter Chính:** Khi người dùng chuyển sang cấp độ hoặc chủ đề mới, BẮT BUỘC xóa trắng ô input tìm kiếm và đặt biến keyword về rỗng, ngăn ngừa việc từ khóa cũ vô tình lọc sạch dữ liệu của bộ lọc mới.
- **Chuẩn Hóa Kiểu Dữ Liệu So Sánh:** Luôn ép kiểu và chuẩn hóa chuỗi `String(val || '').trim()` trước khi so sánh mảng hoặc gửi tham số lên API, tránh lỗi lệch kiểu dữ liệu giữa String và Number/UUID.
