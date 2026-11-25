# Thiết Kế: Kích Hoạt Đăng Nhập Gmail Thật (Google OAuth2)

- **Ngày tạo**: 2026-11-26
- **Trạng thái**: Chờ phê duyệt (Pending Approval)
- **Phạm vi**: Thay đổi nhỏ (Auth / Spring Security Backend) — `KizunaTutor`

---

## 1. Mục tiêu & Ràng buộc (Intent, Goals & Non-goals)

### 1.1. Mục tiêu
- Cho phép người dùng đăng nhập/đăng ký bằng **tài khoản Gmail thật** thông qua nút **"Đăng nhập với Google"** (`/oauth2/authorization/google`) ở trang đăng nhập và **"Đăng ký trực tiếp với Google"** ở trang đăng ký.
- Cho phép người dùng **chọn vai trò (`ROLE_LEARNER` — Học viên hoặc `ROLE_TUTOR` — Gia sư)** ngay trên bộ chọn role sẵn có ở trang đăng ký (`/register`) khi tạo tài khoản mới bằng Gmail, tái sử dụng trực tiếp logic khởi tạo `User` + `Learner`/`Tutor` của luồng đăng ký thường.
- Hoàn thiện trọn vẹn luồng callback `/login/oauth2/code/google` phía backend, cấp JWT + cookie `access_token` và điều hướng về `/login?token=...` khớp hoàn toàn với frontend [login.js](file:///C:/Users/Administrator/Desktop/KizunaTutor/frontend/pages/login/login.js).
- Duy trì an toàn: nếu máy chủ chưa cấu hình `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` thật (vẫn là placeholder), chặn ngay ở bước khởi tạo OAuth2 và trả về `/login?error=oauth2_not_configured`.

### 1.2. Phi mục tiêu & Anti-goals
- Không thay đổi cấu trúc bảng `users` / `learners` trong CSDL.
- Không bật lại đăng nhập giả lập (`/api/auth/mock-google-login` tiếp tục khóa mặc định).
- Không commit `GOOGLE_CLIENT_ID` hoặc `GOOGLE_CLIENT_SECRET` thật vào repository.
- Không cho phép tài khoản đang bị khóa (`accountStatus != ACTIVE` hoặc `deletedAt != null`) đăng nhập qua Google.

---

## 2. Phương án kiến trúc (Approaches)

### Cách 1 (Được chọn): Bật Spring Security `oauth2Login()`
- Tận dụng dependency `spring-boot-starter-oauth2-client` đã có sẵn trong `backend/pom.xml`.
- Spring Security tự xử lý tạo `state`, chống CSRF, đổi `authorization_code` lấy OIDC/userinfo tại `/login/oauth2/code/google`.
- Tùy biến `OAuth2AuthorizationRequestResolver` (kiểm tra placeholder Client ID), `AuthenticationSuccessHandler` (tìm/tạo `User` + `Learner`, cấp JWT) và `AuthenticationFailureHandler`.
- **Lý do chọn**: Chuẩn bảo mật OAuth2/OIDC có sẵn, ít mã tự viết, dễ bảo trì và mở rộng.

### Cách 2 (Không chọn): Tự gọi REST API Google trong `GoogleOAuthController`
- Tự quản lý `state`, tự gọi `oauth2.googleapis.com/token` và `userinfo` bằng HTTP client.
- **Lý do loại**: Tăng mã nguồn tự viết và rủi ro sai sót bảo mật `state`/token validation so với Spring Security chuẩn.

---

## 3. Thiết kế chi tiết (Các quyết định mặc định đề xuất)

1. **Cấu hình & Chống lệch cổng Proxy (`localhost:3000` -> `8085`)**
   - Trong `application.properties`: giữ `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` đọc từ biến môi trường; đặt `redirect-uri=${GOOGLE_REDIRECT_URI:http://localhost:3000/login/oauth2/code/google}` (thay vì `{baseUrl}` để tránh bị dựng thành cổng `8085` khi đi qua proxy `frontend/server.js`).
   - Nếu deploy lên domain thật, chỉ cần set biến môi trường `GOOGLE_REDIRECT_URI=https://<domain>/login/oauth2/code/google` và khai báo khớp trên Google Cloud Console.

2. **Chặn cấu hình Placeholder & Mang theo Role đã chọn**
   - Khi `spring.security.oauth2.client.registration.google.client-id` rỗng hoặc trùng placeholder mặc định, custom `OAuth2AuthorizationRequestResolver` / guard sẽ redirect về `/login?error=oauth2_not_configured` (vẫn giữ tương thích test của `GoogleOAuthControllerTest`).
   - Trên trang `/register`, `register.js` đọc radio `userRole` đang chọn (`ROLE_LEARNER` hoặc `ROLE_TUTOR`) và điều hướng tới `/oauth2/authorization/google?role=...`.
   - Trước khi chuyển sang Google, resolver kiểm tra **whitelist chỉ cho phép `ROLE_LEARNER` hoặc `ROLE_TUTOR`** (mặc định `ROLE_LEARNER`, tuyệt đối chặn mọi role nội bộ như `ROLE_ADMIN`, `ROLE_MANAGER`...) và lưu vào `HttpSession` để dùng lại khi Google callback về.

3. **Xử lý User khi Google xác thực thành công (`OAuth2SuccessHandler` + `AuthService`)**
   - Trích xuất `email` (lowercase), `name`, `picture` từ `OAuth2User`. Nếu thiếu email (hoặc `email_verified == false`) -> redirect `/login?error=email_not_provided`.
   - Trong transaction (`AuthService.authenticateWithGoogle(email, fullName, avatarUrl, requestedRole)`):
     - **Chưa có `User`**: tách phần tạo `User` + `Learner`/`Tutor` từ `completeRegistration` thành helper dùng chung trong `AuthServiceImpl` (khởi tạo `Tutor` với `UNVERIFIED`, `FREE` nếu chọn `ROLE_TUTOR`; hoặc `Learner` `N5 -> N3` nếu chọn `ROLE_LEARNER`), `accountStatus = ACTIVE`, `avatarUrl` từ Google (giới hạn 500 ký tự), `passwordHash` ngẫu nhiên đã mã hóa.
     - **Đã có `User` cùng email**: kiểm tra `deletedAt == null && accountStatus == ACTIVE` (vi phạm -> từ chối và redirect `/login?error=oauth2_failure`), cập nhật `avatarUrl` nếu đang trống, **giữ nguyên role hiện tại** của tài khoản (không ghi đè role cũ).
   - Ghi nhận đăng nhập qua `LoginActivityRecorder`, cấp JWT từ `JwtTokenProvider`, gắn cookie `access_token` (`HttpOnly`, `Path=/`, `MaxAge=86400`), và redirect tương đối về `/login?token=...&role=...&email=...&name=...` (nếu là `ROLE_TUTOR`, `login.js` đã tự động điều hướng tới `/tutor/dashboard` để gia sư nộp xác minh).

4. **Bảo toàn API Error Handling trong `SecurityConfig`**
   - Kích hoạt `.oauth2Login(...)` trên `securityFilterChain`, đồng thời giữ nguyên `restAuthErrorHandlers` cho `/api/**` (để các API chưa xác thực vẫn trả `401 JSON` thay vì bị redirect 302 sang trang login).

5. **Kiểm thử**
   - Bổ sung unit tests cho `AuthService.authenticateWithGoogle` (tạo mới `ROLE_LEARNER` + `Learner`, tạo mới `ROLE_TUTOR` + `Tutor`, chặn role ngoài whitelist về `ROLE_LEARNER`, đăng nhập lại user cũ giữ nguyên role, chặn user bị khóa) và guard/handler OAuth2; đảm bảo toàn bộ test auth hiện tại vẫn pass.
