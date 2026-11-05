# Quickstart: Kiểm thử Bộ lọc tìm kiếm, Đồng bộ URL & Dynamic Routing

**Feature**: FE-04 — Tutor Discovery & Search (Learner)  
**Date**: 2026-11-01  

---

## 1. Khởi chạy hệ thống

Mở 2 cửa sổ terminal riêng biệt:

1. **Khởi động Backend Spring Boot (Port 8085)**:
   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```
2. **Khởi động Frontend Dev Server (Port 3000)**:
   ```powershell
   node frontend/server.js
   ```

---

## 2. Kịch bản kiểm thử tính năng (Verification Scenarios)

### Kịch bản 1: Đồng bộ URL khi chọn bộ lọc (State -> URL)
1. Mở trình duyệt tại: `http://localhost:3000/tutors` hoặc `http://localhost:3000/pages/tutor-discovery/`.
2. Bấm vào chip **`N2`**:
   - Quan sát thanh địa chỉ URL: Tự động đổi thành `http://localhost:3000/tutors?jlpt=N2`.
   - Danh sách thẻ hiển thị đúng các gia sư có chứng chỉ N2.
3. Chọn dropdown **Mục tiêu học tập**: "Luyện giao tiếp":
   - URL tự động cập nhật: `?jlpt=N2&goal=kaiwa`.
4. Nhập từ khóa vào ô tìm kiếm: "Kenji":
   - Sau 300ms (debounce), URL đổi thành: `?q=Kenji&jlpt=N2&goal=kaiwa`.

### Kịch bản 2: Tải lại trang giữ nguyên trạng thái (F5 Persistence)
1. Đang ở đường dẫn `http://localhost:3000/tutors?jlpt=N2&goal=kaiwa`.
2. Nhấn `F5` hoặc `Ctrl + R`:
   - Sau khi trang tải xong, chip **N2** vẫn được tô màu xanh (active).
   - Dropdown **Mục tiêu học tập** vẫn hiển thị "Luyện giao tiếp".
   - Danh sách gia sư vẫn giữ đúng kết quả đã lọc.

### Kịch bản 3: Hỗ trợ nút Back / Forward của trình duyệt (`popstate`)
1. Từ trạng thái có bộ lọc, chọn thêm mức giá "150k - 250k / giờ".
2. Bấm nút **Quay lại (Back)** trên trình duyệt:
   - URL quay về trạng thái trước đó.
   - Dropdown mức giá tự động quay về "Tất cả mức giá" và danh sách được lọc lại tương ứng mà không cần tải lại trang.

### Kịch bản 4: Xóa từng chip hoặc Đặt lại bộ lọc
1. Bấm nút `×` trên chip đang áp dụng hoặc bấm nút **"Đặt lại bộ lọc"**:
   - URL tự động xóa các query params tương ứng và trở về `/tutors`.
   - Danh sách gia sư hiển thị lại đầy đủ tất cả gia sư khả dụng.

### Kịch bản 5: Điều hướng động từ Trang chủ (Home Deep Linking)
1. Mở `http://localhost:3000/pages/home/`.
2. Bấm vào badge cấp độ "N1" hoặc nút "Tìm gia sư":
   - Trình duyệt chuyển hướng mượt mà sang `http://localhost:3000/tutors?jlpt=N1`.
   - Trang `/tutors` tự động kích hoạt lọc N1 ngay từ đầu.

