# API Contract: FE-07 Assignments

Base: `/api/classrooms/{spaceId}/assignments`. Header `Authorization: Bearer <token>`. Phản hồi `{success, message, data, timestamp}` như FE-06; lỗi 400/401/403/404/409/413 với message tiếng Việt.

## GET /
Gia sư/học viên của lớp. Sắp xếp `createdAt` giảm dần.
```json
{"data":[{"assignmentId":"uuid","title":"…","description":"…","dueDate":"…|null","totalPoints":100.00,
  "attachments":[{"fileId":"uuid","fileName":"de-bai.pdf","fileType":"PDF|DOC|DOCX|PNG|JPG|MP3","fileSize":123456}],"createdAt":"…","dueOver":false,"submissionState":"NOT_SUBMITTED|ON_TIME|LATE|OVERDUE_NOT_SUBMITTED|GRADED",
  "attemptCount":0,"grade":null,"canSubmit":true,"canEditSubmission":false,"canGrade":false,"canEdit":true}]}
```
`submissionState` là mã nội bộ; UI ánh xạ sang nhãn tiếng Việt.

## POST /  *(gia sư — giao bài tập, MỘT lần gửi)*
`multipart/form-data`: `title`, `description`, `dueDate` (ISO, tùy chọn, vd `2026-11-10T16:59:00Z`), `totalPoints` (tùy chọn, mặc định 100), nhiều phần `file` (tệp đề bài, tối đa 5).
Kiểm tra tất cả: tiêu đề; **nội dung hoặc ≥ 1 tệp**; hạn nộp ở tương lai; điểm 1–999,99; từng tệp (định dạng, nội dung khớp đuôi, ≤ 25 MB).
**Lỗi bất kỳ (kể cả một tệp) → không lưu gì**; lỗi tệp ghi rõ tên tệp: `Tệp "x.docx": …` (`fieldErrors.file`). → 201 AssignmentSummary.

## GET /{assignmentId}
`data` = AssignmentSummary + `submissions: [{submissionId, attemptNumber, content, files:[{fileId, fileName, fileType, fileSize}], submittedAt, onTime, grade, tutorFeedback, graded, gradedAt}]`.
Mỗi học viên **một** bài nộp cho mỗi bài tập: mảng có 0 hoặc 1 phần tử (dữ liệu cũ nhiều lần nộp → trả lần mới nhất).

## PUT /{assignmentId}  *(gia sư — sửa bài tập, MỘT lần gửi)*
`multipart/form-data` như POST + `removeFileIds` (lặp lại, id tệp đề bài cần gỡ). Thay tệp = gỡ + thêm trong cùng lần gửi.
Chỉ kiểm tra **kết quả cuối** (≤ 5 tệp; nội dung hoặc ≥ 1 tệp); lỗi bất kỳ → bài tập giữ nguyên như trước. → 200, "Cập nhật bài tập thành công." 409 nếu đã có bài được chấm.

## Tệp đề bài — `assignment_files` (migration `013_fe07_multi_files.sql`)
Lưu cùng lúc với POST/PUT bài tập (không còn API thêm/gỡ tệp riêng). Định dạng PDF, DOC, DOCX, PNG, JPG, MP3; mỗi tệp ≤ 25 MB; ≤ 5 tệp; mỗi lần gửi ≤ 100 MB (413 nếu vượt).
- `GET /{assignmentId}/files/{fileId}` *(gia sư/học viên của lớp)* → nội dung tệp (`inline`; tệp Word trình duyệt tải về).

## POST /{assignmentId}/submissions  *(học viên — nộp lần đầu)*
`multipart/form-data`: `content` (tùy chọn), nhiều phần `file` (tùy chọn, ≤ 5 tệp); cần nội dung hoặc ít nhất một tệp. → 201, message "Nộp bài tập thành công."
Còn hạn, hoặc quá hạn nhưng chưa nộp lần nào (nộp muộn 1 lần). 409 nếu đã nộp (phải sửa bài nộp), đã chấm, lớp chỉ xem.

## PUT /{assignmentId}/submissions/{submissionId}  *(học viên — sửa bài đã nộp)*
`multipart/form-data`: `content` (nội dung mới), `removeFileIds` (lặp lại, id tệp cần gỡ), nhiều phần `file` (tệp thêm). → 200, message "Cập nhật bài nộp thành công.", `submittedAt` cập nhật.
Chỉ khi còn hạn (bài không có hạn: tới khi được chấm) và chưa được chấm. Sau khi sửa vẫn phải còn nội dung hoặc ≥ 1 tệp; tổng ≤ 5 tệp.
Lỗi: 400 dữ liệu/tệp; 403 không phải bài nộp hiện tại của mình; 404 tệp cần gỡ không thuộc bài nộp; 409 quá hạn / đã chấm / lớp chỉ xem.

## GET /{assignmentId}/submissions/{submissionId}/files/{fileId}
Gia sư/học viên của lớp → nội dung tệp bài làm (`inline`), lỗi trả JSON. Lưu ở bảng `submission_files`.

## PUT /{assignmentId}/submissions/{submissionId}/grade  *(gia sư)*
`{"grade": 85.5, "feedback": "…"}` → 200, message "Đã gửi điểm và nhận xét." 409 nếu chưa hết hạn / đã chấm / không phải bài nộp hiện tại.
