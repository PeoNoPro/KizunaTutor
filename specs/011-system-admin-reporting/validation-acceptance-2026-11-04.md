# FE-11 — Nghiệm thu phạm vi mới (T076)

Ngày: 2026-11-04. Môi trường: PostgreSQL 18 cục bộ, DB `kizuna_admin_shell_operations_test` (test backend) và `kizuna_staffqa_admin_dev` (giao diện, API). **Chưa chạy trên Supabase**; bảng `staff_role_change_requests` chưa có ở đó. Chưa commit, chưa push.

## Kết quả theo tiêu chí

| Tiêu chí | Kết quả | Bằng chứng |
| --- | --- | --- |
| SC-008 quyền nhân viên | Đạt | `StaffRoleChangeTest`, `StaffImportTest`; ma trận đăng nhập thật bên dưới; `staff-role-change.browser.mjs` |
| SC-009 nhập Excel | Đạt theo chính sách đã chốt | `StaffImportTest` (8 test); `staff-import.browser.mjs` với tệp `.xlsx` thật |
| SC-010 xuất sự cố | Đạt | `OperationsIncidentContractTest`; mở bằng Microsoft Excel thật |
| SC-011 tổng quan/biểu đồ | Đạt, có ghi chú | SQL viết độc lập khớp API; không còn mục sức khỏe trong menu/dashboard |

Tham chiếu yêu cầu: FR-019 ứng với SC-008, FR-020 với SC-009, FR-021 với SC-010, FR-022 với SC-011.

## Kiểm thử đã chạy

- **Backend:** `-Dtest=com.kizuna.operations.*Test` — 64 test, 0 lỗi, 0 bỏ qua. Không chạy các test ngoài gói này.
- **Giao diện với backend thật:** `admin-operations`, `staff-role-change`, `management-profile`, `admin-staff`, `staff-import`, cùng script kiểm trang thống kê và nút xuất Excel; có desktop và mobile 390/320 px ở các màn mới.
- **Giao diện dùng dữ liệu giả:** `admin-dashboard`, `cs-ticket-detail`. Hai test này chỉ kiểm hiển thị, không tính là bằng chứng quyền hoặc dữ liệu.
- **Khác:** `cs-dashboard`, `support-tickets` đạt sau khi sửa khung dùng chung.

## Ma trận quyền qua đăng nhập thật

Backend không bật phiên phát triển; đăng nhập bằng tài khoản thử trên DB QA rồi gọi API bằng cookie thật.

| Vai trò | DS nhân viên | Màn điều chuyển | Đề xuất | Mẫu nhập | Xuất sự cố | Thống kê |
| --- | --- | --- | --- | --- | --- | --- |
| Admin | 200 | 403 | 200 | 200 | 200 | 200 |
| Manager | 403 | 200 | 200 | 403 | 403 | 403 |
| CS, Cán bộ học vụ, Gia sư, Học viên | 403 | 403 | 403 | 403 | 403 | 403 |

Manager với cookie thật: gửi đề xuất thiếu CSRF 403; có CSRF 201; tự duyệt 403; hủy đề xuất của mình 200.

## Đối chiếu độc lập

- **Tài khoản:** số theo 6 vai trò, 5 trạng thái, 30 ngày tài khoản mới theo giờ Việt Nam và tổng 29 tài khoản khớp SQL viết riêng, tại thời điểm chạy.
- **Sự cố:** 7 mới ghi nhận trong kỳ, 1 còn mở cuối kỳ khớp SQL.
- **Microsoft Excel 12.0 (2007), mở chỉ đọc:** tệp toàn bộ 8/8 dòng, tệp lọc 2/2 dòng khớp SQL. Ô `=HYPERLINK(...)` vẫn là chữ (`HasFormula=False`), cả tệp 0 ô công thức. Tệp mẫu có danh sách chọn vai trò và cột số điện thoại định dạng văn bản. Phiên bản Excel mới hơn chưa được thử.
- **Xuất tệp không sửa dữ liệu:** phiên bản và trạng thái sự cố giữ nguyên sau khi xuất.
- **Lô đủ:** 100 tài khoản tạo trong khoảng 1,2 giây trên máy phát triển, dưới giới hạn transaction 15 giây. Một lần đo, không phải kiểm thử tải.

## Lỗi phát hiện và xử lý trong lượt này

- `cs-ticket-detail` lúc đạt lúc lỗi (2/5 lần đạt khi chạy riêng): test điền form khi `renderActions` đang chờ `/managers`. Đã sửa test để đợi form; sau đó 8/8 lần đạt. Không đổi mã sản phẩm.
- `staff-role-change` từng thăng chức tài khoản mẫu CS trên DB QA, làm một kiểm tra quyền báo sai. Đã trả tài khoản về vai trò cũ trên DB QA và giới hạn test chỉ dùng tài khoản `@test.invalid`. Lịch sử đề xuất và nhật ký của lần đó vẫn còn trên DB QA.

## Chưa nghiệm thu hoặc còn phụ thuộc

| Mục | Tình trạng |
| --- | --- |
| Supabase | Chưa chạy `fe11_operations_schema.sql`, cần nhóm đồng ý; chưa kiểm luồng mới trên DB chung |
| Thông báo toàn hệ thống | Phụ thuộc FE-12, chưa nối |
| Sửa hồ sơ, đổi mật khẩu trong hồ sơ | Thuộc feature sở hữu; trang hồ sơ FE-11 vẫn chỉ đọc |
| FR-004 áp dụng cấu hình vào đặt học thử | Vẫn **blocked**, xem `validation.md` |
| Chính sách, AI | Tiếp tục hoãn theo Clarifications |
| SC-006 tải | Không chạy lại cho các màn mới; số cũ ở `validation-performance.md` không chứng minh cho phần này |
| Đổi mật khẩu lần đầu | Đăng nhập có chuyển hướng, chưa thấy server chặn người chưa đổi; cần nhóm đăng nhập xử lý, nhất là với mật khẩu dùng chung của lô nhập |
| Hạ chức Manager, tạo Admin mới | Chưa có luồng trong hệ thống |
| Sức khỏe | Đã bỏ khỏi menu/dashboard; API, phần đo ở backend và trang `/pages/operations-health/` vẫn còn và mở được nếu gõ đúng địa chỉ |
| Script nghiệm thu | Đối chiếu SQL, ma trận đăng nhập thật và kiểm Excel chạy bằng script tạm, chưa nằm trong repo |
