# Quickstart: FE-06

## Chuẩn bị (người phụ trách thực hiện)

1. Supabase → Storage → tạo bucket **riêng tư** `learning-materials`.
2. Đặt biến môi trường (PowerShell, phiên hiện tại):
   ```powershell
   $env:SUPABASE_URL = "https://<project-ref>.supabase.co"
   $env:SUPABASE_SERVICE_ROLE_KEY = "<service role key>"   # không commit, không dán vào chat
   ```
3. Chạy migration trên DB (SQL Editor của Supabase):
   ```sql
   ALTER TABLE learning_materials ADD COLUMN IF NOT EXISTS description TEXT;
   ```
   Kiểm tra: `SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name='learning_materials';`

## Chạy

```powershell
cd backend; .\mvnw.cmd spring-boot:run      # cổng 8085
node frontend/server.js                       # cổng 3000
```

## Kiểm thử tự động

```powershell
cd backend; .\mvnw.cmd test -Dtest="ClassroomServiceImplTest,LearningMaterialServiceImplTest,MaterialFileValidatorTest,CurrentUserResolverTest"
node --test "frontend/tests/*.test.js"
```

## Kiểm thử thủ công

1. Đăng nhập gia sư có yêu cầu kết nối `ACCEPTED` → mở `http://localhost:3000/tutor/classrooms` → "Thêm học viên vào lớp" → tạo lớp.
2. Mở lớp → tải lên PDF < 10 MB; thử tệp `.exe` đổi đuôi `.pdf`, tệp 11 MB → phải bị từ chối.
3. Sửa tiêu đề, thay tệp → mở tài liệu thấy tệp mới.
4. Đăng nhập học viên → menu "Lớp học của tôi" → xem lớp, mở tài liệu.
5. Đăng nhập học viên khác → gọi `GET /api/classrooms/{spaceId}/materials` → 403.
