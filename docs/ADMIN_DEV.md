# Duyệt giao diện quản trị khi chưa có đăng nhập

Môi trường này chỉ dành cho phát triển trên máy cá nhân. Nó dùng tài khoản quản trị đang hoạt động trong PostgreSQL cục bộ và vẫn kiểm tra quyền/trạng thái tài khoản trong DB ở mỗi yêu cầu. Không phát hành token và không thay đổi đăng nhập của bản chạy thật.

## Chạy trên workspace hiện tại

Cửa sổ PowerShell thứ nhất:

```powershell
cd "D:\do an 2\KizunaTutor"
powershell -ExecutionPolicy Bypass -File .\scripts\start-admin-dev.ps1
```

Đợi backend báo `Started ...` tại cổng 8085. Script khởi động PostgreSQL cục bộ cổng 5433 nếu cần, tạo DB `kizuna_admin_dev` từ DB mẫu **cục bộ** `kizuna_db` nếu DB đích chưa tồn tại. Không ghi đè DB đã có, không lấy dữ liệu Supabase. Sau preflight, chỉ chuẩn bị bảng lịch sử nhân sự từ `database/seeds/admin_staff_schema.sql` trên DB phát triển riêng; không tự chạy migration toàn hệ thống. Dữ liệu có sẵn trong bản sao là dữ liệu phát triển; các thay đổi tạo trong giao diện chỉ lưu ở bản sao đó.

Cửa sổ PowerShell thứ hai:

```powershell
cd "D:\do an 2\KizunaTutor"
node frontend/server.js
```

Mở <http://localhost:3000/admin/dashboard> hoặc <http://localhost:3000/pages/admin-dashboard/>. Không cần đi qua trang đăng nhập. Địa chỉ gốc <http://localhost:3000/> dẫn về trang chủ công khai. Header hiện tên và ảnh trong DB; nếu chưa có ảnh dùng chữ viết tắt tên. Trang công khai vẫn truy cập trực tiếp ở `/pages/home/`.

Hai mục sidebar đã sử dụng được: `/pages/admin-staff/` cho tạo/sửa/tìm kiếm/khóa/mở tài khoản nhân viên và `/pages/admin-roles/` cho xem quyền quản trị theo vai trò, gán vai trò nội bộ. Thay đổi được lưu DB cùng lịch sử. Không tự khóa/đổi vai trò tài khoản đang dùng; không có quyền tùy chỉnh theo từng nút. Mật khẩu mới lưu BCrypt, không gửi email tự động. Đăng nhập thật và đổi mật khẩu lần đầu vẫn do nhóm auth triển khai.

Các trang cấu hình, sức khỏe, danh sách/chi tiết sự cố và báo cáo cùng dùng sidebar quản trị. Bạn có thể thử ghi nhận, nhận xử lý và cập nhật sự cố trong DB phát triển.

Trang **Báo cáo tổng hợp** ở `/pages/manager-reports/` dành cho Admin/Manager; trang **Hồ sơ cá nhân** ở `/pages/admin-profile/` xem đúng tài khoản hiện tại. Manager chỉ thấy báo cáo và hồ sơ, không có quyền vào nhân viên/cấu hình/sự cố. Báo cáo lọc kết nối/học thử theo ngày tạo trong kỳ và dùng trạng thái hiện tại, không phải trạng thái lịch sử cuối kỳ.

Nếu cần kiểm thử riêng bằng tài khoản Manager đang có trong DB, dừng backend hiện tại rồi chạy script với `-AccountRole Manager`. Khi mở dashboard gốc, phiên Manager được chuyển về báo cáo. Mặc định script vẫn chọn Admin; không tự chuyển vai trò tài khoản trong DB. Phần cấu hình/giới hạn AI được để sau theo yêu cầu mới nhất của người dùng.

Bốn thẻ tài khoản và phần cơ cấu lấy số đếm thật từ DB đang kết nối. DB cục bộ hiện có 6 tài khoản: 1 học viên, 1 gia sư, 4 nhân sự. Con số thay đổi theo dữ liệu trong DB. Lưu lượng/thiết bị và chức năng chưa có nguồn vẫn hiển thị chưa có dữ liệu; không dùng số minh họa Figma. Sau khi cập nhật mã backend, dừng backend cũ bằng Ctrl+C và chạy lại script để nạp API mới.

## Nếu máy khác chưa có DB cục bộ

Script trên dành cho cluster `.kizuna-local-pg/data` đã chuẩn bị trong workspace này. Không sao chép thư mục dữ liệu PostgreSQL đang chạy sang máy khác. Máy khác cần PostgreSQL cục bộ và DB riêng có tên kết thúc `_admin_dev`, schema của dự án cùng schema FE-11 theo `database/migrations/README-fe11.md`, và một tài khoản quản trị phát triển đang hoạt động. Người phụ trách DB có thể chuẩn bị bản sao phát triển theo quy trình của nhóm; không chạy `DatabasePgAdmin4.txt` lên dữ liệu hiện hữu.

Sau khi chuẩn bị, có thể chạy trực tiếp trong `backend/`:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'admin-dev'
$env:ADMIN_DEV_DB_URL = 'jdbc:postgresql://127.0.0.1:5433/kizuna_admin_dev'
$env:ADMIN_DEV_DB_USERNAME = 'postgres'
$env:ADMIN_DEV_DB_PASSWORD = Read-Host 'Mat khau DB cuc bo' -MaskInput
$env:ADMIN_DEV_EMAIL = Read-Host 'Email quan tri dang hoat dong trong DB cuc bo'
.\mvnw.cmd spring-boot:run
```

`-MaskInput` cần PowerShell 7; trên Windows PowerShell 5 có thể đặt biến mật khẩu bằng cơ chế quản lý bí mật của máy. Không lưu mật khẩu vào Git.

## Phạm vi và cách tắt

- Profile `admin-dev` phải được chọn riêng; server chỉ lắng nghe `127.0.0.1`, DB phải ở `localhost`/`127.0.0.1` và có hậu tố `_admin_dev`.
- Phiên tạm chỉ áp dụng cho yêu cầu loopback không mang token tới `/api/admin/operations/**`. Token sai vẫn bị từ chối; các API khác không được nhận phiên tạm.
- Kiểm tra quyền theo DB, CSRF cho thao tác ghi và giới hạn nguồn frontend vẫn hoạt động.
- Dừng backend bằng Ctrl+C. Script khôi phục các biến môi trường của cửa sổ gọi khi kết thúc. Chạy backend bình thường không chọn profile này sẽ yêu cầu đăng nhập như trước.
- Chỉ dùng một backend tại cổng 8085 trong mỗi thời điểm. Nếu backend khác đang chạy, dừng đúng cửa sổ đó trước khi chạy script.
