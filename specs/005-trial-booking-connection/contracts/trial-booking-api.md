# REST API Contract: FE-05 — Trial Lesson Booking & Connection

**Base Path**: `/api/v1/trial-bookings`  
**Authentication**: Bearer JWT (`Authorization: Bearer <token>`)  
**Data Format**: JSON (UTF-8, No BOM)  

---

## 1. Danh sách Endpoints

| Method | Endpoint | Quyền (Role) | Mô tả tóm tắt |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/trial-bookings` | `ROLE_LEARNER` | Học viên gửi yêu cầu kết nối & đặt lịch học thử 1-1 |
| `GET` | `/api/v1/trial-bookings/my-requests` | `ROLE_LEARNER`, `ROLE_TUTOR` | Lấy danh sách lịch học thử của người dùng hiện tại |
| `GET` | `/api/v1/trial-bookings/{bookingId}` | `ROLE_LEARNER`, `ROLE_TUTOR` | Lấy chi tiết thông tin buổi học thử |
| `PUT` | `/api/v1/trial-bookings/{bookingId}/respond` | `ROLE_TUTOR` | Gia sư duyệt yêu cầu: Chấp nhận hoặc Từ chối |
| `PUT` | `/api/v1/trial-bookings/{bookingId}/cancel` | `ROLE_LEARNER`, `ROLE_TUTOR` | Hủy buổi học thử kèm lý do (trước 24h đối với buổi đã xác nhận) |
| `PUT` | `/api/v1/trial-bookings/{bookingId}/agree-connection` | `ROLE_TUTOR` | Sau khi buổi học thử `COMPLETED`, gia sư đồng ý kết nối với học viên (xem 2.4b) |
| `GET` | `/api/v1/tutor/availability` | `ROLE_TUTOR` | Lấy danh sách các ca rảnh giảng dạy của gia sư đang đăng nhập |
| `POST` | `/api/v1/tutor/availability` | `ROLE_TUTOR` | Thêm mới một ca rảnh hàng tuần |
| `DELETE` | `/api/v1/tutor/availability/{id}` | `ROLE_TUTOR` | Xóa ca rảnh (chặn xóa nếu đang có lịch PENDING hoặc CONFIRMED) |
| `PATCH` | `/api/v1/tutor/availability/{id}/toggle` | `ROLE_TUTOR` | Bật hoặc tạm ẩn ca rảnh hàng tuần |

---

## 2. Chi tiết hợp đồng API

### 2.1. Đăng ký học thử 1-1

- **Endpoint**: `POST /api/v1/trial-bookings`
- **Quyền**: `ROLE_LEARNER`

#### Request Body
```json
{
  "tutorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "scheduledStartTime": "2026-11-10T09:00:00+07:00",
  "durationMinutes": 50,
  "learningMode": "ONLINE",
  "locationDetails": null,
  "currentLevel": "N4",
  "learningGoals": "Luyện phản xạ Kaiwa giao tiếp công việc và sửa phát âm chuẩn",
  "initialMessage": "Chào Sensei, em muốn học thử 1 buổi để Sensei đánh giá phản xạ nói tiếng Nhật của em ạ."
}
```

#### Response 201 Created
```json
{
  "success": true,
  "code": 201,
  "message": "Đã gửi yêu cầu đăng ký học thử thành công. Vui lòng chờ gia sư phản hồi.",
  "data": {
    "bookingId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "requestId": "8f2eab4c-2a1d-4fbc-8acc-1a0c6a2bca5c",
    "tutorId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "tutorName": "Yamada Kenji",
    "scheduledStartTime": "2026-11-10T09:00:00+07:00",
    "scheduledEndTime": "2026-11-10T09:50:00+07:00",
    "durationMinutes": 50,
    "learningMode": "ONLINE",
    "bookingStatus": "PENDING",
    "statusLabel": "Chờ gia sư xác nhận",
    "createdAt": "2026-11-04T20:30:00+07:00"
  }
}
```

#### Response 409 Conflict (Trùng lịch)
```json
{
  "success": false,
  "code": 409,
  "message": "Khung giờ này vừa có người đăng ký. Vui lòng chọn khung giờ khác.",
  "data": null
}
```

---

### 2.2. Lấy danh sách yêu cầu học thử của tôi

- **Endpoint**: `GET /api/v1/trial-bookings/my-requests?status=PENDING&page=0&size=10`
- **Quyền**: `ROLE_LEARNER` hoặc `ROLE_TUTOR`
- **Mô tả**: Tự động lọc theo vai trò của người dùng trong JWT token. Học viên xem lịch mình đã đặt; Gia sư xem lịch học viên gửi cho mình.

#### Response 200 OK
```json
{
  "success": true,
  "code": 200,
  "message": "Lấy danh sách yêu cầu học thử thành công",
  "data": {
    "items": [
      {
        "bookingId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
        "requestId": "8f2eab4c-2a1d-4fbc-8acc-1a0c6a2bca5c",
        "partnerId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
        "partnerName": "Yamada Kenji",
        "partnerAvatarUrl": "https://example.com/avatar.jpg",
        "partnerRole": "Gia sư",
        "scheduledStartTime": "2026-11-10T09:00:00+07:00",
        "scheduledEndTime": "2026-11-10T09:50:00+07:00",
        "durationMinutes": 50,
        "learningMode": "ONLINE",
        "meetingLink": "https://meet.google.com/abc-defg-hij",
        "locationDetails": null,
        "learningGoals": "Luyện phản xạ Kaiwa giao tiếp công việc",
        "initialMessage": "Chào Sensei...",
        "bookingStatus": "CONFIRMED",
        "statusLabel": "Đã xác nhận",
        "createdAt": "2026-11-04T20:30:00+07:00"
      }
    ],
    "totalItems": 1,
    "totalPages": 1,
    "currentPage": 0
  }
}
```

---

### 2.3. Gia sư phản hồi yêu cầu học thử (Chấp nhận / Từ chối)

- **Endpoint**: `PUT /api/v1/trial-bookings/{bookingId}/respond`
- **Quyền**: `ROLE_TUTOR` (phải là gia sư nhận lịch)

#### Request Body (Chấp nhận)
```json
{
  "action": "ACCEPT",
  "meetingLink": "https://meet.google.com/kzt-trial-10001",
  "note": "Sensei đã nhận lịch. Hẹn gặp bạn vào sáng thứ Ba nhé!"
}
```

#### Request Body (Từ chối)
```json
{
  "action": "DECLINE",
  "declineReason": "Trùng lịch giảng dạy tại trường đại học",
  "note": "Rất tiếc thời gian này Sensei bận. Bạn có thể chọn khung giờ buổi chiều giúp Sensei nhé."
}
```

#### Response 200 OK
```json
{
  "success": true,
  "code": 200,
  "message": "Đã xử lý yêu cầu học thử thành công",
  "data": {
    "bookingId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "bookingStatus": "CONFIRMED",
    "statusLabel": "Đã xác nhận",
    "meetingLink": "https://meet.google.com/kzt-trial-10001",
    "updatedAt": "2026-11-04T20:45:00+07:00"
  }
}
```

---

### 2.4b. Đồng ý kết nối sau khi học thử (nối sang lớp học FE-06)

Luồng: đặt học thử → gia sư **chấp nhận lịch** (`respond` ACCEPT: buổi học thử `CONFIRMED`, yêu cầu kết nối **vẫn `PENDING`**) → học thử xong (`COMPLETED`) → gia sư **đồng ý kết nối** → gia sư tạo lớp + lịch học hằng tuần ở FE-06.

- **Endpoint**: `PUT /api/v1/trial-bookings/{bookingId}/agree-connection` — không có body.
- **Quyền**: `ROLE_TUTOR`, đúng gia sư của buổi học thử.
- **Điều kiện**: buổi học thử ở trạng thái `COMPLETED`; gọi lại khi đã đồng ý thì trả kết quả cũ.
- **Kết quả**: yêu cầu kết nối của buổi học thử → `ACCEPTED` (nếu buổi học thử không gắn yêu cầu kết nối thì dùng/tạo yêu cầu `ACCEPTED` cho cặp gia sư–học viên); `trial_bookings.official_arrangement_agreed = true`. Response 200 có thêm trường `officialArrangementAgreed: true`.
- **Lỗi**: 400 buổi học thử chưa hoàn thành; 403 không phải gia sư của buổi học thử; 404 không tìm thấy.
- **Giao diện**: sau khi thành công chuyển sang `/tutor/classrooms?taoLop=1&learnerId={learnerId}` (FE-06 mở sẵn hộp thoại Tạo lớp).

Buổi học **chính thức** không lưu trong `trial_bookings`: lịch hằng tuần của lớp thuộc FE-06 (bảng `class_schedules`). Lưới tuần "Lịch giảng dạy" đọc qua `GET /api/classrooms/schedules/me`.

---

### 2.4. Hủy buổi học thử

- **Endpoint**: `PUT /api/v1/trial-bookings/{bookingId}/cancel`
- **Quyền**: `ROLE_LEARNER` hoặc `ROLE_TUTOR` (phải là người tham gia buổi học)

#### Request Body
```json
{
  "cancelReason": "Có việc gia đình đột xuất"
}
```

#### Response 200 OK
```json
{
  "success": true,
  "code": 200,
  "message": "Đã hủy lịch học thử thành công",
  "data": {
    "bookingId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
    "bookingStatus": "CANCELLED",
    "statusLabel": "Đã hủy",
    "updatedAt": "2026-11-04T20:48:00+07:00"
  }
}
```

---

### 2.5. Quản lý ca rảnh giảng dạy của gia sư

#### 2.5.1. Lấy danh sách ca rảnh
- **Endpoint**: `GET /api/v1/tutor/availability`
- **Quyền**: `ROLE_TUTOR`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Lấy danh sách ca rảnh giảng dạy thành công",
  "data": [
    {
      "availabilityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "dayOfWeek": 2,
      "dayName": "Thứ hai",
      "startTime": "18:00",
      "endTime": "20:00",
      "isActive": true
    }
  ],
  "timestamp": "2026-11-07T17:00:00+07:00"
}
```

#### 2.5.2. Thêm mới ca rảnh
- **Endpoint**: `POST /api/v1/tutor/availability`
- **Quyền**: `ROLE_TUTOR`
- **Request Body**:
```json
{
  "dayOfWeek": 2,
  "startTime": "18:00",
  "endTime": "20:00",
  "isActive": true
}
```
- **Response 201 Created**:
```json
{
  "success": true,
  "message": "Đã thêm ca rảnh thành công",
  "data": {
    "availabilityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "dayOfWeek": 2,
    "dayName": "Thứ hai",
    "startTime": "18:00",
    "endTime": "20:00",
    "isActive": true
  },
  "timestamp": "2026-11-07T17:00:00+07:00"
}
```

#### 2.5.3. Xóa ca rảnh
- **Endpoint**: `DELETE /api/v1/tutor/availability/{availabilityId}`
- **Quyền**: `ROLE_TUTOR`
- **Response 200 OK**:
```json
{
  "success": true,
  "message": "Đã xóa ca rảnh thành công",
  "data": null,
  "timestamp": "2026-11-07T17:00:00+07:00"
}
```
- **Response 409 Conflict** (Khi ca rảnh đang có lịch học thử PENDING hoặc CONFIRMED):
```json
{
  "success": false,
  "message": "Không thể xóa ca rảnh này vì đang có lịch học thử của học viên đang chờ hoặc đã xác nhận. Vui lòng xử lý lịch học trước."
}
```

