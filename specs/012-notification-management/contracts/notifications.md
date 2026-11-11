# Contracts: FE-12 Notification Management

## Hợp đồng hiện hành — đã triển khai 2026-11-09

Phần này là nguồn đúng cho code hiện tại; các mục thiết kế bên dưới giữ để truy vết. SQL: `database/seeds/fe12_notification.sql` (giai đoạn 1) rồi `fe12_notification_dispatch.sql` (giai đoạn 2). Cả hai không xóa dữ liệu, chạy lại được.

**Cá nhân — `/api/v1/notifications`** (sáu vai trò, tài khoản ACTIVE; nằm trong chuỗi bảo mật JWT cookie + CSRF chung với `/api/v1/tickets`; lệnh ghi lấy mã ở `GET /csrf`)

| Method/path | Hợp đồng |
| --- | --- |
| `GET /me` | `{viewRole: learner/tutor/academic/cs/manager/admin, displayName, unreadCount}` để giao diện chọn đúng khung |
| `GET ?read=all/unread&type&page&size` | `{items:[{id,title,preview,type,typeLabel,receivedAt,isRead,readAt}],page,size,totalElements,totalPages,unreadCount}`; mới nhất trước, size 1–50 (mặc định 20) |
| `GET /unread-count` | `{unreadCount}` |
| `GET /{id}` | Chi tiết có `content`; không tự đánh dấu đọc. Của người khác hoặc đã xóa → 404 |
| `POST /{id}/read` | `{isRead:true,readAt,unreadCount}`; lặp lại không đổi `readAt` |
| `DELETE /{id}` | Xóa mềm `{deleted:true,unreadCount}`; lặp lại đúng chủ vẫn 200 |
| `GET /preferences` / `PUT /preferences` | `{version,groups:[{type,typeLabel,inAppEnabled,emailEnabled}]}`; PUT gửi đủ 5 nhóm + `expectedVersion`, phiên bản cũ → 409. Mặc định bật trong ứng dụng, tắt email |

**Quản trị — `/api/admin/operations/notification-broadcasts`** (Admin và Quản lý còn hoạt động — Quản lý bổ sung 2026-11-09; CSRF lấy ở `GET /notification-broadcasts/csrf`; đặt dưới khu vận hành để dùng chung kiểm quyền thay vì `/api/admin/notification-broadcasts` trong thiết kế)

| Method/path | Hợp đồng |
| --- | --- |
| `POST /preview` | Body `{title(1–150),content(1–10.000),type,targetMode:ROLES/ALL,roles[],channels[IN_APP,EMAIL],urgent,scheduledAt?}`. Không lưu; trả `estimatedRecipients`, `requiresConfirmation` |
| `POST` | Body như trên + `submissionKey`(UUID), `confirmationText`. ALL chỉ khi `urgent=true`, luôn có IN_APP, không hẹn giờ; giao diện hỏi lại bằng hộp xác nhận (bỏ bước gõ "Xác nhận" từ 2026-11-09, không còn trường `confirmationText`). `scheduledAt` phải ở tương lai, tối đa 1 năm. Trả 202 `{broadcastId,status,statusLabel,scheduledAt,recipientCount,message}`. Cùng key + cùng nội dung → trả đợt cũ; khác nội dung → 409 |
| `GET ?fromDate&toDate&status&channel&role&title&page&size` | Lịch sử đợt do Admin gửi, mới nhất trước |
| `GET /{id}` | Nội dung, người tạo, thời điểm tạo/hẹn/chốt, số dự kiến và thực tế, theo từng kênh: `success,pending,failed,skipped,unknown,total` |

**Bản làm gọn đã thống nhất (thay outbox/lease trong thiết kế bên dưới, chạy một máy chủ):**

- Gửi ngay: chốt người nhận và tạo bản trong ứng dụng ngay trong transaction xác nhận. Hẹn giờ: tác vụ nền (5 giây/lần) chốt khi đến hạn theo vai trò và trạng thái tài khoản lúc đó.
- Bảng `notification_deliveries` vừa là tập người nhận đã chốt vừa là kết quả từng người/kênh; `UNIQUE(dispatch,user,channel)` và `UNIQUE(notification,user)` chống nhận trùng khi xử lý lại.
- Kênh trong ứng dụng: bật theo tùy chọn, khẩn thì luôn có. Email: chỉ người đã bật nhóm đó, kể cả khẩn; tắt thì ghi "bỏ qua theo tùy chọn". Kiểm lại tài khoản và tùy chọn ngay trước khi gửi email.
- Email gửi ngoài transaction qua bộ gửi thư FE-11. Chắc chắn chưa gửi (không đăng nhập/kết nối được) → thử lại tối đa 4 lần sau 30 giây/2 phút/5 phút; máy chủ thư từ chối địa chỉ → thất bại; hết thời gian chờ hoặc đang gửi quá 2 phút → "chưa xác định", không gửi lại.
- Trạng thái đợt: Đã lên lịch, Đang chờ, Đang gửi, Hoàn tất, Hoàn tất một phần, Thất bại, Không có người nhận, Cần kiểm tra (theo FR-012).
- Chưa làm trong bản này: `GET /{id}/deliveries` chi tiết từng người, thông báo tự động từ học thử/bài tập/thanh toán/hỗ trợ (FR-015, giai đoạn 3).

## Thiết kế ban đầu (giữ để truy vết)

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

### Giao diện chuông thông báo — bổ sung 2026-11-09

Mọi khung (khu quản trị, CS, học vụ, gia sư, học viên, header công khai) có nút chuông `data-notification-bell` dùng chung `js/pages/notification-badge.js`: hiện số chưa đọc (cập nhật 30 giây/lần khi tab đang mở), bấm mở khung xem nhanh 6 thông báo mới nhất; bấm một dòng mở trang chi tiết (trang đó tự đánh dấu đã đọc), có "Xem tất cả", "Tùy chọn" và với Admin/Quản lý thêm "Gửi thông báo". Gửi thông báo là tab "Gửi thông báo" trong trang Thông báo (chỉ Admin/Quản lý thấy), không còn mục riêng trên sidebar; sidebar CS có mục "Thông báo". Bấm một thông báo mở hộp chi tiết ngay trên trang.

Cập nhật 2026-11-10 — vị trí "Thông báo" trên sidebar thống nhất: mục "Thông báo" (có số chưa đọc) nằm **cuối sidebar, ngay trên thẻ tài khoản**, không lặp trong menu chính. CS: bỏ mục trùng trong menu (trước đây hiện hai lần). Gia sư: chuyển từ menu chính xuống cuối, bỏ nút chuông cạnh logo trên máy tính (điện thoại vẫn có chuông ở thanh trên). Học viên và Cán bộ học vụ chưa đổi.

### Thông báo tự động (giai đoạn 3) — bổ sung 2026-11-09

SQL `database/seeds/fe12_notification.sql (phần 3)`: thêm `action_path` (đường dẫn nội bộ `^/[^/]`) cho `notification_dispatches` và `notifications`. Inbox trả thêm `actionPath`; bấm thông báo có `actionPath` ở chuông thì đánh dấu đã đọc rồi mở trang đó, hộp chi tiết có nút "Mở trang liên quan".

`NotificationEvents.publish(eventKey,type,title,content,actionPath,recipientIds)` (Java, không có REST): tạo đợt `origin=EVENT`, `target_mode=EXPLICIT`, kênh IN_APP+EMAIL theo tùy chọn người nhận, chốt người nhận ngay. Ghi trong savepoint JDBC của transaction nghiệp vụ: thành công thì lưu cùng nghiệp vụ, lỗi chỉ ghi log và không làm hỏng nghiệp vụ; không có transaction thì tự mở transaction riêng. **Sửa 2026-11-10:** bản đầu dùng `Propagation.NESTED`, nhưng JpaTransactionManager + HibernateJpaDialect không hỗ trợ savepoint nên mọi lời gọi đều lỗi và bị nuốt (DB không có đợt `origin=EVENT` nào); nay `Writer` tự đặt/rollback savepoint trên kết nối của transaction hiện tại. `event_key` unique: xử lý lại cùng sự kiện không tạo thêm. Lịch sử gửi của Admin chỉ hiện đợt `origin=ADMIN`.

Đã gắn: FE-10 (trả lời, bổ sung, giải quyết, giao/tự giao, chuyển lên, đổi Quản lý, trả kết quả; 2026-11-10 thêm "Yêu cầu hỗ trợ cần bàn giao" → mọi Quản lý khi CS phụ trách không còn hoạt động) và FE-11 (Quản lý báo sự cố → mọi Admin; nhận/khắc phục/đóng/trả lại/xử lý tiếp → người báo). Học thử, bài tập, thanh toán: xem `integration-guide.md`, các module đó tự thêm lời gọi.

## Bổ sung 2026-11-11 (review PR #21)

- `POST /api/admin/operations/notification-broadcasts/{id}/cancel`: hủy đợt `SCHEDULED` chưa chốt người nhận. Người tạo hoặc Quản trị viên; khác người tạo (Quản lý) 403; đã gửi/đã hủy hoặc vừa đến giờ gửi 409. Trạng thái mới `CANCELLED` ("Đã hủy"); lịch sử và chi tiết trả thêm `canCancel`.
- Tiêu đề thông báo chỉ một dòng: có ký tự điều khiển (xuống dòng, tab) thì 400. Thông báo tự động đổi ký tự điều khiển trong tiêu đề thành dấu cách.
- `actionPath` chỉ nhận `^/([A-Za-z0-9_.~%=&?#+-][A-Za-z0-9/_.~%=&?#+-]*)?$`, tối đa 300 ký tự (chặn `//…`, `/\…`, xuống dòng). Giao diện kiểm lại cùng biểu thức trước khi mở.
- Số người "dự kiến" và lúc chốt dùng chung một điều kiện tài khoản/vai trò.
- Email: mỗi lượt giữ chỗ 5 thư, đánh dấu lại thời điểm ngay trước khi gửi từng thư.
- Nhật ký thao tác (FE-11) có nhóm "Thông báo": gửi, hẹn giờ, gửi khẩn toàn hệ thống, hủy hẹn giờ.
- SQL: `database/seeds/fe11_fe12_review_fixes.sql`.
