# BẢNG ĐẶC TẢ CHI TIẾT CÁC CA KIỂM THỬ (UNIT TEST SPECIFICATION)
**Dự án: Kizuna Tutor System (KTS) – Mã tài liệu: Report_5.1_UnitTest_v1.0**  
*Cập nhật ngày: 16/11/2026 | Người thực hiện: Hoangvm | Kết quả: 60/60 Passed (100%)*

---

## TỔNG HỢP THEO CHỨC NĂNG (TEST REPORT SUMMARY)

| STT | Mã chức năng | Đạt (Passed) | Hỏng (Failed) | Chưa test | Ca thông thường (N) | Ca bất thường (A) | Ca biên (B) | Tổng số ca test |
| :---: | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 1 | `login` | 10 | 0 | 0 | 6 | 3 | 1 | 10 |
| 2 | `googleAuth` | 3 | 0 | 0 | 1 | 2 | 0 | 3 |
| 3 | `sendOtp` | 3 | 0 | 0 | 1 | 2 | 0 | 3 |
| 4 | `verifyOtp` | 4 | 0 | 0 | 2 | 2 | 0 | 4 |
| 5 | `resendOtp` | 3 | 0 | 0 | 1 | 1 | 1 | 3 |
| 6 | `logout` | 2 | 0 | 0 | 1 | 0 | 1 | 2 |
| 7 | `changePassword` | 4 | 0 | 0 | 1 | 3 | 0 | 4 |
| 8 | `resetPassword` | 7 | 0 | 0 | 2 | 4 | 1 | 7 |
| 9 | `flashcards` | 7 | 0 | 0 | 4 | 3 | 0 | 7 |
| 10 | `vocabularies` | 3 | 0 | 0 | 2 | 0 | 1 | 3 |
| 11 | `grammar` | 3 | 0 | 0 | 2 | 1 | 0 | 3 |
| 12 | `aiWriting` | 3 | 0 | 0 | 2 | 1 | 0 | 3 |
| 13 | `kanjiGame` | 3 | 0 | 0 | 2 | 0 | 1 | 3 |
| 14 | `readingAssistant` | 5 | 0 | 0 | 3 | 1 | 1 | 5 |
| **Tổng** | **14 Chức năng** | **60** | **0** | **0** | **30 (50.0%)** | **23 (38.3%)** | **7 (11.7%)** | **60** |

---

## CHI TIẾT TỪNG CHỨC NĂNG KIỂM THỬ

### 1. `login` – AuthService.login
- **Yêu cầu:** Đăng nhập hệ thống bằng email và mật khẩu, cấp JWT token & cookie, điều hướng 6 vai trò, kiểm tra cờ mustChangePassword.
- **Tổng ca test:** 10 (Passed: 10, Failed: 0, N: 6, A: 3, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 | UTCID08 | UTCID09 | UTCID10 |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Precondition** | Email tồn tại trong DB, ACTIVE | O | O | O | O | O | O | O | | O | |
| | Email không tồn tại trong DB | | | | | | | | O | | |
| | Email tồn tại, BANNED / INACTIVE | | | | | | | | | | O |
| **User Role** | ROLE_ADMIN | O | | | | | | | | | |
| | ROLE_MANAGER | | O | | | | | | | | |
| | ROLE_CS_STAFF | | | O | | | | | | | |
| | ROLE_ACADEMIC_OFFICER | | | | O | | | | | | |
| | ROLE_TUTOR | | | | | O | | | | | |
| | ROLE_LEARNER | | | | | | O | O | | O | |
| **Flag** | mustChangePassword = false | O | O | O | O | O | O | | | | |
| | mustChangePassword = true | | | | | | | O | | | |
| **Input** | Mật khẩu khớp với hash CSDL | O | O | O | O | O | O | O | | | O |
| | Mật khẩu nhập sai | | | | | | | | | O | O |
| **Confirm** | Chuyển hướng Dashboard Admin | O | | | | | | | | | |
| | Chuyển hướng Dashboard Manager | | O | | | | | | | | |
| | Chuyển hướng Dashboard CS Staff | | | O | | | | | | | |
| | Chuyển hướng Dashboard Academic | | | | O | | | | | | |
| | Chuyển hướng Dashboard Tutor | | | | | O | | | | | |
| | Chuyển hướng /pages/home/ (Learner) | | | | | | O | O | | | |
| | Cấp JWT Token & Cookie access_token | O | O | O | O | O | O | O | | | |
| | mustChangePassword = true trong response | | | | | | | O | | | |
| **Exception** | BadRequestException (Sai tài khoản/mật khẩu) | | | | | | | | O | O | |
| | BadRequestException (Tài khoản bị khóa) | | | | | | | | | | O |
| **Result** | Loại ca (N: Normal, A: Abnormal, B: Boundary) | N | N | N | N | N | N | B | A | A | A |
| | Trạng thái (Passed/Failed) | P | P | P | P | P | P | P | P | P | P |

---

### 2. `googleAuth` – GoogleOAuthController.authorizeGoogle
- **Yêu cầu:** Chuyển hướng Google Consent an toàn và chặn mock nguy hiểm khi chưa cấu hình Client ID thật.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 1, A: 2, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Google Client ID** | Được cấu hình bằng Client ID thật | O | | |
| | Rỗng / null | | O | |
| | Mang giá trị placeholder mặc định | | | O |
| **Confirm** | Chuyển hướng sang Google Consent URL | O | | |
| | Chuyển hướng về `/login?error=oauth2_not_configured` | | O | O |
| **Result** | Loại ca (N, A, B) | N | A | A |
| | Trạng thái | P | P | P |

---

### 3. `sendOtp` – AuthService.initiateRegistration
- **Yêu cầu:** Đăng ký tài khoản bước 1: Validate email/pass, sinh mã OTP 6 số và gửi Gmail xác thực.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 1, A: 2, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Precondition** | Email chưa tồn tại trong DB | O | O | |
| | Email đã đăng ký trong DB | | | O |
| **Input** | Thông tin hợp lệ, vai trò LEARNER / TUTOR | O | O | |
| | Mục tiêu JLPT (N5 - N1) | O | O | |
| **Confirm** | Sinh OTP 6 số, lưu cache pending, gửi mail | O | O | |
| **Exception** | BadRequestException (Email đã tồn tại) | | | O |
| **Result** | Loại ca (N, A, B) | N | N | A |
| | Trạng thái | P | P | P |

---

### 4. `verifyOtp` – AuthService.completeRegistration
- **Yêu cầu:** Xác thực OTP, tạo User & Learner/Tutor profile, cấp JWT token & cookie.
- **Tổng ca test:** 4 (Passed: 4, Failed: 0, N: 2, A: 2, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Pending Cache** | Tồn tại, chưa hết hạn | O | O | | O |
| | Không tồn tại hoặc đã hết hạn | | | O | |
| **Vai trò mục tiêu** | ROLE_LEARNER | O | | | |
| | ROLE_TUTOR | | O | | |
| **Precondition** | Email chưa kích hoạt trong DB | O | O | | |
| | Email đã kích hoạt bởi request đồng thời | | | | O |
| **Input** | Mã OTP nhập khớp với pending OTP | O | O | O | O |
| **Confirm** | Tạo User + Learner profile, trả JWT | O | | | |
| | Tạo User + Tutor profile APPROVED, trả JWT | | O | | |
| **Exception** | BadRequestException (Phiên hết hạn) | | | O | |
| | BadRequestException (Đã đăng ký trước đó) | | | | O |
| **Result** | Loại ca (N, A, B) | N | N | A | A |
| | Trạng thái | P | P | P | P |

---

### 5. `resendOtp` – OtpService.resendOtp
- **Yêu cầu:** Gửi lại mã OTP với cooldown 60 giây và tối đa 5 lần thử.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 1, A: 1, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Pending Cache** | Tồn tại | O | O | |
| | Không tồn tại | | | O |
| **Cooldown** | Đã qua 60 giây (>= 60s) | O | | |
| | Chưa qua 60 giây (< 60s) | | O | |
| **Confirm** | Sinh mã mới, làm mới expiresAt | O | | |
| **Exception** | BadRequestException (Chưa hết thời gian chờ) | | O | |
| | BadRequestException (Phiên kết thúc) | | | O |
| **Result** | Loại ca (N, A, B) | N | A | B |
| | Trạng thái | P | P | P |

---

### 6. `logout` – AuthService.logout
- **Yêu cầu:** Đăng xuất hệ thống, xóa sạch Cookie JWT (maxAge = 0, path = /).
- **Tổng ca test:** 2 (Passed: 2, Failed: 0, N: 1, A: 0, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 |
| :--- | :--- | :---: | :---: |
| **HttpServletResponse** | Được cung cấp hợp lệ | O | |
| | Null | | O |
| **Confirm** | Thêm cookie access_token maxAge = 0 | O | |
| | Xử lý an toàn, không ném NullPointerException | O | O |
| **Result** | Loại ca (N, A, B) | N | B |
| | Trạng thái | P | P |

---

### 7. `changePassword` – AuthService.changePasswordFirstTime
- **Yêu cầu:** Đổi mật khẩu lần đầu khi mustChangePassword = true, xóa cờ đổi mật khẩu.
- **Tổng ca test:** 4 (Passed: 4, Failed: 0, N: 1, A: 3, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Precondition** | User tồn tại, mustChangePassword = true | O | O | O | |
| | User không tồn tại trong DB | | | | O |
| **Mật khẩu hiện tại** | Khớp với hash lưu trong DB | O | O | | |
| | Không khớp với hash lưu trong DB | | | O | |
| **Mật khẩu mới** | Mới và xác nhận khớp nhau | O | | O | O |
| | Mới và xác nhận không khớp | | O | | |
| **Confirm** | Cập nhật hash mới, tắt mustChangePassword | O | | | |
| **Exception** | BadRequestException (Mật khẩu xác nhận không khớp) | | O | | |
| | BadRequestException (Mật khẩu tạm thời không đúng) | | | O | |
| | ResourceNotFoundException (Không tìm thấy user) | | | | O |
| **Result** | Loại ca (N, A, B) | N | A | A | A |
| | Trạng thái | P | P | P | P |

---

### 8. `resetPassword` – AuthService.resetPassword (PR #20 Security)
- **Yêu cầu:** Quên & Đặt lại mật khẩu: Giới hạn 3 lần/15 phút, chặn 4 vai trò nhân sự nội bộ (403), không lộ email chưa đăng ký.
- **Tổng ca test:** 7 (Passed: 7, Failed: 0, N: 2, A: 4, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Precondition** | Email tồn tại (Học viên / Gia sư) | O | | | O | O | | |
| | Email tồn tại (Admin, Manager, CS, Academic) | | O | | | | | |
| | Email không tồn tại | | | O | | | | |
| **Rate Limit** | < 3 lần trong 15 phút | O | O | O | O | O | O | |
| | >= 3 lần (lần thứ 4 liên tiếp) | | | | | | | O |
| **Input** | Mật khẩu tạm đúng & mật khẩu mới khớp | | | | O | | | |
| | Mật khẩu mới không khớp mật khẩu xác nhận | | | | | O | | |
| | Mật khẩu tạm nhập sai | | | | | | O | |
| **Confirm** | Sinh mật khẩu tạm, gửi qua email (bước 1) | O | | | | | | |
| | Trả về bình thường không gửi mail (chống dò email) | | | O | | | | |
| | Cập nhật passwordHash mới thành công (bước 2) | | | | O | | | |
| **Exception** | BusinessException HTTP 403 (Chặn nhân sự nội bộ) | | O | | | | | |
| | BadRequestException (Mật khẩu xác nhận không khớp) | | | | | O | | |
| | BadRequestException (Mật khẩu tạm không đúng) | | | | | | O | |
| | BusinessException HTTP 429 (Vượt quá tần suất) | | | | | | | O |
| **Result** | Loại ca (N, A, B) | N | A | N | N | A | A | B |
| | Trạng thái | P | P | P | P | P | P | P |

---

### 9. `flashcards` – FlashcardDeckController
- **Yêu cầu:** Quản lý bộ thẻ Flashcards cá nhân, kiểm tra bảo mật quyền sở hữu (owner_key: 401/403).
- **Tổng ca test:** 7 (Passed: 7, Failed: 0, N: 4, A: 3, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **JWT Auth** | Token hợp lệ | | O | O | O | O | O | O |
| | Thiếu hoặc token không hợp lệ | O | | | | | | |
| **Deck DB** | Tồn tại, owner_key trùng email caller | | | O | O | O | O | |
| | Tồn tại, owner_key khác email caller | | O | | | | | O |
| **Input** | GET /decks (Lấy danh sách) | O | | O | | | | |
| | POST /decks (Tạo mới) | O | | | O | | | |
| | PUT /decks/{id} (Cập nhật) | | O | | | O | | |
| | DELETE /decks/{id} (Xóa) | O | O | | | | O | O |
| **Confirm** | HTTP 200 + Danh sách bộ thẻ của caller | | | O | | | | |
| | HTTP 200 + Dữ liệu bộ thẻ và thẻ bài | | | | O | O | | |
| | HTTP 200 + success = true | | | | | | O | |
| **Exception** | BusinessException HTTP 401 (Chưa đăng nhập) | O | | | | | | |
| | BusinessException HTTP 403 (Không có quyền can thiệp) | | O | | | | | O |
| **Result** | Loại ca (N, A, B) | A | A | N | N | N | N | A |
| | Trạng thái | P | P | P | P | P | P | P |

---

### 10. `vocabularies` – VocabularyController
- **Yêu cầu:** Tra cứu từ vựng theo chủ đề JLPT N5-N1 và từ khóa (kho 7.811 từ vựng).
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 2, A: 0, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Precondition** | CSDL chứa 7.811 từ vựng qua các cấp độ | O | O | O |
| **Input** | GET /topics (Đếm số lượng theo chủ đề) | O | | |
| | topic = 'JLPT_N5', keyword = 'nihon' | | O | |
| | Phân trang âm (page = -5, limit = -10) | | | O |
| **Confirm** | Trả về 7 chủ đề kèm số lượng | O | | |
| | Trả về danh sách từ vựng khớp bộ lọc | | O | |
| | Tự động kẹp giá trị âm về 0 và limit mặc định | | | O |
| **Result** | Loại ca (N, A, B) | N | N | B |
| | Trạng thái | P | P | P |

---

### 11. `grammar` – GrammarController
- **Yêu cầu:** Tra cứu kho ngữ pháp OpenJLPT, lọc cấp độ N5-N1 và xem chi tiết cấu trúc.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 2, A: 1, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Nguồn dữ liệu** | Bảng grammars PostgreSQL hoặc file fallback JSON | O | O | O |
| **Input** | level = 'N5', keyword = 'desu' | O | | |
| | idOrCode = 'N3-050' (Mã ngữ pháp tồn tại) | | O | |
| | idOrCode = 'UNKNOWN-999' | | | O |
| **Confirm** | HTTP 200 + Danh sách ngữ pháp N5 khớp từ khóa | O | | |
| | HTTP 200 + Chi tiết cấu trúc, cách chia, ví dụ | | O | |
| **Exception** | HTTP 404 Not Found | | | O |
| **Result** | Loại ca (N, A, B) | N | N | A |
| | Trạng thái | P | P | P |

---

### 12. `aiWriting` – AiWritingService.checkWriting
- **Yêu cầu:** Luyện viết & Chữa câu Sakubun AI, nhận diện lỗi trợ từ và gợi ý từ vựng.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 2, A: 1, B: 0)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **AI Engine** | Đã cấu hình (Gemini) hoặc Fallback Offline Engine | O | O | O |
| **Input** | Câu tiếng Nhật hợp lệ có lỗi dùng sai trợ từ | O | | |
| | level = 'N5' (getGrammarChallengePrompts) | | O | |
| | Đoạn văn bản rỗng / khoảng trắng | | | O |
| **Confirm** | Trả về điểm, câu sửa, danh sách lỗi trợ từ | O | | |
| | Trả về danh sách chủ đề thử thách viết theo N5 | | O | |
| | Điểm = 0, danh sách lỗi rỗng, thông báo nhắc nhập | | | O |
| **Result** | Loại ca (N, A, B) | N | N | A |
| | Trạng thái | P | P | P |

---

### 13. `kanjiGame` – KanjiGameService
- **Yêu cầu:** Game Mưa Kanji & Ghép thẻ: Nạp dữ liệu chơi, ghi điểm session.
- **Tổng ca test:** 3 (Passed: 3, Failed: 0, N: 2, A: 0, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 |
| :--- | :--- | :---: | :---: | :---: |
| **Kanji Data** | Có sẵn dữ liệu Kanji JLPT N5-N1 | O | O | O |
| **Input** | jlpt = 'N5', limit = 20 | O | | |
| | Request lưu phiên chơi hợp lệ kèm danh sách lỗi | | O | |
| | limit <= 0 hoặc số âm | | | O |
| **Confirm** | Trả về 20 KanjiPlayDataDto kèm Furigana/On/Kun | O | | |
| | Lưu session, tính độ chính xác, trả về Response | | O | |
| | Tự động kẹp limit về mức tối thiểu 10 phần tử | | | O |
| **Result** | Loại ca (N, A, B) | N | N | B |
| | Trạng thái | P | P | P |

---

### 14. `readingAssistant` – ReadingAssistantService / Controller *(Thêm mới hoàn toàn)*
- **Yêu cầu:** AI Trợ lý Đọc hiểu & Phân tích Ngữ pháp: Bóc tách Furigana ruby, nhận diện cấu trúc ngữ pháp, từ vựng trọng tâm và kiểm thử Controller API.
- **Tổng ca test:** 5 (Passed: 5, Failed: 0, N: 3, A: 1, B: 1)

| Tiêu chí | Điều kiện / Dữ liệu vào | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **AI Settings** | Đã cấu hình (Gemini) hoặc Fallback Engine | O | O | O | O | O |
| **Input** | Đoạn văn tiếng Nhật hợp lệ (N3, includeFurigana = true) | O | | | | |
| | Đoạn văn bản rỗng / chỉ có khoảng trắng | | O | | O | |
| | POST /api/learner/reading/analyze đoạn văn hợp lệ | | | O | | |
| | targetJlpt = null (Toàn bộ các cấp độ) | | | | | O |
| **Confirm** | Trả về ReadingAnalysisResponse (Ruby html, ngữ pháp, từ vựng) | O | | | | |
| | Trả về danh sách rỗng an toàn, không ném ngoại lệ | | O | | O | |
| | HTTP 200 + Toàn bộ cấu trúc body phân tích đọc hiểu | | | O | | |
| | HTTP 200 + Fallback cấp độ mặc định N5/ALL mượt mà | | | | | O |
| **Result** | Loại ca (N, A, B) | N | A | N | A | B |
| | Trạng thái | P | P | P | P | P |
