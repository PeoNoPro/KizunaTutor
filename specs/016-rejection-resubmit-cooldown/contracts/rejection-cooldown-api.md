# API Contract: FE-16 — Nộp lại sau 3 ngày bị từ chối

Không thêm endpoint. Chỉ thêm một trường vào hai phản hồi có sẵn và đổi lời nhắn lỗi. Định dạng phản hồi giữ nguyên như hợp đồng gốc ([003 verification-api](../../003-tutor-verification/contracts/verification-api.md), API bài đăng `GET/PUT /api/tutor/post`).

## GET `/api/tutor/verification` (gia sư)

Thêm trường `resubmitAvailableAt` (ISO-8601, có thể vắng mặt):

- Có giá trị khi `status = "REJECTED"` và tìm thấy lần từ chối: thời điểm từ chối + 3 ngày.
- Vắng mặt ở mọi trạng thái khác.
- `editable` = `true` khi `status` là `UNVERIFIED` / `ADDITIONAL_INFO_REQUIRED`, hoặc `REJECTED` và (không có `resubmitAvailableAt` hoặc hiện tại ≥ `resubmitAvailableAt`).

```json
{
  "success": true,
  "data": {
    "status": "REJECTED",
    "statusLabel": "Bị từ chối",
    "editable": false,
    "resubmitAvailableAt": "2026-12-03T14:30:00+07:00",
    "latestFeedback": { "kind": "REJECT", "kindLabel": "Từ chối", "content": "..." }
  }
}
```

## PUT `/api/tutor/verification`, POST/PUT/DELETE `/api/tutor/verification/documents...`, POST `/api/tutor/verification/submit`

Trong thời gian chờ: **409**, `code = "VERIFICATION_REJECTED"` (giữ mã cũ), lời nhắn:

> Hồ sơ của bạn đã bị từ chối. Bạn có thể chỉnh sửa và nộp lại hồ sơ từ 14:30 ngày 03/12/2026.

Sau thời gian chờ: hoạt động như hồ sơ "Chưa nộp". Nộp khi chỉ còn giấy tờ "Không được chấp nhận" → lỗi thiếu giấy tờ như hiện tại (`SUBMISSION_INCOMPLETE`).

## GET `/api/tutor/post` (gia sư)

`MyPostView` thêm trường `resubmitAvailableAt` (ISO-8601 hoặc `null`):

- Có giá trị khi bài mới nhất `REJECTED`, chưa có bản đã duyệt và bài có `reviewedAt`: `reviewedAt` + 3 ngày.
- `null` ở mọi trường hợp khác.

```json
{
  "canPost": true,
  "blockedReason": null,
  "editable": false,
  "resubmitAvailableAt": "2026-12-03T14:30:00+07:00",
  "membershipExpiresOn": "2027-01-01",
  "post": { "reviewStatus": "REJECTED", "reviewFeedback": "...", "published": null }
}
```

## PUT `/api/tutor/post`

Trong thời gian chờ: **409**, lời nhắn:

> Bài đăng của bạn đã bị từ chối. Bạn có thể gửi bài đăng mới từ 14:30 ngày 03/12/2026.

Sau thời gian chờ: tạo bài mới `PENDING_REVIEW` như lần gửi đầu.
