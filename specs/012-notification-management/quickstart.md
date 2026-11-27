# Quickstart Validation: FE-12

Hướng dẫn sau implementation; các endpoint/test chưa tồn tại ở bước plan. Không gửi email thật hoặc chạy migration trong lượt này.

## Prerequisites

- Java17/Maven Wrapper, PostgreSQL16 test DB cách ly, migrations FE-12 tăng dần có README/preflight/backup.
- Profile dự kiến notification-test nhận NOTIFICATION_TEST_DB_URL/USERNAME/PASSWORD từ env, fail khi thiếu hoặc DB không đánh dấu test. Không credential trong lệnh/file chia sẻ; không trỏ DB vận hành.
- Hai account mỗi vai trò cần thiết, đủ sáu vai trò, active/locked/deleted, preferences khác nhau. Fake clock và SMTP test adapter hỗ trợ accepted/permanent failure/transient known-unsent/timeout ambiguity, chỉ dùng test.
- Nguồn thật cho học thử/assignment/support đang stub phải ghi blocker; event fixture kiểm adapter không thay E2E. Membership callback cần chữ ký và successful transition hợp lệ trong sandbox; không thu tiền thật hoặc dùng direct activation làm proof payment.

## Commands after code exists

PowerShell tại repo:

```powershell
java -version
.\mvnw.cmd -version
docker compose up -d kizuna-postgres
```

Compose mặc định không tự tạo test DB cách ly. Tạo test DB riêng, chạy SQL theo README migration thực tế khi implement, psql ON_ERROR_STOP; không dùng DatabasePgAdmin4.txt có DROP CASCADE để nâng cấp. Không sửa migrations FE-10/11 đã dùng.

Sau khi test code/profile được tạo và env thiết lập:

```powershell
.\mvnw.cmd "-Dspring.profiles.active=notification-test" "-Dtest=Notification*Test" test
.\mvnw.cmd "-Dspring.profiles.active=notification-test" test
```

App dev dùng config hiện có, bảo đảm không gửi broadcast tới người thật khi thử:

```powershell
.\mvnw.cmd spring-boot:run
```

## Independent scenarios

1. **Inbox/read/delete**: Tạo nguồn thật/test fixture cho hai users. UserA chỉ thấy mình, list20/max50/filter ổn định. GET detail không mutate; UI sau load thành công POST read, timestamp đầu tiên không đổi khi lặp, unread count đúng. Detail load lỗi không mark; read-save lỗi không giảm count giả. DELETE owner lặp không âm count, không xóa UserB/source/admin history; detail deleted404. Hai thiết bị cùng read/delete không mất consistency.
2. **Preferences**: Absent:true/false cho cả năm nhóm. PUT full matrix/version, reload giữ; stale409; lỗi save rollback toàn bộ. Tắt một kênh rồi gửi thường, record SKIPPED, không lỗi. Email tắt không nhận thư kể cả urgent; urgent in-app vẫn tạo dù in-app tắt, admin phải chọn IN_APP. OTP/security email không bị FE-12 preference chặn.
3. **Group broadcast**: Admin preview đúng nhóm nhưng không queue; cancel không tạo dispatch. Confirm title/content/channel/roles hợp lệ tạo queued; double same submissionKey chỉ mộtID, khác body409. Test mọi role không-admin bị cấm admin endpoints. Empty group0→NO_RECIPIENTS.
4. **Schedule/frozen cohort**: Clock UTC+7, scheduled future; trước hạn không phát. Thêm người vào nhóm/rời nhóm trước đến hạn, freeze lúc worker bắt đầu đúng active cohort; kết quả thực khác preview hợp lệ. Restart sau freeze không thêm người mới; recheck inactive và preferences trước send. Bắt đầu≤60giây khi worker/source khả dụng.
5. **Urgent all**: Sáu vai trò active đều trongcohort, locked/deleted không thuộc. Gõ sai/hủy “Xác nhận” không gửi. Queue khẩn luôn in-app, email chỉ selected+opt-in. Chứng minh account/role restrictions không bị bypass vì urgent.
6. **Delivery outcomes**: SMTP accepted không đồng nghĩa delivered/read; timeout sau network start→UNKNOWN/NEEDS_REVIEW, không resend uncertain. Claim không vượt số slot rảnh đã giữ; lease60s/token riêng, renewal60s ngay trước phát; ép mất lease và kiểm worker cũ không bắt đầu gửi/không finalize. EMAIL_SENDING hết lease không được gửi lại. Known-unsent transient gọi tối đa4lần gồm lần đầu và3retry sau30/120/300s; kiểm số lần thực gọi transport và permanent/hết attempt4→FAILED. Một emailfail/in-appsuccess→PARTIAL; allskip→COMPLETED vớisuccess0, allfailed→FAILED, cònpending không complete. Crash tại CLAIMED/EMAIL_SENDING/IN_APP commit và restart không trùng in-app hoặc mất history; không gọi @Async void thành sent.
7. **Events thật**: Thực hiện tối thiểu các transition trong ma trận contracts tại bốn module; chỉ khi nghiệp vụ commit mới outbox, rollback không notification. Repeat source event/valid callback không trùng; createPaymentUrl/PENDING hoặc invalid signature không payment success. Internal support escalation notes không xuất hiện trong inbox/email. Module còn stub ghi chưa đạt FR-015, không dùng synthetic emission để tick.
8. **Security/UI**: Current role/account revoke dù JWT còn hạn, owner isolation, CSRF POST/PUT/DELETE; không allow public `/api/user/**` phát thông báo. Body escaped/script không chạy, actionPath server allowlist và destination recheck access. Six shells menu/badge/preferences hoạt động, learner/tutor DESIGN mục7, Việt đúng dấu, không raw backend/all-caps; empty khác error.

## Performance and acceptance evidence

- Dataset10.000personal notifications và100users test, chạy10phút; ghi p95browser list/detail≤2s và enqueue≤2s, error response≤5s tính từ request. Dùng browser instrumentation đo tới render, HTTP load metrics riêng; enqueue không chờ SMTP.
- Gửi100recipients test với fault ở một phần; đối chiếu actual users riêng tổng lượt channels và counters pending/success/failed/skipped/unknown, kể cả sau xóa inbox. Kiểm EXPLAIN trước thêm index/migration mới.
-10người thử tìm/unread/read/delete trong2phút, đạt≥9người không cần trợ giúp. Thiếu người hoặc môi trường thì ghi chưa chạy.
- Ghi validation.md theo FR-001–016/SC-001–007: môi trường/schema, lệnh, test report, clock/mail outcomes, source blockers và giới hạn. Chạy test fixture không là bằng chứng production delivery; mailbox thật chỉ thử sau có người nhận được cho phép ở bước triển khai riêng.

Tham chiếu [data-model.md](data-model.md), [contracts/notifications.md](contracts/notifications.md), [research.md](research.md).
