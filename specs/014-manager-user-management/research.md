# Research: FE-14 — Quản lý người dùng cho Quản lý vận hành

## 1. Vô hiệu hóa phiên đăng nhập (UC-12 bước 8)

- **Decision**: Không thêm cơ chế thu hồi token. Chỉ đổi `users.account_status`.
- **Rationale**: `core/security/JwtAuthenticationFilter` đọc lại người dùng từ DB ở mỗi request và chỉ xác thực khi `account_status = ACTIVE` và `deleted_at IS NULL`; `AuthServiceImpl.login` từ chối tài khoản không `ACTIVE`. Chuỗi bảo mật FE-11 (`/api/admin/operations/**`) cũng kiểm tra trạng thái qua `OperationsActorResolver`. Như vậy token cũ hết hiệu lực ngay ở request kế tiếp.
- **Alternatives**: Bảng token bị thu hồi / cột `token_version` — cần migration và sửa `core/security` (của nhóm), thừa so với hiện trạng.

## 2. Nơi lưu nhật ký (UC-12 bước 9)

- **Decision**: Bảng có sẵn `system_audit_logs`: `action_type = 'USER_ACCOUNT_STATUS_CHANGE'`, `target_entity = 'users'`, `target_id = user_id`, `details` JSONB `{fromStatus, toStatus, reason, messageToUser}`.
- **Rationale**: Đây là bảng "Activity Audit Log" theo thực thể SystemAuditLog của SRS; FE-13 đã dùng cùng cách. Không tạo bảng, không migration.
- **Alternatives**: Bảng riêng `user_status_changes` — cần migration; thêm vào `staff_account_audit_events` (FE-11) — sai ý nghĩa (chỉ dành cho nhân viên) và là bảng của thành viên khác.
- **Ghi chú**: Trang "Nhật ký thao tác" của Quản trị viên (FE-11) không đọc `system_audit_logs`, nên bản ghi này chưa hiện ở đó. Muốn hiện cần người phụ trách FE-11 thêm nguồn; ngoài phạm vi.

## 3. Đổi trạng thái an toàn khi hai Quản lý cùng thao tác

- **Decision**: Client gửi `expectedStatus` (trạng thái lúc mở biểu mẫu). `UPDATE users SET account_status = ? WHERE user_id = ? AND account_status = ?::user_account_status_enum AND deleted_at IS NULL`; 0 dòng → 409.
- **Rationale**: Khóa lạc quan đơn giản, không cần cột version.

## 4. Email (UC-12 bước 10)

- **Decision**: Lớp riêng `UserStatusMailService` trong feature, dùng `JavaMailSender` chung (bean `@Primary` của `admin/config/AdminMailConfiguration`), gửi **sau khi giao dịch commit** trên luồng nền; lỗi chỉ ghi log — giống `verification/service/VerificationNotificationService`.
- **Rationale**: Không gọi lớp của feature khác; người bị khóa không vào được hệ thống nên thông báo trong ứng dụng (FE-12) vô ích, và `NotificationEvents` bỏ qua tài khoản không hoạt động.

## 5. Truy cập dữ liệu

- **Decision**: Repository riêng bằng `JdbcTemplate` trên các bảng `users`, `roles`, `tutors`, `learners`, `system_audit_logs`.
- **Rationale**: Truy vấn danh sách có lọc động + đếm + nối bảng hồ sơ; giống FE-13. Các bảng thuộc lõi tài khoản (`core/entity`), không phải repository của feature khác. Không thêm entity hay sửa `core/repository/UserRepository`.
- **Alternatives**: Spring Data JPA trên entity lõi — phải thêm phương thức vào `core/repository` (dùng chung) hoặc Specification; phức tạp hơn và đụng `core/`.

## 6. Phân quyền

- **Decision**: Thêm một matcher `"/api/manager/users/**", "/api/manager/users"` → `hasAuthority("ROLE_MANAGER")` trong `core/security/SecurityConfig` (chuỗi chính, đặt cạnh matcher `/api/manager/tutor-memberships/**` của FE-09).
- **Rationale**: Cùng cách FE-09, FE-13. Chuỗi chính đã tắt CSRF và dùng JWT Bearer (như mọi API `authRequest` hiện có), nên không cần endpoint `/csrf`.
- **Ghi chú**: Sửa `core/` (chỉ thêm dòng) — ghi ở plan Complexity Tracking để báo nhóm.

## 7. Giao diện

- **Decision** (sửa 2026-11-13 theo phản hồi người phụ trách): Trang `/pages/manager-users/` (alias `/manager/users`) dùng **khung quản trị chung `admin-shell`** qua `js/pages/operations.js`, cùng lớp giao diện với các trang Quản lý khác (`operations-card`, `staff-table`, `portal-dialog`, `portal-badge`); chi tiết và đổi trạng thái là hai hộp thoại `<dialog>`. CSS riêng của trang (skeleton, lưới thông tin, lịch sử) ở `pages/manager-users/manager-users.css`.
- **Điểm vào**: **thêm một mục** "Quản lý người dùng" (`/pages/manager-users/`) vào sidebar Quản lý và `MANAGER_PATHS` trong `components/admin-shell/admin-shell.js` (FE-11). Chỉ thêm, không sửa/xóa mục khác.
- **Rationale**: Quản lý đăng nhập vào `/manager/dashboard` (khung `admin-shell`). Bản đầu dùng `components/staff-layout` (khung FE-09) khiến bấm vào mục là đổi sang sidebar khác — đã bỏ, `staff-layout` trả về như cũ.

## 8. Kiểm thử

- **Decision**: JUnit 5 + Mockito (đã có trong `spring-boot-starter-test`) cho quy tắc nghiệp vụ của service (repository giả lập); `node --test` cho hàm thuần frontend. Kiểm chứng SQL trên Supabase bằng giao dịch rollback (không có DB test — A-4).
