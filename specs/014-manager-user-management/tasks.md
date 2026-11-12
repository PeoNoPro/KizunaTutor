---
description: "Danh sách công việc triển khai FE-14 — Quản lý người dùng cho Quản lý vận hành"
---

# Tasks: FE-14 — Quản lý người dùng cho Quản lý vận hành

**Input**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/manager-users-api.md](contracts/manager-users-api.md), [quickstart.md](quickstart.md).

**Tests**: Có (constitution V). Test service không chạm DB; SQL kiểm chứng bằng giao dịch rollback trên Supabase.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: chạy song song được · **[Story]**: US1…US3 theo spec.md
- Viết tắt: `BE` = `backend/src/main/java/com/kizuna`, `UM` = `BE/features/user_management`, `BT` = `backend/src/test/java/com/kizuna/features/user_management`, `FE` = `frontend`.

---

## Phase 1: Setup

- [X] T001 Thêm matcher `/api/manager/users`, `/api/manager/users/**` → `ROLE_MANAGER` trong `BE/core/security/SecurityConfig.java` (chỉ thêm dòng — plan Complexity Tracking)
- [X] T002 [P] Thêm route `/manager/users` trong `BE/core/config/WebMvcConfig.java` và `FE/server.js`
- [X] T003 [P] Thêm mục "Quản lý người dùng" (managerOnly) và đường dẫn vào `MANAGER_PATHS` trong `FE/components/admin-shell/admin-shell.js` (chỉ thêm)

## Phase 2: Foundational

- [X] T004 `UM/service/ManagedAccountStatus.java`: nhãn trạng thái, nhóm `TUTOR`/`LEARNER` ↔ vai trò, trạng thái đích hợp lệ
- [X] T005 [P] DTO `UM/dto/ManagedUserResponse.java`, `ManagedUserDetailResponse.java`, `UpdateUserStatusRequest.java` (Jakarta Validation)
- [X] T006 `UM/repository/ManagedUserRepository.java`: `list`, `findAccount`, `tutorProfile`, `learnerProfile`, `statusHistory`, `updateStatus` (khóa lạc quan), `insertAudit`
- [X] T007 `UM/service/UserManagementService.java` (interface)

## Phase 3: User Story 1 — Danh sách (P1)

- [X] T008 [US1] `UserManagementServiceImpl.list`: chuẩn hóa `category`, `status`, `keyword` (≤ 100), `page`, `size` (10/20/50) — FR-001…FR-003
- [X] T009 [US1] `GET /api/manager/users` trong `UM/controller/ManagerUserController.java`
- [X] T010 [US1] `FE/js/manager-users-helpers.js` (nhãn, kiểm tra biểu mẫu, thông báo rỗng) + `FE/tests/manager-users-helpers.test.mjs`
- [X] T011 [US1] Nhóm `managerUsers` trong `FE/js/api.js`; trang `FE/pages/manager-users/` trong khung `admin-shell` (sửa 2026-11-13: ban đầu dùng `staff-layout`) — chọn nhóm, bộ lọc, bảng, phân trang, skeleton, rỗng, lỗi + thử lại

## Phase 4: User Story 2 — Chi tiết (P1)

- [X] T012 [US2] `UserManagementServiceImpl.detail`: tài khoản + hồ sơ theo vai trò + lịch sử 20 dòng + `allowedStatuses` — FR-004…FR-006
- [X] T013 [US2] `GET /api/manager/users/{userId}`
- [X] T014 [US2] Ngăn chi tiết trên trang: thông tin, hồ sơ, lịch sử; đóng giữ nguyên bộ lọc / trang

## Phase 5: User Story 3 — Đổi trạng thái (P1)

- [X] T015 [US3] `UserManagementServiceImpl.updateStatus` (`@Transactional`): kiểm tra tồn tại, `PENDING`, trùng trạng thái, `expectedStatus`; UPDATE có điều kiện; ghi nhật ký — FR-007…FR-010, FR-012
- [X] T016 [US3] `UM/service/UserStatusMailService.java`: email sau commit, không chứa lý do nội bộ — FR-011
- [X] T017 [US3] `PUT /api/manager/users/{userId}/status`
- [X] T018 [US3] Hộp thoại đổi trạng thái (từ dòng và từ chi tiết): chọn trạng thái, lý do, lời nhắn, lỗi theo trường, Hủy không đổi gì — FR-013
- [X] T019 [P] [US3] `BT/UserManagementServiceTest.java`: danh sách chuẩn hóa tham số; chi tiết 404 / nhân viên; đổi trạng thái hợp lệ ghi nhật ký + email; `PENDING`; trùng trạng thái; 409 khi `expectedStatus` lệch hoặc UPDATE 0 dòng; trạng thái đích sai

## Phase 6: Polish & Validation

- [X] T020 `.\mvnw.cmd test-compile`; `.\mvnw.cmd test -Dtest=UserManagementServiceTest`; `node --test "frontend/tests/*.test.mjs"`
- [X] T021 Kiểm chứng SQL trên Supabase trong giao dịch rollback (danh sách, chi tiết, UPDATE có điều kiện, nhật ký)
- [ ] T022 Kiểm tra trang theo [quickstart.md](quickstart.md): ngôn ngữ, viết hoa, rỗng, lỗi, phân quyền

## Kết quả kiểm chứng (2026-11-13)

- `UserManagementServiceTest`: 15/15 xanh. `node --test "frontend/tests/*.test.mjs"`: 37/37 xanh. `test-compile` qua.
- T021: trên Supabase, trong giao dịch rollback — danh sách 10 gia sư / 10 học viên, lọc `PENDING` = 1, từ khóa `%` thoát đúng (0 kết quả), UPDATE sai `expectedStatus` = 0 dòng, đúng = 1 dòng, nhật ký đọc lại đúng; sau rollback trạng thái và nhật ký như cũ.
- API trên backend đang chạy (tài khoản Quản lý thử): danh sách, chi tiết gia sư / học viên 200; vai trò gia sư gọi API → 403; tài khoản Quản lý làm mục tiêu → 404; trùng trạng thái → 400; `expectedStatus` lệch → 409; thiếu lý do → 400 kèm lỗi trường; nhóm sai → 400. Không lần nào ghi dữ liệu.
- **Chưa xác minh (T022)**: chưa mở trang trên trình duyệt; chưa đổi trạng thái thật một tài khoản (DB dùng chung — người phụ trách làm theo quickstart bước 5–6 với tài khoản thử); chưa kiểm tra email thực nhận. Máy chủ frontend đang chạy được bật trước khi thêm route nên `/manager/users` cần khởi động lại `node frontend/server.js` (đường dẫn `/pages/manager-users/` vẫn mở được).

## Dependencies

- Phase 1 → Phase 2 → US1 → US2 → US3 (US3 dùng hộp thoại trên trang của US1, US2).
- T019 viết song song với T015–T016.
