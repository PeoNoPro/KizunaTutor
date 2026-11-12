# FE-12 — Hướng dẫn gắn thông báo tự động cho các module khác

Ngày 2026-11-09. Dành cho các bạn phụ trách học thử (FE-04/05), bài tập (FE-07) và thanh toán hội viên (FE-09). Mỗi chỗ chỉ cần **thêm vài dòng** vào code của bạn; phần lưu thông báo, kiểm tùy chọn người nhận, gửi email và chống trùng đã có sẵn ở FE-12.

## 1. Cách gọi

Tiêm `NotificationEvents` vào service của bạn, rồi gọi `publish` **ngay sau khi lưu xong thay đổi**, trong cùng `@Transactional`:

```java
private final com.kizuna.features.notification.service.NotificationEvents notificationEvents;

notificationEvents.publish(
    "payment:" + tx.getTransactionId() + ":success",   // mã sự kiện cố định, mỗi lần chuyển trạng thái một mã
    "PAYMENT",                                          // nhóm: SYSTEM, TRIAL, ASSIGNMENT, PAYMENT, TICKET
    "Thanh toán thành công",                            // tiêu đề, tối đa 150 ký tự
    "Gói " + plan.getPlanName() + " đã được kích hoạt.", // nội dung chữ thường, tối đa 10.000 ký tự
    "/tutor/membership",                                // trang mở khi bấm thông báo (đường dẫn nội bộ, bắt đầu bằng "/")
    List.of(user.getUserId()));                         // người nhận (user_id)
```

Quy tắc:

- **Mã sự kiện** phải giống nhau nếu cùng một sự kiện bị xử lý lại (ví dụ VNPay gọi IPN hai lần) → FE-12 chỉ tạo **một** thông báo. Không dùng `UUID.randomUUID()`.
- **Nội dung** không chứa mật khẩu, mã OTP, dữ liệu cổng thanh toán, ghi chú nội bộ.
- Người nhận tự quyết định nhận hay không trong "Tùy chọn nhận thông báo": trong ứng dụng mặc định **bật**, email mặc định **tắt**. Bạn không cần kiểm tra việc này.
- Lỗi khi tạo thông báo **không làm hỏng** thao tác của bạn (chỉ ghi cảnh báo vào log).
- Trang mở từ thông báo vẫn phải tự kiểm quyền như bình thường.

## 2. Chỗ cần thêm (đề xuất)

| Module | File / chỗ | Người nhận | Gợi ý |
| --- | --- | --- | --- |
| Thanh toán (FE-09) | `PaymentResultProcessor.activate(...)`, ngay sau `membershipRepository.save(...)` | `user.getUserId()` (gia sư) | mã `payment:<transactionId>:success`, nhóm `PAYMENT`, link `/tutor/membership` |
| Học thử (FE-05) | `TrialBookingServiceImpl.createTrialBooking(...)`, sau `trialBookingRepository.save(trialBooking)` | `trialBooking.getTutor().getUser().getUserId()` | mã `trial:<bookingId>:created`, nhóm `TRIAL`, link `/tutor/connections` |
| Học thử (FE-05) | `respondTrialBooking(...)`, sau `trialBookingRepository.save(trialBooking)` | `trialBooking.getLearner().getUser().getUserId()` | mã `trial:<bookingId>:<ACCEPT/DECLINE>`, link `/learner/connections` |
| Học thử (FE-05) | `cancelTrialBooking(...)`, sau `save` | người còn lại (gia sư hoặc học viên, không phải người vừa hủy) | mã `trial:<bookingId>:cancelled` |
| Bài tập (FE-07) | `SubmissionServiceImpl.grade(...)`, sau `submissionRepository.save(...)` | học viên nộp bài | mã `assignment:<submissionId>:graded`, nhóm `ASSIGNMENT` |
| Bài tập (FE-07) | chỗ giao/công bố bài tập mới | học viên của lớp | mã `assignment:<assignmentId>:published` |

Email thanh toán/xác minh hiện các bạn đang tự gửi (`PaymentNotificationService`, `VerificationNotificationService`) vẫn giữ nguyên; FE-12 không tắt chúng.

## 3. Đã gắn sẵn (phần của FE-10/FE-11)

- Yêu cầu hỗ trợ: CS trả lời → người gửi; người gửi bổ sung → CS phụ trách; giải quyết → người gửi; giao/tự giao → CS; chuyển lên/đổi người nhận → Quản lý; Quản lý trả kết quả → CS.
- Sự cố kỹ thuật: Quản lý báo sự cố → mọi Admin; Admin nhận/khắc phục/đóng/trả lại → người báo.

## 4. Đã gắn thêm vào module của các bạn (2026-11-10, chỉ thêm dòng, không sửa code có sẵn)

- Thanh toán: `PaymentResultProcessor` — kích hoạt gói thành công / giao dịch thất bại → gia sư (`/tutor/membership`).
- Học thử: `TrialBookingServiceImpl` — tạo yêu cầu → gia sư; nhận/từ chối → học viên; hủy, đổi lịch → người còn lại.
- Bài tập: `AssignmentServiceImpl.create` → học viên; `SubmissionServiceImpl.submit` → gia sư; `grade` → học viên.
- Mỗi chỗ dùng trường `@Autowired(required = false) NotificationEvents` nên constructor và test dựng tay của các bạn không đổi.
