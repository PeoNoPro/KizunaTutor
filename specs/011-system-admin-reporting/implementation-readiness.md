# FE-11 — Readiness triển khai

Ngày kiểm tra: 2026-10-31.

- Baseline: Java 17.0.9, Maven Wrapper 3.9.6, Spring Boot 3.3.4; compile trước thay đổi đạt.
- Schema tham chiếu: `DatabasePgAdmin4.txt`; chưa kết nối DB vận hành. `SystemConfiguration` và `TrialBooking` mới có mapping khung, cần sửa đúng snake_case.
- Booking: `TrialBookingServiceImpl` rỗng, không có consumer tạo booking đủ ownership/availability. T016 và nghiệm thu FR-004 toàn luồng bị phụ thuộc module booking; không dựng API giả.
- Approved policy corpus: chưa có nguồn đã duyệt được cung cấp. Hoàn thiện cơ chế đọc/empty state và hướng dẫn nhập; chưa nghiệm thu nội dung thật.
- Optional agent-memory: chưa xác minh hợp đồng health read-only, giữ “Chưa có nguồn kiểm tra”.
- Quyết định DB/auth: luôn kiểm tài khoản/vai trò hiện tại. Snapshot collector độc lập DB nhưng endpoint chỉ trả khi xác minh được quyền. Identity DB lỗi thì fail-closed với thông báo hữu hạn; không bypass hoặc cache quyền để trả snapshot. Người dùng đồng ý triển khai sau bản đề xuất này.
- Packaging: AGENTS.md hiện tại yêu cầu feature slice, nên mã mới ở `com.kizuna.features.admin.operations` và `com.kizuna.features.booking`; giữ các entity legacy đúng vị trí cũ. Hợp đồng thành công bọc `ApiResponse` có timestamp theo quy định hiện tại; data là projection được định nghĩa trong contracts.
- Nguồn kiểm thử: PostgreSQL 18 có sẵn trên máy, tạo cluster riêng tại `D:\do an 2\.operations-test-pg`, loopback cổng 55441. Không sửa cài đặt hệ thống hoặc DB dự án. PostgreSQL 16 của môi trường đích vẫn cần kiểm tương thích riêng.
