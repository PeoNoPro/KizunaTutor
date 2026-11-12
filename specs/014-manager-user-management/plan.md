# Implementation Plan: FE-14 — Quản lý người dùng cho Quản lý vận hành

**Branch**: `han` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/014-manager-user-management/spec.md`

## Summary

Quản lý vận hành xem danh sách gia sư / học viên (lọc, tìm, phân trang), xem chi tiết hồ sơ kèm lịch sử đổi trạng thái, và đổi trạng thái tài khoản (Đang hoạt động / Tạm khóa / Khóa vĩnh viễn) có lý do nội bộ, lời nhắn, nhật ký và email (SRS UC-20, UC-21, UC-12). Backend: phân hệ mới `com.kizuna.features.user_management` (controller → service interface/impl → repository `JdbcTemplate`), 3 endpoint dưới `/api/manager/users`. Chặn truy cập tức thời dựa trên bộ lọc JWT hiện có (đọc lại trạng thái mỗi request). Nhật ký ở bảng có sẵn `system_audit_logs`. Frontend: trang `/manager/users` trong khung `staff-layout`. Không tạo bảng, không migration.

## Technical Context

**Language/Version**: Java 17 (Spring Boot 3.3.4); JavaScript ES6+ module.

**Primary Dependencies**: Spring Web, Validation, Security (JWT có sẵn), Spring JDBC (`JdbcTemplate`), Spring Mail (`JavaMailSender` chung), Jackson; Tailwind CDN, `components/staff-layout`, `components/pagination`. Không thêm thư viện.

**Storage**: PostgreSQL Supabase dùng chung. Bảng đã có: `users`, `roles`, `tutors`, `learners`, `system_audit_logs` ([data-model.md](data-model.md)).

**Testing**: JUnit 5 + Mockito + AssertJ cho `UserManagementServiceImpl` (repository giả lập); `node --test` cho `frontend/js/manager-users-helpers.js`. SQL kiểm chứng trên Supabase bằng giao dịch rollback.

**Target Platform**: Backend 8085, frontend Node 3000; trình duyệt desktop và di động.

**Project Type**: Web application.

**Performance Goals**: Trang danh sách < 2 giây (SC-004); dữ liệu hiện ~20 gia sư / học viên.

**Constraints**: Không đổi luồng đăng nhập, không sửa mã nghiệp vụ của thành viên khác; chỉ thêm dòng ở `core/` và ở khung sidebar dùng chung.

**Scale/Scope**: 3 endpoint, 1 trang, 1 hộp thoại.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Nguyên tắc | Đánh giá | Ghi chú |
| --- | --- | --- |
| I. Tiếng Việt, không lộ mã, không ALL-CAPS | Đạt | Nhãn trạng thái / vai trò dịch ở backend; mã chỉ dùng nội bộ. |
| II.1–4 Dữ liệu thật, rỗng, lỗi, skeleton | Đạt | Mọi dữ liệu từ API; có skeleton, trạng thái rỗng, lỗi + thử lại. |
| III.2 Không gọi Repository của feature khác | Đạt | Chỉ dùng bảng lõi tài khoản; không import lớp của `admin`, `verification`, `notification`. Dùng `payment/service/CurrentUserAccessor` (component của Hân, như FE-13). |
| III.3 `core/` không chứa nghiệp vụ | Đạt; **sửa `core/` (thêm dòng)** | 1 matcher `SecurityConfig`, 1 route `WebMvcConfig`. Xem Complexity Tracking. |
| III.4 / BACKEND_RULES 2–4 | Đạt | `UserManagementService` + `impl/UserManagementServiceImpl`; DTO `*Request` / `*Response`; `ApiResponse`. |
| IV. Giao diện | Đạt | Khung quản trị chung `admin-shell` và lớp giao diện của các trang Quản lý (FE-11); không inline style / handler. |
| V. Ranh giới, test, Git | Đạt | Không thanh toán, AI; có test; không commit / push. |

**Re-check sau Phase 1**: thiết kế không phát sinh lệch mới. Qua cổng.

## Project Structure

### Documentation (this feature)

```text
specs/014-manager-user-management/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/manager-users-api.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/kizuna/features/user_management/
├── controller/ManagerUserController.java
├── dto/
│   ├── ManagedUserResponse.java
│   ├── ManagedUserDetailResponse.java      (kèm TutorProfile, LearnerProfile, StatusOption, StatusChange)
│   └── UpdateUserStatusRequest.java
├── repository/ManagedUserRepository.java
└── service/
    ├── UserManagementService.java
    ├── ManagedAccountStatus.java           (nhãn trạng thái, nhóm vai trò)
    ├── UserStatusMailService.java
    └── impl/UserManagementServiceImpl.java

backend/src/test/java/com/kizuna/features/user_management/UserManagementServiceTest.java

backend/src/main/java/com/kizuna/core/security/SecurityConfig.java   (+1 matcher)
backend/src/main/java/com/kizuna/core/config/WebMvcConfig.java       (+1 route)

frontend/pages/manager-users/{index.html, manager-users.js, manager-users.css}   (khung admin-shell qua js/pages/operations.js)
frontend/js/manager-users-helpers.js
frontend/tests/manager-users-helpers.test.mjs
frontend/js/api.js                                  (+ nhóm managerUsers)
frontend/server.js                                  (+ route /manager/users)
frontend/components/admin-shell/admin-shell.js      (+ 1 mục sidebar Quản lý, + 1 đường dẫn MANAGER_PATHS)
```

**Structure Decision**: Phân hệ riêng `user_management` (mới), không đặt vào `admin` (FE-11 của thành viên khác) hay `profile` (rỗng, dành cho UC-18/19 của học viên, gia sư).

## Complexity Tracking

| Lệch | Vì sao cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Thêm 1 matcher trong `core/security/SecurityConfig.java` và 1 route trong `core/config/WebMvcConfig.java` | Phân quyền API theo vai trò và đường dẫn trang sạch, giống FE-09, FE-13 | `@PreAuthorize` trên controller: dự án đang phân quyền tập trung ở `SecurityConfig` |
| Thêm 1 mục vào sidebar Quản lý trong `components/admin-shell/admin-shell.js` (FE-11, Huyền) | Quản lý vào hệ thống qua `/manager/dashboard` dùng khung này; thiếu mục thì không tìm thấy trang | Chỉ thêm ở `staff-layout`: Quản lý không thấy được từ trang tổng quan |
| Repository dùng `JdbcTemplate` thay vì Spring Data JPA (BACKEND_RULES 3.3) | Lọc động + đếm + nối hồ sơ và đọc nhật ký JSONB; không sửa `core/repository` dùng chung | JPA cần thêm phương thức vào `core/repository/UserRepository` (dùng chung) |

Ba điểm trên cần báo nhóm (người phụ trách `core/` và FE-11). Không sửa / xóa dòng nào có sẵn.

## Ghi chú triển khai

- **Lệch so với bản plan đầu (2026-11-13)**: trang ban đầu dùng `components/staff-layout` (khung FE-09); bấm mục ở sidebar Quản lý thì chuyển sang sidebar khác. Đã đổi sang khung `admin-shell` như các trang Quản lý còn lại; bỏ mục đã thêm trong `staff-layout`. Logic, API, hàm thuần không đổi.

- Bộ lọc JWT (`core/security/JwtAuthenticationFilter`) đã từ chối tài khoản không `ACTIVE` ở mỗi request → đáp ứng UC-12 bước 8 mà không sửa `core/`.
- Bản ghi nhật ký chưa hiện ở trang "Nhật ký thao tác" của Quản trị viên (FE-11 đọc các bảng sự kiện riêng) — báo người phụ trách FE-11 nếu cần.
