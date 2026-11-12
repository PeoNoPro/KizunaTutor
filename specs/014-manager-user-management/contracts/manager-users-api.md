# Contract: FE-14 — API quản lý người dùng

Tiền tố `/api/manager/users`. Chỉ `ROLE_MANAGER` (khác → 403). Xác thực JWT Bearer như các API `authRequest` hiện có. Mọi phản hồi bọc `ApiResponse { success, message, data }`; lỗi theo `GlobalExceptionHandler` (message tiếng Việt).

## 1. `GET /api/manager/users`

Query:

| Tham số | Kiểu | Mặc định | Ghi chú |
| --- | --- | --- | --- |
| `category` | `TUTOR` \| `LEARNER` | `TUTOR` | giá trị khác → 400 |
| `keyword` | string | rỗng | cắt khoảng trắng, tối đa 100 ký tự (dài hơn bị cắt); tìm `full_name`, `email` không phân biệt hoa thường |
| `status` | `PENDING` \| `ACTIVE` \| `SUSPENDED` \| `BANNED` | rỗng = tất cả | giá trị khác → 400 |
| `page` | int | 0 | < 0 → 0 |
| `size` | int | 20 | ngoài 10/20/50 → 20 |

`data`: `PageResponse<ManagedUserResponse>`

```json
{
  "items": [
    {
      "userId": "uuid",
      "fullName": "Nguyễn Văn A",
      "email": "a@example.com",
      "category": "TUTOR",
      "categoryLabel": "Gia sư",
      "accountStatus": "ACTIVE",
      "accountStatusLabel": "Đang hoạt động",
      "createdAt": "2026-11-01T08:00:00Z"
    }
  ],
  "page": 0, "size": 20, "totalElements": 10, "totalPages": 1
}
```

## 2. `GET /api/manager/users/{userId}`

`data`: `ManagedUserDetailResponse`

```json
{
  "userId": "uuid",
  "fullName": "...", "email": "...", "phone": "..." ,
  "avatarUrl": "..." ,
  "category": "TUTOR", "categoryLabel": "Gia sư",
  "accountStatus": "ACTIVE", "accountStatusLabel": "Đang hoạt động",
  "createdAt": "...", "updatedAt": "...",
  "allowedStatuses": [ { "value": "SUSPENDED", "label": "Tạm khóa" }, { "value": "BANNED", "label": "Khóa vĩnh viễn" } ],
  "statusChangeBlockedReason": null,
  "tutorProfile": {
    "tutorCode": 10001, "headline": "...", "experienceYears": 3, "highestJlptLevel": "N2",
    "avgRating": 4.8, "totalReviews": 12, "tutorStateLabel": "Đang Hoạt Động",
    "dateOfBirth": "1998-02-01", "address": "...", "verifiedAt": "..."
  },
  "learnerProfile": null,
  "statusHistory": [
    { "changedAt": "...", "actorName": "Quản lý B", "fromStatusLabel": "Đang hoạt động", "toStatusLabel": "Tạm khóa",
      "reason": "...", "messageToUser": "..." }
  ]
}
```

- `tutorProfile` chỉ có với `TUTOR`, `learnerProfile` (`currentLevel`, `targetLevel`, `learningGoals`, `province`, `address`) chỉ có với `LEARNER`; chưa có hồ sơ → `null`.
- `phone`: số của tài khoản, nếu trống thì lấy số trong hồ sơ gia sư.
- `allowedStatuses` rỗng và `statusChangeBlockedReason` = "Tài khoản chưa hoàn tất đăng ký nên chưa thể đổi trạng thái." khi `PENDING`.
- `statusHistory`: tối đa 20, mới nhất trước.
- Không tồn tại / đã xóa mềm / không phải gia sư hay học viên → 404 "Không tìm thấy tài khoản gia sư hoặc học viên này."

## 3. `PUT /api/manager/users/{userId}/status`

Body `UpdateUserStatusRequest`:

| Trường | Bắt buộc | Ràng buộc |
| --- | --- | --- |
| `status` | có | `ACTIVE` \| `SUSPENDED` \| `BANNED` |
| `expectedStatus` | có | trạng thái lúc mở biểu mẫu |
| `reason` | có | 1–1.000 ký tự sau khi cắt khoảng trắng |
| `message` | không | tối đa 1.000 ký tự |

Thành công 200: `message` = "Đã cập nhật trạng thái tài khoản thành {nhãn}." ; `data` = `ManagedUserDetailResponse` mới.

Lỗi:

| Trường hợp | HTTP | message |
| --- | --- | --- |
| Thiếu / sai trường | 400 | lỗi theo trường (`fieldErrors`) |
| `status` ngoài 3 giá trị | 400 | "Trạng thái mới không hợp lệ." |
| Tài khoản đang `PENDING` | 400 | "Tài khoản chưa hoàn tất đăng ký nên chưa thể đổi trạng thái." |
| `status` = trạng thái hiện tại | 400 | "Tài khoản đã ở trạng thái này." |
| Không tìm thấy | 404 | như mục 2 |
| `expectedStatus` ≠ hiện tại, hoặc UPDATE 0 dòng | 409 | "Trạng thái tài khoản vừa được thay đổi. Vui lòng tải lại trước khi cập nhật." |

Tác dụng phụ: 1 dòng `system_audit_logs`; email sau commit (lỗi gửi không ảnh hưởng phản hồi).
