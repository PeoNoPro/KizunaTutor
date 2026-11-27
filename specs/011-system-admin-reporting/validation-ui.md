# Kiểm tra giao diện FE-11

## Theo Figma MỚI — 2026-11-03

Đối chiếu file `YDYN9ibpq0SZO1HzG5y0Is`: 1457:8800, 1457:8958, 1457:8628, 1457:9365, 1457:9416, 1457:9456, 1457:9487, 1457:9574. Khung mới nằm ở `frontend/styles/admin-portal.css` và `admin-shell.js`; phần ghi chú cũ bên dưới là lịch sử lượt đầu.

Giữ theo spec/rule thay vì Figma: logo `kizuna-logo.png`; nhãn viết hoa chuyển sang sentence case; chỉ số báo cáo Quản lý theo spec; không có Xuất báo cáo, Cấu hình tích hợp, Ngày áp dụng và chọn trạng thái tự do cho sự cố (FR-013); tạo sự cố giữ đủ trường FR-011. Thêm các tab Sức khỏe và Báo cáo vì spec yêu cầu mà Figma không có màn riêng. Mỗi tab giữ đường dẫn riêng để mở trực tiếp. Dashboard giữ bố cục 1485:5490, chỉ đổi khung và màu. Menu Quản lý chỉ có mục đã có trang. Khung CS giữ theo 1113:39.

Đã chạy với backend tạm và DB QA `kizuna_staffqa_admin_dev`: `admin-operations`, `admin-staff`, `admin-dashboard` đạt; với DB `kizuna_support_ticket_test`: `support-tickets` đạt; `cs-ticket-detail` đạt. `management-profile` và `cs-dashboard` chưa chạy lại vì cần thêm backend/cấu hình riêng.

Môi trường: Chrome headless, Node 24, harness FE-11 loopback với PostgreSQL kiểm thử riêng. Chưa chạy toàn ứng dụng trên DB triển khai.

Đã kiểm bằng trình duyệt:

- Bốn màn hình quản trị mở được, gọi API và hiển thị nội dung thật từ DB kiểm thử.
- Tạo sự cố bằng biểu mẫu và chuyển đến chi tiết.
- Tiêu đề có `<script>` hiển thị như văn bản, không trở thành phần tử script.
- Hai trang danh mục chính sách của learner/tutor ở kích thước 390×844; không tràn ngang, có trạng thái chưa có tài liệu, không chèn chính sách mẫu.
- Shell đúng phân hệ, liên kết menu chính sách và bốn liên kết quản trị hiện diện. Form có label, thông báo aria-live, focus lỗi và trạng thái đang tải riêng.
- JUnit render template kiểm token CSRF và cả hai shell; backend kiểm allowedActions/quyền độc lập giao diện.
- Lượt cuối đã thử Tab đưa focus tới phần tử tương tác, mở/đóng menu mobile và mô phỏng API không trả lời: thông báo lỗi sau khoảng 4,5 giây. Đã sửa xung đột hàm toggleSidebar của fragment learner trong phạm vi layout FE-11.

Ảnh và kết quả máy hiện tại: `target/fe11-browser/booking-policy.png`, `health.png`, `incidents.png`, `reports.png`, `learner-policies-mobile.png`, `tutor-policies-mobile.png`, `results.json`. Đây là artifact kiểm thử, không stage vào Git.

Chưa nghiệm thu: nội dung policy thật, 10 người dùng tìm chính sách, thử bàn phím/trình đọc màn hình toàn diện trên thiết bị thực tế. Không dùng ảnh empty state để khẳng định nội dung chính sách đã được phê duyệt.
