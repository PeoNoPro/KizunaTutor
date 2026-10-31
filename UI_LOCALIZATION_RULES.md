# QUY TẮC HIỂN THỊ GIAO DIỆN & BẢN ĐỊA HÓA (UI LOCALIZATION GUIDELINES)
**Dự án KizunaTutor - Hệ Thống Kết Nối Gia Sư & Luyện Chữ Hán Thông Minh**

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

> ⚠️ **ĐIỀU KHOẢN BẮT BUỘC (STRICT PROTOCOL):**
> Mọi trợ lý AI (Antigravity, Cursor, Claude Code, Copilot, ChatGPT), lập trình viên và người bảo trì hệ thống **BẮT BUỘC** phải tuân thủ nghiêm ngặt các nguyên tắc dưới đây trong mọi file HTML, Template Thymeleaf, JavaScript, CSS, Thông báo API và Dữ liệu trả về cho người dùng.

---

## 1. NGUYÊN TẮC "100% TIẾNG VIỆT THÂN THIỆN CHO HỌC VIÊN"
- Toàn bộ giao diện người dùng (User Interface) bao gồm: tiêu đề trang, nhãn (labels), nút bấm (buttons), gợi ý (placeholders), hộp thoại (modals/dialogs), bảng biểu, tooltip, cảnh báo (alerts), thống kê phân tích và thông điệp trạng thái **PHẢI HOÀN TOÀN BẰNG TIẾNG VIỆT**.
- Giọng văn: Chu đáo, sư phạm, tôn trọng người học, chuẩn văn hóa giáo dục Nhật - Việt.
- **Ngoại lệ duy nhất:** Mặt chữ Hán (Kanji), cách đọc Kana (Hiragana/Katakana), Romaji và cấp độ JLPT (N5 - N1) thuộc nội dung học tập chuyên môn. Các nhãn giải thích, tiêu đề đi kèm bắt buộc phải dùng Tiếng Việt.

---

## 2. TUYỆT ĐỐI KHÔNG ĐỂ LỘ THUẬT NGỮ KỸ THUẬT & CODE RA NGOÀI GIAO DIỆN (NO BACKEND LEAKS ON UI)
- **Cấm hoàn toàn:** Không để lộ tên biến code, mã enum, tên ngoại lệ, tên hàm, tên class, cấu trúc bảng database, phương thức HTTP, định dạng camelCase hay snake_case ra ngoài màn hình:
  - ❌ *Không dùng:* `id`, `uuid`, `userId`, `kanjiId`, `folder_items`, `session`, `play-data`, `request`, `response`, `DTO`, `entity`, `API`, `endpoint`, `ROLE_ADMIN`, `ROLE_LEARNER`, `NullPointerException`, `BadCredentialsException`, `500 INTERNAL_SERVER_ERROR`...
  - ✅ *Chuyển thành:* `Mã nhận diện`, `Bộ thẻ học tập`, `Phiên luyện tập`, `Dữ liệu bài học`, `Thông tin chi tiết`, `Quản trị viên`, `Học viên`...
- Thông báo lỗi hiển thị cho học viên phải là ngôn ngữ tự nhiên:
  - ❌ *Không dùng:* `500 Internal Server Error`, `Failed to fetch`, `NullPointerException`, `BadCredentialsException`
  - ✅ *Chuyển thành:* `Hệ thống đang bận, vui lòng thử lại sau ít phút`, `Email hoặc mật khẩu không chính xác`, `Đã có lỗi xảy ra trong quá trình xử lý`

---

## 3. BẢNG QUY ĐỔI THUẬT NGỮ TIẾNG ANH $\rightarrow$ TIẾNG VIỆT CHUẨN KIZUNA

| Thuật ngữ tiếng Anh | Tuyệt đối KHÔNG hiển thị | Hiển thị 100% TIẾNG VIỆT Chuẩn Sư Phạm |
| :--- | :--- | :--- |
| **Kanji Rain Typing** | Rain Typing / Minigame | **Mưa Chữ Kanji / Trò Chơi Luyện Phản Xạ Chữ Hán** |
| **Deadline** | DEADLINE | **Vạch ranh giới nguy hiểm (Chạm đáy sẽ dừng ván)** |
| **Game Over** | GAME OVER | **Kết thúc ván đấu (Chưa vượt qua thử thách)** |
| **You Win** | YOU WIN / VICTORY | **Chiến thắng xuất sắc!** |
| **Accuracy** | Accuracy % | **Độ Chính Xác (%)** |
| **Words Typed** | Words Typed Count | **Số Từ Bắn Trúng / Số Chữ Hoàn Thành** |
| **Score** | Score | **Điểm Số Đạt Được / Điểm Tích Lũy** |
| **Combo** | Combo Streak | **Chuỗi Gõ Liên Hoàn** |
| **Timer / Duration** | Timer / Duration | **Thời Gian Còn Lại / Thời Lượng Ván Đấu** |
| **Speed Multiplier** | Speed: x1.10 | **Nhịp Rơi: Mức 1.1x (Cứ 20 giây tăng 10%)** |
| **Live Typing / Echo** | Live Echo Bar | **Ký Tự Bạn Đang Nhập** |
| **Pre-Game Config** | Game Config Modal | **Thiết Lập Ván Học Tập** |
| **Folder Manager** | Folder Manager | **Quản Lý Bộ Thẻ Học Tập** |
| **Practice / Library** | Practice / Library | **Luyện Viết Nét / Danh Sách Chữ Hán** |
| **Rank Titles** | Kanji Master, Samurai | **Bậc Thầy Chữ Hán 🏆, Cao Thủ Gõ Phím ⚔️, Học Giả Tiến Bộ 📜, Tân Binh Luyện Chữ 🌸** |

---

## 4. QUY CHUẨN HIỂN THỊ VĂN BẢN TRÊN GIAO DIỆN (UI COPYWRITING STANDARDS)

### 4.1. Bảng chuyển ngữ thuật ngữ Backend sang Giao diện
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

### 4.2. Quy chuẩn viết hoa (Casing Rules)
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

## 5. BẢO ĐẢM MÃ HÓA UTF-8 TUYỆT ĐỐI (CHỐNG LỖI FONT MOJIBAKE)
- Mọi tệp `.html`, `.js`, `.css`, `.properties`, `.java`, `.sql`, `.json` đều phải được lưu trữ và đọc dưới chuẩn mã hóa **UTF-8 (không có BOM)**.
- Khi biên dịch Maven và chạy Spring Boot, thuộc tính `server.servlet.encoding.charset=UTF-8` và `server.servlet.encoding.force=true` luôn được kích hoạt.
- Tuyệt đối không dùng các script PowerShell với mã hóa mặc định ANSI/Windows-1252 để ghi đè chuỗi tiếng Việt.

---

## 6. QUY CHUẨN CỐ ĐỊNH KHUNG TỪNG PHÂN KHU (SECTION FIXED FRAME CONTAINMENT & ZERO LAYOUT SHIFT)
- **Cố định khung bao ngoài từng phân khu:** Toàn bộ Header, Sidebar, Toolbar, Card Grid, Chi tiết bài học, Drawer và Phân trang phải có kích thước/chiều cao cố định (`fixed height/min-height/max-height`) và phân vùng cuộn độc lập (`overflow-y-auto scrollbar-thin`).
- **Ngăn chặn triệt để Layout Shift:** Khi nạp dữ liệu động từ API hoặc khi thay đổi số lượng bản ghi hiển thị, khung phân khu tuyệt đối không được giật nảy, phình to hay co rút gây xô lệch giao diện người dùng.

---

## 7. QUY CHUẨN BỘ LỌC DỮ LIỆU: BẢNG CHỌN DROPDOWN THAY VÌ NÚT BẤM TRỰC TIẾP
- **Bắt buộc hiển thị dạng Bảng Chọn (Select / Dropdown Picker):** Toàn bộ các bộ lọc phân loại dữ liệu (Cấp độ JLPT, Chủ đề từ vựng/ngữ pháp, Loại từ, Trạng thái) BẮT BUỘC hiển thị dưới dạng Bảng Chọn (Dropdown `<select>` hoặc Popover Menu) kèm mũi tên xổ xuống (`▼`).
- **Tuyệt đối cấm dàn hàng Nút Bấm (`<button>`) trực tiếp:** Cấm bố trí một hàng nút bấm trực tiếp lộ thiên gây tràn màn hình, vỡ dòng hoặc phình to thanh công cụ khi chuyển đổi giữa các thiết bị.

---

## 8. QUY CHUẨN KIẾN TRÚC TÁCH BIỆT FRONTEND & BACKEND CHẠY RIÊNG (DECOUPLED RESTFUL ARCHITECTURE)
- **Tách biệt hoàn toàn Client và Server:** Frontend và Backend vận hành trên hai tiến trình/cổng riêng biệt, không gắn chặt chu trình sống và loại bỏ phụ thuộc SSR.
- **Giao tiếp độc quyền qua Chuẩn RESTful API (JSON):** Tầng Client chỉ gọi dữ liệu từ Tầng Backend thông qua các Endpoint RESTful API tiêu chuẩn (`GET`, `POST`, `PUT`, `DELETE`) với payload JSON UTF-8 và xác thực bằng Bearer Token Header.

---
*Văn bản này được ban hành cho toàn bộ các dịch vụ và thành phần trong hệ sinh thái KizunaTutor.*
