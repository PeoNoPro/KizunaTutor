# Contract: FE-03 Verification REST API

Base URL: `http://localhost:8085/api` (qua `frontend/js/config/api.config.js`). Mọi request gửi `Authorization: Bearer <kizuna_token>`. JSON UTF-8. Ngày giờ trả về ISO-8601 có offset; frontend định dạng `dd/MM/yyyy`. Các trường kiểu mã (`status`, `type`, `action`, `reasonCode`) luôn đi kèm trường `…Label` tiếng Việt; UI chỉ hiển thị trường `…Label`.

## Lỗi chung

```json
{ "code": "VERIFICATION_LOCKED", "message": "Hồ sơ của bạn đang được thẩm định nên chưa thể chỉnh sửa. Vui lòng liên hệ bộ phận hỗ trợ nếu cần thay đổi gấp.", "fields": { "phone": "Số điện thoại chưa đúng định dạng." } }
```

| HTTP | `code` | `message` (hiển thị) |
| --- | --- | --- |
| 401 | `UNAUTHENTICATED` | Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại. |
| 403 | `FORBIDDEN` | Bạn không có quyền thực hiện thao tác này. |
| 404 | `NOT_FOUND` | Không tìm thấy hồ sơ hoặc giấy tờ được yêu cầu. |
| 400 | `VALIDATION_FAILED` | Thông tin chưa hợp lệ. Vui lòng kiểm tra các trường được đánh dấu. (+ `fields`) |
| 400 | `SUBMISSION_INCOMPLETE` | Không thể nộp hồ sơ. Vui lòng hoàn thiện các trường bắt buộc và tải lên đủ giấy tờ trước khi nộp. (+ `fields`, `missingDocuments`) |
| 400 | `COMMITMENT_REQUIRED` | Vui lòng xác nhận cam kết trước khi nộp hồ sơ. |
| 400 | `INVALID_FILE` / 413 | MSG13 — Tệp không hợp lệ. Vui lòng chọn tệp PDF, PNG hoặc JPG có dung lượng tối đa 10MB. |
| 409 | `VERIFICATION_LOCKED` | (như ví dụ trên — UC-23 AF-1) |
| 409 | `VERIFICATION_REJECTED` | Hồ sơ của bạn đã bị từ chối nên không thể chỉnh sửa hoặc nộp lại. Vui lòng liên hệ bộ phận hỗ trợ nếu bạn cần trao đổi thêm. |
| 409 | `ALREADY_PROCESSED` | Hồ sơ này vừa được xử lý bởi cán bộ khác. Vui lòng tải lại trang. |
| 500 | `INTERNAL_ERROR` | Hệ thống đang bận, bạn vui lòng thử lại sau. |

Không bao giờ trả stack trace, tên exception hay `storageKey`.

## Phía gia sư — `ROLE_TUTOR`, chỉ hồ sơ của chính mình

### `GET /tutor/verification` (UC-22, UC-82)

```json
{
  "status": "ADDITIONAL_INFO_REQUIRED", "statusLabel": "Cần bổ sung thông tin",
  "editable": true,
  "submittedAt": "2026-11-03T09:15:00+07:00", "verifiedAt": null,
  "profile": { "fullName": "…", "phone": "…", "dateOfBirth": "1995-04-12", "address": "…", "bio": "…", "experienceYears": 3, "highestJlptLevel": "N2" },
  "documents": [ { "id": "uuid", "type": "JLPT_CERTIFICATE", "typeLabel": "Chứng chỉ JLPT", "title": "…", "fileName": "jlpt-n2.pdf", "contentType": "application/pdf", "status": "PENDING", "statusLabel": "Chờ thẩm định", "uploadedAt": "…", "fileAvailable": true } ],
  "missingRequirements": [ "Giấy tờ tùy thân" ],
  "latestFeedback": { "kind": "REQUEST_INFO", "kindLabel": "Yêu cầu bổ sung", "content": "…", "createdAt": "…" }
}
```

`latestFeedback` null khi chưa có phản hồi. Không có trường ghi chú nội bộ. `fileName` là tên hiển thị được sinh từ tiêu đề + đuôi, không phải tên lưu.

### `PUT /tutor/verification` (UC-23) — lưu nháp

Body = object `profile` ở trên (mọi trường tùy chọn). 200 → payload như GET, kèm `message`: "Đã cập nhật thông tin xác minh. Vui lòng nộp hồ sơ khi đã sẵn sàng." 409 `VERIFICATION_LOCKED` nếu không thể chỉnh sửa.

### `POST /tutor/verification/documents` (UC-23) — multipart

Phần: `type` (mã `CredentialType`), `title` (1–150), `file`. 201 → object document. 400/413 `INVALID_FILE`; 409 `VERIFICATION_LOCKED`.

### `PUT /tutor/verification/documents/{documentId}` — multipart

Phần tùy chọn: `type`, `title`, `file`. Thay tệp → `status` về `PENDING`, `uploadedAt` mới.

### `DELETE /tutor/verification/documents/{documentId}`

204. Gỡ được kể cả giấy tờ bắt buộc cuối cùng (chỉ chặn ở bước nộp).

### `GET /tutor/verification/documents/{documentId}/file`

Byte tệp; header `Content-Type` (pdf/png/jpeg), `Content-Disposition: inline; filename="…"`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`. 404 nếu giấy tờ không thuộc gia sư hiện tại **hoặc** tệp mất trên máy chủ (UI hiển thị "Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ.").

### `POST /tutor/verification/submit` (UC-24)

Body `{ "commitment": true }`. 200 → payload như GET, `message`: MSG12 (nội dung tại spec, mục "Thông điệp hệ thống dùng trong FE-03"). Lỗi: `COMMITMENT_REQUIRED`, `SUBMISSION_INCOMPLETE`, `VERIFICATION_LOCKED` (đang chờ/đã duyệt — gồm cả lần bấm thứ hai).

`UC-83` (xem phản hồi) dùng `latestFeedback` của `GET /tutor/verification`; không có endpoint riêng.

## Phía cán bộ học vụ — `ROLE_ACADEMIC_OFFICER`

### `GET /academic/verifications/summary` (III.3.1.1)

```json
{ "totalTutors": 6, "notSubmittedCount": 1, "pendingCount": 12, "pendingNewToday": 3, "pendingOver24h": 2,
  "needsInfoCount": 4, "approvedCount": 40, "rejectedCount": 3, "approvedThisMonth": 8, "rejectedThisMonth": 2 }
```

(Mở rộng khi triển khai 2026-11-03 để phục vụ thiết kế Stitch "Cổng học vụ": tổng gia sư, phân bố trạng thái, hồ sơ chờ quá 24 giờ.)

### `GET /academic/verifications` (UC-25)

Query: `status` = `PENDING` (mặc định) | `ADDITIONAL_INFO_REQUIRED` | `APPROVED` | `REJECTED` | `ALL` (mọi hồ sơ đã từng nộp; không gồm "Chưa nộp hồ sơ"); `keyword` (≤ 100, tên hoặc email); `jlptLevel` (`N1`…`N5`); `submittedFrom`, `submittedTo` (`yyyy-MM-dd`, bao gồm hai đầu); `sort` = `oldest` (mặc định) | `newest` (hồ sơ chưa có ngày nộp luôn ở cuối); `page` (0-based), `size` (mặc định 10, tối đa 50). Giá trị lọc sai → 400 `VALIDATION_FAILED`. (Thay cho tham số `group` của bản thiết kế ban đầu.)

```json
{ "content": [ { "tutorId": "uuid", "fullName": "…", "email": "…", "highestJlptLevel": "N2", "experienceYears": 3, "submittedAt": "…", "status": "PENDING", "statusLabel": "Đang chờ thẩm định" } ],
  "page": 0, "size": 10, "totalElements": 12, "totalPages": 2 }
```

Sắp xếp `submittedAt` cũ nhất trước. `content` rỗng → UI hiển thị MSG01 (có bộ lọc) hoặc "Hiện không còn hồ sơ nào chờ thẩm định." (không có bộ lọc, nhóm PENDING).

### `GET /academic/verifications/recent-decisions?limit=10` (III.3.1.1)

```json
[ { "tutorId": "uuid", "fullName": "…", "action": "REJECT", "actionLabel": "Từ chối", "reasonLabel": "Thông tin không trùng khớp với giấy tờ", "officerName": "…", "createdAt": "…" } ]
```

### `GET /academic/verifications/{tutorId}` (UC-27)

Như payload GET phía gia sư (không có `editable`), thêm `email`, `tutorId`. Hồ sơ `UNVERIFIED` → 404.

### `GET /academic/verifications/{tutorId}/documents/{documentId}/file`

Như endpoint tải tệp phía gia sư; 404 nếu giấy tờ không thuộc `tutorId`.

### `POST /academic/verifications/{tutorId}/request-info` (UC-26)

Body `{ "message": "…" }` (1–2.000 ký tự sau trim; rỗng → 400 "Vui lòng nêu rõ thông tin cần bổ sung trước khi gửi yêu cầu."). 200 `{ "message": "Đã gửi yêu cầu bổ sung thông tin.", "status": "ADDITIONAL_INFO_REQUIRED", "statusLabel": "…" }`. 409 `ALREADY_PROCESSED` nếu không còn `PENDING`.

### `POST /academic/verifications/{tutorId}/approve` (UC-28)

Body `{}`. 200 `{ "message": "Đã phê duyệt hồ sơ gia sư.", "status": "APPROVED", … }`. 409 `ALREADY_PROCESSED`.

### `POST /academic/verifications/{tutorId}/reject` (UC-28)

Body `{ "reasonCode": "INFORMATION_MISMATCH", "feedback": "…(0–2.000, gửi gia sư)", "internalNote": "…(1–2.000, bắt buộc)" }`. Thiếu `reasonCode` hoặc `internalNote` → 400 "Vui lòng chọn lý do từ chối và nhập ghi chú trước khi xác nhận." 409 `ALREADY_PROCESSED`.

### `GET /academic/verifications/{tutorId}/history` (UC-84)

```json
[ { "action": "SUBMIT", "actionLabel": "Nộp hồ sơ", "feedback": null, "reasonLabel": null, "internalNote": null, "officerName": null, "createdAt": "…" } ]
```

Sắp xếp `createdAt` tăng dần. Mảng rỗng → "Chưa có lịch sử thẩm định." `officerName` null với `SUBMIT` → UI hiển thị "Gia sư".

## Nhãn hành động lịch sử

`SUBMIT` Nộp hồ sơ · `REQUEST_INFO` Yêu cầu bổ sung · `APPROVE` Phê duyệt · `REJECT` Từ chối.

## Route trang (frontend dev server)

| Alias | Trang |
| --- | --- |
| `/tutor/verification` | `pages/tutor-verification/index.html` |
| `/academic/verifications`, `/academic/dashboard` | `pages/academic-verifications/index.html` |
| `/academic/verifications/detail?tutorId=…` | `pages/academic-verification-detail/index.html` |
