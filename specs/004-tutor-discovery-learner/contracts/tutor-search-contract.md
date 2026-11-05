# API Contract: Tìm kiếm và Khám phá Gia sư (FE-04)

**Base URL**: `http://localhost:8085/api/tutors`  
**Protocol**: REST / JSON  
**Auth**: Public (Không yêu cầu đăng nhập)

---

## 1. Tìm kiếm và Lọc đa tiêu chí

- **Endpoint**: `GET /api/tutors/search` hoặc `GET /api/tutors`
- **Query Parameters**:
  - `keyword` (optional): Chuỗi từ khóa tìm theo tên, chuyên môn, kỹ năng.
  - `jlptLevel` (optional): `N1`, `N2`, `N3`, `N4`, `N5`.
  - `goal` (optional): `kaiwa`, `jlpt`, `business`, `it`.
  - `format` (optional): `online`, `offline`, `hybrid`.
  - `minPrice` (optional): Số nguyên VNĐ (ví dụ `150000`).
  - `maxPrice` (optional): Số nguyên VNĐ (ví dụ `250000`).
  - `schedule` (optional): `today`, `weekend`, `evening`.
  - `sortBy` (optional, default `rating`): `rating`, `price_asc`, `price_desc`, `exp`.
  - `page` (optional, default `0`): Chỉ số trang (0-indexed).
  - `size` (optional, default `10`): Số kết quả mỗi trang.

- **Thành công (HTTP 200 OK)**:
```json
{
  "content": [
    {
      "tutorId": "00000000-0000-0000-0002-000000000001",
      "tutorCode": 10001,
      "fullName": "Yamada Kenji",
      "avatarUrl": "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
      "headline": "Giảng viên tiếng Nhật bản xứ 5 năm kinh nghiệm",
      "highestJlptLevel": "N1",
      "experienceYears": 5,
      "hourlyRate": 300000,
      "avgRating": 4.9,
      "totalReviews": 38,
      "teachingFormat": "Online 1-1",
      "location": "Hà Nội, Việt Nam",
      "specialties": ["Kaiwa thương mại", "Luyện phát âm chuẩn Tokyo"],
      "isVerified": true,
      "bio": "Xin chào! Tôi là Yamada Kenji, giảng viên bản xứ sống tại Hà Nội..."
    }
  ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 6,
  "totalPages": 1,
  "last": true
}
```

---

## 2. Lấy chi tiết gia sư theo mã tự tăng

- **Endpoint**: `GET /api/tutors/code/{tutorCode}`
- **Path Variable**: `tutorCode` (Long, e.g. `10001`)
- **Thành công (HTTP 200 OK)**: Trả về đối tượng `TutorSummaryResponseDTO`.
- **Thất bại (HTTP 404 Not Found)**: Khi mã không tồn tại trong hệ thống.

