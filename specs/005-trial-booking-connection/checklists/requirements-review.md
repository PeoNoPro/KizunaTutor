# Requirements Review Checklist: FE-05 — Trial Lesson Booking & Connection

**Purpose**: Bộ tiêu chí kiểm định chất lượng yêu cầu (Unit Tests for Requirements) đánh giá tính đầy đủ, rõ ràng, nhất quán và độ sẵn sàng của tính năng FE-05 trước khi code.  
**Created**: 2026-11-04  
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [data-model.md](../data-model.md) | [contracts/trial-booking-api.md](../contracts/trial-booking-api.md)  

**Note**: Bảng kiểm tra này được sinh ra bởi lệnh `/speckit-checklist` để rà soát chất lượng văn bản yêu cầu và hợp đồng kỹ thuật.  
**Review Ownership**: Thuộc quyền sở hữu của Reviewer / Lập trình viên. Đánh dấu `[x]` khi và chỉ khi tiêu chí chất lượng tương ứng đã được rà soát và thỏa mãn trong tài liệu đặc tả.  
**Marker Semantics**: `[x]` nghĩa là yêu cầu đã được viết rõ ràng, đầy đủ và đạt chuẩn chất lượng đặc tả; `[x]` KHÔNG mang nghĩa là code đã hoàn thành.  

---

## 1. Requirement Completeness (Tính đầy đủ của yêu cầu nghiệp vụ)

- [x] CHK001 - Tài liệu đặc tả đã định nghĩa đầy đủ luồng đăng ký học thử 1-1 từ hồ sơ gia sư theo đúng mục 5.1 của Report 3 chưa? [Completeness, Spec §User Story 1]
- [x] CHK002 - Quy tắc buổi học thử miễn phí (0đ - không phát sinh giao dịch cọc ví/VNPay) đã được thể chế hóa rõ ràng và nhất quán với Hiến pháp dự án chưa? [Completeness, Spec §FR-003, Constitution §V]
- [x] CHK003 - Hai mốc thời lượng 25 phút và 50 phút đã được quy định chi tiết trong cả spec, hợp đồng API và giao diện modal chưa? [Completeness, Spec §FR-004, Contract §2.1]
- [x] CHK004 - Cả hai hình thức Học trực tuyến (Online) và Học trực tiếp (Offline) đã có đầy đủ trường dữ liệu lưu trữ tương ứng (`meeting_link` và `location_details`) chưa? [Completeness, Spec §FR-005, Data-Model §2.2]
- [x] CHK005 - Quy trình Gia sư tiếp nhận, xem thông tin và duyệt Chấp nhận / Từ chối (kèm lý do) đã được đặc tả hoàn chỉnh chưa? [Completeness, Spec §User Story 2, Contract §2.3]
- [x] CHK006 - Quy trình hủy lịch trước buổi học kèm ghi nhận lý do đã được quy định chi tiết chưa? [Completeness, Spec §User Story 4, Contract §2.4]

---

## 2. Requirement Clarity & Precision (Độ rõ ràng, đo lường được và phi kỹ thuật)

- [x] CHK007 - Tiêu chí thành công SC-001 (thời gian hoàn tất đặt lịch dưới 60 giây) có thể đo lường khách quan mà không phụ thuộc vào công nghệ triển khai không? [Measurability, Spec §SC-001]
- [x] CHK008 - Tiêu chí thành công SC-002 (thời gian phản hồi API < 500ms) và SC-004 (tỷ lệ xung đột lịch 0%) đã có phương pháp đo lường cụ thể chưa? [Measurability, Spec §SC-002, SC-004]
- [x] CHK009 - Ngưỡng thời gian hủy lịch (>24 giờ so với giờ học) đã được định lượng rõ ràng thay vì dùng từ ngữ mơ hồ ("hủy sớm", "hủy trễ") chưa? [Clarity, Spec §User Story 4]
- [x] CHK010 - Thời hạn hết hạn tự động (SLA Timeout 48 giờ khi gia sư không phản hồi) đã được lượng hóa chính xác chưa? [Clarity, Spec §Edge Cases]

---

## 3. Constitution & Guardrail Compliance (Tuân thủ Hiến pháp & Quy chuẩn thiết kế)

- [x] CHK011 - Đặc tả đã đảm bảo 100% nhãn trạng thái và thông điệp hiển thị bằng tiếng Việt tự nhiên, không rò rỉ tên enum nội bộ (`PENDING`, `CONFIRMED`) ra UI chưa? [Compliance, Constitution §I, Spec §FR-010]
- [x] CHK012 - Quy tắc cấm viết hoa toàn bộ (No ALL-CAPS) đã được kiểm tra trên toàn bộ các câu thông báo, alert và tiêu đề modal chưa? [Compliance, Constitution §I]
- [x] CHK013 - Đã quy định rõ ràng yêu cầu hiển thị trạng thái rỗng (Empty State) và cấm tiêm mảng giả/mock data trên trang danh sách yêu cầu chưa? [Compliance, Constitution §II, Spec §User Story 3]
- [x] CHK014 - Nguyên tắc cô lập phân hệ (Feature Isolation) đã được đảm bảo: gói `booking` cung cấp public service cho các phân hệ khác, không gọi Repository chéo chưa? [Compliance, Constitution §III, Plan §Constitution Check]
- [x] CHK015 - Giao diện đặt lịch và danh sách yêu cầu đã tuân thủ quy chuẩn Component dùng chung (chỉ dùng thẻ placeholder `#header-placeholder`, `#footer-placeholder`) chưa? [Compliance, Constitution §IV, .agents/rules/components_rule.md]

---

## 4. API & Data Model Consistency (Tính nhất quán CSDL & Hợp đồng API)

- [x] CHK016 - Sơ đồ ERD và cấu trúc bảng `connection_requests`, `trial_bookings`, `trial_booking_histories` có hoàn toàn khớp với schema gốc `DatabasePgAdmin4.txt` không? [Consistency, Data-Model §1, DatabasePgAdmin4.txt]
- [x] CHK017 - Hợp đồng REST API đã bao phủ đầy đủ tất cả 5 thao tác cần thiết (Tạo, Xem danh sách, Xem chi tiết, Duyệt, Hủy) chưa? [Coverage, Contract §1]
- [x] CHK018 - Cơ chế phòng chống đặt trùng lịch (Time-range overlap check) đã được mô tả giải pháp nhất quán giữa `research.md`, `data-model.md` và `plan.md` chưa? [Consistency, Research §Quyết định 2, Plan §Summary]
- [x] CHK019 - Máy trạng thái (State Machine) của buổi học thử có các bước chuyển đổi trạng thái hợp lệ, không có trạng thái bế tắc (deadlock state) không? [Consistency, Data-Model §3]

---

## 5. Scenario & Edge Case Coverage (Độ bao phủ kịch bản & Tình huống biên)

- [x] CHK020 - Kịch bản xử lý khi khách vãng lai (chưa đăng nhập) bấm nút "Đặt lịch học thử 1-1" đã được quy định hiển thị `#authModal` chưa? [Coverage, Spec §Edge Cases]
- [x] CHK021 - Kịch bản xử lý khi có hai học viên cùng bấm đặt trùng một khung giờ tại cùng một tích tắc đã được định nghĩa mã lỗi `409 Conflict` và câu thông báo tiếng Việt chưa? [Coverage, Spec §Edge Cases, Contract §2.1]
- [x] CHK022 - Kịch bản người dùng hủy lịch sau thời hạn quy định (sát giờ học) đã có thông báo cảnh báo và lưu vết vào `trial_booking_histories` chưa? [Coverage, Spec §User Story 4]
- [x] CHK023 - Hướng dẫn kiểm chứng End-to-End (`quickstart.md`) đã có đầy đủ dữ liệu mẫu và các bước thực tế để kiểm tra cả luồng thành công lẫn luồng ngoại lệ chưa? [Traceability, Quickstart §2]

---

## Notes

- Người phụ trách rà soát đánh dấu `[x]` cho từng mục sau khi kiểm tra chéo các file tài liệu.
- Tài liệu kiểm định chất lượng này dùng làm căn cứ nghiệm thu (Gate check) trước khi chạy lệnh `$speckit-tasks` và triển khai mã nguồn `$speckit-implement`.

