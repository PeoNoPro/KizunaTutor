# Research: FE-06 — Classroom & Learning Materials

> **Thay đổi thiết kế (2026-11-03, người phụ trách quyết định):** bỏ Supabase Storage; nội dung tệp lưu
> trực tiếp trong cột mới `learning_materials.file_data BYTEA` (migration `007`, người phụ trách tự chạy).
> `file_url` lưu tên tệp gốc đã làm sạch. Endpoint `/file` trả thẳng nội dung tệp sau khi kiểm quyền;
> frontend tải bằng `fetch` kèm token rồi mở bằng blob URL. Không cần bucket, khóa dịch vụ hay biến môi trường.
> Mục R1, R2 (phần storage) và R6 bên dưới giữ lại làm lịch sử quyết định.
> Đánh đổi: dung lượng DB tăng theo số tệp (tối đa 10 MB/tệp).

## R1. Lưu trữ tệp — Supabase Storage qua REST

- **Decision**: Gọi Supabase Storage REST API bằng `org.springframework.web.client.RestClient` (có sẵn trong Spring Boot 3.3 / Spring 6.1, **không thêm dependency, không sửa `pom.xml`**).
  - Tải lên: `POST {SUPABASE_URL}/storage/v1/object/{bucket}/{path}` — header `Authorization: Bearer {service_key}`, `apikey`, `Content-Type`, `x-upsert: false`.
  - Ký liên kết: `POST {SUPABASE_URL}/storage/v1/object/sign/{bucket}/{path}` body `{"expiresIn": 300}` → `{"signedURL": "/object/sign/..."}`; URL trả về = `{SUPABASE_URL}/storage/v1` + `signedURL`.
  - Xóa: `DELETE {SUPABASE_URL}/storage/v1/object/{bucket}` body `{"prefixes": [path]}`.
- **Rationale**: Người phụ trách đã có dự án Supabase; bucket riêng tư + signed URL đảm bảo chỉ người có quyền mở được tệp (FR-010). Không dùng SDK để tránh sửa `pom.xml`.
- **Alternatives**: Lưu đĩa máy chủ (mất tệp khi đổi máy, không mở rộng); nhập URL (không đáp ứng "upload file" của UC-65).
- **Đường dẫn đối tượng**: `spaces/{spaceId}/{uuid}.{ext}` (ext suy từ loại tệp đã kiểm, không dùng tên gốc). Lưu vào `file_url` (VARCHAR 500 đủ).
- **Secret**: service role key chỉ đọc từ biến môi trường `SUPABASE_SERVICE_ROLE_KEY`; không log, không trả về client.

## R2. Cấu hình

Thêm vào `backend/src/main/resources/application.properties` (được duyệt theo Q2 — cần cấu hình mới):

```properties
# 12. CAU HINH SUPABASE STORAGE (FE-06 tai lieu hoc tap)
app.storage.supabase.url=${SUPABASE_URL:}
app.storage.supabase.service-key=${SUPABASE_SERVICE_ROLE_KEY:}
app.storage.supabase.bucket=${SUPABASE_STORAGE_BUCKET:learning-materials}
app.storage.supabase.signed-url-ttl-seconds=300
spring.servlet.multipart.max-file-size=12MB
spring.servlet.multipart.max-request-size=13MB
```

- Multipart mặc định của Spring là 1 MB → bắt buộc nâng lên để đạt 10 MB. Ảnh hưởng toàn ứng dụng: chỉ nới giới hạn, không làm hỏng endpoint khác.
- Đặt 12 MB (lớn hơn giới hạn nghiệp vụ 10 MB) vì `MaxUploadSizeExceededException` phát sinh trước khi xác định controller nên advice riêng của classroom không bắt được; tệp 10–12 MB đi tới service và nhận lỗi 413 tiếng Việt. Tệp > 12 MB gửi thẳng API nhận thông báo chung của `GlobalExceptionHandler`; frontend chặn > 10 MB trước khi gửi.

## R3. Kiểm tra tệp

- Cho phép: `application/pdf` (`%PDF`), `image/png` (`89 50 4E 47 0D 0A 1A 0A`), `image/jpeg` (`FF D8 FF`), `audio/mpeg` (`ID3` hoặc frame sync `FF Fx` với x ∈ {B, 3, 2, A}).
- Đuôi tệp hợp lệ: `.pdf .png .jpg .jpeg .mp3`. Đuôi + chữ ký đầu tệp phải cùng một loại; `Content-Type` client chỉ để tham khảo.
- `file_type` lưu giá trị chuẩn hóa: `PDF`, `PNG`, `JPG`, `MP3` (≤ 50 ký tự).

## R4. Xác thực người dùng hiện tại

- **Decision**: `features/classroom/util/CurrentUserResolver` đọc header `Authorization: Bearer <token>`, `JwtTokenProvider.validateToken` → `getEmailFromToken` → `UserRepository.findByEmail` → `TutorRepository/LearnerRepository.findByUser_UserId`. Cùng cách `AuthController` đang làm; không sửa `SecurityConfig` hay `core/`.
- Thiếu/sai token → 401 "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."; sai vai trò → 403.

## R5. Điều kiện kết nối

- **Decision**: Native query chỉ đọc trong `LearningSpaceRepository` (classroom):
  - Học viên đủ điều kiện: `SELECT DISTINCT cr.learner_id FROM connection_requests cr WHERE cr.tutor_id = :tutorId AND cr.status = 'ACCEPTED' AND NOT EXISTS (SELECT 1 FROM learning_spaces ls WHERE ls.tutor_id = cr.tutor_id AND ls.learner_id = cr.learner_id AND ls.status IN ('ACTIVE','PAUSED'))`.
  - Kiểm một cặp: `SELECT EXISTS (... status = 'ACCEPTED')`.
- So sánh enum PG với chuỗi literal hợp lệ trong PostgreSQL (ép kiểu ngầm từ literal). Không import entity `ConnectionRequest` của `tutor_search`.

## R6. Migration

- `database/migrations/` đang trống. Tạo `database/migrations/006_fe06_learning_materials_description.sql`:
  ```sql
  ALTER TABLE learning_materials ADD COLUMN IF NOT EXISTS description TEXT;
  ```
  Idempotent, không DROP, không đổi cột khác. Không chạy tự động; người phụ trách chạy trên Supabase (hoặc cho phép chạy) sau khi duyệt.
- Đồng bộ tài liệu schema: thêm dòng `description TEXT,` vào khối `CREATE TABLE learning_materials` trong `database/DatabasePgAdmin4.txt` (chỉ sửa văn bản, **không chạy file này**) — để nguồn schema của constitution không lệch với DB.

## R7. Phát hiện ngoài dự kiến

- `LearningMaterial.java` hiện ánh xạ sai (`materialId` thay vì `material_id`, kế thừa `BaseEntity` có `updated_at` không tồn tại trong bảng). Sửa mapping trong package classroom (thuộc FE-06).
- Đăng nhập gia sư chuyển hướng tới `/tutor/dashboard` (chưa tồn tại, không thuộc FE-06). Trang FE-06 dành cho gia sư đặt tại `/tutor/classrooms`.
