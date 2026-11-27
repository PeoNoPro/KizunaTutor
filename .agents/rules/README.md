# KizunaTutor

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

**Japanese Language Tutor Matching and Learning Support System**

## Overview

KizunaTutor is a platform that connects Japanese language learners
with tutors and provides online learning support.

## Features

- Authentication and Access Control (OTP registration, Google OAuth2, first-time password change)
- User Administration (Multi-role management: Admin, Manager, CS Staff, Academic Officer, Tutor, Learner)
- Tutor Onboarding & Verification
- Tutor Search and Matching
- Trial Lesson Booking
- Online Learning Support (Kanji Practice, Flashcards, Minigame)
- Contract and Payment Management
- AI-powered Learning Support (Vector search, handwriting recognition)

## Guidelines & System Specifications

- [AGENTS.md](AGENTS.md): Coding guidelines and system rules for AI assistants and engineers.
- [DESIGN.md](DESIGN.md): Design tokens, typography, Kanji practice grid, and anti-slop principles.
- [UI_LOCALIZATION_RULES.md](UI_LOCALIZATION_RULES.md): Strict Vietnamese localization and UI copywriting standards.

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
