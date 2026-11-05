# Triển khai schema FE-11

Chỉ chạy bằng tài khoản triển khai được phép, sau backup và preflight. Không chạy `DatabasePgAdmin4.txt`: nguồn đó chứa DROP CASCADE.

1. Đối chiếu live schema với users(user_id), trial_bookings(booking_id) và system_configurations. Hai khóa TRIAL_DEFAULT_DURATION_MINUTES/BOOKING_MIN_HOURS_IN_ADVANCE phải có giá trị nguyên đúng khoảng 15–120/1–168. Thiếu/sai thì migration dừng; người phụ trách phải xác minh giá trị thật, không tự seed 45/12.
2. Đặt biến môi trường chuẩn PostgreSQL `PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE`; nhập mật khẩu tại máy hoặc dùng PGPASSFILE riêng. Không đưa thông tin đăng nhập vào source/log/chat.
3. Backup: `pg_dump -Fc -f fe11-before.dump`; phục hồi thử vào DB kiểm thử trống: `pg_restore --exit-on-error --dbname=kizuna_restore_test fe11-before.dump`. So sánh số lượng booking và toàn bộ giá trị cấu hình. Chỉ gọi backup hợp lệ sau khi restore thử đạt.
4. Chạy: `psql -X --set ON_ERROR_STOP=on --file database/seeds/fe11_operations_schema.sql`. Migration có transaction/advisory lock/bảng version; chạy lại không tạo baseline hay thay snapshot cũ. Không sửa file migration sau khi đã áp dụng; thay đổi tiếp bằng migration mới.
5. Kiểm version fe11_001, config giữ nguyên, booking legacy có snapshot NULL; thử cấu hình/incident trên DB kiểm thử trước triển khai. Rollback triển khai dùng bản backup đã thử theo quy trình bảo trì, không xóa lịch sử bằng down migration tùy ý.

Migration test tự tạo schema cô lập trong DB có tên kết thúc `operations_test`; không đọc application.properties. PostgreSQL 18 là môi trường kiểm thử máy hiện tại; cần chạy lại trên PostgreSQL 16 đích.
