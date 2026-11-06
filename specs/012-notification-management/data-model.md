# Data Model: FE-12

Ngày2026-10-31. Bảng mới UUID PK; timestamp TIMESTAMPTZ/OffsetDateTime, UI/schedule Asia/Ho_Chi_Minh; mapping snake_case explicit.

## Notification và UserNotification (hiện hữu)

notifications: notification_id UUID; title varchar150 không trắng; content text1..10.000 không trắng; notification_type enum SYSTEM/TRIAL/ASSIGNMENT/PAYMENT/TICKET; created_at; source_dispatch_id FK mới nullable legacy. Content đã xác nhận bất biến; không dùng chung read state ở đây. Không kế thừa BaseEntity nếu không có updated_at.

user_notifications: user_notification_id UUID; notification_id FK; user_id FK; is_read bool defaultfalse; read_at nullable; received_at mới (legacy đánh dấu provenance nếu cần); deleted_at nullable soft-delete; UNIQUE(notification_id,user_id). Legacy không bịa read timestamp khi thiếu; preflight report trước backfill. Không giả định có created_at/updated_at từ BaseEntity.

Unread=owner AND deleted_at IS NULL AND is_read=false. First-read conditional update chỉ khi false; giữ read_at cũ khi retry trên bản chưa xóa. Soft-delete conditional update một lần, không cascade nguồn. Count lấy committed state, không decremented blindly ở UI. Race read/delete serialize row; bản đã xóa không được GET detail hoặc POST read, cả hai trả404. DELETE lặp của đúng owner trả200 với deleted=true và count hiện tại, không giảm lần nữa hoặc sống lại dữ liệu; người khác404.

## NotificationPreference

notification_preference_state: state_id UUID; user_id unique FK; version bigint @Version; updated_at. notification_preferences: preference_id UUID; user_id FK; type enum năm nhóm; in_app_enabled bool; email_enabled bool; UNIQUE(user_id,type). Update cả matrix năm nhóm chung transaction/version; thiếu hàng dùng defaults true/false. Không tác động OTP/security emails.

Khi tạo delivery, lock preference state và user hiện tại để tuyến tính hóa với update preferences/account khi cần. Quyết định phát dựa giá trị tại lúc claim gửi; đổi sau khi đã bắt đầu truyền không thu hồi thư đã gửi. Urgent flag chỉ admin all-users, in-app luôn chọn/eligible; email vẫn theo preference nhóm.

## NotificationDispatch (broadcast hoặc sự kiện)

dispatch_id UUID; origin ADMIN/EVENT; creator_id FK nullable event; submission_key UUID nullable event; event_key varchar255 nullable admin; type; title/content validation; urgency NORMAL/URGENT; target_mode ROLES/ALL/EXPLICIT; target_roles danh mục sáu role khi ROLES; requested_channels không rỗng; scheduled_at nullable; created_at; cohort_frozen_at nullable; version; status; recipient_count nullable trước freeze; preview_count chỉ dự kiến.

UNIQUE(creator_id,submission_key) với admin; unique event_key với EVENT. Same key+same body trả đợt cũ, same key+different body conflict. EVENT chỉ explicit recipients đã xác minh bởi module gốc, không cho external client nhập event hoặc urgent. URGENT yêu cầu ADMIN/ALL, scheduled_at null, IN_APP luôn trong channels. Scheduled NORMAL phải future lúc submit; lưu Instant, không đổi nội dung hoặc cohort rule sau xác nhận.

cohort table notification_dispatch_recipients: row_id UUID, dispatch_id FK, user_id FK, snapshotted_role, selected_at; UNIQUE(dispatch_id,user_id). Freeze đúng một lần trong transaction khóa dispatch và insert-select active roles tại snapshot; publish frozen_at/count chỉ sau insert đầy đủ. Không freeze empty từng phần theo page; worker pagination gửi sau frozen cohort. Restart không thêm người mới, trùng role không trùng người; recheck active trước phát, scope event theo module.

## NotificationOutboxEvent

outbox_id UUID; event_key unique stable source/type/aggregate transition ID; event_type allowlist; aggregate_reference internal; committed facts/recipient IDs tối thiểu; created_at; processed_at nullable; lease_owner/lease_until; outcome. Insert trong cùng transaction mutation nguồn; rollback nguồn không sinh event. Không queue after-commit RAM đơn lẻ. Consumer tạo dispatch với cùng event_key rồi processed atomically, retry không duplicate.

Payload không raw DTO toàn nguồn/password/OTP/payment gateway log/ticket internal notes. Producer phải kiểm recipient và nội dung liên quan; consumer recheck active/scope trước phát nơi cần. Chỉ thông báo công khai từ support, không lấy escalation decision nội bộ.

## NotificationDelivery và Attempt

delivery_id UUID; dispatch_id FK; user_id FK thuộc cohort; channel IN_APP/EMAIL; state PENDING/CLAIMED/EMAIL_SENDING/IN_APP_AVAILABLE/EMAIL_ACCEPTED/FAILED/SKIPPED/UNKNOWN; reason_code nội bộ mapped label; attempt_count; next_attempt_at; lease_until/worker; claim_token UUID mới cho mỗi lần claim; updated_at; unique(dispatch_id,user_id,channel).

delivery_attempts: attempt_id UUID; delivery_id FK; attempt_number; started_at/finished_at; safe_outcome; provider_reference nullable nếu transport thật cung cấp; không secret hoặc SMTP raw response chứa PII. UNIQUE(delivery_id,attempt_number).

Transitions:

- PENDING→CLAIMED chỉ sau giữ chỗ slot thực thi; mỗi lượt claim tối đa min(100, số slot rảnh đã giữ), lease riêng60giây và claim_token mới. Trước phát phải kiểm token/lease còn hạn và gia hạn60giây bằng conditional update; không được phát nếu mất quyền sở hữu. Eligibility/pref check; nếu không được phát→SKIPPED có reason. Không để delivery đã claim xếp hàng chờ quá số slot thực thi.
- IN_APP: insert notification recipient + IN_APP_AVAILABLE outcome chung transaction; lỗi rollback về pending/retry hữu hạn theo chính sách worker. Unique constraint tránh duplicate.
- EMAIL: claim→EMAIL_SENDING và lease được xác nhận/gia hạn bằng token có điều kiện, commit trước network send; call adapter với timeout10giây; kết quả rõ accepted→EMAIL_ACCEPTED, lỗi xác định permanent→FAILED, timeout/kết quả không rõ→UNKNOWN. Network call ngoài DB transaction dài; finalize phải còn token sở hữu và lease hợp lệ, worker mất lease không ghi đè kết quả. Lease không bảo đảm exactly-once SMTP nếu tiến trình bị treo, do đó EMAIL_SENDING hết hạn không được phát lại.
- Crash khi CLAIMED trước transmission có thể reclaim bằng token mới; crash/lease hết ở EMAIL_SENDING→UNKNOWN, tuyệt đối không blind resend. Email xác định chưa gửi và transient có tối đa4lần gọi tổng cộng: attempt1 ngay, retry attempt2 sau30giây, attempt3 sau120giây, attempt4 sau300giây kể từ kết quả known-unsent trước đó. Trước retry đưa về PENDING với next_attempt_at, không đánh dấu FAILED terminal sớm; hết attempt4→FAILED. UNKNOWN không áp dụng retry. Chỉ lần gọi transport thực sự tăng attempt_count.
- IN_APP_AVAILABLE/EMAIL_ACCEPTED/SKIPPED/FAILED/UNKNOWN không tự resend bản này; không nút manual retry. UNKNOWN lưu để operator điều tra, không tự coi FAILED/SENT.

Không lưu state đọc của email; accepted chỉ chấp nhận SMTP, không inbox delivery. User deletion không xóa historical counters. Aggregation dựa delivery records/cohort, không dựa user_notifications còn thấy sau soft-delete.

## Dispatch aggregation

SCHEDULED khi chưa hạn; QUEUED khi đã lưu chờ xử lý; SENDING khi còn pending/claimed/sending; NEEDS_REVIEW khi có UNKNOWN sau mọi lượt khác kết thúc; NO_RECIPIENTS khi cohort0. Sau không còn pending/unknown: COMPLETED nếu toàn terminal success hoặc skip; PARTIAL nếu cóFAILED và cósuccess/skip; FAILED nếu toàn lượt cần gửi làFAILED và khôngsuccess/skip. All-skipped làCOMPLETED với sốaccepted0 và skipped rõ; không nói mọi người đã đọc/nhận. Tổng theo kênh = pending+success+failed+skipped+unknown, unique actual users khác tổng lượt hai kênh.

## Index/migration/concurrency

Indexes owner(deleted_at,is_read,received_at,id), notification(type,created_at,id), dispatch(scheduled_at,status), delivery(state,next_attempt_at,lease_until), unique source/submission/cohort/delivery. Filter history stable(created_at,id). Validate query EXPLAIN trên10.000rows.

SQL tăng dần có migration version/lock; ddl-auto hiện none, không chạy source DROP CASCADE; preflight duplicates và missing fields trước unique constraints. Không sửa FE-10/11 migrations đã dùng. Read/delete commands idempotent không yêu cầu version; preferences dùng expectedVersion; dispatch submit dùng submissionKey, không generic patch/cancel API.
