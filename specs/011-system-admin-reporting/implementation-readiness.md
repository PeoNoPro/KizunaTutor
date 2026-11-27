# FE-11 — Readiness triển khai

## Phạm vi hiện hành — 2026-11-05

Triển khai theo FR-023–029, SC-012–014 và T077–T084. Thông tin nền tảng cần rà consumer/footer và dữ liệu nhóm cung cấp; giới hạn tải chỉ áp dụng đính kèm hỗ trợ hiện có. Bảo trì hoãn chờ code mới (T085). Kiểm bàn giao chờ hợp đồng (T086). Các blocker booking/sức khỏe trong phần lịch sử không phải điều kiện làm cấu hình mới. Lượt này chỉ đồng bộ tài liệu, chưa xác minh ứng dụng, chưa chạy DB hoặc migration. Trạng thái hoàn thành cũ không thay bằng chứng mới.

## Cập nhật phạm vi 2026-11-03

Áp dụng Clarifications 2026-11-03 của spec.md và task T067–T076 trước ghi chú lịch sử bên dưới. Lượt này chỉ sửa tài liệu, không chạy DB hoặc kiểm thử ứng dụng. Giám sát sức khỏe đã bỏ khỏi phạm vi, AI/chính sách tiếp tục hoãn. Admin tạo/import/sửa thông tin/khóa/mở; Manager đổi vai trò sau tạo theo ma trận chưa chốt. Code/hợp đồng cũ chưa đáp ứng phân quyền mới. Import Excel tài khoản và export Excel sự cố chưa được triển khai; phải hoàn thiện hợp đồng, giới hạn và kiểm chứng trước khi dùng. Không xóa hoặc thay bằng chứng triển khai cũ.

Ngày kiểm tra: 2026-10-31.

- Baseline: Java 17.0.9, Maven Wrapper 3.9.6, Spring Boot 3.3.4; compile trước thay đổi đạt.
- Schema tham chiếu: `DatabasePgAdmin4.txt`; chưa kết nối DB vận hành. `SystemConfiguration` và `TrialBooking` mới có mapping khung, cần sửa đúng snake_case.
- Booking: `TrialBookingServiceImpl` rỗng, không có consumer tạo booking đủ ownership/availability. T016 và nghiệm thu FR-004 toàn luồng bị phụ thuộc module booking; không dựng API giả.
- Approved policy corpus: chưa có nguồn đã duyệt được cung cấp. Hoàn thiện cơ chế đọc/empty state và hướng dẫn nhập; chưa nghiệm thu nội dung thật.
- Optional agent-memory: chưa xác minh hợp đồng health read-only, giữ “Chưa có nguồn kiểm tra”.
- Quyết định DB/auth: luôn kiểm tài khoản/vai trò hiện tại. Snapshot collector độc lập DB nhưng endpoint chỉ trả khi xác minh được quyền. Identity DB lỗi thì fail-closed với thông báo hữu hạn; không bypass hoặc cache quyền để trả snapshot. Người dùng đồng ý triển khai sau bản đề xuất này.
- Packaging: AGENTS.md hiện tại yêu cầu feature slice, nên mã mới ở `com.kizuna.features.admin.operations` và `com.kizuna.features.booking`; giữ các entity legacy đúng vị trí cũ. Hợp đồng thành công bọc `ApiResponse` có timestamp theo quy định hiện tại; data là projection được định nghĩa trong contracts.
- Nguồn kiểm thử: PostgreSQL 18 có sẵn trên máy, tạo cluster riêng tại `D:\do an 2\.operations-test-pg`, loopback cổng 55441. Không sửa cài đặt hệ thống hoặc DB dự án. PostgreSQL 16 của môi trường đích vẫn cần kiểm tương thích riêng.
