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

## 4. Backend Feature-Driven Package Architecture (Clean Domain Names)

Khi tạo mới Controller, Service, Repository, Entity hoặc DTO, AI BẮT BUỘC đặt vào đúng package Feature tương ứng theo [BACKEND_FEATURE_ARCHITECTURE.md](docs/guidelines/BACKEND_FEATURE_ARCHITECTURE.md):
- `com.kizuna.core`: Hạ tầng dùng chung (config, security, exception, util, base entity, user, role).
- `com.kizuna.features.auth`: Authentication, OTP, OAuth2, Email verification.
- `com.kizuna.features.profile`: Quản lý hồ sơ Learner & Tutor.
- `com.kizuna.features.verification`: Xét duyệt bằng cấp & lý lịch gia sư.
- `com.kizuna.features.tutor_search`: Tìm kiếm & kết nối gia sư.
- `com.kizuna.features.booking`: Đặt lịch học thử & kích hoạt buổi học.
- `com.kizuna.features.classroom`: Quản lý lớp học, thư mục Kanji, tài liệu.
- `com.kizuna.features.progress`: Bài tập, nộp bài, chấm điểm & tiến độ.
- `com.kizuna.features.learning_support`: Kanji practice, Minigame, Flashcards, AI Memory.
- `com.kizuna.features.payment`: Thanh toán gói hội viên, VNPay gateway.
- `com.kizuna.features.ticket`: Hỗ trợ kỹ thuật, CSKH & khiếu nại.
- `com.kizuna.features.admin`: Quản trị hệ thống, tài khoản nhân viên & báo cáo.
- `com.kizuna.features.notification`: Quản lý và phát thông báo hệ thống.

---

## 5. Quy tắc Git & Quản trị nhánh (Strict Local Only - Không tự ý Push)

- **TUYỆT ĐỐI KHÔNG TỰ Ý CHẠY `git push` LÊN REMOTE:** Mọi thay đổi hoặc commit chỉ được thực hiện ở máy cục bộ (Local). AI tuyệt đối không bao giờ được tự ý thực hiện lệnh `git push` lên GitHub / GitLab / Remote server. Việc đẩy code lên remote hoàn toàn do người dùng trực tiếp kiểm tra và tự gõ lệnh push.


