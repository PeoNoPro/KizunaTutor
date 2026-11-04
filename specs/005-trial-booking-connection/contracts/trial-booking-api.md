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
| `PUT` | `/api/v1/trial-bookings/{bookingId}/cancel` | `ROLE_LEARNER`, `ROLE_TUTOR` | Hủy buổi học thử kèm lý do |

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

