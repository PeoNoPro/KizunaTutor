# Research: FE-09 — Membership & Payment Management

Mỗi mục: Quyết định · Lý do · Phương án đã cân nhắc.

## 1. Cách nhận kết quả thanh toán — Phương án C (người dùng chốt 2026-11-04)

- **Quyết định**: IPN (`GET /api/payments/vnpay/ipn`) là nguồn chính. Trang kết quả gọi `GET /api/tutor/memberships/payments/vnpay-return?{tham số vnp_*}`; backend kiểm chữ ký, nếu giao dịch còn `PENDING` thì gọi querydr và xử lý kết quả. Thao tác "Kiểm tra với cổng" của quản trị viên dùng lại querydr. Ba đường cùng đi qua một `PaymentResultProcessor` idempotent.
- **Lý do**: Đúng API-EXT-02; vẫn chạy được trên `localhost` (IPN không tới được máy cục bộ); đáp ứng UC-98 bước 5 và UC-104 AF-1.
- **Đã cân nhắc**: Chỉ IPN (phải dựng tunnel mỗi lần thử); chỉ Return URL (mất kết quả khi gia sư đóng trình duyệt, không đúng SRS).

## 2. Tham số VNPay (bản 2.1.0) — cần đối chiếu tài liệu sandbox khi cài đặt

- Tạo thanh toán: `vnp_Version=2.1.0`, `vnp_Command=pay`, `vnp_TmnCode`, `vnp_Amount` = số tiền × 100, `vnp_CurrCode=VND`, `vnp_TxnRef` = `transaction_id` bỏ gạch nối, `vnp_OrderInfo` = "Thanh toan goi hoi vien {txnRef}" (không dấu), `vnp_OrderType=other`, `vnp_Locale=vn`, `vnp_ReturnUrl`, `vnp_IpAddr`, `vnp_CreateDate`, `vnp_ExpireDate` (+15 phút), định dạng `yyyyMMddHHmmss` theo Asia/Ho_Chi_Minh.
- Ký: sắp xếp tham số theo tên tăng dần, mã hóa URL giá trị (US-ASCII), nối `k=v&...`, HMAC-SHA512 bằng hash secret → `vnp_SecureHash` (hex thường).
- Kiểm chữ ký kết quả: bỏ `vnp_SecureHash`, `vnp_SecureHashType`, ký lại như trên, so sánh không phân biệt hoa thường bằng so sánh thời gian hằng.
- Thành công khi `vnp_ResponseCode = 00` **và** `vnp_TransactionStatus = 00`.
- IPN trả JSON `{"RspCode":"..","Message":".."}`: `00` Confirm Success · `01` Order not found · `02` Order already confirmed · `04` Invalid amount · `97` Invalid signature · `99` Unknown error. Đây là định dạng bắt buộc của cổng nên endpoint IPN **không** dùng khung `success/data` của BACKEND_RULES 6.2 (ngoại lệ có lý do).
- querydr: `POST` JSON tới `api-url`, `vnp_Command=querydr`, `vnp_RequestId` ngẫu nhiên, `vnp_TransactionDate` = thời điểm tạo giao dịch (`transaction_time`), chữ ký HMAC-SHA512 trên chuỗi nối bằng `|` theo thứ tự tài liệu VNPay; kiểm chữ ký phản hồi trước khi dùng.
- **Rủi ro**: thứ tự trường chữ ký querydr phải khớp chính xác tài liệu VNPay; task T014 có bước đối chiếu với sandbox.

## 3. Tính idempotent và đồng thời

- **Quyết định**: `PaymentResultProcessor.process(txnRef, gatewayResult)` chạy trong một transaction: khóa dòng `payment_transactions` (`PESSIMISTIC_WRITE`) → nếu không còn `PENDING` thì trả "đã xử lý" (IPN `02`) → kiểm số tiền → cập nhật → khóa dòng `tutors` của gia sư → tính ngày (mục 4) → tạo `tutor_memberships` → cập nhật `tutor_state`, `membership_status`. Lỗi bất kỳ → rollback, giao dịch vẫn `PENDING`, VNPay gửi lại IPN hoặc lượt tra cứu sau sẽ xử lý lại (UC-104 AF-1).
- **Lý do**: Cùng mẫu khóa dòng đã kiểm chứng ở FE-03; thứ tự khóa cố định (giao dịch → gia sư) tránh deadlock; hai giao dịch của cùng gia sư được tuần tự hóa nên cộng nối đúng.
- **Đã cân nhắc**: Ràng buộc UNIQUE trên `gateway_transaction_id` (cần đổi schema, không được phép); khóa lạc quan (không có cột version).

## 4. Tính ngày hiệu lực (cộng nối — người dùng chốt 3a)

- Ngày bắt đầu = `max(today, maxEnd + 1)` với `maxEnd` = ngày hết hạn xa nhất trong các bản ghi `ACTIVE` của gia sư; không có thì `today`. Ngày hết hạn = `start.plusMonths(n).minusDays(1)`. `today` theo Asia/Ho_Chi_Minh (ASM-11).
- Hết hạn khi đọc (chốt 4): mỗi lần đọc thông tin hội viên của một gia sư (gia sư, quản lý), các bản ghi `ACTIVE` có `end_date < today` chuyển `EXPIRED`; nếu không còn bản ghi `ACTIVE` nào có `end_date >= today` thì `tutor_state` `ACTIVE_PAID` → `APPROVED_UNPAID` và `membership_status` → `EXPIRED`. Danh sách của quản lý tính trạng thái hiển thị theo ngày (không ghi hàng loạt khi liệt kê).

## 5. Lưu ghi chú điều tra của quản trị viên

- **Quyết định**: Một dòng `payment_processing_logs` với `request_payload = {"type":"ADMIN_NOTE","note":"...","adminId":"...","adminName":"..."}`, `response_payload = null`, `http_status_code = null`.
- **Lý do**: Không đổi schema; bảng đã là nhật ký theo giao dịch.
- **Đã cân nhắc**: Thêm cột/bảng ghi chú (cần duyệt schema, không cần thiết).
- Các loại nhật ký khác: `CREATE_PAYMENT_URL`, `IPN`, `RETURN`, `QUERYDR`; không bao giờ ghi hash secret; `vnp_SecureHash` được che bớt.

## 6. Định dạng phản hồi và lỗi (người dùng chốt 8: theo BACKEND_RULES)

- **Quyết định**: Thêm `core/dto/ApiResponse<T>` (`success`, `message`, `data`, `timestamp`) — file mới. Lỗi nghiệp vụ dùng `BadRequestException` (400), `ResourceNotFoundException` (404) có sẵn và lớp mới `core/exception/ConflictException` (409) kèm **một phương thức mới** trong `GlobalExceptionHandler`. Không sửa phương thức có sẵn nào.
- **Lý do**: BACKEND_RULES 6.2–6.3 cấm handler riêng của feature; quy định "chỉ thêm, không sửa code người khác".
- **Ghi chú**: FE-03 đang dùng handler riêng và chưa theo khung `success/data`; đồng bộ FE-03 là việc riêng, không làm trong FE-09.

## 7. Cấu hình và bí mật

- **Quyết định**: Thêm mục mới `# 13. FE-09 VNPAY` trong `application.properties` với khóa `app.payment.vnpay.*` đọc từ biến môi trường, mặc định rỗng cho mã website/khóa bí mật (thiếu thì báo MSG37, ứng dụng vẫn khởi động). **Không sửa** khối `vnpay.*` cũ (của thành viên khác); FE-09 không đọc khối đó.
- **Báo người phụ trách khối cũ**: `vnpay.hash-secret` đang có giá trị mặc định ghi trong repo; `vnpay.return-url` sai cổng (8080).
- Đường dẫn trả về mặc định: `http://localhost:3000/tutor/membership/result` (đổi bằng `VNPAY_RETURN_URL`).
- IPN URL khi deploy/thử bằng tunnel: `{domain}/api/payments/vnpay/ipn`, đăng ký trong trang quản trị merchant sandbox của VNPay.

## 8. Gọi querydr

- **Quyết định**: `java.net.http.HttpClient` có sẵn trong JDK, timeout 10 giây, Jackson có sẵn để đọc JSON.
- **Lý do**: Không thêm thư viện (BACKEND_RULES mục 1).

## 9. Entity có sẵn

- `MembershipPlan`, `TutorMembership`, các enum: dùng nguyên trạng. `TutorMembership.paymentStatus` mặc định `"SUCCESS"` trong Java (DB mặc định `PENDING`): FE-09 luôn gán giá trị rõ ràng.
- `PaymentTransaction`, `PaymentProcessingLog`: là khung rỗng của thành viên khác (TonHai2507), map sai tên cột (`transactionId`, `logId`) và kế thừa `BaseEntity` trong khi bảng không có `created_at/updated_at` → dùng sẽ lỗi SQL. **Cần người dùng quyết định (D-1 trong plan)**: hoàn thiện hai file này (khuyến nghị) hay hỏi người tạo.

## 10. Giao diện quản lý và quản trị

- Chưa có trang `/manager/*`, `/admin/*` nào. FE-09 tạo `components/staff-layout/` (file mới, nhận cấu hình vai trò) chỉ chứa mục của FE-09; không tạo dashboard của quản lý/quản trị (không thuộc FE-09).
- Phong cách: theo `FRONTEND_RULES.md` và theme Stitch đã dùng ở cổng học vụ cho nhất quán khu vực nhân viên.

## 11. CSRF

- BACKEND_RULES 7.4 yêu cầu CSRF nhưng dự án chưa có endpoint `/csrf` và `SecurityConfig` đang tắt CSRF; API dùng JWT Bearer không dựa cookie. FE-09 giữ như FE-03, ghi nhận chênh lệch. IPN phải được miễn CSRF trong mọi trường hợp.
