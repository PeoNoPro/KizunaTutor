# Feature Specification: FE-04 — Tutor Discovery & Search (Learner)

**Feature Branch**: `haipt`

**Created**: 2026-10-31

**Status**: Complete — Đã làm rõ và chốt quyết định thiết kế qua Clarification Session

**Input**: Đặc tả chi tiết phân hệ Tutor Discovery (Tìm kiếm & Khám phá gia sư) dành cho Học viên dựa trên Capstone Project Report 3 (UC-29, UC-30, UC-31, UC-77), tuân thủ FRONTEND_RULES.md, sử dụng HTML5, CSS3, JavaScript ES6+, Tailwind CSS và dữ liệu mẫu (mock data).

---

## Clarifications

### Session 2026-10-31

- Q: Khi người dùng nhập từ khóa hoặc thay đổi các tiêu chí lọc, giao diện sẽ lọc kết quả ngay lập tức hay chỉ khi nhấn nút? → A: Cập nhật tức thì (Live Filtering) ngay khi gõ từ khóa (áp dụng debounce 300ms) hoặc ngay khi chọn bất kỳ dropdown bộ lọc nào; nút "Tìm kiếm gia sư" vẫn hỗ trợ trigger tìm kiếm đồng bộ.
- Q: Các tiêu chí tìm kiếm và lọc nào được áp dụng cho hồ sơ gia sư? → A: Hệ thống cung cấp tìm kiếm và lọc kết hợp qua 5 tiêu chí trọng tâm: (1) Mục tiêu học tập, (2) Trình độ JLPT, (3) Hình thức học (Online/Trực tiếp), (4) Mức học phí / giờ, và (5) Khung giờ rảnh (Availability).
- Q: Danh sách gia sư sẽ hỗ trợ các tùy chọn sắp xếp nào và thứ tự sắp xếp mặc định khi người dùng mới vào trang là gì? → A: Mặc định sắp xếp theo Đánh giá cao nhất (điểm sao kết hợp số lượng đánh giá); người dùng có thể tùy chọn sắp xếp: Học phí: Thấp đến cao, Học phí: Cao đến thấp, hoặc Nhiều năm kinh nghiệm nhất.
- Q: Danh sách tìm kiếm gia sư có hiển thị gia sư chưa xác thực không? → A: Chỉ hiển thị 100% gia sư đã được phê duyệt hồ sơ và sẵn sàng nhận lớp (tuân thủ quy tắc GB-06), tất cả đều có gắn huy hiệu tích xanh xác thực.
- Q: Tính năng "Đề xuất gia sư" (UC-77) sẽ được xử lý như thế nào trong phạm vi hiện tại? → A: Sử dụng danh sách "Gia sư tiêu biểu" (Featured Tutors có đánh giá ≥ 4.9 sao) làm nội dung đề xuất nổi bật (như đã bố trí trên trang Home), không tự bịa đặt thuật toán AI/ML ngoài tài liệu.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tìm kiếm & Lọc gia sư tức thì qua 5 tiêu chí (Priority: P1)

Học viên hoặc khách vãng lai tìm kiếm và lọc danh sách gia sư tức thì thông qua 5 tiêu chí nghiệp vụ: Mục tiêu học tập, Trình độ JLPT (N5–N1), Hình thức học (Online qua Web / Trực tiếp tại Hà Nội, TP.HCM), Mức học phí/giờ, và Khung giờ rảnh trong tuần.

**Why this priority**: Là tính năng cốt lõi giúp học viên nhanh chóng tìm thấy giáo viên đồng điệu với nhu cầu, trình độ và ngân sách mà không mất thời gian chờ đợi.

**Independent Test**: Khi người dùng nhập từ khóa hoặc chọn bất kỳ dropdown bộ lọc nào, danh sách thẻ gia sư tự động cập nhật trong vòng < 300ms. Khi không có kết quả, hiển thị trạng thái rỗng thân thiện (MSG01). Nút "Đặt lại bộ lọc" đưa toàn bộ về trạng thái mặc định ban đầu.

**Acceptance Scenarios**:
1. **Given** đang ở trang tìm kiếm gia sư, **When** người dùng gõ từ khóa hoặc chọn cấp độ JLPT / mục tiêu / hình thức / mức giá / khung giờ rảnh, **Then** danh sách gia sư tự động lọc tức thì theo điều kiện kết hợp (AND logic).
2. **Given** bộ lọc không khớp với bất kỳ gia sư nào, **When** áp dụng bộ lọc, **Then** giao diện hiển thị trạng thái rỗng (Empty State) với thông báo tiếng Việt lịch sự: *"Không tìm thấy gia sư phù hợp với tiêu chí tìm kiếm. Vui lòng điều chỉnh lại bộ lọc."* (MSG01), kèm nút bấm *"Xóa bộ lọc"*.
3. **Given** các bộ lọc đang được áp dụng, **When** người dùng nhấn nút *"Đặt lại"* hoặc *"Xóa bộ lọc"*, **Then** tất cả các dropdown và ô tìm kiếm trở về mặc định, danh sách hiển thị lại toàn bộ gia sư khả dụng.
4. **Given** danh sách kết quả, **Then** 100% thẻ gia sư hiển thị đều mang trạng thái đã duyệt (`status: Approved`, `teaching_status: Available`) và có huy hiệu tích xanh xác thực.

---

### User Story 2 - Sắp xếp kết quả tìm kiếm (Priority: P1)

Học viên có thể linh hoạt thay đổi thứ tự sắp xếp danh sách gia sư để ưu tiên xem theo đánh giá hoặc mức học phí.

**Why this priority**: Giúp học viên tối ưu việc ra quyết định dựa trên ưu tiên cá nhân (chất lượng cao nhất hoặc chi phí tiết kiệm nhất).

**Independent Test**: Thay đổi dropdown "Sắp xếp theo", danh sách thẻ gia sư tái sắp xếp ngay lập tức theo đúng thứ tự logic đã chọn.

**Acceptance Scenarios**:
1. **Given** người dùng mới vào trang hoặc vừa tìm kiếm, **Then** thứ tự sắp xếp mặc định là *"Đánh giá cao nhất"* (điểm sao từ 5.0 giảm dần kết hợp số lượng đánh giá).
2. **Given** danh sách đang hiển thị, **When** người dùng chọn *"Học phí: Thấp đến cao"*, **Then** danh sách sắp xếp tăng dần theo trường `hourlyRate`.
3. **Given** danh sách đang hiển thị, **When** người dùng chọn *"Học phí: Cao đến thấp"*, **Then** danh sách sắp xếp giảm dần theo trường `hourlyRate`.
4. **Given** danh sách đang hiển thị, **When** người dùng chọn *"Kinh nghiệm giảng dạy"*, **Then** danh sách sắp xếp giảm dần theo số năm kinh nghiệm (`experienceYears`).

---

### User Story 3 - Xem chi tiết hồ sơ & Lịch rảnh gia sư (Priority: P2)

Học viên bấm vào thẻ gia sư để xem toàn bộ thông tin chi tiết: tiểu sử, video giới thiệu, chứng chỉ JLPT đã xác minh, đánh giá từ học viên và ma trận lịch rảnh 7 ngày trong tuần (UC-31).

**Why this priority**: Cung cấp bức tranh toàn diện và minh bạch về năng lực sư phạm của gia sư trước khi học viên gửi yêu cầu kết nối hoặc đặt lịch học thử.

**Independent Test**: Nhấp vào nút *"Xem hồ sơ chi tiết"* trên một thẻ gia sư, hệ thống hiển thị trang/modal chi tiết với đầy đủ các khối thông tin, bao gồm lịch rảnh tương tác.

**Acceptance Scenarios**:
1. **Given** nhấp vào gia sư cụ thể, **Then** thông tin hiển thị gồm: Họ tên, avatar, danh hiệu, cấp độ JLPT, số năm kinh nghiệm, khu vực (quận/thành phố tương đối, không lộ địa chỉ nhà riêng theo GB-15), video giới thiệu và danh sách nhận xét đánh giá.
2. **Given** khối lịch rảnh (Availability Widget), **Then** hiển thị trực quan các ca dạy trong tuần (Sáng, Chiều, Tối) với trạng thái *"Có thể đặt lịch"* (màu xanh) hoặc *"Đã có lớp / Nghỉ"* (màu xám).
3. **Given** màn hình chi tiết, **Then** có nút hành động chính (CTA): *"Gửi yêu cầu kết nối"* và *"Đặt lịch học thử"*.

---

### User Story 4 - Đề xuất Gia sư tiêu biểu (Priority: P2)

Học viên xem khối các gia sư tiêu biểu nổi bật (Featured Tutors) có điểm đánh giá xuất sắc (≥ 4.9 sao) ngay trên Trang chủ và đầu trang Tìm kiếm (UC-77).

**Why this priority**: Giúp học viên mới dễ dàng lựa chọn ngay những giáo viên uy tín hàng đầu mà không cần phải lọc tìm kiếm phức tạp.

**Independent Test**: Khối Gia sư tiêu biểu hiển thị đúng 4 gia sư có thành tích cao nhất được định nghĩa sẵn trong mock data.

---

## Data Model & Mock Data Structure (`frontend/js/data/tutors.mock.js`)

Mỗi bản ghi gia sư trong mock data bao gồm các trường chuẩn:

```javascript
{
  id: "tut-001",
  fullName: "Sensei Đặng Quỳnh Chi",
  avatarUrl: "...",
  verified: true, // Luôn true trong danh sách tìm kiếm (GB-06)
  jlptLevel: "N1", // N1, N2, N3, N4, N5
  jlptScore: "172/180",
  title: "Thạc sĩ Ngôn ngữ học • Cựu SV ĐH Ngoại ngữ",
  specialties: ["Kaiwa Thương mại BJT", "Phát âm Tokyo chuẩn"],
  learningGoals: ["kaiwa", "business", "jlpt"],
  teachingFormat: "online", // "online" | "offline" | "both"
  location: "Cầu Giấy, Hà Nội", // Vùng tương đối theo GB-15
  experienceYears: 6,
  hourlyRate: 280000, // VND / 60p
  rating: 5.0,
  reviewsCount: 68,
  availability: {
    monday: ["evening"],
    tuesday: ["evening"],
    thursday: ["evening"],
    saturday: ["morning", "afternoon"],
    sunday: ["morning"]
  },
  bio: "Tốt nghiệp Thạc sĩ Ngôn ngữ Nhật, 6 năm kinh nghiệm luyện thi JLPT và Kaiwa doanh nghiệp...",
  featured: true
}
```

---

## Technical & UI Constraints

1. **Công nghệ**: HTML5, CSS3, JavaScript ES6+, Tailwind CSS.
2. **Dữ liệu**: 100% Mock Data tĩnh trong thư mục frontend, không gọi API Spring Boot hay Supabase ở giai đoạn này.
3. **Bản địa hóa**: 100% tiếng Việt tự nhiên, không lộ mã kỹ thuật hay từ viết tắt không chuẩn. Tuân thủ bảng màu Indigo Aizome (`#006194`), Navy (`#0F2545`), Washi Background (`#FCFBF7`).
4. **Responsive**: Đảm bảo hiển thị hoàn hảo trên Mobile (375px), Tablet (768px) và Desktop (1240px).
