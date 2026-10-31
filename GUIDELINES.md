# KizunaTutor System Guidelines (GUIDELINES.md)

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

## 1. Mục Đích & Phạm Vi Tài Liệu
Tài liệu này xác lập các tiêu chuẩn phát triển phần mềm cho toàn bộ các thành viên tham gia phát triển dự án KizunaTutor (Developers, Tech Leads, QA và các tác nhân AI Code Assistants).

---

## 2. Tiêu Chuẩn Frontend & Trải Nghiệm Người Dùng (UI/UX)
1. **Design System:** Tuân thủ triệt để [DESIGN.md](DESIGN.md) theo chuẩn nhaitopik.com:
   - Font heading: `Itim`, cursive.
   - Font body: `Segoe UI`, sans-serif.
   - Bo góc đồng nhất: Duy nhất `rounded-[5px]`. Không dùng bo góc lớn (`rounded-xl`, `rounded-2xl`).
   - Đường viền và bóng đổ: Viền đen mảnh `border border-black`, bóng đổ khối cứng `shadow-[4px_4px_0px_0px_#000000]`.
2. **Master Shell Layout 3 Phân Khu Cố Định (Fixed Header, Sidebar & Footer Shell):**
   - Thẻ `<html>` và `<body>` BẮT BUỘC khai báo `h-screen overflow-hidden flex flex-col m-0 p-0`. Tuyệt đối cấm dùng `min-h-screen` ở body làm phình to cửa sổ trình duyệt gây trôi trượt các thanh điều hướng.
   - Mỗi phân hệ (Admin, Tutor, Learner, CS Staff, Manager, Academic Officer) có một Master Shell hoàn chỉnh:
     - **Header cố định & Loại bỏ Breadcrumbs:** Với phân hệ Học viên (Learner), đồng bộ sử dụng Header `<header th:replace="~{learner/layout/header :: header}"></header>` (`md:hidden h-11`). Trên Desktop hoàn toàn không hiển thị thanh header độc lập (không Breadcrumbs, không nút phụ dàn ngang), trả lại 100% diện tích cho nội dung học tập. Với các phân hệ khác, Header cố định `h-14` và không chứa Breadcrumbs dài dòng.
     - **Sidebar cố định:** `w-60 min-w-[15rem] max-w-[15rem] h-full shrink-0 border-r border-black overflow-y-auto z-30` không bao giờ co dãn hay cuộn theo trang.
     - **Footer cố định:** `h-8 shrink-0 border-t border-black z-20` luôn ghim đáy trang.
     - **Main Content:** VÙNG CUỘN DUY NHẤT `flex-1 overflow-y-auto scrollbar-thin`. Mọi thao tác cuộn chuột chỉ xảy ra trong vùng này, ngăn chặn 100% hiện tượng vỡ khung, trượt khung.
   - Khi chuyển trang bên trong phân hệ, tuyệt đối không được làm mất Header/Sidebar/Footer hoặc điều hướng ra trang ngoài gây lệch layout.
3. **Trạng Thái Rỗng (Empty States):**
   - Khi chưa có dữ liệu, hiển thị hộp rỗng thân thiện kèm icon và lời nhắn khích lệ, tuyệt đối không dùng mảng giả hardcoded.
4. **Quy Tắc Cố Định Khung Từng Phần & Chống Co Dãn Bố Cục (Section Fixed Frame Containment & Zero Layout Shift):**
   - Toàn bộ các phân khu chính trên trang (Header, Sidebar, Toolbar/Filter Bar, Lưới thẻ Card Grid, Drawer/Inspector chi tiết, Thanh phân trang) BẮT BUỘC phải khai báo khung cố định (`fixed height`, `min-h-[...]`, `max-h-[...]`) và phân định vùng cuộn độc lập (`overflow-y-auto scrollbar-thin`).
   - Tuyệt đối ngăn chặn hiện tượng giật nảy, xô lệch layout (Layout Shift) khi tải dữ liệu động từ API hoặc khi thay đổi số lượng bản ghi hiển thị.
   - Mọi ô box thẻ bài/thư mục (Flashcard Decks, Kanji Cards, Course Cards) phải có kích thước và chiều cao cố định đồng nhất (`h-[360px] max-h-[360px]`).
   - Khung chứa nội dung con (danh sách Hán tự, từ vựng ví dụ, empty state) phải cố định chiều cao (`h-[155px] max-h-[155px] overflow-y-auto`), không được làm phình to hay co dãn khung cha.
   - Menu tùy chọn 3 chấm [⋮] phải định vị tuyệt đối (`position: absolute; z-index: 50; shrink-0`) nổi độc lập trên bề mặt, cấm làm xô lệch hoặc co kéo các nút flex lân cận.
5. **Hệ Thống Phản Hồi Không Làm Phiền & Chống Lệch Bố Cục (Non-blocking & Zero Layout Shift Feedback):**
   - Phân tách rõ ràng giữa Modal tương tác (`KizunaDialog`) và Toast thông báo trạng thái thụ động (`KizunaToast`).
   - Toàn bộ thông báo CRUD, lưu dữ liệu, xóa tạm thời hoặc copy link bắt buộc hiển thị dạng Toast nổi ở góc dưới bên phải (`fixed bottom-5 right-5 z-[9999]`), tự động ẩn sau 3.5s và không chặn thao tác click/scroll của người dùng.
   - Cấm chèn khoảng trống hay ô alertbox tĩnh trực tiếp vào giữa luồng giao diện làm đẩy lùi, co dãn hay xô lệch các phần tử danh sách/lưới bên dưới.
6. **Kiểm Soát Tỷ Lệ Bộ Lọc & Tối Giản Công Cụ (Compact Controls & Streamlined Toolbars):**
   - Ô nhập số nét trên thanh lọc Hán tự và Flashcards bắt buộc duy trì chiều rộng gọn gàng `w-20` (80px) để bảo toàn diện tích cho các bộ lọc Bộ thủ và Cấp độ JLPT.
   - Thanh công cụ Shodo Canvas (`practice-canvas.html`) chỉ duy trì các nút chức năng cốt lõi, nút phát hướng dẫn tự đổi trạng thái thành "Phát Lại" sau khi hoàn thành nét vẽ, loại bỏ triệt để các nút bấm trùng lặp gây rối mắt.
7. **Quy Chuẩn Bộ Lọc Dữ Liệu: Bảng Chọn Dropdown Thay Vì Nút Bấm Trực Tiếp (Filter Controls Pattern):**
   - Toàn bộ các bộ lọc phân loại dữ liệu (Cấp độ JLPT, Chủ đề từ vựng/ngữ pháp, Loại từ, Trạng thái phân loại) BẮT BUỘC hiển thị dưới dạng Bảng Chọn (Dropdown Select hoặc Popover Menu) với biểu tượng mũi tên mở danh sách (`▼`).
   - Tuyệt đối cấm hiển thị hàng loạt nút bấm `<button>` trực tiếp dàn hàng ngang/dọc gây tràn màn hình, vỡ layout và xô lệch giao diện trên các kích thước màn hình khác nhau.
   - Styling chuẩn Retro-Brutalist: `rounded-[5px] border border-black bg-white px-3 py-1.5 text-xs font-itim shadow-[1.5px_1.5px_0px_0px_#0F172A] outline-none cursor-pointer focus:ring-1 focus:ring-black`.

---

## 3. Tiêu Chuẩn Kiến Trúc Tách Biệt Frontend & Backend Chạy Riêng (Decoupled Client-Server & REST API Architecture)
1. **Kiến Trúc Tách Rời Hoàn Toàn (Decoupled Architecture):**
   - Frontend và Backend là hai thực thể độc lập hoàn toàn, chạy trên các tiến trình/cổng riêng biệt (ví dụ: Frontend Client và Backend API Server chạy độc lập).
   - Tuyệt đối không gắn chặt giao diện vào cơ chế Server-Side Rendering (SSR coupling). Giao diện client chủ động render và điều phối trạng thái, không phụ thuộc vào chu trình render phía máy chủ.
2. **Giao Tiếp Độc Quyền Qua RESTful API:**
   - 100% việc trao đổi dữ liệu giữa Frontend và Backend thực hiện qua các Endpoint RESTful API chuẩn (`GET`, `POST`, `PUT`, `DELETE`).
   - Định dạng payload chuẩn hóa: JSON UTF-8 (`application/json`).
   - Xác thực và phân quyền dựa trên HTTP Header Bearer Token (JWT), duy trì máy chủ Backend ở trạng thái Stateless.
3. **Phân Định Trách Nhiệm (Separation of Concerns):**
   - **Backend:** Đảm nhiệm xử lý nghiệp vụ, xác thực, phân quyền (RBAC), tính toán thuật ngữ AI, quản lý giao dịch CSDL (PostgreSQL).
   - **Frontend:** Đảm nhiệm hiển thị giao diện theo Design System, cố định khung layout, hiển thị bảng chọn filter, và gọi API bất đồng bộ qua REST Client (Fetch/Axios). Cấm nhúng logic nghiệp vụ backend trực tiếp vào mã nguồn client.
4. **Xử Lý Bộ Lọc An Toàn (Async Safe Filter & Anti-Flicker Architecture):**
   - Mọi thao tác lọc dữ liệu bất đồng bộ bắt buộc sử dụng `AbortController` hủy các request cũ khi chuyển filter liên tục để triệt tiêu Race Condition.
   - Duy trì `MemoryCache` nạp tức thời khi chuyển lại bộ lọc cũ (0ms latency, zero flicker).
   - Tự động xóa trắng ô tìm kiếm khi đổi danh mục hoặc cấp độ mới để chống xung đột điều kiện lọc.

---

## 4. Tiêu Chuẩn Backend (Spring Boot & PostgreSQL)
1. **Kiến Trúc Đa Tầng:**
   - Controller $\rightarrow$ Service $\rightarrow$ Repository $\rightarrow$ Database Entity.
   - Controller chỉ đảm nhiệm nhận request, validation và điều phối view hoặc response DTO.
2. **Xử Lý Ngoại Lệ & Bảo Mật:**
   - Bắt và chuyển hóa toàn bộ Runtime Exceptions qua `GlobalExceptionHandler`.
   - Phản hồi lỗi API gửi về client luôn ở dạng chuỗi thân thiện, không bao giờ lộ Exception Class hoặc Stack Trace.
3. **Truy Vấn Dữ Liệu:**
   - Sử dụng `LEFT JOIN FETCH` khi cần nạp quan hệ `@OneToOne` hoặc `@ManyToOne` để chống triệt để lỗi Hibernate `LazyInitializationException`.
   - Với PostgreSQL custom Enum types, tránh so sánh chuỗi thô trong JPQL `@Query` để ngăn ngừa lỗi `PSQLException: type does not exist`.

---

## 5. Quy Trình Kiểm Thử Tự Động (Automated Quality Assurance)
- Mọi pull request hoặc commit phải đảm bảo vượt qua toàn bộ bộ kiểm thử tự động `mvnw test`.
- Các route giao diện của 6 Actor bắt buộc có bài test kiểm tra HTTP status 200 trong `ActorSpecificViewsTest.java`.
