# Hướng dẫn kiểm chứng nhanh: FE-02 — Quản lý hồ sơ Học viên & Gia sư (Quickstart Guide)

**Mục đích**: Cung cấp các kịch bản kiểm thử chạy thật từ đầu đến cuối (End-to-End) để xác minh tính năng sau khi triển khai code.  
**Ngày cập nhật**: 2026-11-10  
**Tài liệu tham chiếu**: [spec.md](spec.md), [data-model.md](data-model.md), [contracts/profile-api.md](contracts/profile-api.md).

---

## 1. Điều kiện tiên quyết & Khởi động hệ thống

1. **Khởi động Backend (Spring Boot tại cổng 8085)**:
   ```powershell
   .\mvnw.cmd spring-boot:run -pl backend
   ```
2. **Khởi động Frontend (Node dev server tại cổng 3000)**:
   ```powershell
   node frontend/server.js
   ```

---

## 2. Các kịch bản kiểm thử thực tế trên giao diện (E2E Verification Scenarios)

### Kịch bản 1: Học viên xem và chỉnh sửa hồ sơ cá nhân
1. **Truy cập & Đăng nhập**:
   - Mở trình duyệt truy cập: `http://localhost:3000/pages/login/`.
   - Đăng nhập tài khoản Học viên (ví dụ: `google.learner@kizuna.vn` hoặc tài khoản học viên có sẵn).
2. **Mở trang Hồ sơ**:
   - Truy cập: `http://localhost:3000/pages/learner-profile/` (hoặc bấm vào Ảnh đại diện trên thanh Header ➔ "Hồ sơ cá nhân").
   - **Xác nhận**: Màn hình hiển thị đầy đủ Ảnh đại diện, Họ tên, Email (chế độ chỉ đọc), Trình độ hiện tại, Trình độ mục tiêu và Mục tiêu học tập.
3. **Thực hiện Chỉnh sửa**:
   - Nhấn nút "Chỉnh sửa thông tin".
   - Cập nhật số điện thoại sang: `0987654321`.
   - Chọn Trình độ hiện tại: `BEGINNER` (Mới bắt đầu).
   - Chọn Trình độ mục tiêu: `N3`.
   - Nhập Mục tiêu: `Luyện Kanji và giao tiếp phản xạ tiếng Nhật cùng gia sư Kizuna`.
   - Bấm "Lưu thay đổi".
4. **Xác nhận kết quả**:
   - Toast thông báo màu xanh hiển thị: *"Cập nhật hồ sơ học viên thành công."*
   - Thông tin trên trang lập tức cập nhật mà không cần F5 lại trang.

---

### Kịch bản 2: Gia sư xem và chỉnh sửa thông tin giảng dạy
1. **Đăng nhập vai trò Gia sư**:
   - Đăng nhập tài khoản Gia sư (ví dụ: `tutor.ha@kizuna.vn`).
2. **Xem trước hồ sơ giảng dạy**:
   - Truy cập trang xem hồ sơ: `http://localhost:3000/pages/tutor-profile/`.
   - **Xác nhận**: Vì gia sư đang đăng nhập chính là chủ sở hữu hồ sơ, hệ thống hiển thị nút **"Chỉnh sửa hồ sơ"** nổi bật.
3. **Cập nhật thông tin giảng dạy**:
   - Bấm nút "Chỉnh sửa hồ sơ" ➔ Form Modal/Drawer trượt ra.
   - Nhập Tiêu đề mới: `Gia sư tiếng Nhật giao tiếp phản xạ chuyên sâu`.
   - Nhập Học phí đề xuất: `280000` (hiển thị định dạng tiền tệ `280.000 đ / giờ`).
   - Chọn Tỉnh/Thành: `Hà Nội`, nhập chi tiết: `Cầu Giấy`.
   - Bấm "Lưu hồ sơ".
4. **Xác nhận kết quả**:
   - Modal đóng lại, hiển thị Toast thành công.
   - Các khối thông tin giá tiền, tiêu đề trên trang hồ sơ lập tức đổi sang dữ liệu mới.

---

### Kịch bản 3: Quản lý vận hành (Manager) tra cứu & cập nhật trạng thái người dùng
1. **Đăng nhập vai trò Quản lý**:
   - Đăng nhập tài khoản có quyền `ROLE_MANAGER` hoặc `ROLE_ADMIN`.
2. **Mở trang Quản trị người dùng**:
   - Truy cập: `http://localhost:3000/pages/manager-users/`.
   - **Xác nhận**: Bảng hiển thị danh sách người dùng kèm phân trang.
3. **Kiểm tra bộ lọc & tìm kiếm**:
   - Bấm chọn tab "Gia sư" ➔ Bảng chỉ hiển thị danh sách các tài khoản gia sư.
   - Nhập tên hoặc email vào ô tìm kiếm ➔ Bảng lọc kết quả theo thời gian thực.
4. **Cập nhật trạng thái tài khoản**:
   - Chọn một tài khoản, bấm nút "Đổi trạng thái".
   - Chọn trạng thái: `Tạm khóa` (`SUSPENDED`).
   - Nhập lý do: `Tài khoản vi phạm quy tắc hủy lịch đột xuất nhiều lần`.
   - Bấm "Xác nhận".
5. **Xác nhận kết quả**:
   - Badge trạng thái của tài khoản đổi sang màu cam `Tạm khóa`.
   - Đăng nhập thử bằng tài khoản vừa bị khóa ➔ Hệ thống chặn và báo lỗi tiếng Việt thân thiện: *"Tài khoản của bạn hiện đang bị tạm khóa. Vui lòng liên hệ hỗ trợ."*

---

## 3. Lệnh kiểm thử tự động (Automated Test Commands)

1. **Biên dịch mã nguồn Backend**:
   ```powershell
   .\mvnw.cmd test-compile -pl backend
   ```

2. **Chạy các Unit Test và MockMvc Test của phân hệ Profile**:
   ```powershell
   .\mvnw.cmd test -pl backend -Dtest="*Profile*Test"
   ```

3. **Kiểm tra Frontend**:
   ```powershell
   node --test "frontend/tests/*.test.mjs"
   ```

