# Feature Specification: FE-05 — Trial Lesson Booking & Connection (Đăng Ký Học Thử & Kết Nối Gia Sư)

**Feature Branch**: `haipt`

**Created**: 2026-11-04

**Status**: Ready for Planning — Đã hoàn thành làm rõ yêu cầu nghiệp vụ (Clarified)

**Input**: Dựa trên tài liệu đặc tả SRS Capstone Project Report 3 (Mục 2.3 Search & Trial Lesson Booking, Mục 5. Trial Lesson Booking & Learning Activation bao gồm các Use Case từ 5.1 đến 5.11), cấu trúc cơ sở dữ liệu (`connection_requests`, `trial_bookings`, `trial_booking_histories`) và kết quả thống nhất làm rõ với Product Owner / Nhóm phát triển.

---

## Clarifications

### Session 2026-11-04

- **Q1: Chi phí và luồng thanh toán cho Buổi học thử**:  
  → **A**: Buổi học thử 1-1 là **hoàn toàn miễn phí (0đ)**. Đây là buổi gặp mặt kết nối ban đầu để học viên và gia sư làm quen, kiểm tra trình độ và đánh giá độ tương thích giảng dạy. Hệ thống không thu tiền đặt cọc, không phát sinh giao dịch thanh toán ví hay VNPay, yêu cầu được gửi thẳng đến Gia sư ở trạng thái Chờ duyệt (`PENDING`). Tuân thủ tuyệt đối Hiến pháp KizunaTutor và Luật số 8 trong `AGENTS.md`.
- **Q2: Thời lượng tiêu chuẩn của Buổi học thử**:  
  → **A**: Cho phép học viên **linh hoạt lựa chọn thời lượng là 25 phút hoặc 50 phút** ngay trên giao diện đặt lịch:
  - Mốc **25 phút**: Thích hợp cho buổi phỏng vấn nhanh, làm quen, kiểm tra phản xạ và trao đổi lộ trình học.
  - Mốc **50 phút**: Thích hợp cho buổi học thử trải nghiệm trọn vẹn 1 tiết học tiêu chuẩn.
- **Q3: Hình thức tổ chức Buổi học thử (Online / Offline)**:  
  → **A**: **Linh hoạt hỗ trợ cả 2 hình thức (Online & Offline)** tùy theo nhu cầu của Học viên và khả năng đáp ứng của Gia sư:
  - Nếu chọn **Học trực tuyến (Online)**: Khi Gia sư bấm "Chấp nhận", Gia sư cung cấp liên kết phòng học trực tuyến (Google Meet / Zoom / Web Room) để lưu vào trường `meeting_link` và hiển thị trên thẻ lịch học của cả hai bên.
  - Nếu chọn **Học trực tiếp (Offline)**: Học viên nhập địa chỉ học mong muốn (tại nhà học viên, nhà gia sư hoặc quán cà phê học tập); áp dụng khi gia sư có cung cấp hình thức dạy trực tiếp tại cùng tỉnh/thành phố.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Học viên đặt lịch học thử 1-1 từ hồ sơ gia sư (Priority: P1)

Là một Học viên, sau khi tìm thấy gia sư phù hợp và xem trang chi tiết hồ sơ, tôi muốn chọn một khung giờ rảnh khả dụng của gia sư, lựa chọn thời lượng (25 phút hoặc 50 phút), chọn hình thức học (Online hoặc Trực tiếp tại nhà), gửi kèm thông tin trình độ hiện tại cùng lời nhắn giới thiệu mục tiêu học tập, để gia sư nắm được thông tin và đồng ý buổi học thử 1-1 miễn phí.

**Why this priority**: Đây là tính năng chuyển đổi cốt lõi (Core Conversion) của nền tảng KizunaTutor, kết nối học viên với gia sư và kích hoạt toàn bộ chu kỳ học tập tiếp theo.

**Independent Test**: Khi học viên bấm vào một khung giờ rảnh khả dụng trên hồ sơ gia sư, modal đặt lịch mở ra cho phép chọn 25m/50m, hình thức Online/Offline, nhập mục tiêu học tập, gửi lời nhắn và xác nhận tạo yêu cầu thành công trong thời gian dưới 1 giây.

**Acceptance Scenarios**:

1. **Given** Học viên đã đăng nhập và đang xem trang chi tiết của một gia sư có các khung giờ rảnh, **When** Học viên bấm vào nút "Đặt lịch học thử 1-1" hoặc chọn một khung giờ còn trống, **Then** hệ thống hiển thị hộp thoại chọn ngày học, thời lượng (25 phút hoặc 50 phút), hình thức học (Online qua Meet hoặc Trực tiếp) và form nhập mục tiêu học tập cùng lời nhắn gửi gia sư.
2. **Given** Học viên chọn hình thức Học trực tiếp (Offline), **When** nhập thông tin, **Then** hệ thống yêu cầu cung cấp địa chỉ cụ thể (ví dụ: Số nhà, ngõ, đường, quận/huyện).
3. **Given** Học viên điền đủ thông tin và xác nhận đặt lịch, **When** hệ thống xử lý, **Then** một bản ghi `connection_requests` và `trial_bookings` được tạo với trạng thái Chờ duyệt (`PENDING`), đồng thời khung giờ đó được tạm giữ để tránh người khác đặt trùng lặp.
4. **Given** có người khác vừa đặt đúng khung giờ đó trước vài giây, **When** Học viên bấm xác nhận, **Then** hệ thống thông báo lịch đã có người chọn và hướng dẫn chọn khung giờ khác.

---

### User Story 2 - Gia sư tiếp nhận và xử lý yêu cầu học thử (Priority: P1)

Là một Gia sư, tôi muốn nhận được thông báo khi có học viên gửi yêu cầu học thử và có thể xem chi tiết thông tin học viên, mục tiêu học tập, hình thức học mong muốn (kèm địa chỉ nếu học Offline), từ đó đưa ra quyết định chấp nhận (Accept) - kèm link phòng học nếu học Online - hoặc từ chối lịch kèm lý do (Decline), để chủ động quản lý lịch dạy của mình.

**Why this priority**: Đảm bảo quyền tự chủ của gia sư đối với thời gian biểu và xác lập cam kết giảng dạy hai chiều giữa học viên và gia sư.

**Independent Test**: Gia sư vào danh sách yêu cầu học thử trên Bảng điều khiển (Dashboard), bấm xem chi tiết và bấm "Chấp nhận" hoặc "Từ chối". Trạng thái của buổi học thử lập tức chuyển sang Đã xác nhận (`CONFIRMED`) hoặc Đã từ chối (`DECLINED`), kèm thông báo cập nhật cho học viên.

**Acceptance Scenarios**:

1. **Given** Gia sư có yêu cầu học thử mới đang ở trạng thái Chờ duyệt (`PENDING`) với hình thức Online, **When** Gia sư bấm "Chấp nhận" và nhập/xác nhận link Google Meet, **Then** trạng thái buổi học chuyển sang `CONFIRMED`, khung giờ được chốt cố định trên lịch dạy và học viên nhận được link phòng học trên giao diện.
2. **Given** Gia sư nhận yêu cầu học Offline, **When** Gia sư bấm "Chấp nhận", **Then** trạng thái chuyển sang `CONFIRMED` và địa chỉ hẹn gặp được lưu vào chi tiết buổi học.
3. **Given** Gia sư bị trùng lịch đột xuất hoặc không phù hợp chuyên môn, **When** Gia sư bấm "Từ chối" và chọn lý do (ví dụ: bận lịch cá nhân, không đúng chuyên môn), **Then** trạng thái chuyển sang `DECLINED`, khung giờ được giải phóng và học viên nhận được thông báo giải thích lịch sự.

---

### User Story 3 - Quản lý và theo dõi danh sách lịch học thử tập trung (Priority: P2)

Là một Học viên hoặc Gia sư, tôi muốn theo dõi toàn bộ danh sách các yêu cầu kết nối và buổi học thử sắp tới của mình ở một nơi tập trung (trang Danh sách yêu cầu kết nối & Lịch học `/pages/connection-requests/`), để không bị bỏ lỡ buổi học và dễ dàng chuẩn bị bài học.

**Why this priority**: Giúp cả hai bên quản lý kế hoạch học tập, biết rõ buổi học nào đang chờ duyệt, buổi nào đã xác nhận (có nút vào phòng học hoặc xem địa chỉ) và lịch sử các buổi học trước đó.

**Independent Test**: Truy cập trang `/connection-requests` hoặc Bảng điều khiển, danh sách hiển thị đầy đủ các thẻ yêu cầu được phân loại theo bộ lọc trạng thái (Chờ phản hồi, Đã xác nhận, Lịch sử), hiển thị đúng tên, ảnh đại diện, ngày giờ, hình thức học và nút tác vụ tương ứng.

**Acceptance Scenarios**:

1. **Given** người dùng truy cập trang danh sách yêu cầu kết nối, **When** trang tải xong, **Then** hiển thị danh sách các buổi học thử với đầy đủ thông tin: Tên đối tác, ngày học, khung giờ, thời lượng, hình thức (Online/Offline) và huy hiệu trạng thái rõ ràng bằng tiếng Việt.
2. **Given** buổi học ở trạng thái `CONFIRMED` và hình thức Online, **Then** hiển thị nút "Vào phòng học" dẫn thẳng tới đường link cuộc họp.
3. **Given** danh sách chưa có yêu cầu nào, **Then** hiển thị trạng thái rỗng (Empty State) với nút dẫn đến trang Tìm gia sư.

---

### User Story 4 - Hủy hoặc Đổi lịch học thử trước giờ học (Priority: P3)

Là một Học viên hoặc Gia sư, nếu có việc đột xuất trước buổi học, tôi muốn có thể hủy lịch hoặc đề xuất chuyển sang khung giờ rảnh khác của gia sư trước thời hạn quy định (trước 12h - 24h), để tôn trọng thời gian của nhau.

**Why this priority**: Tăng tính linh hoạt trong thực tế khi lịch trình cá nhân của người dùng có sự thay đổi đột xuất.

**Independent Test**: Người dùng bấm "Hủy lịch" trên thẻ buổi học sắp diễn ra, nhập lý do hủy. Hệ thống cập nhật trạng thái `CANCELLED`, giải phóng khung giờ rảnh và gửi thông báo tới đối tác.

**Acceptance Scenarios**:

1. **Given** buổi học thử đã xác nhận còn cách giờ bắt đầu trên 24 giờ, **When** người dùng bấm hủy lịch và nhập lý do, **Then** trạng thái chuyển sang Đã hủy (`CANCELLED`) và khung giờ được mở lại cho người khác đăng ký.
2. **Given** thời gian đến buổi học ít hơn ngưỡng quy định, **When** người dùng cố gắng hủy, **Then** hệ thống cảnh báo về việc hủy sát giờ và ghi nhận lý do vào nhật ký lịch sử.

---

## Edge Cases

- **Đặt trùng khung giờ (Race Condition)**: Hai học viên cùng mở modal và bấm đặt cùng một khung giờ của một gia sư gần như đồng thời. Hệ thống sử dụng cơ chế khóa giao dịch (Optimistic/Pessimistic Locking) để người xác nhận trước được giữ chỗ, người sau nhận thông báo khung giờ vừa được đăng ký và hướng dẫn chọn giờ khác.
- **Học viên đặt nhiều lịch trùng giờ**: Hệ thống ngăn chặn một học viên gửi yêu cầu học thử bị trùng khoảng thời gian với một buổi học đã xác nhận khác.
- **Gia sư không phản hồi yêu cầu (Quá hạn - SLA Timeout)**: Nếu gia sư không bấm Chấp nhận hoặc Từ chối trước giờ học (hoặc quá 48 giờ kể từ khi gửi), yêu cầu tự động chuyển sang trạng thái Hết hạn (`EXPIRED`) để giải phóng lịch và báo cho học viên biết.
- **Tài khoản chưa đăng nhập**: Nếu khách vãng lai bấm "Đặt lịch học thử 1-1" trên trang hồ sơ gia sư, hệ thống hiển thị Modal đăng nhập nhanh (`#authModal`) để tiếp tục quy trình mà không làm mất thông tin gia sư đang xem.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hệ thống PHẢI cho phép Học viên đã đăng nhập gửi yêu cầu kết nối và đăng ký buổi học thử 1-1 với một Gia sư có hồ sơ hợp lệ.
- **FR-002**: Hệ thống PHẢI hiển thị các khung giờ rảnh thực tế của gia sư trong tuần để học viên lựa chọn trực quan, loại bỏ các khung giờ đã có người đặt trước.
- **FR-003**: Buổi học thử 1-1 là **MIỄN PHÍ (0đ)** nhằm mục đích kết nối, trao đổi nhu cầu và đánh giá năng lực ban đầu (không thu học phí hay tiền cọc qua cổng thanh toán, tuân thủ Hiến pháp KizunaTutor). Yêu cầu sau khi tạo sẽ chuyển ngay sang trạng thái Chờ duyệt (`PENDING`) gửi tới Gia sư.
- **FR-004**: Hệ thống PHẢI cho phép Học viên linh hoạt lựa chọn thời lượng buổi học thử là **25 phút** (buổi trao đổi & định hướng nhanh) hoặc **50 phút** (buổi trải nghiệm 1 tiết học đầy đủ).
- **FR-005**: Hệ thống PHẢI hỗ trợ cả hai hình thức: **Học trực tuyến (Online)** và **Học trực tiếp (Offline)**:
  - Nếu chọn **Học trực tuyến (Online)**: Khi Gia sư chấp nhận lịch, Gia sư PHẢI cung cấp liên kết phòng học trực tuyến (Google Meet / Zoom / Web Room) để lưu vào `meeting_link`.
  - Nếu chọn **Học trực tiếp (Offline)**: Học viên nhập địa chỉ hoặc địa điểm mong muốn học (tại nhà hoặc quán cà phê học tập); chỉ khả dụng khi Gia sư có hỗ trợ dạy trực tiếp tại khu vực đó.
- **FR-006**: Hệ thống PHẢI tự động ghi nhận bản ghi vào bảng `connection_requests` và `trial_bookings` với trạng thái ban đầu là `PENDING`.
- **FR-007**: Hệ thống PHẢI cho phép Gia sư xem danh sách các yêu cầu đang chờ duyệt, xem hồ sơ học viên, xem lời nhắn ban đầu và thực hiện thao tác Chấp nhận (`CONFIRMED`) hoặc Từ chối (`DECLINED`) kèm lý do.
- **FR-008**: Hệ thống PHẢI ghi lại lịch sử thay đổi trạng thái của buổi học thử vào bảng `trial_booking_histories` để phục vụ tra cứu và kiểm toán.
- **FR-009**: Hệ thống PHẢI cho phép hủy buổi học thử trước thời hạn quy định, cập nhật trạng thái `CANCELLED` và giải phóng khung giờ rảnh của gia sư.
- **FR-010**: Mọi giao diện tương tác (modal đặt lịch, danh sách yêu cầu, thông báo, badge trạng thái) PHẢI hiển thị 100% bằng tiếng Việt tự nhiên, tuân thủ `design.md` và `UI_LOCALIZATION_RULES.md`.

### Key Entities

- **ConnectionRequest (`connection_requests`)**: Đại diện cho yêu cầu kết nối ban đầu giữa Học viên và Gia sư. Các thuộc tính chính: `requestId`, `learnerId`, `tutorId`, `initialMessage`, `status` (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELLED`, `EXPIRED`), `createdAt`, `respondedAt`.
- **TrialBooking (`trial_bookings`)**: Đại diện cho phiên học thử cụ thể với thời gian chốt lịch. Các thuộc tính chính: `bookingId`, `requestId`, `learnerId`, `tutorId`, `scheduledStartTime`, `scheduledEndTime`, `meetingLink`, `locationDetails`, `bookingStatus` (`PENDING`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `RESCHEDULED`), `learnerConfirmed`, `tutorConfirmed`.
- **TrialBookingHistory (`trial_booking_histories`)**: Lưu vết toàn bộ các lần chuyển đổi trạng thái của buổi học thử, ai là người thực hiện (`changedByUserId`), trạng thái cũ, trạng thái mới và lý do thay đổi.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Học viên có thể hoàn thành việc chọn lịch và gửi yêu cầu học thử thành công trong vòng dưới 60 giây.
- **SC-002**: Tốc độ phản hồi của API tạo và duyệt yêu cầu học thử đạt dưới 500ms trong điều kiện mạng bình thường.
- **SC-003**: 100% các trạng thái trên giao diện hiển thị bằng tiếng Việt rõ ràng, không xuất hiện bất kỳ mã enum thô hay lỗi kỹ thuật nào.
- **SC-004**: Tỷ lệ xung đột lịch (double-booking) đạt mức 0% nhờ cơ chế khóa kiểm tra lịch khả dụng thời gian thực.
- **SC-005**: Gia sư có thể thao tác phê duyệt hoặc từ chối yêu cầu chỉ với tối đa 2 lượt nhấn chuột từ Bảng điều khiển.

---

## Assumptions

- Học viên và Gia sư đã có tài khoản hợp lệ trên hệ thống KizunaTutor.
- Hồ sơ của Gia sư đã được duyệt và có thiết lập thời gian biểu rảnh trong tuần trước khi học viên có thể đặt lịch.
- Giao diện đặt lịch tái sử dụng modal đã dựng trên trang chi tiết gia sư (`/pages/tutor-profile/`) kết hợp với trang danh sách yêu cầu kết nối (`/pages/connection-requests/`).
- Việc gửi thông báo được xử lý qua hệ thống thông báo nội bộ (In-app notification) và cập nhật giao diện thời gian thực.
