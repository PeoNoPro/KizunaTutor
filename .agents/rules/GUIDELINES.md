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
1. **Design System:** Tuân thủ triệt để [DESIGN.md](DESIGN.md) và hệ quy chuẩn **Shodo Neo-Brutalist** chuẩn hóa từ trang `/handwriting`:
   - **Phông chữ:**
     - Văn bản, tiêu đề UI: `"Inter"`, `"Plus Jakarta Sans"`, sans-serif. (Tuyệt đối không dùng phông viết tay `Itim` gây lệch tính học thuật).
     - Chữ Hán Kanji & Kana nét bút: `"Noto Serif JP"`, `"Klee One"`, serif.
   - **Bo góc đồng nhất:** Thẻ nội dung chính dùng `rounded-xl` (12px), khung Canvas dùng `rounded-[8px]`, nút bấm dùng `rounded-lg`.
   - **Đường viền và bóng đổ:** Viền mực đen `border-2 border-[#0F172A]` cho thẻ, `border border-[#0F172A]` cho nút bấm; bóng đổ khối cứng `shadow-[3px_3px_0px_0px_#0F172A]` cho thẻ, `shadow-[2px_2px_0px_0px_#0F172A]` cho nút chính.
   - **Bảng màu nút bấm:**
     - Nút hành động chính (Primary CTA): Nền Đỏ Hồng Trầm `bg-[#9F1239]` (hover `#BE185D`), chữ trắng, viền `#0F172A`, bóng `shadow-[2px_2px_0px_0px_#0F172A]`.
     - Nút phụ (Secondary): Nền Hồng Anh Đào `bg-[#FFE5EC]`, chữ `#831843`, viền `#FBCFE8`.
     - Nút viền mảnh (Outline): Nền trắng `bg-white`, viền `#0F172A`, chữ `#0F172A`.
   - **Khóa chặt khổ rộng:** Toàn bộ trang học tập dùng duy nhất `max-w-6xl mx-auto p-4 sm:p-6`.
2. **Master Shell Layout:**
   - Mỗi phân hệ (Admin, Tutor, Learner, CS Staff, Manager, Academic Officer) có một Master Shell hoàn chỉnh gồm Header, Sidebar phân nhóm rõ ràng và Compact Footer.
   - Khi chuyển trang bên trong phân hệ, tuyệt đối không được làm mất Header/Sidebar hoặc điều hướng ra trang ngoài gây lệch layout.
3. **Trạng Thái Rỗng (Empty States):**
   - Khi chưa có dữ liệu, hiển thị hộp rỗng thân thiện kèm icon và lời nhắn khích lệ, tuyệt đối không dùng mảng giả hardcoded.

---

## 3. Tiêu Chuẩn Backend (Spring Boot & PostgreSQL)
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

## 4. Quy Trình Kiểm Thử Tự Động (Automated Quality Assurance)
- Mọi pull request hoặc commit phải đảm bảo vượt qua toàn bộ bộ kiểm thử tự động `mvnw test`.
- Các route giao diện của 6 Actor bắt buộc có bài test kiểm tra HTTP status 200 trong `ActorSpecificViewsTest.java`.

---

## 5. Kỷ Luật Phạm Vi Sửa Đổi (Strict Scope Boundary)
- **Không tự ý sửa phần khác:** Chỉ tác động đúng thành phần và phạm vi mà người dùng yêu cầu. Tuyệt đối không tự ý sửa đổi, tái cấu trúc hoặc can thiệp vào các khu vực khác khi chưa được phép.
- **Quy tắc khi xóa thành phần:** Khi người dùng yêu cầu xóa hoặc thu gọn một chi tiết/nút bấm, chỉ thực hiện thao tác xóa tại đúng vị trí đó; tuyệt đối không tự ý di chuyển các nút sang chỗ khác hoặc tự thêm các tính năng/thành phần mới vào màn hình nếu chưa được người dùng cho phép.
