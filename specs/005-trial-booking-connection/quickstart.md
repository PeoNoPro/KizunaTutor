# Quickstart & Verification Guide: FE-05 — Trial Lesson Booking & Connection

**Feature**: FE-05 Trial Lesson Booking & Connection (Đăng ký học thử & Kết nối gia sư)  
**Spec Reference**: `specs/005-trial-booking-connection/spec.md`  
**API Contracts**: `specs/005-trial-booking-connection/contracts/trial-booking-api.md`  
**Data Model**: `specs/005-trial-booking-connection/data-model.md`  
**Date**: 2026-11-04  

---

## 1. Điều kiện tiên quyết (Prerequisites)

1. Backend Spring Boot đang chạy tại cổng `8085`:
   ```bash
   cd backend && .\mvnw.cmd spring-boot:run
   ```
2. Frontend Node Dev Server đang chạy tại cổng `3000`:
   ```bash
   node frontend/server.js
   ```
3. Tài khoản kiểm thử có sẵn trong CSDL:
   - Học viên: `learner@kizuna.vn` / `Password123!`
   - Gia sư: `tutor.yamada@kizuna.vn` / `Password123!` (Gia sư #10001 - Yamada Kenji)

---

## 2. Kịch bản kiểm chứng End-to-End (Validation Scenarios)

### Kịch bản 1: Học viên đăng ký học thử 1-1 từ hồ sơ gia sư (P1)

1. **Mở trình duyệt**: Truy cập `http://localhost:3000/gs/10001` (Trang hồ sơ Gia sư Yamada Kenji).
2. **Đăng nhập**: Nhấn Đăng nhập nhanh với tài khoản `learner@kizuna.vn`.
3. **Mở modal đặt lịch**:
   - Nhấn nút *"Đặt lịch học thử 1-1"*.
   - Chọn thời lượng: `50 mins`.
   - Chọn một ngày trong tuần (ví dụ: ngày mai) và chọn một khung giờ khả dụng (ví dụ: `09:00 - 09:50`).
   - Nhập mục tiêu: *"Luyện thi Kaiwa N3"*.
   - Nhập lời nhắn: *"Chào Sensei, em muốn học thử 1 buổi làm quen ạ."*.
   - Nhấn nút *"Tiếp tục"*.
4. **Kiểm tra kết quả**:
   - Modal hiển thị thông báo thành công: *"Đã gửi yêu cầu đăng ký học thử thành công. Vui lòng chờ gia sư phản hồi."*.
   - Kiểm tra CSDL hoặc gọi API `GET /api/v1/trial-bookings/my-requests`: Xuất hiện 1 bản ghi mới với trạng thái `PENDING`.

---

### Kịch bản 2: Kiểm tra chống đặt trùng lịch (Race Condition - Edge Case)

1. Sử dụng công cụ `curl` hoặc Postman gửi yêu cầu đặt lịch trùng đúng khung giờ vừa đặt ở Kịch bản 1:
   ```bash
   curl -X POST http://localhost:8085/api/v1/trial-bookings \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <LEARNER_TOKEN>" \
     -d '{
       "tutorId": "<YAMADA_KENJI_ID>",
       "scheduledStartTime": "2026-11-10T09:00:00+07:00",
       "durationMinutes": 50,
       "learningMode": "ONLINE"
     }'
   ```
2. **Kiểm tra kết quả**:
   - Backend phản hồi mã `409 Conflict`.
   - Nội dung thông báo tiếng Việt: *"Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác."*.

---

### Kịch bản 3: Gia sư phê duyệt yêu cầu học thử (P1)

1. **Đăng nhập**: Đăng nhập với tài khoản Gia sư `tutor.yamada@kizuna.vn`.
2. **Truy cập trang quản lý yêu cầu**: Truy cập `http://localhost:3000/connection-requests` (hoặc Bảng điều khiển gia sư).
3. **Thao tác duyệt**:
   - Thấy thẻ yêu cầu của học viên ở tab *"Chờ phản hồi"*.
   - Nhấn nút *"Chấp nhận"*.
   - Nhập link phòng học Google Meet: `https://meet.google.com/abc-defg-hij`.
   - Nhấn *"Xác nhận"*.
4. **Kiểm tra kết quả**:
   - Thẻ chuyển sang tab *"Đã xác nhận"* với huy hiệu xanh lá.
   - Trạng thái cập nhật thành `CONFIRMED`.
   - Học viên kiểm tra trên giao diện của mình thấy nút *"Vào phòng học"* dẫn tới link Google Meet.

---

### Kịch bản 4: Hủy buổi học thử trước giờ học (P3)

1. Học viên hoặc Gia sư vào trang `http://localhost:3000/connection-requests`.
2. Chọn buổi học sắp tới, nhấn nút *"Hủy lịch"*.
3. Nhập lý do: *"Có lịch thi đột xuất tại trường"*, nhấn *"Xác nhận hủy"*.
4. **Kiểm tra kết quả**:
   - Buổi học chuyển sang trạng thái `CANCELLED`.
   - Khung giờ trên hồ sơ của gia sư được tự động giải phóng để học viên khác có thể chọn lại.

