# Kiểm chứng dashboard quản trị — 2026-11-01

Nguồn thiết kế: [Figma frame 1485:5490](https://www.figma.com/design/YDYN9ibpq0SZO1HzG5y0Is/Untitled?node-id=1485-5490). Đã đọc frontend rules. Người dùng yêu cầu trang này theo khung quản trị Figma; giữ nguyên header/footer các trang khác. Logo và font dùng lại từ frontend hiện có, biểu tượng Figma được lưu ở `frontend/assets/icons/admin-dashboard/`.

Trang: `/pages/admin-dashboard/`; alias `/admin`, `/admin/dashboard`, `/admin/operations`. Các trang vận hành có liên kết quay về tổng quan.

## Kiểm tra

- `node --check frontend/pages/admin-dashboard/admin-dashboard.js`: đạt.
- `node --test --test-isolation=none "frontend/tests/*.test.mjs"`: 4/4 đạt. Runner mặc định bị sandbox chặn tạo process con (`spawn EPERM`); dùng chế độ không tạo process con, không bỏ test.
- `node frontend/tests/admin-dashboard.browser.mjs`: đạt với Chrome headless và dev server cổng 3000. Dữ liệu giả lập được chặn tại trình duyệt và chỉ khai báo trong thư mục tests; không đưa fixture vào mã chạy thật.
- Kiểm viewport 320, 390, 768, 1280 và 1440px không tràn ngang toàn trang; bảng cuộn trong khung trên mobile. Menu mở/đóng, Escape và focus, lọc sự cố, định dạng Asia/Ho_Chi_Minh đạt.
- Kết quả API thành công, rỗng, nguồn lỗi từng phần, chưa đăng nhập và timeout được hiển thị riêng; lỗi không đổi thành số 0. Quan sát quá 60 giây được đánh dấu quá hạn. Chuỗi HTML trong sự cố/lịch sử hiển thị như văn bản, không thực thi.
- Tài nguyên ảnh cục bộ tải được; không còn URL tài nguyên Figma tạm trong trang. Đã xem ảnh desktop/mobile. Ảnh và kết quả kiểm thử nằm ở `../.dashboard-qa/` tính từ gốc repository (ngoài mã frontend).

## Giới hạn

Chưa kiểm tra bằng phiên đăng nhập quản trị thật và DB triển khai. Không khẳng định đã nghiệm thu tích hợp toàn hệ thống.

Các chỉ số tài khoản, tăng trưởng/lưu lượng và thiết bị chưa có hợp đồng dữ liệu trong FE-11 nên hiển thị trống; thống kê vận hành hiển thị tổng hợp sự cố có nguồn thật. Nhật ký lấy lịch sử cấu hình học thử, không giả làm nhật ký toàn bộ hệ thống. Các thao tác nhân sự, phân quyền, thông báo, đăng xuất và xuất báo cáo ngoài phạm vi chưa được tích hợp. Không sao chép số liệu, tên quản trị viên, máy chủ hoặc chứng nhận an toàn của bản thiết kế để giả vờ hoàn tất.

Không sửa backend/schema, không migrate, commit, push hoặc triển khai hosting.
