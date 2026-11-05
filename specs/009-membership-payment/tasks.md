# Tasks: FE-09 — Membership & Payment Management

**Input**: [plan.md](plan.md), [spec.md](spec.md), [data-model.md](data-model.md), [contracts/membership-api.md](contracts/membership-api.md), [research.md](research.md)

Quy ước: `[P]` chạy song song được (file khác nhau, không phụ thuộc); `[USn]` user story. Đường dẫn backend tính từ `backend/src/main/java/com/kizuna/`, test từ `backend/src/test/java/com/kizuna/`. **Chưa task nào được bắt đầu — chờ người dùng phê duyệt.**

## Phase 1: Setup

- [X] T001 Thêm mục `# 13. FE-09 VNPAY` vào cuối `backend/src/main/resources/application.properties`: `app.payment.vnpay.tmn-code=${VNPAY_TMN_CODE:}`, `hash-secret=${VNPAY_HASH_SECRET:}`, `pay-url`, `api-url` (mặc định sandbox), `return-url=${VNPAY_RETURN_URL:http://localhost:3000/tutor/membership/result}`, `expire-minutes=15`, `query-timeout-seconds=10`. Không sửa khối `vnpay.*` có sẵn.
- [X] T002 [P] Tạo `features/payment/config/VnpayProperties.java` (`@ConfigurationProperties`, `isConfigured()`).

## Phase 2: Foundational (chặn mọi user story)

- [X] T003 [P] Tạo `core/dto/ApiResponse.java` (`success`, `message`, `data`, `timestamp` +07:00; `ok(...)`, `created(...)`).
- [X] T004 [P] Tạo `core/exception/ConflictException.java`; **thêm** phương thức `handleConflictException` (409, cùng định dạng các handler có sẵn) vào `core/exception/GlobalExceptionHandler.java` — không đổi phương thức cũ.
- [X] T005 (D-1 đã duyệt) Hoàn thiện `features/payment/entity/PaymentTransaction.java` và `PaymentProcessingLog.java` theo data-model §2, §4 (bỏ `extends BaseEntity`, map đúng cột, `@JdbcType(PostgreSQLEnumJdbcType)`, JSONB qua `@JdbcTypeCode(SqlTypes.JSON)`).
- [X] T006 [P] Tạo repository trong `features/payment/repository/`: `MembershipPlanRepository`, `PaymentTransactionRepository` (`findLockedById` `PESSIMISTIC_WRITE`), `TutorMembershipRepository`, `PaymentProcessingLogRepository`, `MembershipTutorRepository` (khóa dòng `tutors` theo `user_id`).
- [X] T007 [P] Tạo `features/payment/service/MembershipLabels.java` (nhãn tiếng Việt cho trạng thái giao dịch, hội viên, gói, `tutor_state`, loại nhật ký) và `CurrentTutorProvider.java` (lấy người dùng từ `SecurityContext`).
- [X] T008 **Thêm** vào `core/security/SecurityConfig.java`, trước `anyRequest()`: `/api/tutor/memberships/**` → `ROLE_TUTOR`; `/api/manager/membership-plans/**`, `/api/manager/tutor-memberships/**` → `ROLE_MANAGER`; `/api/admin/payment-transactions/**` → `ROLE_ADMIN`; `/api/payments/vnpay/ipn` → `permitAll`.
- [X] T009 [P] Test `features/payment/PaymentSecurityTest.java` (`@WebMvcTest`): 401 không token, 403 sai vai trò, IPN không cần token.

## Phase 3: US1 — Mua gói qua VNPay (P1) 🎯 MVP

- [X] T010 [P] [US1] Test `VnpaySignerTest`: ký và kiểm chữ ký với bộ tham số cố định, bỏ qua `vnp_SecureHash`/`vnp_SecureHashType`, mã hóa khoảng trắng/ký tự đặc biệt, so sánh không phân biệt hoa thường.
- [X] T011 [P] [US1] Test `MembershipPeriodCalculatorTest`: không có gói; cộng nối; gói cũ đã hết hạn; 31/01 + 1 tháng; 29/02 năm nhuận; múi giờ Asia/Ho_Chi_Minh.
- [X] T012 [P] [US1] Tạo `service/VnpaySigner.java` (HMAC-SHA512, sắp xếp tham số, URL-encode US-ASCII, so sánh thời gian hằng).
- [X] T013 [P] [US1] Tạo `service/MembershipPeriodCalculator.java` (research §4).
- [ ] T014 [US1] Tạo `service/VnpayGatewayClient.java`: `buildPaymentUrl(transaction, ip)` và `queryTransaction(transaction, ip)` (querydr qua `HttpClient`, kiểm chữ ký phản hồi). **Đối chiếu thứ tự trường chữ ký querydr với tài liệu sandbox VNPay trước khi đánh dấu xong.**
- [X] T015 [P] [US1] Tạo `service/PaymentLogWriter.java` (`REQUIRES_NEW`, che chữ ký, không ghi secret) và `service/PaymentNotificationService.java` (gửi email sau commit trên executor riêng; lỗi chỉ ghi log).
- [X] T016 [US1] Test `PaymentResultProcessorTest`: thành công tạo đúng 1 hội viên + `ACTIVE_PAID` + `PREMIUM`; gọi lặp → "đã xử lý"; số tiền sai → không đổi; thất bại → `FAILED`; lỗi khi tạo hội viên → ném lỗi (rollback), giao dịch vẫn `PENDING`; lỗi email không ảnh hưởng; tài khoản bị khóa sau khi thanh toán vẫn được kích hoạt (spec Edge Cases).
- [X] T017 [US1] Tạo `service/PaymentResultProcessor.java` (research §3; kết quả `PROCESSED`, `ALREADY_PROCESSED`, `AMOUNT_MISMATCH`, `NOT_FOUND`).
- [X] T018 [US1] Tạo `service/TutorMembershipService.java` + `impl/TutorMembershipServiceImpl.java`: `listPlans`, `createPayment` (FR-002/003, MSG34/35/37), `handleReturn` (FR-005), `listMyPayments`.
- [X] T019 [P] [US1] Tạo DTO trong `dto/request` (`CreatePaymentRequest`) và `dto/response` (`PlanView`, `PaymentCreatedView`, `PaymentResultView`, `PaymentView`, `PageView`).
- [X] T020 [US1] Test `TutorMembershipServiceTest` (điều kiện mua, giá chụp lúc tạo, gói ngừng bán, cấu hình thiếu, return chữ ký sai, return của gia sư khác → 404, return còn PENDING → gọi querydr).
- [X] T021 [US1] Tạo `controller/TutorMembershipController.java` (A1, A3, A4, A5) và `controller/VnpayIpnController.java` (B1, ánh xạ kết quả → `RspCode`).
- [X] T022 [P] [US1] Test `TutorMembershipControllerTest`, `VnpayIpnControllerTest` (đủ 6 mã RspCode; khung `success/data` cho A*).
- [X] T023 [P] [US1] Thêm API FE-09 vào `frontend/js/config/api.config.js` và nhóm `API.tutorMembership` vào `frontend/js/api.js` (chỉ thêm).
- [X] T024 [US1] Tạo `frontend/pages/tutor-membership/` (`index.html`, `tutor-membership.js`): thẻ gói, xác nhận, chuyển sang `paymentUrl`; trạng thái tải/rỗng/lỗi.
- [X] T025 [US1] Tạo `frontend/pages/tutor-membership-result/`: đọc query string, gọi A5, hiện MSG31/32/33, nút "Thanh toán lại", "Xem gói của tôi".
- [X] T026 [US1] Thêm mục "Gói hội viên" (`/tutor/membership`) vào `frontend/components/tutor-layout/tutor-layout.js`; thêm forward `/tutor/membership`, `/tutor/membership/result` vào `core/config/WebMvcConfig.java` và alias vào `frontend/server.js` (chỉ thêm).

## Phase 4: US2 — Xem gói và ngày hết hạn (P1)

- [X] T027 [P] [US2] Test `MembershipExpiryServiceTest`: chuyển `EXPIRED`, trả `tutor_state` về `APPROVED_UNPAID` khi hết gói, không đổi khi còn gói nối tiếp.
- [X] T028 [US2] Tạo `service/MembershipExpiryService.java` (khóa dòng tutor, cập nhật khi đọc).
- [X] T029 [US2] Thêm `getMyMembership` (A2) vào `TutorMembershipService` + endpoint `GET /me`; DTO `MyMembershipView`.
- [X] T030 [US2] Bổ sung khối "Gói hiện tại / Đã gia hạn / Lịch sử giao dịch" vào `pages/tutor-membership/`; rỗng "Bạn chưa có gói hội viên"; lỗi + "Thử lại".

## Phase 5: US3 — Cấu hình gói, giá, quyền lợi (P2)

- [X] T031 [P] [US3] Test `MembershipPlanAdminServiceTest` (tạo, trùng tên → 409, giá ngoài khoảng, quyền lợi > 20, không đổi số tháng, bật/tắt bán).
- [X] T032 [US3] Tạo `service/MembershipPlanAdminService.java` + impl và `controller/ManagerMembershipPlanController.java` (C1–C4); DTO `PlanAdminView`, `CreatePlanRequest`, `UpdatePlanRequest`.
- [X] T033 [P] [US3] Test `ManagerMembershipPlanControllerTest`.
- [X] T034 [US3] Tạo `frontend/components/staff-layout/staff-layout.js` (sidebar theo vai trò, logo → `/home`, menu tài khoản dùng `user-menu`).
- [X] T035 [US3] Tạo `frontend/pages/manager-membership-plans/` (bảng gói, form tạo/sửa, bật/tắt bán, MSG04/36/02); forward `/manager/membership-plans` trong `WebMvcConfig`, alias `server.js`; thêm `API.managerMembership`.

## Phase 6: US4 — Danh sách và chi tiết hội viên (P2)

- [X] T036 [P] [US4] Test `TutorMembershipMonitorServiceTest` (lọc, phân trang ≤ 50, trạng thái theo ngày, chi tiết khi lịch sử thanh toán lỗi → `paymentsUnavailable`).
- [X] T037 [US4] Tạo `repository/MembershipQuerySpecification.java`, `service/TutorMembershipMonitorService.java` + impl, `controller/ManagerTutorMembershipController.java` (C5, C6).
- [X] T038 [P] [US4] Test `ManagerTutorMembershipControllerTest`.
- [X] T039 [US4] Tạo `frontend/pages/manager-tutor-memberships/` (bảng, bộ lọc, MSG01, ngăn kéo chi tiết); forward + alias.

## Phase 7: US5 — Xử lý sự cố thanh toán (P3)

- [X] T040 [P] [US5] Test `PaymentIssueServiceTest` (lọc; gateway-check PENDING → xử lý, SUCCESS/FAILED → chỉ ghi log; cổng lỗi → 503; ghi chú 1–2.000 ký tự).
- [X] T041 [US5] Tạo `service/PaymentIssueService.java` + impl, `controller/AdminPaymentTransactionController.java` (D1–D4).
- [X] T042 [P] [US5] Test `AdminPaymentTransactionControllerTest`.
- [X] T043 [US5] Tạo `frontend/pages/admin-payments/` (bảng, bộ lọc, ngăn kéo chi tiết + nhật ký + ghi chú, nút "Kiểm tra với cổng thanh toán"); forward + alias; `API.adminPayments`.

## Phase 8: US6 — Đăng bài và duyệt bài (P3) — BỊ CHẶN

- [ ] T044 [US6] **Chặn một phần**: bảng `tutor_posts` đã tạo 2026-11-04; còn chờ UC mới (srs-gaps G-01). Khi có UC: chạy lại `$speckit-plan` cho phần này.

## Phase 9: Polish

- [X] T045 Chạy `.\mvnw.cmd test-compile` và `.\mvnw.cmd test -Dtest="*Membership*,*Payment*,*Vnpay*"`; chạy toàn bộ `.\mvnw.cmd test` để chắc không làm hỏng test có sẵn; ghi kết quả.
- [ ] T046 Rà soát giao diện 6 trang (1240px, 375px): tiếng Việt, không in hoa toàn bộ, không mã nội bộ, số tiền `199.000 đ`, ngày `dd/MM/yyyy`.
- [ ] T047 Chạy quickstart với VNPay sandbox (thẻ thử); ghi thời gian phản hồi quan sát trong DevTools cho SC-005 (không phải đo tải); ghi kết quả từng kịch bản vào mục "Kết quả kiểm chứng" bên dưới.
- [ ] T048 Báo cáo: file đã tạo/sửa, file dùng chung đã thêm, vấn đề báo người khác (khối `vnpay.*` cũ, FE-01 gán PREMIUM).

## Dependencies

- Phase 1 → Phase 2 → các user story. T005 chặn mọi task có repository.
- US1 trước US2 (dùng chung trang và service). US3, US4, US5 độc lập với nhau sau Phase 2; US5 dùng `PaymentResultProcessor` (T017).
- T034 (`staff-layout`) trước T039, T043.

## Parallel Example (US1)

```text
T010, T011 song song → T012, T013 song song → T014 → T016 → T017 → T018/T020 → T021 → T022, T023 song song → T024, T025, T026
```

## Implementation Strategy

MVP = Phase 1–4 (US1 + US2): gia sư mua và xem gói. Sau đó US3 → US4 → US5. US6 khi được mở khóa.

## Kết quả kiểm chứng

_Cập nhật 2026-11-04._

**Test tự động** (JDK 17, `.\mvnw.cmd test`): **122/122 đạt**, gồm 44 test FE-09 mới — `VnpaySignerTest` (5), `MembershipPeriodCalculatorTest` (6), `PaymentResultProcessorTest` (10), `MembershipServicesTest` (15), `PaymentControllersTest` (8). Không có test DB (không có DB kiểm thử riêng). JDK 25 mặc định trên máy không biên dịch được dự án (Lombok); phải đặt `JAVA_HOME` = JDK 17.

**Chạy thật trên Supabase — chỉ API đọc, không ghi dữ liệu:**

| Kiểm tra | Kết quả |
| --- | --- |
| A1 gói đang mở bán | 3 gói, giá và quyền lợi đọc đúng từ JSONB |
| A2 gói của `tutor@kizuna.vn` | "Đang hiệu lực", 27/10/2026 – 25/01/2027, còn 83 ngày |
| A3 khi chưa đặt biến môi trường VNPay | 503 MSG37, không tạo giao dịch |
| C1, C5, C6 quản lý vận hành | Đúng dữ liệu; bộ lọc trạng thái sai → 400 tiếng Việt |
| D1 quản trị viên | Danh sách rỗng (chưa có giao dịch) |
| Phân quyền chéo vai trò | 403 cả 3 trường hợp |
| B1 IPN chữ ký sai | `{"RspCode":"97"}` |
| 5 trang mới qua cổng 8085 | 200 |

**Chưa kiểm chứng (cần người dùng):**

- T014/T047: luồng thanh toán thật với VNPay sandbox (cần đặt `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`), gồm thứ tự trường chữ ký querydr và IPN qua tunnel.
- Ghi gói thật (C2–C4) trên Supabase: chưa chạy để không đổi dữ liệu dùng chung; câu lệnh `CAST(... AS jsonb)` mới được kiểm tra bằng mock.
- T046: rà soát giao diện bằng mắt ở 1240px và 375px.
