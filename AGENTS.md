# Agent & Coding Guidelines for KizunaTutor

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
