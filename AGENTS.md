# Agent & Coding Guidelines for KizunaTutor

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

This document provides system-level guidance for all AI assistants (IntelliJ AI Assistant, Cursor, Claude Code, Copilot, Antigravity) working on the **KizunaTutor** codebase.

---

## 1. Design & UI Guidelines (from `pbakaus/impeccable` & `Leonxlnx/taste-skill`)

When creating or modifying frontend components, pages, or templates:
1. **Source of Truth**: Always respect [DESIGN.md](DESIGN.md) and [UI_LOCALIZATION_RULES.md](UI_LOCALIZATION_RULES.md).
2. **Palette Compliance**:
   - Primary: Indigo Aizome (`#1B2A4A`)
   - Secondary: Sakura Pink (`#FAD0C4`)
   - Accent/Success: Matcha Green (`#88A064`)
   - Background: Washi Off-white (`#FAF9F6`)
   - Text: Sumi Charcoal (`#1F2421`)
3. **Anti-Slop Protocol**:
   - Do NOT use generic purple AI gradients.
   - Do NOT output repetitive 3-column card layouts without semantic hierarchy.
   - Respect Japanese typography rules: ensure `font-family` supports Japanese glyphs (`"Noto Sans JP"`, `"Zen Kaku Gothic New"`).
   - Kanji practice canvases must strictly align with the `109x109` KanjiVG coordinate system.

---

## 2. Backend & Architecture Guidelines (Spring Boot + PostgreSQL)

1. **Database Schema**:
   - Sourced from [DatabasePgAdmin4.txt](DatabasePgAdmin4.txt) and [Database_Minigame_KanjiFolder.sql](Database_Minigame_KanjiFolder.sql).
   - Entities must use standard Spring Data JPA annotations (`@Entity`, `@Table`, `@Id`, UUID keys).
2. **AI Memory Microservice**:
   - Use `com.kizuna.service.ai.AgentMemoryService` to communicate with the `agent-memory` Docker container on port `8081`.
3. **Kanji Asset Integration**:
   - Kanji data is sourced from `data/kanjivg/kanji` and managed via `KanjiDataSeeder`.

---

## 3. Strict UI Localization & Human-First Rule (BẮT BUỘC TUÂN THỦ)

1. **100% TIẾNG VIỆT CHO NGƯỜI DÙNG**:
   - Tất cả nội dung hiển thị cho người học (tiêu đề, nút bấm, nhãn, hướng dẫn, hộp thoại, thông báo, cảnh báo, thống kê) **BẮT BUỘC 100% BẰNG TIẾNG VIỆT**.
   - Ngoại lệ duy nhất: Mặt chữ Kanji, Kana (Hiragana/Katakana), Romaji và bậc JLPT (N5-N1) trong nội dung học tập.
2. **TUYỆT ĐỐI KHÔNG ĐỂ LỘ MÃ CODE HOẶC DATABASE**:
   - Không hiển thị tên biến, tên hàm, tên class, trường database (như ID, UUID, entity, session, request, response, DTO, camelCase, snake_case...).
   - Không dùng tiếng Anh thay thế khi đã có từ tiếng Việt chuẩn xác (Deadline -> Vạch ranh giới nguy hiểm; Game Over -> Kết thúc ván đấu; You Win -> Chiến thắng xuất sắc; Accuracy -> Độ chính xác; Combo -> Chuỗi gõ liên hoàn).
3. **MÃ HÓA UTF-8 TUYỆT ĐỐI**:
   - Tất cả các file HTML, JS, CSS, Java phải được lưu dưới chuẩn UTF-8 không BOM để chống mọi lỗi biến dạng font chữ (Mojibake).

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

## 4. Minimalist Engineering & Lazy Senior Dev Protocol (from `DietrichGebert/ponytail`)

Áp dụng toàn bộ triết lý cốt lõi từ bộ skill **Ponytail** (`.skills/ponytail`) để tối ưu hóa hiệu suất, tránh phình to mã nguồn và giữ codebase KizunaTutor luôn trong sáng, dễ bảo trì:

1. **The Best Code is the Code Never Written (KISS & YAGNI)**:
   - Luôn chọn giải pháp có **diff nhỏ nhất** giải quyết trọn vẹn và an toàn bài toán được giao.
   - Tuyệt đối không thêm abstraction, generic wrapper, pattern phức tạp, option hoặc tham số cấu hình "dành cho tương lai" mà chưa có yêu cầu thực tế.
2. **Kế thừa và Tái sử dụng tối đa (Reuse First)**:
   - Trước khi tạo mới bất kỳ class/hàm/thư viện nào, hãy kiểm tra codebase KizunaTutor trước:
     - Tận dụng các helper/service hiện có (Spring Data JPA repositories, `AgentMemoryService`, `KanjiDataSeeder`).
     - Tận dụng Design tokens và class CSS có sẵn trong `design-system.css`, HTML5 native elements (date picker, dialog, form validation).
     - Tuyệt đối không cài thêm dependency bên ngoài chỉ để giải quyết vài dòng code.
3. **Sửa dứt điểm nguyên nhân gốc rễ (Root Cause over Patches)**:
   - Khi sửa lỗi (bugfix): Grep toàn bộ caller của hàm/method liên quan trước khi sửa, và sửa dứt điểm tại mã dùng chung.
   - Tối thiểu hóa diff nhưng không được sơ sài về mặt phạm vi: cập nhật đầy đủ caller, DTO, mapper và test liên quan bị ảnh hưởng.
4. **Tuyệt đối KHÔNG ĐƯỢC CẮT BỎ (Never Cut)**:
   - **3 Nguyên tắc bảo vệ hệ thống bất biến** ở đầu tài liệu (Zero Backend Leaks, No ALL-CAPS UI, No Fake Mock Data).
   - Kiểm tra hợp lệ dữ liệu (Validation) tại các biên tin cậy (Request Body, DTO, Params).
   - Xử lý lỗi (Error handling) ngăn ngừa mất mát dữ liệu hoặc rò rỉ mã lỗi hệ thống.
   - Quy chuẩn bảo mật & phân quyền 6 Actor (Admin, Manager, CS Staff, Academic Officer, Tutor, Learner).
5. **Kiểm thử cho logic phi tầm thường (Test Risky Logic)**:
   - Bất kỳ thay đổi logic tính toán, rẽ nhánh phức tạp hoặc thuật toán nghiệp vụ mới (như tính điểm Kanji, logic xếp lịch gia sư) phải đi kèm ít nhất 1 bài test bảo vệ.
6. **Minh bạch phạm vi & Rủi ro**:
   - Khi hoàn thành tác vụ, luôn nêu rõ: phần việc đã hoàn thành, những gì chưa kiểm tra / bỏ qua và rủi ro tiềm ẩn cần lưu ý.
7. **Bộ kỹ năng & Lệnh Slash Commands hỗ trợ**:
   - Tích hợp 6 module từ `.skills/ponytail`:
     - `/ponytail` (Chế độ Senior Dev tối giản)
     - `/ponytail-review` (Rà soát code review, phát hiện lỗi tiềm ẩn)
     - `/ponytail-audit` (Kiểm toán tính tinh gọn và rủi ro)
     - `/ponytail-debt` (Phát hiện và dọn dẹp nợ kỹ thuật)
     - `/ponytail-gain` (Đánh giá mức độ tối ưu sau thay đổi)
     - `/ponytail-help` (Hướng dẫn sử dụng các chế độ)

