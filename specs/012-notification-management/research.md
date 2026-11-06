# Research: FE-12 Notification Management

Ngày 2026-10-31. Căn cứ spec với năm quyết định clarify, constitution và mã nguồn hiện có. Chưa gửi thông báo/email, chạy migration hoặc kiểm thử ứng dụng.

## 1. Kiến trúc và schema hiện tại

- Decision: Giữ Java17/Spring Boot3.3.4, Thymeleaf, Security, JPA, PostgreSQL và JavaMailSender. Không thêm Kafka/Redis hoặc nhà cung cấp email mới.
- Rationale: NotificationServiceImpl/INotificationService còn khung; Notification/UserNotification chỉ có ID, tên cột notificationId/userNotificationId lệch snake_case schema. BaseEntity thêm updated_at/created_at không tương ứng schema notification chỉ có created_at và user_notifications chỉ có is_read/read_at. Map explicit theo bảng, không tự cho rằng entity đã đủ nghiệp vụ.
- Alternatives considered: Queue in-memory/@Async đơn lẻ mất tác vụ khi restart, không đáp ứng FR-010. Dựng broker mới tăng vận hành không cần cho tập nghiệm thu.

## 2. Ghi bền vững và chống trùng

- Decision: PostgreSQL outbox cho sự kiện nghiệp vụ, broadcast bền vững cho quản trị; worker scheduler poll5giây, claim bằng FOR UPDATE SKIP LOCKED tối đa min(100, số slot thực thi đang rảnh đã được giữ chỗ). Mỗi delivery có lease60giây và token sở hữu riêng; không đưa tác vụ đã claim vào hàng đợi chờ không giới hạn. Ngay trước phát, kiểm token/lease còn hạn và gia hạn60giây bằng cập nhật có điều kiện; thất bại thì không bắt đầu gửi. Finalize chỉ khi token còn sở hữu, worker mất lease không ghi đè kết quả. Insert event cùng transaction nghiệp vụ; không chỉ publish after-commit vào bộ nhớ.
- Rationale: Commit nghiệp vụ nhưng crash trước tạo thông báo không được làm mất sự kiện. Unique(source_event_id,recipient,channel) và unique(notification,user) chống trùng trong ứng dụng; admin submission_key unique theo creator chống double submit.
- Alternatives considered: Gửi email trực tiếp trong transaction nghiệp vụ chậm và không rollback được thư; tự retry toàn đợt sẽ phát lại bản thành công.
- Decision: Tập nhận nhóm hẹn giờ materialize một lần bằng transaction nhất quán khi đến hạn, freeze cohort. Số preview chỉ dự kiến; phục hồi dùng cohort đã chốt, không mở rộng. Recheck active và preferences trước phát; bảo toàn nguồn scope cho business events.

## 3. Email có trạng thái trung thực

- Decision: Adapter notification email dùng JavaMailSender đồng bộ trong delivery worker, trả kết quả có kiểu; không gọi phương thức OTP @Async void nuốt exception. Không refactor OTP/staff credentials ngoài phạm vi; tùy chọn FE-12 không chặn thư bảo mật.
- Rationale: IEmailService hiện chuyên OTP/credentials/diagnostic, không có kết quả gửi broadcast. Dùng chung transport/cấu hình hiện có nhưng cần adapter riêng để ghi ACCEPTED/FAILED/UNKNOWN. SMTP acceptance không là delivered/read.
- Decision: Không retry tự động email đã bắt đầu truyền nhưng timeout/crash trước lưu kết quả: UNKNOWN cần kiểm tra. Lease hết sau EMAIL_SENDING cũng thành UNKNOWN, không gửi lại mù. Trước bắt đầu SMTP có thể reclaim an toàn; in-app transaction rollback/retry an toàn. Không hứa email exactly-once.
- Decision: Email known-unsent transient được gọi tối đa4lần tổng cộng: lần đầu +3retry sau30/120/300giây tương ứng. Hết attempt4 hoặc lỗi permanent →FAILED; UNKNOWN không retry. Mỗi attempt thực sự gọi transport mới tăng attempt_count, không tăng vì polling hoặc bỏ qua preference.
- Alternatives considered: Gắn SENT ngay khi enqueue sai nghĩa; retry timeout có thể gửi trùng. Claim nhiều hơn số slot rảnh làm lease hết khi còn chờ; không chọn mô hình đó.

## 4. Tùy chọn, quyền và trạng thái đọc

- Decision: Unique preference(user,type), hai bool in_app/email, absent mặc định true/false, version chống lost update. Chỉ bản khẩn admin-to-all bắt buộc in-app, email vẫn opt-in; không cho producer tùy ý nâng thành khẩn để vượt tùy chọn.
- Decision: GET detail không side effect. Client tải chi tiết thành công rồi POST read, server idempotent first-read timestamp; lỗi read hiển thị chưa lưu, count giữ trạng thái đã commit. DELETE soft-delete bản cá nhân, không xóa nguồn/audit/bản người khác; retry không sống lại hoặc đếm âm.
- Rationale: Quyết định auto-mark-read là hành vi UI, không cần mutation trong GET; tránh prefetch/crawler vô tình thay đổi dữ liệu.
- Decision: Reload User/role/ACTIVE/deletedAt mỗi request, object owner checks mọi mutation/read. Admin-only broadcast/history; scoped CSRF cho POST/PUT/DELETE dựa cookie JWT; không lộ nội bộ FE-10/11 qua email hoặc deep link. UI/email escaped và tên nhóm tiếng Việt.

## 5. Nguồn sự kiện và phụ thuộc

- Decision: NotificationEventPort chỉ nội bộ, không API public phát sự kiện tùy ý. Mỗi source event có stable ID gắn transition đã commit, recipient được module gốc xác định/kiểm quyền. Ma trận integration trong contracts là phạm vi dự kiến cần đối chiếu implementation thực.
- Findings: TrialBookingServiceImpl, AssignmentServiceImpl, SupportTicketServiceImpl và MembershipPaymentServiceImpl là stub. TutorMembershipServiceImpl có tạo URL PENDING, xử lý trả thanh toán và directActivate; phải kiểm xác minh chữ ký/trạng thái/thành viên trước phát payment-success, không dùng createPaymentUrl/directActivate như bằng chứng thanh toán thật.
- Delivery gate: Từng nguồn thiếu transaction nghiệp vụ thật phải ghi blocker và chưa đạt FR-015, không dùng test event giả hoặc FE-10 spec thay code có sẵn. Không tự xây toàn module học thử/bài tập/payment/support trong FE-12.

## 6. Migration và kiểm chứng

- Decision: SQL tăng dần có version/lock, preflight mapping và duplicate recipient/source trước unique index; không chạy schema nguồn DROP CASCADE. Không tự backfill read_at giả hoặc coi email legacy đã được gửi khi không có chứng cứ.
- Decision: PostgreSQL test riêng cho claim/lease/transaction, SMTP giả lập chỉ môi trường test, Clock inject cho scheduling. Test browser timing cho SC-006, không thay bằng HTTP latency. SMTP thật chỉ kiểm với mailbox được cho phép ngoài lượt plan.
- Alternatives considered: H2 không chứng minh locking/partial indexes tương đương; build pass không chứng minh bốn module đã phát đúng người.

Không còn quyết định nghiệp vụ chặn thiết kế; các nguồn event còn thiếu và khả năng SMTP thực là dependency giao hàng cần đo/ghi rõ.
