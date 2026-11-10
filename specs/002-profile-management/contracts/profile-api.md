# Hợp đồng giao tiếp API: FE-02 — Quản lý hồ sơ Học viên & Gia sư (Profile API Contracts)

**Base URL**: `http://localhost:8085/api`  
**Định dạng**: JSON (UTF-8) / `multipart/form-data` cho tải ảnh  
**Xác thực**: JWT Bearer token qua Header `Authorization: Bearer <token>` hoặc Cookie `access_token`.

---

## 1. Nhóm API Hồ sơ Học viên (Learner Profile)

### 1.1. Lấy thông tin hồ sơ học viên đang đăng nhập
- **Endpoint**: `GET /api/profile/learner/me`
- **Quyền truy cập**: `ROLE_LEARNER`
- **Mô tả**: Trả về toàn bộ thông tin hồ sơ cá nhân và lộ trình học tập của học viên hiện tại.

#### Response (200 OK):
```json
{
  "success": true,
  "data": {
    "userId": "e8a947a1-8d02-45e6-b631-f925b6a3e110",
    "email": "learner@kizuna.vn",
    "fullName": "Nguyễn Văn An",
    "phone": "0987654321",
    "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
    "currentLevel": "BEGINNER",
    "targetLevel": "N3",
    "learningGoals": "Luyện thi JLPT N3 và nâng cao kỹ năng giao tiếp tiếng Nhật trong công việc.",
    "createdAt": "2026-11-01T10:00:00Z",
    "updatedAt": "2026-11-10T15:30:00Z"
  }
}
```

---

### 1.2. Cập nhật thông tin hồ sơ học viên
- **Endpoint**: `PUT /api/profile/learner/me`
- **Quyền truy cập**: `ROLE_LEARNER`
- **Mô tả**: Cập nhật thông tin cá nhân và mục tiêu học tập.

#### Request Body:
```json
{
  "fullName": "Nguyễn Văn An",
  "phone": "0987654321",
  "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
  "currentLevel": "N5",
  "targetLevel": "N3",
  "learningGoals": "Tập trung luyện chữ Hán Kanji và ngữ pháp N3."
}
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Cập nhật hồ sơ học viên thành công.",
  "data": {
    "userId": "e8a947a1-8d02-45e6-b631-f925b6a3e110",
    "email": "learner@kizuna.vn",
    "fullName": "Nguyễn Văn An",
    "phone": "0987654321",
    "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
    "currentLevel": "N5",
    "targetLevel": "N3",
    "learningGoals": "Tập trung luyện chữ Hán Kanji và ngữ pháp N3.",
    "updatedAt": "2026-11-10T16:00:00Z"
  }
}
```

---

## 2. Nhóm API Hồ sơ Gia sư (Tutor Profile)

### 2.1. Lấy thông tin hồ sơ giảng dạy của gia sư đang đăng nhập
- **Endpoint**: `GET /api/profile/tutor/me`
- **Quyền truy cập**: `ROLE_TUTOR`
- **Mô tả**: Lấy đầy đủ thông tin giảng dạy phục vụ xem trước và chỉnh sửa hồ sơ.

#### Response (200 OK):
```json
{
  "success": true,
  "data": {
    "userId": "a2b3c4d5-1111-2222-3333-444455556666",
    "tutorId": "f1e2d3c4-9999-8888-7777-666655554444",
    "tutorCode": 10001,
    "email": "tutor.ha@kizuna.vn",
    "fullName": "Nguyễn Thu Hà",
    "phone": "0912345678",
    "avatarUrl": "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
    "headline": "Gia sư tiếng Nhật giao tiếp và luyện thi JLPT",
    "bio": "Cử nhân ngành Nhật Bản học, có 3 năm kinh nghiệm giảng dạy.",
    "experienceYears": 3,
    "hourlyRate": 250000.00,
    "highestJlptLevel": "N2",
    "province": "Hà Nội",
    "addressDetail": "Cầu Giấy",
    "fullAddress": "Hà Nội - Cầu Giấy",
    "avgRating": 5.0,
    "totalReviews": 0,
    "verificationStatus": "APPROVED",
    "isOwner": true
  }
}
```

---

### 2.2. Cập nhật thông tin giảng dạy của gia sư
- **Endpoint**: `PUT /api/profile/tutor/me`
- **Quyền truy cập**: `ROLE_TUTOR`
- **Mô tả**: Cập nhật thông tin giảng dạy của gia sư và đồng bộ thông tin cá nhân.

#### Request Body:
```json
{
  "fullName": "Nguyễn Thu Hà",
  "phone": "0912345678",
  "avatarUrl": "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
  "headline": "Gia sư tiếng Nhật giao tiếp chuyên sâu",
  "bio": "Cử nhân Nhật Bản học, tận tâm và kiên nhẫn đồng hành cùng học viên.",
  "experienceYears": 4,
  "hourlyRate": 280000.00,
  "highestJlptLevel": "N2",
  "province": "Hà Nội",
  "addressDetail": "Cầu Giấy"
}
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Cập nhật hồ sơ giảng dạy thành công.",
  "data": {
    "tutorId": "f1e2d3c4-9999-8888-7777-666655554444",
    "headline": "Gia sư tiếng Nhật giao tiếp chuyên sâu",
    "hourlyRate": 280000.00,
    "updatedAt": "2026-11-10T16:05:00Z"
  }
}
```

---

## 2A. Đổi mật khẩu của chính mình (Change Password)

> Bổ sung 2026-11-10 theo yêu cầu người dùng: học viên và gia sư đổi mật khẩu ngay trong trang hồ sơ cá nhân.

### 2A.1. Đổi mật khẩu
- **Endpoint**: `PUT /api/profile/password`
- **Quyền truy cập**: `ROLE_LEARNER`, `ROLE_TUTOR` (luôn đổi mật khẩu của tài khoản trong phiên đăng nhập)
- **Quy tắc**: mật khẩu mới dài 8–64 ký tự, có cả chữ và số (cùng quy tắc với nhân sự nội bộ); `confirmPassword` phải trùng `newPassword`; mật khẩu mới phải khác mật khẩu hiện tại.

#### Request Body:
```json
{
  "currentPassword": "MatKhauCu123",
  "newPassword": "MatKhauMoi456",
  "confirmPassword": "MatKhauMoi456"
}
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Đổi mật khẩu thành công.",
  "data": null
}
```

#### Lỗi:
- `400` — "Mật khẩu hiện tại chưa đúng." / "Mật khẩu xác nhận không khớp." / "Mật khẩu mới cần khác mật khẩu hiện tại." / lỗi kiểm tra từng trường (`fieldErrors`).
- `403` — tài khoản không phải học viên hoặc gia sư.

---

## 3. API Tải ảnh đại diện (Avatar Upload)

### 3.1. Tải file ảnh đại diện lên máy chủ
- **Endpoint**: `POST /api/profile/avatar`
- **Content-Type**: `multipart/form-data`
- **Quyền truy cập**: Bất kỳ người dùng đã đăng nhập (`isAuthenticated()`)
- **Tham số Request**: `file` (MultipartFile, dung lượng tối đa 5MB)

#### Response (201 Created):
```json
{
  "success": true,
  "message": "Tải ảnh đại diện lên thành công.",
  "data": {
    "avatarUrl": "/api/profile/avatar/f5a7b8c9_avatar.png",
    "fileName": "f5a7b8c9_avatar.png",
    "size": 182400
  }
}
```

### 3.2. Truy cập hình ảnh đại diện đã tải
- **Endpoint**: `GET /api/profile/avatar/{fileName}`
- **Quyền truy cập**: Công khai (`permitAll()`)
- **Response**: Trả về trực tiếp nội dung Binary của tệp hình ảnh kèm header `Content-Type: image/png` hoặc `image/jpeg`.

---

## 4. Nhóm API Quản trị người dùng cho Manager (Manager User Management)

### 4.1. Lấy danh sách người dùng phân trang & đa tiêu chí
- **Endpoint**: `GET /api/profile/users`
- **Quyền truy cập**: `ROLE_MANAGER`, `ROLE_ADMIN`
- **Query Params**:
  - `role`: `ALL` (mặc định), `ROLE_TUTOR`, `ROLE_LEARNER`
  - `status`: `ALL` (mặc định), `ACTIVE`, `SUSPENDED`, `BANNED`
  - `keyword`: Tìm kiếm theo Họ tên hoặc Email (tùy chọn)
  - `page`: Chỉ số trang, bắt đầu từ 0 (mặc định 0)
  - `size`: Kích thước trang (mặc định 10)

#### Response (200 OK):
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": "e8a947a1-8d02-45e6-b631-f925b6a3e110",
        "email": "learner@kizuna.vn",
        "fullName": "Nguyễn Văn An",
        "phone": "0987654321",
        "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
        "role": "ROLE_LEARNER",
        "roleBadge": "Học viên",
        "accountStatus": "ACTIVE",
        "createdAt": "2026-11-01T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "last": false
  }
}
```

---

### 4.2. Xem thông tin chi tiết một người dùng
- **Endpoint**: `GET /api/profile/users/{userId}`
- **Quyền truy cập**: `ROLE_MANAGER`, `ROLE_ADMIN`

#### Response (200 OK):
```json
{
  "success": true,
  "data": {
    "userId": "e8a947a1-8d02-45e6-b631-f925b6a3e110",
    "email": "learner@kizuna.vn",
    "fullName": "Nguyễn Văn An",
    "phone": "0987654321",
    "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
    "role": "ROLE_LEARNER",
    "roleBadge": "Học viên",
    "accountStatus": "ACTIVE",
    "createdAt": "2026-11-01T10:00:00Z",
    "learnerDetails": {
      "currentLevel": "BEGINNER",
      "targetLevel": "N3",
      "learningGoals": "Luyện thi JLPT N3 và nâng cao kỹ năng giao tiếp tiếng Nhật trong công việc."
    }
  }
}
```

---

### 4.3. Cập nhật trạng thái tài khoản người dùng
- **Endpoint**: `PUT /api/profile/users/{userId}/status`
- **Quyền truy cập**: `ROLE_MANAGER`, `ROLE_ADMIN`

#### Request Body:
```json
{
  "status": "SUSPENDED",
  "reason": "Vi phạm quy định hủy lịch học liên tục không báo trước."
}
```

#### Response (200 OK):
```json
{
  "success": true,
  "message": "Cập nhật trạng thái tài khoản thành công.",
  "data": {
    "userId": "e8a947a1-8d02-45e6-b631-f925b6a3e110",
    "accountStatus": "SUSPENDED",
    "reason": "Vi phạm quy định hủy lịch học liên tục không báo trước.",
    "updatedAt": "2026-11-10T16:10:00Z"
  }
}
```

