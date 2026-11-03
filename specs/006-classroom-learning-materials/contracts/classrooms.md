# API Contract: FE-06 Classrooms

Base: `http://localhost:8085/api`. Mọi endpoint yêu cầu `Authorization: Bearer <token>`.
Response bọc theo chuẩn dự án: `{ "success": bool, "message": string, "data": any, "timestamp": string }`. `message` luôn tiếng Việt.

| Mã | Khi nào |
|---|---|
| 400 | Dữ liệu/tệp không hợp lệ (kèm `data.fieldErrors` nếu là lỗi trường) |
| 401 | Thiếu/sai/hết hạn token |
| 403 | Sai vai trò hoặc không thuộc lớp |
| 404 | Lớp/tài liệu không tồn tại (hoặc không thuộc lớp) |
| 409 | Trùng lớp đang hoạt động; lớp ở chế độ chỉ xem |
| 413 | Tệp vượt 10 MB |
| 503 | Kho lưu trữ chưa cấu hình/không phản hồi |

## GET /classrooms

Gia sư → lớp của mình; học viên → lớp mình tham gia. Sắp xếp `createdAt` giảm dần.

```json
{ "data": [ { "spaceId": "uuid", "title": "N4 cấp tốc", "status": "ACTIVE",
  "tutorId": "uuid", "tutorName": "…", "learnerId": "uuid", "learnerName": "…",
  "createdAt": "2026-11-03T10:00:00Z", "readOnly": false, "materialCount": 3 } ] }
```

## GET /classrooms/eligible-learners  *(chỉ gia sư)*

```json
{ "data": [ { "learnerId": "uuid", "fullName": "…", "email": "…", "avatarUrl": null } ] }
```

## POST /classrooms  *(chỉ gia sư)*

Body `{ "learnerId": "uuid", "title": "string 1–150" }` → 201, `data` = ClassroomSummary.
Lỗi: 400 tên lớp; 403 học viên chưa kết nối; 409 đã có lớp đang hoạt động; 409 hội viên hết hạn.

## GET /classrooms/{spaceId}

Gia sư/học viên của lớp. `data` = ClassroomSummary + `canManage: bool` (chỉ true với gia sư khi không readOnly).

## GET /classrooms/{spaceId}/materials

```json
{ "data": [ { "materialId": "uuid", "title": "…", "description": "…", "fileType": "PDF",
  "createdAt": "…", "uploadedByName": "…" } ] }
```
Không trả `file_url` (đường dẫn nội bộ).

## GET /classrooms/{spaceId}/materials/{materialId}/file

Kiểm quyền → 200 với **nội dung tệp** (không bọc JSON): `Content-Type` theo loại tệp, `Content-Disposition: inline; filename*=UTF-8''<tên tệp>`, `Cache-Control: private, no-store`. Lỗi vẫn trả JSON chuẩn (401/403/404).

## POST /classrooms/{spaceId}/materials  *(gia sư của lớp)*

`multipart/form-data`: `title` (bắt buộc), `description` (tùy chọn), `file` (bắt buộc). → 201, `data` = Material.

## PUT /classrooms/{spaceId}/materials/{materialId}  *(gia sư của lớp)*

`multipart/form-data`: `title` (bắt buộc), `description` (tùy chọn), `file` (tùy chọn — có thì thay tệp). → 200, `data` = Material, `message` = "Cập nhật tài liệu thành công."
