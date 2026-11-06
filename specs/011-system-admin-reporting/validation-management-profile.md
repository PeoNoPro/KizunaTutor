# Báo cáo quản lý và hồ sơ cá nhân — 2026-11-01

Người dùng yêu cầu thêm báo cáo Manager, cấu hình AI và xem hồ sơ; trong khi thực hiện đã yêu cầu để AI sau. Phần AI không được triển khai, không có migration/schema/giới hạn được áp dụng và không gọi dịch vụ AI.

## Đã thực hiện

`/pages/manager-reports/` dùng API `/api/admin/operations/management-report`, chỉ Admin/Manager ACTIVE. Tổng tài khoản hiện tại theo vai trò/trạng thái; tài khoản mới, kết nối và học thử lọc theo ngày tạo trong kỳ; nhóm theo trạng thái hiện tại lúc truy vấn. Tỷ lệ nhận kết nối có định nghĩa, mẫu số0 trảnull. Kỳ mặc định7ngày, tối đa366ngày theo Việt Nam. Không gọi repository xuyên feature: bổ sung các service tổng hợp chỉ đọc trong FE-04/05, giữ nghiệp vụ cũ.

`/pages/admin-profile/` và GET `/profile` xem chính tài khoản nội bộ hiện tại, gồm tên/email/phone/ảnh/role/trạng thái/ngày tạo; không trả hash hoặc nhận lựa chọn người khác. Sidebar và tên header mở hồ sơ. Header/menu Manager không có các mục quản trị; API quản trị vẫn từ chối Manager, không chỉ ẩn nút.

## Kiểm chứng

- Maven test-compile đạt. ManagementReportProfileTest, StaffAccountManagementTest, OperationsAccountSummaryTest, OperationsDevLoginTest, OperationsSplitSecurityTest, OperationsAuthorizationTest: **23 test đạt**, gồm số liệu cohort, ngoài kỳ, mẫu số0, quyền mới không mở staff/cấu hình choManager, maxperiod, hồ sơ không bị đổi bởi userId giả, hồi quy quyền/CSRF.
- Frontend transport **4 test đạt**. Cú pháp module báo cáo/hồ sơ đạt. Browser dashboard hồi quy đạt; sửa ca kiểm thử chờ sự kiện media-query trước khi xác nhận inert, không đổi điều kiện kiểm thử.
- `frontend/tests/management-profile.browser.mjs` dùng hai backend QA thật: Admin8086, Manager8087, DB riêng `kizuna_staffqa_admin_dev`. Cả hai xem số báo cáo khớp API; hồ sơ đúng tài khoản tương ứng; desktop1280/mobile390; Manager root chuyển báo cáo và không có quyền staff/cấu hình: **đạt**. Không giả phản hồi nghiệp vụ, không ghi dữ liệu vào DB người dùng.
- Đã xem ảnh báo cáo Admin desktop và hồ sơ Manager mobile. Bằng chứng: `../.dashboard-qa/management-profile-results.json`, `admin-management-report-1280.png`, `manager-profile-390.png`. Log Maven: `backend/target/management-report-tests.log`.

Giới hạn: báo cáo không tái dựng lịch sử trạng thái cuối kỳ; UI ghi rõ cohort/ngày tạo và trạng thái hiện tại. Không có đăng nhập thật, đổi mật khẩu, sửa hồ sơ, export hay quota AI mới. Nguồn dữ liệu hiện hữu có thể bằng0; không seed hoạt động giả để làm đầy báo cáo. Nghiệm thu tổng thể và các phụ thuộc booking/policy cũ vẫn mở.
