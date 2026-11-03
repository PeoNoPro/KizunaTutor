# Bảng lịch sử quản lý nhân sự

Nguồn schema: `database/seeds/admin_staff_schema.sql`. Chỉ bổ sung bảng audit và index; không sửa/xóa tài khoản hoặc vai trò có sẵn. Tài khoản và audit được ghi cùng transaction qua datasource nghiệp vụ, không dùng pool quan sát chỉ đọc.

Trong chế độ phát triển, `scripts/start-admin-dev.ps1` kiểm tra PostgreSQL loopback, DB có hậu tố `_admin_dev`, schema FE-11 và tài khoản quản trị hoạt động, rồi chạy riêng file bổ sung này với `ON_ERROR_STOP`. DB đích đã có được giữ nguyên dữ liệu. Script không đọc hoặc migrate Supabase.

Trước khi áp dụng lên môi trường dùng chung, người phụ trách DB cần xác nhận bảng users/khóa UUID khớp, kiểm tra quyền tạo bảng/index, sao lưu và phục hồi thử theo quy trình nhóm. Sau đó chạy trên đúng DB đã kiểm tra:

```powershell
psql -X --set ON_ERROR_STOP=on --file database/seeds/admin_staff_schema.sql
```

Lệnh dùng thông tin kết nối được quản lý riêng, không lưu mật khẩu vào Git. Kiểm tra bảng `staff_account_audit_events` và index, thử tạo/sửa tài khoản trên DB kiểm thử. Lỗi ghi audit phải rollback thay đổi tài khoản. Không xóa bảng/lịch sử để rollback tùy ý; phục hồi bằng bản backup đã kiểm chứng nếu cần.
