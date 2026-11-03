# FE-11 — Báo cáo triển khai và kiểm chứng

## Cập nhật 2026-11-03

Mã giao diện đã chuyển sang `frontend/pages/*`; các đường dẫn `templates/...` và số 24 test bên dưới là lịch sử lượt đầu. Lệnh `-Dtest=com.kizuna.operations.*Test` với `OPERATIONS_TEST_DB_URL` trỏ `kizuna_admin_shell_operations_test` là bộ test hiện hành; kết quả lần chạy cuối ghi ở mục này khi đã xác nhận. Bổ sung T062–T065 (menu chính sách, nhật ký chung, địa chỉ gốc, kiểm trường lệnh sự cố). FR-004 vẫn **blocked** vì chưa có luồng đặt học thử gọi `BookingPolicyEvaluator`; T042/T048 vẫn mở.

Ngày: 2026-10-31 đến 2026-11-01. Đã triển khai phần mã độc lập của FE-11; chưa nghiệm thu trọn tính năng do các phụ thuộc bên dưới. Không commit/push hoặc migrate DB dự án.

## Đã triển khai

- Cấu hình allowlist hai tham số, giới hạn số nguyên, lý do, xác nhận trước/sau, version/conflict, revision/audit nguyên tử và snapshot provider cho module booking. Không tự dùng 45/12 khi cấu hình thiếu hoặc lệch revision.
- Collector heartbeat/DB read-only, timeout/budget, poll 30 giây, stale trên 60 giây, nguồn chưa xác minh là chưa xác định. Pool identity/probe riêng và gate quyền trước transaction; DB identity lỗi thì fail-closed.
- Sự cố tạo thủ công, claim/reassign/progress/resolve/resume/close, version/khóa hàng, lịch sử sequence bất biến, hồ sơ đóng không ghi thêm, tái diễn tạo hồ sơ liên kết.
- Báo cáo as-of lịch sử, distinct resolves, tồn trước kỳ, phân nhóm riêng; đối chiếu phân trang giữ kỳ/cutoff. Không thêm báo cáo tài chính hoặc sử dụng.
- Chính sách chỉ đọc theo hiệu lực/công bố, overlap fail-closed, empty state; không có biên tập hoặc nội dung giả trong runtime.
- Bốn màn hình quản trị; hai shell chính sách learner/tutor; menu thật. Menu đã sửa: `templates/admin/layout/sidebar.html`, `templates/learner/layout/sidebar.html`, `templates/tutor/layout/sidebar.html`. API bọc ApiResponse theo AGENTS.md mới, giao diện dùng nhãn Việt và text an toàn.

## Kiểm chứng đã chạy

- Baseline compile trước thay đổi: đạt; Java 17.0.9/Maven Wrapper 3.9.6.
- Lệnh cuối: `.\mvnw.cmd -B -ntp -s '..\.operations-maven-settings.xml' '-Dtest=Operations*Test' test` với ba biến OPERATIONS_TEST_DB_* trỏ DB riêng.
- **24 tests đạt, 0 failure, 0 error, 0 skipped**, BUILD SUCCESS. Bao gồm migration/re-run/invalid config/booking legacy, quyền hiện tại/khóa tài khoản/CSRF, cấu hình cạnh tranh và audit rollback, incident lifecycle/recurrence/Unicode/concurrency/rollback/sequence, báo cáo quá khứ/timezone, policy visibility/overlap, pool cạn và render shell.
- Red/green đã ghi nhận cho migration thiếu và lỗi hợp đồng cấu hình/CSRF. Các test khác được bổ sung trong quá trình triển khai; không khẳng định mọi task đều có lịch sử strict TDD riêng.
- Test chạy trên cluster PostgreSQL 18 tại D, không chạm DB vận hành. Backup/restore thử đạt. Không chạy suite cũ dùng DB mặc định vì có test xóa user.
- Chrome smoke và source-timeout/mobile/keyboard đạt; xem validation-ui.md và target/fe11-browser/results.json.
- Tải 100 tài khoản/600 giây/1.000 sự cố: 19.704 thành công, 0 lỗi; kết quả và giới hạn tại validation-performance.md. Đối chiếu ngắn bản cuối 500 yêu cầu, 0 lỗi.

## Truy vết và phần còn thiếu

| Nhóm yêu cầu | Kết quả |
| --- | --- |
| FR-001,006,017–018 | Có mã và kiểm thử quyền/phiên bản/rollback/UI; chưa chạy toàn suite tích hợp monolith với DB triển khai |
| FR-002–003,005 | Có API/UI/giới hạn/lịch sử và kiểm thử |
| FR-004 / SC-001 toàn luồng | **Blocked**: TrialBookingServiceImpl vẫn stub; evaluator/unit test không thay tạo booking thật |
| FR-007–008 / SC-002 | Collector/UI và timeout/stale/pool tests đạt trong phạm vi nguồn kiểm thử; optional memory chưa có health contract xác minh |
| FR-009–014 / SC-003–004 | Có mã, API lifecycle, fixture as-of, rollback/concurrency và browser tạo incident |
| FR-015–016 / SC-005 | Cơ chế đọc/quyền/visibility/empty/overlap có kiểm thử; **chưa nghiệm thu nội dung chính sách thật** |
| SC-006 | Chưa chốt toàn bộ: thiếu p95 navigation chi tiết dưới tải và lượt 600 giây trên bản cuối/môi trường đích |
| SC-007 | **Chưa chạy**: cần approved corpus và 10 học viên/gia sư tham gia, không giả lập người nghiệm thu |

Task còn mở: **T011, T016, T019, T042, T048**. T011 có kiểm thử evaluator subset nhưng chưa đủ consumer booking/concurrency end-to-end. T040 hoàn tất hướng dẫn import và ghi blocker; không có nghĩa đã nhập nội dung thật. T046 ghi đầy đủ lượt đo và giới hạn, không tự xác nhận SC-006.

## Tiếp tục triển khai

1. Module booking cung cấp consumer tạo booking thật kiểm ownership/availability; gọi BookingPolicyEvaluator trong transaction và lưu đủ snapshot cùng booking, rồi hoàn tất T011/T016/T019.
2. Chủ dự án cung cấp tài liệu đã duyệt qua quy trình quản lý tài liệu; nạp có kiểm soát theo README-fe11-policy-import.md, hoàn tất T042 và nghiệm thu 10 người.
3. Preflight schema thật, backup/restore và migrate theo README-fe11.md; kiểm PostgreSQL 16, auth monolith, chạy tải/navigation bản cuối và chốt T048. Không dùng harness để tuyên bố đã triển khai production.
