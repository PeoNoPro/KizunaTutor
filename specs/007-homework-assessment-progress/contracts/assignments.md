# API Contract: FE-07 Assignments

Base: `/api/classrooms/{spaceId}/assignments`. Header `Authorization: Bearer <token>`. Phản hồi `{success, message, data, timestamp}` như FE-06; lỗi 400/401/403/404/409/413 với message tiếng Việt.

## GET /
Gia sư/học viên của lớp. Sắp xếp `createdAt` giảm dần.
```json
{"data":[{"assignmentId":"uuid","title":"…","description":"…","dueDate":"…|null","totalPoints":100.00,
  "attachmentName":"de-bai.pdf|null","attachmentType":"PDF|PNG|JPG|MP3|null","createdAt":"…","dueOver":false,"submissionState":"NOT_SUBMITTED|ON_TIME|LATE|OVERDUE_NOT_SUBMITTED|GRADED",
  "attemptCount":0,"grade":null,"canSubmit":true,"canGrade":false,"canEdit":true}]}
```
`submissionState` là mã nội bộ; UI ánh xạ sang nhãn tiếng Việt.

## POST /  *(gia sư)*
`{"title":"…","description":"…","dueDate":"2026-11-10T23:59:00+07:00","totalPoints":100}` → 201 AssignmentSummary.

## GET /{assignmentId}
`data` = AssignmentSummary + `submissions: [{submissionId, attemptNumber, content, fileName, fileType, submittedAt, onTime, grade, tutorFeedback, graded, gradedAt}]` (cũ → mới).

## PUT /{assignmentId}  *(gia sư)*
Body như POST → 200, message "Cập nhật bài tập thành công." 409 nếu đã có bài được chấm.

## PUT /{assignmentId}/attachment  *(gia sư — tệp đề bài)*
`multipart/form-data`: `file` (bắt buộc; PDF, PNG, JPG, MP3; ≤ 10 MB). Thêm hoặc thay tệp đề bài (mỗi bài tập tối đa 1 tệp).
→ 200, `data` = `{attachmentName, attachmentType}`, message "Đính kèm tệp cho bài tập thành công."
Lỗi: 400 tệp không hợp lệ; 403 không phải gia sư của lớp; 404 bài tập không thuộc lớp; 409 đã có bài được chấm / lớp chỉ xem; 413 vượt dung lượng.
UI gọi endpoint này ngay sau POST/PUT bài tập nếu gia sư có chọn tệp.

## DELETE /{assignmentId}/attachment  *(gia sư)*
Gỡ tệp đề bài → 200, `data` = `{attachmentName: null, attachmentType: null}`. 404 nếu bài tập chưa có tệp; 403/409 như trên.

## GET /{assignmentId}/attachment
Gia sư/học viên của lớp → nội dung tệp đề bài (`Content-Disposition: inline`), lỗi trả JSON (404 nếu không có tệp).
Lưu trữ: `assignments.attachment_url` (tên tệp đã làm sạch) + `assignments.file_data` (BYTEA) — migration `database/migrations/009_fe07_assignments_attachment.sql`.

## POST /{assignmentId}/submissions  *(học viên)*
`multipart/form-data`: `content` (tùy chọn), `file` (tùy chọn); cần ít nhất một. → 201, message "Nộp bài tập thành công." 409 nếu quá hạn đã nộp / đã chấm / lớp chỉ xem.

## GET /{assignmentId}/submissions/{submissionId}/file
Gia sư/học viên của lớp → nội dung tệp (`Content-Disposition: inline`), lỗi trả JSON.

## PUT /{assignmentId}/submissions/{submissionId}/grade  *(gia sư)*
`{"grade": 85.5, "feedback": "…"}` → 200, message "Đã gửi điểm và nhận xét." 409 nếu chưa hết hạn / đã chấm / không phải lần nộp mới nhất.
