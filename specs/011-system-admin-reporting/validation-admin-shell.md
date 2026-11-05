# Đồng bộ khung quản trị và phiên phát triển — 2026-11-01

## Đã thực hiện

Sáu trang quản trị dùng chung sidebar, header, footer, màu nền, font, thẻ nội dung và điều hướng mobile theo khung dashboard hiện có. Năm trang được chuyển: `operations-config`, `operations-health`, `operations-incidents`, `operations-incident`, `operations-reports`. Giữ ID và logic biểu mẫu/API. Trang đọc chính sách dành cho learner/tutor không chuyển sang khung admin.

Cơ chế email đăng nhập phát triển tồn tại trước lượt này được thu hẹp bằng profile `admin-dev`, binding `127.0.0.1`, DB loopback có hậu tố `_admin_dev`. Chỉ yêu cầu loopback không mang token tới API vận hành được nhận danh tính tạm; token sai không được thay thế. DB vẫn quyết định vai trò/trạng thái tài khoản; CSRF vẫn bảo vệ thao tác ghi.

Script `scripts/start-admin-dev.ps1` chạy theo cấu hình cục bộ riêng. Đã tạo `kizuna_admin_dev` bằng bản sao DB `kizuna_db` **cục bộ**, không ghi đè hay truy cập DB Supabase. Bản ghi sự cố kiểm tra được lưu và đóng trong DB phát triển riêng. Không tạo dữ liệu giả trong frontend hoặc API.

## Kiểm chứng

- `mvnw.cmd -o test-compile -DskipTests`: đạt.
- `OperationsDevLoginTest`, `OperationsJwtVerifierTest`, `OperationsAuthorizationTest`, `OperationsSplitSecurityTest`, `OperationsConfigurationTransactionTest`, `OperationsIncidentContractTest`: **18 test đạt**, không bỏ qua. DB kiểm thử trống `kizuna_admin_shell_operations_test` được bootstrap bởi harness hiện có. Lần đầu dùng bản sao DB đầy đủ bị xung đột khóa role với fixture test; đã chuyển sang DB kiểm thử trống đúng yêu cầu, không sửa test cũ để né lỗi.
- `node --test --test-isolation=none "frontend/tests/*.test.mjs"`: **4 test đạt**.
- `node frontend/tests/admin-dashboard.browser.mjs`: đạt; hồi quy dashboard và menu sau khi tách khung dùng chung.
- `node frontend/tests/admin-operations.browser.mjs` với `ADMIN_DEV_BACKEND=http://localhost:8086`: đạt. Dùng backend thật, DB phát triển thật, không giả phản hồi nghiệp vụ. Do cổng 8085 đang có tiến trình khác, dùng 8086 để không dừng tiến trình của người dùng. Chỉ đổi cấu hình địa chỉ backend trong tab kiểm thử, không sửa cấu hình frontend thật.
- Bốn trang danh sách/cấu hình/health/report được chụp ở 1280 và 390px; chi tiết sự cố ở 390px. Không tràn ngang, đúng một sidebar/header/footer và một mục đang chọn. Đã xem ảnh cấu hình desktop/mobile và health desktop.
- Luồng giao diện thật: tạo sự cố → nhận xử lý → ghi diễn biến → giải quyết → xác nhận đóng; lịch sử và dữ liệu dashboard tải thành công. Cookies/CSRF thực được dùng. Thử nghiệm ban đầu đổi hostname thành `127.0.0.1` bị giới hạn cookie cùng site; bản kiểm cuối giữ hostname `localhost` như cấu hình frontend thực tế và đạt.
- Kết quả/ảnh: `../.dashboard-qa/operations-live-results.json`, `operations-*-1280.png`, `operations-*-390.png` và `operations-detail-390.png`, tính từ gốc repository. Log Maven: `backend/target/admin-dev-tests.log`.

## Giới hạn

Phiên phát triển không thay thế việc nhóm triển khai đăng nhập thật. Nguồn tài khoản/lưu lượng và các thao tác ngoài FE-11 vẫn chưa được tích hợp. Các phụ thuộc booking, tài liệu chính sách và nghiệm thu tổng thể T011/T016/T019/T042/T048 vẫn mở. Không commit, push hoặc triển khai hosting.
