# Nối dữ liệu tài khoản dashboard — 2026-11-01

## Bổ sung danh tính quản trị

Khung chung tải `/api/admin/operations/me` và hiện tên thật trong DB cùng nhãn `Quản trị viên hệ thống`, kể cả trên mobile. `OperationsAccountSummaryTest` bổ sung ca tên theo DB, không trả email, từ chối anonymous và learner. Chạy cùng `OperationsDevLoginTest`: 9 test đạt; frontend transport 4 test đạt và module shell qua kiểm tra cú pháp. Chưa kiểm tra hình ảnh trình duyệt riêng cho thay đổi tên này. Hai mục nhân sự/phân quyền chưa có triển khai nên giữ trạng thái chưa khả dụng, không coi là thiếu quyền tài khoản.

Nguồn chạy thực: PostgreSQL cục bộ `kizuna_admin_dev`, bản sao phát triển đã chuẩn bị ở lượt trước. Đối chiếu SQL và API: tổng 6 tài khoản chưa xóa, 6 đang hoạt động; 1 gia sư, 1 học viên, 4 nhân sự. Không tạo thêm tài khoản để làm đầy giao diện. Không truy cập Supabase.

Thêm hợp đồng/API `/api/admin/operations/dashboard/accounts` và DTO tổng hợp, một truy vấn chỉ đọc users/roles. Quyền quản trị đang hoạt động vẫn được kiểm tra; không trả danh sách, email hoặc mã vai trò. Lỗi nguồn không trở thành số 0. Bốn ô và cơ cấu tài khoản gọi API thật; khoảng ngày báo cáo sự cố không lọc tổng tài khoản hiện tại. Nhãn phụ chuyển sang số tài khoản đang hoạt động theo nhóm, không dùng số người thay cho gói hội viên/lớp học.

Kiểm thử:

- Maven test-compile cùng `OperationsAccountSummaryTest`, `OperationsDevLoginTest`, `OperationsAuthorizationTest`: 10 test đạt trên DB kiểm thử riêng `kizuna_admin_shell_operations_test`.
- Frontend transport: 4 test đạt.
- Browser dashboard: kết quả thật/0/lỗi quyền; số thiếu không thành 0, dữ liệu giả lập chỉ ở thư mục tests.
- Browser nối backend thật ở cổng 8086 (để giữ tiến trình của người dùng ở 8085): các số trên thẻ khớp API từ DB cục bộ, đồng thời luồng sự cố và các trang quản trị vẫn đạt. Script kiểm thử chỉ đổi địa chỉ backend trong tab, không giả dữ liệu nghiệp vụ.
- Ảnh kết quả: `../.dashboard-qa/dashboard-local-accounts-1280.png` tính từ gốc repository; log Maven `backend/target/account-summary-tests.log`.

Nguồn thiết bị truy cập/giờ cao điểm vẫn chưa có nên hiển thị chưa có dữ liệu; không tự suy ra từ bảng tài khoản. Chế độ bỏ qua đăng nhập vẫn chỉ ở profile phát triển, loopback và DB riêng. Cần khởi động lại backend đang chạy để nạp API mới. Không sửa schema, commit/push hoặc deploy.
