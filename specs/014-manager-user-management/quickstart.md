# Quickstart: FE-14 — Quản lý người dùng

## Chạy

```powershell
# backend (JDK 17, Supabase qua cổng 6543)
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'; $env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0'
cd backend; .\mvnw.cmd spring-boot:run

# frontend
node frontend/server.js
```

Không cần chạy SQL: FE-14 không đổi schema.

## Kiểm thử tự động

```powershell
cd backend; .\mvnw.cmd test -Dtest=UserManagementServiceTest
node --test frontend/tests/manager-users-helpers.test.mjs
```

## Kiểm tra tay

1. Đăng nhập tài khoản Quản lý vận hành → sidebar có "Quản lý người dùng" → mở `/manager/users`.
2. Nhóm "Gia sư" hiển thị mặc định; đổi sang "Học viên"; tìm theo email; lọc "Chờ kích hoạt".
3. Tìm từ khóa không có → trạng thái rỗng "Không tìm thấy người dùng phù hợp với điều kiện tìm kiếm."
4. "Xem chi tiết" một gia sư, một học viên: thông tin hồ sơ đúng; đóng quay lại đúng trang.
5. Dùng **tài khoản thử** (không dùng tài khoản thật của thành viên): "Cập nhật trạng thái" → "Tạm khóa", lý do "Kiểm thử FE-14", xác nhận.
   - Thông báo "Đã cập nhật trạng thái tài khoản thành Tạm khóa."; lịch sử có 1 dòng.
   - Ở trình duyệt khác, tài khoản thử đang đăng nhập bị từ chối ở thao tác kế tiếp; đăng nhập lại thất bại.
   - Hộp thư tài khoản thử nhận email (không chứa lý do nội bộ).
6. Đổi lại "Đang hoạt động" → tài khoản thử đăng nhập lại được; email báo khôi phục.
7. Mở biểu mẫu, bấm Hủy → không có dòng lịch sử mới.
8. Để trống lý do → báo lỗi tại ô lý do. Tài khoản "Chờ kích hoạt" → không có nút đổi, có giải thích.
9. Đăng nhập vai trò khác gọi `GET /api/manager/users` → 403.

## Dọn dẹp sau kiểm thử

Trả tài khoản thử về trạng thái ban đầu bằng chính chức năng (bước 6). Bản ghi nhật ký là dấu vết kiểm toán, giữ nguyên.
