# API Contract: FE-09 — Membership & Payment

Khung phản hồi theo BACKEND_RULES 6.2/6.3 (trừ IPN): thành công `{ "success": true, "message": "...", "data": {...}, "timestamp": "..." }`; lỗi `{ "success": false, "message": "...", "fieldErrors": [...]?, "timestamp": "..." }`. Mọi `message` tiếng Việt. Ngày `yyyy-MM-dd`, thời điểm ISO-8601 +07:00, tiền là số nguyên VND. Mọi trường mã (`status`) đi kèm `statusLabel` tiếng Việt.

Mã HTTP: 200, 201, 400 (dữ liệu sai, `fieldErrors`), 401, 403, 404, 409 (sai trạng thái nghiệp vụ), 503 (cổng chưa cấu hình/không phản hồi — MSG37).

## A. Gia sư — `ROLE_TUTOR`, tiền tố `/api/tutor/memberships`

### A1. `GET /plans`
`data`: `[{ planId, planName, durationMonths, price, benefits: [string] }]` — chỉ gói `is_active`, sắp xếp `durationMonths` tăng.

### A2. `GET /me`
Cập nhật hết hạn khi đọc (research §4) rồi trả:
```json
{
  "eligible": true, "ineligibleReason": null,
  "tutorState": "ACTIVE_PAID", "tutorStateLabel": "Đang hoạt động",
  "current": { "membershipId": "...", "planName": "...", "startDate": "2026-11-04", "endDate": "2027-02-03", "status": "ACTIVE", "statusLabel": "Đang hiệu lực", "daysLeft": 91 },
  "upcoming": [{ "membershipId": "...", "planName": "...", "startDate": "...", "endDate": "...", "statusLabel": "Đã gia hạn, bắt đầu từ 04/02/2027" }],
  "expiresOn": "2027-05-03",
  "history": [{ "membershipId": "...", "planName": "...", "startDate": "...", "endDate": "...", "status": "EXPIRED", "statusLabel": "Đã hết hạn", "amount": 199000 }]
}
```
`eligible=false` → `ineligibleReason` = MSG34. Chưa từng mua: `current=null`, `history=[]`.

### A3. `POST /payments` — 201
Body `{ "planId": "uuid" }`. `data`: `{ transactionId, amount, planName, paymentUrl, expiresAt }`.
Lỗi: 400 thiếu `planId`; 409 MSG34 khi hồ sơ chưa phê duyệt (tài khoản bị khóa đã bị chặn ở bộ lọc JWT: 401/403); 404 gói không tồn tại; 409 MSG35 gói ngừng bán; 503 MSG37.

### A4. `GET /payments?page=0&size=20`
`data`: `{ items: [{ transactionId, planName, amount, status, statusLabel, transactionTime, resumable }], page, size, totalItems, totalPages }` — chỉ giao dịch của chính gia sư. `resumable` = còn `PENDING` và chưa quá 15 phút (dùng A7).

### A5. `GET /payments/vnpay-return?{các tham số vnp_* VNPay gửi về}`
Kiểm chữ ký; `vnp_TxnRef` phải thuộc gia sư đang đăng nhập (không thì 404). Nếu còn `PENDING`: querydr → xử lý; querydr lỗi hoặc không xác thực được thì dùng tham số trả về đã kiểm chữ ký (plan, Ghi chú triển khai).
`data`: `{ transactionId, status, statusLabel, resultMessage, planName, amount, membership: { startDate, endDate } | null }`. `resultMessage` ∈ MSG31/MSG32/MSG33. Chữ ký sai → 400 "Kết quả thanh toán không hợp lệ." (không đổi dữ liệu).

### A6. `POST /payments/{transactionId}/check` *(thêm 2026-11-04 theo phản hồi nghiệm thu)*
Gia sư tự kiểm tra một giao dịch **của chính mình** còn `PENDING` với cổng (querydr) — dùng khi đã trả tiền nhưng không quay về được trang kết quả (chạy cục bộ không có IPN). Kết quả xử lý qua cùng `PaymentResultProcessor`.
`data`: như A5. Giao dịch không còn chờ → trả trạng thái hiện tại, không gọi cổng. Không phải của mình → 404. Cổng lỗi → 503 MSG37.

### A7. `POST /payments/{transactionId}/resume` *(thêm 2026-11-04)*
Mở lại trang thanh toán cho giao dịch `PENDING` của chính gia sư **còn trong hạn 15 phút** (cùng mã đơn, cùng thời hạn — không tạo giao dịch mới).
`data`: `{ transactionId, amount, planName, paymentUrl, expiresAt }`. Đã quá hạn hoặc không còn chờ → 409 "Giao dịch đã hết hạn thanh toán. Vui lòng chọn gói để tạo giao dịch mới.". Không phải của mình → 404.

## B. IPN — công khai, xác thực bằng chữ ký

### B1. `GET /api/payments/vnpay/ipn?{vnp_*}`
Luôn HTTP 200, body đúng định dạng VNPay (không dùng khung `success/data`):

| Điều kiện (kiểm theo thứ tự) | Body |
| --- | --- |
| Chữ ký sai | `{"RspCode":"97","Message":"Invalid signature"}` |
| Không tìm thấy `vnp_TxnRef` | `{"RspCode":"01","Message":"Order not found"}` |
| `vnp_Amount` ≠ amount × 100 | `{"RspCode":"04","Message":"Invalid amount"}` |
| Giao dịch không còn `PENDING` | `{"RspCode":"02","Message":"Order already confirmed"}` |
| Xử lý xong (thành công hoặc thất bại) | `{"RspCode":"00","Message":"Confirm Success"}` |
| Lỗi khác (rollback) | `{"RspCode":"99","Message":"Unknown error"}` |

## C. Quản lý vận hành — `ROLE_MANAGER`

### C1. `GET /api/manager/membership-plans`
`data`: `[{ planId, planName, durationMonths, price, benefits, active, activeLabel, createdAt }]`.

### C2. `POST /api/manager/membership-plans` — 201
Body `{ planName, durationMonths, price, benefits: [string] }` (ràng buộc data-model §1). Trùng tên → 409.

### C3. `PUT /api/manager/membership-plans/{planId}`
Body `{ planName, price, benefits }` (không có `durationMonths`). Message MSG04.

### C4. `POST /api/manager/membership-plans/{planId}/activate` · `POST .../deactivate`
Trả gói sau cập nhật. Đã ở trạng thái đích → 200 không đổi.

### C5. `GET /api/manager/tutor-memberships?keyword=&planId=&status=ACTIVE|EXPIRED&page=0&size=20`
`size` tối đa 50. `data`: `{ items: [{ tutorId, tutorName, email, planName, registeredAt, status, statusLabel, expiresOn }], page, size, totalItems, totalPages }` — mỗi gia sư một dòng theo bản ghi hội viên mới nhất; trạng thái tính theo ngày hiện tại.

### C6. `GET /api/manager/tutor-memberships/{tutorId}`
`data`: `{ tutor: { tutorId, fullName, email, phone, tutorStateLabel }, expiresOn, memberships: [{ membershipId, planName, registeredAt, startDate, endDate, status, statusLabel, amount }], payments: [{ transactionId, planName, amount, status, statusLabel, transactionTime, gatewayTransactionId }] | null, paymentsUnavailable: false }`.

## D. Quản trị viên — `ROLE_ADMIN`, tiền tố `/api/admin/payment-transactions`

### D1. `GET /?status=&from=yyyy-MM-dd&to=yyyy-MM-dd&keyword=&page=0&size=20`
`keyword` khớp mã giao dịch (UUID hoặc 32 ký tự hex) hoặc email/tên gia sư. `data`: trang `{ transactionId, txnRef, tutorName, email, planName, amount, status, statusLabel, stale, transactionTime, gatewayTransactionId }`.

### D2. `GET /{transactionId}`
`data`: giao dịch + `membership` (nếu có) + `logs: [{ type, typeLabel, loggedAt, httpStatusCode, summary }]` + `notes: [{ note, adminName, createdAt }]`. `summary` không chứa chữ ký hay khóa.

### D3. `POST /{transactionId}/gateway-check`
Gọi querydr, ghi log. `PENDING` → xử lý như IPN. `data`: `{ statusBefore, statusAfter, statusAfterLabel, gatewayResponseCode, gatewayMessage }`. Cổng lỗi → 503.

### D4. `POST /{transactionId}/notes` — 201
Body `{ "note": "1–2.000 ký tự" }`.

## E. Bài đăng gia sư (US6) — NHÁP, chưa cài đặt cho tới khi có UC và bảng `tutor_posts`

- `POST /api/tutor/posts`, `PUT /api/tutor/posts/{postId}`, `GET /api/tutor/posts` (ROLE_TUTOR, cần gói hiệu lực)
- `GET /api/academic/tutor-posts?status=PENDING_REVIEW`, `POST /api/academic/tutor-posts/{postId}/approve`, `POST .../reject` `{ reason }` (ROLE_ACADEMIC_OFFICER)
