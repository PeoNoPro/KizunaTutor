# Contracts: FE-12 Notification Management

Thiết kế chưa triển khai. Auth hiện có cộng current DB account ACTIVE/deletedAt/role; personal APIs chỉ object owner, broadcast/history chỉ admin hiện tại. Không đặt endpoint dưới `/api/user/**` đang có public matcher. CSRF cho POST/PUT/DELETE dùng cookie, token từ SSR, không đổi auth ngoài phạm vi.

## Common rules

- 400 input sai,401 unauthenticated,403 sai role/account,404 không tồn tại/không có quyền đọc object,409 stale preference/submissionKey đổi body,503 nguồn không khả dụng. Error `{message,fieldErrors?}` labels Việt, không raw exceptions/roles/keys/secrets.
- page>=0,size1..50 default20, query sai400; thời gian ISO và hiển thị UTC+7. Title1..150/content1..10.000 Unicode code points không trắng. Nội dung plain text escaped, không admin-supplied executable HTML hoặc tùy ý remote URL.
- GET không đổi state; UI sau load detail thành công gọi POST read. Nếu POST read lỗi, hiển thị chưa lưu trạng thái, không coi unreadCount đã thay đổi.

## Personal — `/api/v1/notifications`

| Method/path | Input | Output/behavior |
| --- | --- | --- |
| GET base | read=all/unread,type,page,size | `{items,page,size,totalElements,unreadCount}`; scope owner,deleted null,receivedAt desc/id desc |
| GET `/unread-count` | Không | `{unreadCount}` committed owner state |
| GET `/{userNotificationId}` | Không | `{id,title,content,typeLabel,receivedAt,isRead,readAt,actionPath?}` owner/nondeleted; GET không tự mutate |
| POST `/{id}/read` | Không |200 `{isRead:true,readAt,unreadCount}`, timestamp đầu tiên bất biến, repeat idempotent; client tự gọi sau load detail thành công hoặc nút danh sách |
| DELETE `/{id}` | Không |200 `{deleted:true,unreadCount}`, soft-delete sau xác nhận UI; repeat đúng owner vẫn200, không giảm count lặp; người khác404 |
| GET `/preferences` | Không | `{version,groups:[{type,typeLabel,inAppEnabled,emailEnabled}]}` năm nhóm, defaulttrue/false |
| PUT `/preferences` | `{expectedVersion,groups}` | Whole matrix năm nhóm mỗi loại đúng1lần, bool thật; atomic version update,200 giá trị mới,409 stale |

GET detail/bất kỳ read-mark sau xóa trả404; DELETE repeat owner vẫn200 không hồi sinh. DTO cá nhân không có danh sách recipient khác/email address/internal notes. actionPath chỉ relative path allowlist do server tạo và nơi đến recheck quyền, không trust client URL.

## Admin — `/api/admin/notification-broadcasts`

| Method/path | Input | Output/behavior |
| --- | --- | --- |
| POST `/preview` | `{title,content,type,targetMode,roles,channels,urgent,scheduledAt?}` | Validate; trả safe preview,estimatedRecipients,requiresAllUsersConfirmation. Không persist/gửi, số chỉ dự kiến |
| POST base | Cùng fields + `{submissionKey,confirmationText?}` | Validate/admin/CSRF; ALL+urgent yêu cầu confirmationText chính xác “Xác nhận”, IN_APP bắt buộc, không schedule; ROLES thông thường cho future scheduledAt;202 `{broadcastId,statusLabel,scheduledAt?,message}` chỉ queued/scheduled, không sent |
| GET base | fromDate,toDate,role,channel,status,title,page,size | History paginated createdAt desc/id desc; actor,title,target,channels,scheduledAt/status labels |
| GET `/{id}` | Không | Confirmed content/creator,previewCount?,actualRecipientCount?,frozenAt, per-channel pending/success/failed/skipped/unknown, aggregate labels |
| GET `/{id}/deliveries` | channel,state,page,size | Chi tiết lượt phục vụ đối chiếu kết quả, chỉ dữ liệu tối thiểu/admin; không provider raw errors/secrets |

Submission sameKey+sameBody trả202 cùngID, differentBody409. ROLES một hoặc nhiều role allowlist, ALL chỉ urgent scope hiện tại. Channel IN_APP/EMAIL/both; urgent luôn IN_APP, email optional và vẫn preference. Target groups chỉ sáu role active. Không endpoint cancel/reschedule/delete/edit/manual-retry, không nhận raw email list. Active admin kiểm lúc submit; đợt đã xác nhận không bị silently sửa nội dung.

Số0actual là NO_RECIPIENTS, không SENT. COMPLETED all-skipped vẫn phải ghi success0/skip rõ. UNKNOWN không bị ẩn vàofailed/success; history totals từ delivery chứ không từ inbox còn lại. Đếmunique users tách total lượt nhiều kênh.

## Internal NotificationEventPort

Input nội bộ: `{eventKey,eventType,aggregateRef,occurredAt,authorizedRecipientIds,publicFacts}`. Stable eventKey nhận từ nguồn transition, không random mỗi retry; insert outbox cùng transaction nguồn. Consumer dedup+dispatch atomically. Không REST public cho emit event; không cho producer override urgent/preferences bằng payload.

Ma trận tích hợp tối thiểu dự kiến, chỉ thực hiện tại transition thật đã commit:

| Source event | Người nhận | Trạng thái repo/điểm tích hợp |
| --- | --- | --- |
| Trial booking created / confirmed / cancelled | Tutor nhận yêu cầu mới; learner/tutor participant nhận thay đổi liên quan, loại actor gây thay đổi nếu không cần thông báo | TrialBookingServiceImpl stub: phụ thuộc luồng thật; không phát từ API khung |
| Assignment published / graded | Learner được giao; learner sở hữu bài được chấm | AssignmentServiceImpl stub: phụ thuộc publish/grade transaction và enrollment/recipient checks |
| Membership payment confirmed | Tutor sở hữu membership đã xác nhận | TutorMembershipServiceImpl.processVnPayReturn: sau verify chữ ký+successful transition và trong cùng transaction lưu membership/tutor; không create URL hoặc direct activation dùng test |
| Ticket public response / resolved | Chủ phiếu, không bản nội bộ | SupportTicketServiceImpl stub/FE-10 chưa code: phụ thuộc public message/resolve transaction, không lấy escalation notes |

Đây là danh mục thiết kế nguồn dự kiến, không phải tuyên bố code đã có. Trước implement đối chiếu method/thẩm quyền thật; nguồn thiếu ghi blocked FR-015. Một source event không cần truy cập internal private notes để dựng câu thông báo. Thanh toán callback lặp cần guard transition/event identity; dedup outbox không tự sửa mọi side effect callback hiện tại. Tạo điểm tích hợp nhỏ cùng module, không tự xây payment engine mới.

## Mail adapter and worker boundary

NotificationMailAdapter nhận safe recipient/content template, gọi JavaMailSender cấu hình hiện có và trả ACCEPTED/DEFINITELY_FAILED/UNKNOWN. SMTP network timeout tối đa10giây một attempt trong worker riêng; không block enqueue API. Không dùng diagnostic email template hoặc @Async void OTP làm kết quả send. Mail test double chỉ notification-test, không tự fallback fake success khi production SMTP lỗi.

Worker poll5giây; mỗi lần chỉ claim tối đa min(100, số slot thực thi đang rảnh đã giữ), lease60giây và claim_token riêng từng delivery, không xếp hàng claimed vượt khả năng chạy ngay. Ngay trước phát kiểm token/lease còn hạn và gia hạn60giây có điều kiện; mất lease thì không bắt đầu gửi hoặc ghi đè kết quả. Check account/preference trước delivery; finalize có guard token/lease. EMAIL_SENDING crash/timeout hoặc hết lease→UNKNOWN, không blind resend. Known-unsent transient tối đa4lần gọi tổng cộng (lần đầu +3retry sau30/120/300giây); permanent/hết attempt4→FAILED, UNKNOWN không retry. Durable in-app unique+outcome chung transaction; lịch gửi phục hồi được sau restart.

## UI routes

Mỗi shell sáu vai trò có `/ROLE/notifications`, detail `/{id}`, `/ROLE/notification-preferences` với role prefix thật admin/manager/cs/academic/tutor/learner. Shared fragments inbox/thread/badge/preferences, server scoped; learner/tutor theo DESIGN mục7, staff theo chung.

Admin `/admin/notification-broadcasts`, `/new`, `/{id}` có preview/chọn nhóm/kênh/hẹn giờ/confirm/history; không gửi lúc chỉ preview, hủy xác nhận không tạo dữ liệu. Personal mark-read tự gọi sau loaded detail, nút read tại list vẫn có, delete xác nhận. Keyboard/label/focus tốt, loading/empty/error rõ; plain text tiếng Việt, không raw roles/enums/all-caps. Poll unread count hữu hạn30giây khi mở trang; không claim realtime/WebSocket.
