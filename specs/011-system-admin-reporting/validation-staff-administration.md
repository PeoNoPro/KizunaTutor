# Quản trị trực tiếp và hai mục nhân sự — 2026-11-01

## Cập nhật phân công đã duyệt và danh mục 6 vai trò

Đã cập nhật spec.md/plan.md/tasks.md theo phạm vi người dùng duyệt: nội bộ/permissions thuộc FE-11, xác thực/thực thi quyền phối hợp FE-01, hồ sơ FE-02, giới hạn AI phối hợp FE-08, gửi thông báo FE-12. Báo cáo Manager, hợp đồng nguồn báo cáo và danh mục cấu hình AI được ghi là việc chưa hoàn tất, không tự mở quyền Manager cho API admin.

Danh mục `/roles` hiện trả đủ 6 vai trò từ DB cùng cờ `staffAssignable`; chỉ 4 vai trò nội bộ được chọn trong create/change nhân viên. StaffAccountManagementTest: 6 test đạt; frontend transport 4 test đạt; browser kiểm 6 thẻ, dropdown tạo có 4 vai trò cộng lựa chọn trống, không có learner/tutor, luồng sửa/gán/khóa/mở trên DB QA thật đạt. API backend người dùng 8085 sau cập nhật xác nhận 6 vai trò/4 có thể gán. Không đổi schema cho cập nhật này, không sửa số liệu DB hoặc chuyển tài khoản giữa nhân viên và learner/tutor.

Người dùng yêu cầu mở thẳng dashboard như admin để kiểm các phần theo khung Figma. Root frontend chuyển tới dashboard. Header lấy tên/ảnh DB; local admin hiện là System Administrator, chưa có ảnh nên dùng chữ viết tắt. Không gán tên hoặc ảnh người trong mẫu Figma cho tài khoản thật.

Hai liên kết sidebar và lối tắt nhân viên/vai trò mở trang thật; danh sách, tìm kiếm, tạo/sửa, gán vai trò, khóa/mở và lịch sử gọi API. Quyền quản trị theo role hiện có được backend kiểm tra; không dựng các công tắc quyền tùy chỉnh giả. Tài khoản người đang thao tác không tự khóa/đổi role; tài khoản learner/tutor không vào phạm vi nhân sự.

## Bằng chứng

- Maven test-compile đạt. Suite StaffAccountManagementTest, OperationsAccountSummaryTest, OperationsDevLoginTest, OperationsSplitSecurityTest: **16 test đạt**. Kiểm BCrypt/must_change_password, tạo/sửa/đổi role/khóa/mở, tìm kiếm, không trả password, email trùng, dữ liệu cũ409, sai role401/403, CSRF, tự đổi quyền, role ngoài phạm vi, rollback account khi audit lỗi. DB test riêng `kizuna_admin_shell_operations_test`.
- Frontend transport **4 test đạt**, cú pháp module staff đạt. Browser dashboard hồi quy đạt.
- `frontend/tests/admin-staff.browser.mjs` dùng backend thật ở8086 với DB QA riêng `kizuna_staffqa_admin_dev`: root→dashboard, danh tính từDB, avatar, hai link hoạt động, tạo/sửa/tìm, gán vai trò, khóa/mở, audit, desktop1280/mobile390/320: **đạt**. Không giả response nghiệp vụ. Các tài khoản thử chỉ ở DB QA.
- Đã xem ảnh staff desktop và role mobile; sửa empty icon để không lộ tên icon khi thiếu font, điều chỉnh khoảng cách thẻ và nút mobile.
- API `/me`, `/staff`, `/roles` ở backend người dùng 8085 trả200 không cần qua màn đăng nhập, sau khi kiểm tra và khởi động lại đúng tiến trình trong checkout. DB duyệt vẫn là `kizuna_admin_dev`, không phải DB QA.
- Schema `admin_staff_schema.sql` chỉ bổ sung bảng/index audit; chạy trên DB dev vàtest, không migrate Supabase. Script chạy có preflight tênDB/loopback; DB đã có không bị ghi đè.
- Kết quả/ảnh: `../.dashboard-qa/staff-results.json`, `staff-1280.png`, `roles-1280.png`, `admin-staff-390.png`, `admin-roles-390.png`. Log Maven: `backend/target/staff-tests.log`.

## Phạm vi còn lại

Đăng nhập thật/đổi mật khẩu lần đầu thuộc module auth. Thông báo toàn hệ thống, xuất tệp, trang hồ sơ đầy đủ và quyền tùy chỉnh từng hành động không được tuyên bố hoàn tất trong lượt bổ sung hai mục này. Những phụ thuộc FE-11 booking/policy/nghiệm thu cũ vẫn giữ mở. Không commit/push/deploy hosting.
