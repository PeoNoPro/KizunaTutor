# API Contract: FE-06 Classrooms

Base: `http://localhost:8085/api`. Mọi endpoint yêu cầu `Authorization: Bearer <token>`.
Response bọc theo chuẩn dự án: `{ "success": bool, "message": string, "data": any, "timestamp": string }`. `message` luôn tiếng Việt.

| Mã | Khi nào |
|---|---|
| 400 | Dữ liệu/tệp không hợp lệ (kèm `data.fieldErrors` nếu là lỗi trường) |
| 401 | Thiếu/sai/hết hạn token |
| 403 | Sai vai trò hoặc không thuộc lớp |
| 404 | Lớp/tài liệu không tồn tại (hoặc không thuộc lớp) |
| 409 | Trùng lớp đang hoạt động; trùng lịch học; lớp ở chế độ chỉ xem |
| 413 | Tệp vượt 25 MB hoặc một lần tải vượt 100 MB |
| 503 | Kho lưu trữ chưa cấu hình/không phản hồi |

## GET /classrooms

Gia sư → lớp của mình; học viên → lớp mình tham gia. Sắp xếp `createdAt` giảm dần.

```json
{ "data": [ { "spaceId": "uuid", "title": "N4 cấp tốc", "status": "ACTIVE",
  "tutorId": "uuid", "tutorName": "…", "learnerId": "uuid", "learnerName": "…",
  "createdAt": "2026-11-03T10:00:00Z", "readOnly": false, "materialCount": 3,
  "schedules": [ { "scheduleId": "uuid", "dayOfWeek": 1, "startTime": "19:00", "endTime": "20:30" } ] } ] }
```
`dayOfWeek`: 1 = Thứ Hai … 7 = Chủ nhật. `schedules` sắp theo thứ rồi giờ bắt đầu.

## GET /classrooms/eligible-learners  *(chỉ gia sư)*

```json
{ "data": [ { "learnerId": "uuid", "fullName": "…", "email": "…", "avatarUrl": null } ] }
```

## POST /classrooms  *(chỉ gia sư)*

Tạo lớp = chọn học viên đã kết nối + tên lớp + lịch học hằng tuần. Chấp nhận kết nối KHÔNG tự tạo lớp.
Body `{ "learnerId": "uuid", "title": "string 1–150", "schedules": [ { "dayOfWeek": 1, "startTime": "19:00", "endTime": "20:30" } ] }` → 201, message "Tạo lớp học thành công.", `data` = ClassroomSummary.
Quy tắc lịch: 1–14 buổi/tuần; mỗi buổi 30 phút – 4 giờ, giờ bắt đầu < giờ kết thúc; không trùng giờ nhau, không trùng lịch các lớp ACTIVE/PAUSED của gia sư hoặc của học viên (không lộ tên lớp của gia sư khác); không trùng buổi học thử `CONFIRMED`/`RESCHEDULED` chưa diễn ra của gia sư hoặc học viên (đọc `trial_bookings`, giờ Việt Nam).
Lỗi: 400 tên lớp / lịch không hợp lệ (`fieldErrors.schedules`); 403 học viên chưa kết nối; 409 đã có lớp đang hoạt động; 409 trùng lịch; 409 hội viên hết hạn.

## GET /classrooms/schedules/me
Thời khóa biểu của người đang đăng nhập (gia sư hoặc học viên): lịch hằng tuần của mọi lớp `ACTIVE`/`PAUSED`, sắp theo thứ rồi giờ. Dùng cho lưới tuần "Lịch giảng dạy" (FE-05).
```json
{ "data": [ { "scheduleId": "uuid", "spaceId": "uuid", "classTitle": "Lớp N4", "partnerName": "Trần Minh Anh",
  "classStatus": "ACTIVE", "dayOfWeek": 1, "startTime": "19:00", "endTime": "20:30", "classStartedAt": "2026-11-08T09:00:00Z" } ] }
```
`partnerName` = tên học viên khi người xem là gia sư, tên gia sư khi người xem là học viên. Giao diện chỉ vẽ buổi học từ tuần mở lớp (`classStartedAt`) trở đi.

## Mở sẵn hộp thoại Tạo lớp
Trang `/tutor/classrooms?taoLop=1&learnerId={learnerId}` tự mở hộp thoại Tạo lớp và chọn sẵn học viên (dùng cho nút "Tạo lớp học" của trang Yêu cầu kết nối / Lịch giảng dạy). Học viên chưa đủ điều kiện thì hiện thông báo dưới ô chọn học viên.

## GET /classrooms/{spaceId}/schedules
Gia sư/học viên của lớp → danh sách buổi học.

## POST /classrooms/{spaceId}/schedules  *(gia sư của lớp)*
Body `{ "dayOfWeek": 3, "startTime": "18:00", "endTime": "19:30" }` → 201, message "Thêm buổi học thành công.". Quy tắc như trên; 409 nếu đủ 14 buổi, trùng lịch, lớp chỉ xem.

## PUT /classrooms/{spaceId}/schedules/{scheduleId}  *(gia sư của lớp)*
Body như POST → 200, message "Cập nhật lịch học thành công.". 404 nếu buổi học không thuộc lớp; 403 nếu không phải gia sư của lớp; 409 trùng lịch / lớp chỉ xem.

## DELETE /classrooms/{spaceId}/schedules/{scheduleId}  *(gia sư của lớp)*
Xóa một buổi học → 200, message "Xóa buổi học thành công.". 409 (`fieldErrors.schedules`) nếu đó là buổi cuối cùng của lớp (lớp luôn cần ít nhất 1 buổi/tuần); 404 nếu buổi học không thuộc lớp; 403/409 như PUT.
Lưu trữ: bảng `class_schedules` — migration `database/migrations/012_fe06_class_schedules.sql`.

## GET /classrooms/{spaceId}

Gia sư/học viên của lớp. `data` = ClassroomSummary + `canManage: bool` (chỉ true với gia sư khi không readOnly).

## GET /classrooms/{spaceId}/materials

```json
{ "data": [ { "materialId": "uuid", "title": "…", "description": "…", "fileType": "PDF",
  "files": [ { "fileId": "uuid", "fileName": "bai-1.pdf", "fileType": "PDF", "fileSize": 123456 } ],
  "createdAt": "…", "uploadedByName": "…" } ] }
```
Không trả `file_url` (đường dẫn nội bộ).

## POST /classrooms/{spaceId}/materials  *(gia sư của lớp)*

Một tài liệu = tiêu đề (bắt buộc, 1–150) + mô tả (tùy chọn) + **1–5 tệp** (PDF, DOC, DOCX, PNG, JPG, MP3; mỗi tệp ≤ 25 MB; mỗi lần ≤ 100 MB).
`multipart/form-data`: `title`, `description`, nhiều phần `file` → 201, `data` = LearningMaterial
`{materialId, title, description, fileType, files:[{fileId, fileName, fileType, fileSize}], createdAt, uploadedByName}`.
Tệp không hợp lệ ở bất kỳ tệp nào → 400, không lưu gì. Tệp lưu ở bảng `learning_material_files` (migration `014_fe06_learning_material_files.sql`).

## PUT /classrooms/{spaceId}/materials/{materialId}  *(gia sư của lớp)*

`multipart/form-data`: `title`, `description`, `removeFileIds` (lặp lại, id tệp cần gỡ), nhiều phần `file` (tệp thêm). Thay tệp = gỡ tệp cũ + thêm tệp mới.
Sau khi sửa phải còn 1–5 tệp → 200, `data` = LearningMaterial. 404 tệp cần gỡ không thuộc tài liệu.

## GET /classrooms/{spaceId}/materials/{materialId}/files/{fileId}

Gia sư/học viên của lớp → **nội dung một tệp** (không bọc JSON): `Content-Type` theo loại tệp, `Content-Disposition: inline; filename*=UTF-8''<tên tệp>`, `Cache-Control: private, no-store`; tệp Word trình duyệt tải về. Lỗi vẫn trả JSON chuẩn (401/403/404).

## DELETE /classrooms/{spaceId}/materials/{materialId}  *(gia sư của lớp — Delete Learning Material)*

Không có body. → 200, `data` = `null`, `message` = "Xóa tài liệu thành công.".
Lỗi: 403 không phải gia sư của lớp; 404 tài liệu không thuộc lớp; 409 lớp không hoạt động hoặc hội viên hết hạn (chỉ xem).
