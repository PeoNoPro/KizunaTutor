# Data Model: FE-14 — Quản lý người dùng cho Quản lý vận hành

**Không tạo bảng, không đổi schema, không migration.** FE-14 chỉ đọc các bảng tài khoản, cập nhật đúng một cột `users.account_status` và thêm bản ghi `system_audit_logs`.

## 1. Bảng đọc

### 1.1 `users` (đã có)

| Cột | Dùng cho |
| --- | --- |
| `user_id` UUID | khóa |
| `role_id` → `roles.role_name` | chỉ `ROLE_TUTOR` (nhóm `TUTOR`), `ROLE_LEARNER` (nhóm `LEARNER`) |
| `email`, `full_name`, `phone`, `avatar_url` | hiển thị; tìm kiếm trên `full_name`, `email` |
| `account_status` `user_account_status_enum` | `PENDING`, `ACTIVE`, `SUSPENDED`, `BANNED` |
| `created_at` | "Ngày đăng ký"; sắp xếp mới trước |
| `updated_at` | "Cập nhật gần nhất" (trigger tự cập nhật) |
| `deleted_at` | khác NULL → coi như không tồn tại |

### 1.2 `tutors` (đã có, 1-1 với `users`)

`tutor_code`, `headline`, `experience_years`, `highest_jlpt_level`, `avg_rating`, `total_reviews`, `tutor_state` (nhãn lấy từ `core/entity/TutorState.displayName`), `phone`, `date_of_birth`, `address`, `verified_at`. Không có dòng → phần hồ sơ "Chưa cập nhật".

### 1.3 `learners` (đã có, 1-1 với `users`)

`current_level`, `target_level`, `learning_goals`, `province`, `address`. Không có dòng → "Chưa cập nhật".

## 2. Bảng ghi

### 2.1 `users.account_status` — chuyển trạng thái

```
PENDING ──(không đổi được ở FE-14)

ACTIVE ⇄ SUSPENDED
ACTIVE ⇄ BANNED
SUSPENDED ⇄ BANNED
```

- Đích hợp lệ: `ACTIVE`, `SUSPENDED`, `BANNED`; khác trạng thái hiện tại.
- Câu lệnh: `UPDATE users SET account_status = ?::user_account_status_enum WHERE user_id = ? AND account_status = ?::user_account_status_enum AND deleted_at IS NULL` (khóa lạc quan theo `expectedStatus`).
- Không đổi `deleted_at`, `role_id` hay cột nào khác.

### 2.2 `system_audit_logs` — mỗi lần đổi một dòng

| Cột | Giá trị |
| --- | --- |
| `log_id` | `gen_random_uuid()` |
| `actor_user_id` | Quản lý đang đăng nhập |
| `action_type` | `USER_ACCOUNT_STATUS_CHANGE` |
| `target_entity` | `users` |
| `target_id` | `user_id` của người bị đổi |
| `details` JSONB | `{ "fromStatus": "ACTIVE", "toStatus": "SUSPENDED", "reason": "...", "messageToUser": "..." \| null }` |
| `created_at` | mặc định |

Đọc lịch sử: `WHERE action_type = 'USER_ACCOUNT_STATUS_CHANGE' AND target_id = ? ORDER BY created_at DESC LIMIT 20`, nối `users` lấy tên người thực hiện.

## 3. Nhãn hiển thị

| Mã | Nhãn |
| --- | --- |
| `PENDING` | Chờ kích hoạt |
| `ACTIVE` | Đang hoạt động |
| `SUSPENDED` | Tạm khóa |
| `BANNED` | Khóa vĩnh viễn |
| `TUTOR` / `LEARNER` | Gia sư / Học viên |

## 4. Giao dịch

Đổi trạng thái: một giao dịch = đọc tài khoản (kiểm tra vai trò, xóa mềm, trạng thái) → UPDATE có điều kiện → INSERT nhật ký. Lỗi ở bước nào → rollback. Email gửi sau commit.
