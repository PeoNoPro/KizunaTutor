# Implementation Plan: FE-09 — Membership & Payment Management

**Branch**: `han` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

## Summary

Gia sư đã phê duyệt mua gói hội viên qua VNPay sandbox (phương án C: IPN là nguồn chính, tra cứu cổng khi quay về trang kết quả và khi quản trị viên kiểm tra). Kích hoạt idempotent, cộng nối thời hạn, hết hạn cập nhật khi đọc. Quản lý vận hành cấu hình gói và giám sát hội viên; quản trị viên xử lý sự cố thanh toán. Đăng bài (US6) chờ UC và schema.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.3.4; HTML/ES module/Tailwind CDN
**Primary Dependencies**: có sẵn — Spring Web, Security (JWT filter FE-03), Data JPA, Mail, Jackson, `java.net.http.HttpClient`, `javax.crypto.Mac` (HMAC-SHA512). Không thêm thư viện.
**Storage**: PostgreSQL Supabase (`ddl-auto=none`): `membership_plans`, `payment_transactions`, `tutor_memberships`, `payment_processing_logs`, `tutors`, `users`. Không đổi schema cho US1–US5.
**Testing**: JUnit 5, Mockito, `@WebMvcTest` + Spring Security Test. Không test DB (không có DB kiểm thử).
**Target Platform**: backend 8085, frontend 3000 (và được backend phục vụ qua `WebMvcConfig`).
**Performance Goals**: SC-005 — 2 giây/95% yêu cầu, không tính cổng; querydr timeout 10 giây.
**Constraints**: idempotent tuyệt đối với kết quả thanh toán; bí mật chỉ từ biến môi trường; chỉ thêm vào file dùng chung, không sửa code người khác.
**Scale/Scope**: 7 UC + 1 luồng chưa có UC; ~20 endpoint; 6 trang.

## Constitution Check

| Nguyên tắc | Trước thiết kế | Sau thiết kế và bằng chứng |
| --- | --- | --- |
| I. Tiếng Việt, không lộ kỹ thuật | Đạt | Mọi mã có `…Label`; MSG31–37; IPN trả mã chuẩn VNPay chỉ cho cổng, không hiện cho người dùng; log che chữ ký |
| II. Dữ liệu thật | Đạt | Gói lấy từ `membership_plans` có sẵn; trạng thái rỗng "Bạn chưa có gói hội viên"; không chèn dữ liệu mẫu |
| III. Kiến trúc/quy ước | Đạt có ghi chú | Gói `com.kizuna.features.payment` có sẵn; khung phản hồi BACKEND_RULES 6.2; lỗi qua `GlobalExceptionHandler`. Ghi chú: IPN dùng định dạng VNPay (research §2); CSRF giữ như dự án (research §11) |
| IV. Thiết kế theo phân hệ | Đạt | Trang gia sư dùng `tutor-layout` + theme Stitch (như FE-03); trang nhân viên dùng `staff-layout` mới cùng theme cổng học vụ |
| V. Phạm vi/kiểm chứng | Đạt có điều kiện | Ma trận FR bên dưới; file dùng chung chỉ thêm. **Điều kiện**: D-1 (2 entity của thành viên khác) cần người dùng duyệt |

## Quyết định cần người dùng duyệt trước khi cài đặt

- **D-1 — ĐÃ DUYỆT 2026-11-04 (phương án a: FE-09 hoàn thiện hai file)**: `PaymentTransaction.java`, `PaymentProcessingLog.java` (TonHai2507 tạo) là khung rỗng, map sai cột và kế thừa `BaseEntity` → không dùng được. Khuyến nghị: FE-09 hoàn thiện hai file này (thay toàn bộ thân lớp theo data-model §2, §4) vì chúng nằm trong gói `payment` của FE-09 và chưa được nơi nào dùng (đã grep). Phương án khác: báo TonHai2507 tự sửa, FE-09 chờ.
- **D-2**: Bảng `tutor_posts` đã tạo 2026-11-04 (data-model §6); US6 còn chờ UC (srs-gaps G-01).
- **D-3 — ĐÃ XÁC NHẬN 2026-11-04**: có gói = `PREMIUM`, hết gói = `EXPIRED`, không dùng `STANDARD`.

## Project Structure

### Documentation

```text
specs/009-membership-payment/
├── spec.md · srs-gaps.md · plan.md · research.md · data-model.md · quickstart.md · tasks.md
├── contracts/membership-api.md
└── checklists/requirements.md · checklists/payment.md
```

### Source Code

```text
backend/src/main/java/com/kizuna/
├── core/
│   ├── dto/ApiResponse.java                         # MỚI (khung BACKEND_RULES 6.2)
│   ├── exception/ConflictException.java             # MỚI
│   ├── exception/GlobalExceptionHandler.java        # THÊM 1 phương thức handle ConflictException (409)
│   └── security/SecurityConfig.java                 # THÊM requestMatchers FE-09 trước anyRequest
└── features/payment/
    ├── config/VnpayProperties.java                  # MỚI @ConfigurationProperties("app.payment.vnpay")
    ├── entity/PaymentTransaction.java               # HOÀN THIỆN (D-1)
    ├── entity/PaymentProcessingLog.java             # HOÀN THIỆN (D-1)
    ├── entity/MembershipPlan.java, TutorMembership.java, enums   # GIỮ NGUYÊN
    ├── repository/ MembershipPlanRepository, PaymentTransactionRepository (findLockedById),
    │               TutorMembershipRepository, PaymentProcessingLogRepository,
    │               MembershipTutorRepository (khóa dòng tutors), MembershipQuerySpecification
    ├── service/ VnpaySigner, VnpayGatewayClient (tạo URL, querydr), PaymentResultProcessor,
    │            MembershipPeriodCalculator, MembershipExpiryService, PaymentLogWriter,
    │            PaymentNotificationService, CurrentTutorProvider,
    │            TutorMembershipService(+impl), MembershipPlanAdminService(+impl),
    │            TutorMembershipMonitorService(+impl), PaymentIssueService(+impl), MembershipLabels
    ├── controller/ TutorMembershipController, VnpayIpnController,
    │               ManagerMembershipPlanController, ManagerTutorMembershipController,
    │               AdminPaymentTransactionController
    └── dto/request, dto/response
backend/src/main/resources/application.properties    # THÊM mục "13. FE-09 VNPAY" (app.payment.vnpay.*); KHÔNG sửa khối vnpay.* cũ
backend/src/test/java/com/kizuna/features/payment/   # MỚI
core/config/WebMvcConfig.java                        # THÊM forward 4 route trang

frontend/
├── components/tutor-layout/tutor-layout.js          # THÊM mục "Gói hội viên" (file FE-03 của mình)
├── components/staff-layout/staff-layout.js          # MỚI (sidebar quản lý/quản trị, menu tài khoản dùng user-menu)
├── js/api.js, js/config/api.config.js, server.js    # THÊM nhóm API/route FE-09
├── pages/tutor-membership/                          # MỚI /tutor/membership
├── pages/tutor-membership-result/                   # MỚI /tutor/membership/result
├── pages/manager-membership-plans/                  # MỚI /manager/membership-plans
├── pages/manager-tutor-memberships/                 # MỚI /manager/tutor-memberships (+ chi tiết dạng ngăn kéo)
└── pages/admin-payments/                            # MỚI /admin/payments (+ chi tiết dạng ngăn kéo)
```

## Ảnh hưởng tới module khác

| Module | Ảnh hưởng | Biện pháp |
| --- | --- | --- |
| FE-01 Auth | Gán sẵn `PREMIUM` khi đăng ký → trạng thái gói sai từ đầu | Không sửa; người dùng báo người phụ trách |
| FE-03 Verification (của mình) | FE-09 đọc `verification_status`; FE-03 đặt `APPROVED_UNPAID` khi duyệt | Không sửa FE-03 ngoài sidebar |
| FE-04 Tutor search | Có thể cần lọc gia sư còn gói | Ngoài phạm vi; ghi chú cho nhóm |
| FE-06 Classroom | BF I.2.4 khóa lớp khi hết gói | FE-06 đọc `tutor_state`/`tutor_memberships` |
| FE-10 Ticket | `support_tickets.related_transaction_id/membership_id` | Không đổi |
| Chung | `SecurityConfig`, `GlobalExceptionHandler`, `WebMvcConfig`, `application.properties`, `api.js`, `server.js` | Chỉ thêm dòng/phương thức mới, không đổi dòng có sẵn |

## Implementation Sequence

1. Nền tảng: ApiResponse, ConflictException + handler, VnpayProperties, cấu hình, entity (D-1), repository.
2. US1+US2 (MVP): VnpaySigner → GatewayClient → PeriodCalculator → ResultProcessor → service/controller gia sư + IPN → trang gia sư.
3. US3, US4: quản lý vận hành.
4. US5: quản trị viên.
5. US6: chỉ khi D-2 được duyệt.
6. Polish: rà soát giao diện, quickstart, báo cáo.

## Validation and Requirement Coverage

| FR | Thành phần | Kiểm chứng |
| --- | --- | --- |
| FR-001, 012 | TutorMembershipService | ControllerTest, QS 1 |
| FR-002 | TutorMembershipService.createPayment | ServiceTest, QS 2 |
| FR-003, 011 | VnpayGatewayClient, PaymentLogWriter | VnpaySignerTest, QS 3 |
| FR-004 | VnpayIpnController | IpnControllerTest (97/01/04/02/00/99) |
| FR-005 | TutorMembershipService.handleReturn | ServiceTest, QS 4 |
| FR-006, 007 | PaymentResultProcessor | ProcessorTest (lặp, rollback), QS 5 |
| FR-008 | MembershipPeriodCalculator | CalculatorTest (cuối tháng, năm nhuận, cộng nối) |
| FR-009 | PaymentResultProcessor | ProcessorTest, QS 8 |
| FR-010 | PaymentNotificationService (sau commit) | ProcessorTest (lỗi mail không rollback) |
| FR-013 | MembershipExpiryService | ExpiryServiceTest, QS 16 |
| FR-014–016 | MembershipPlanAdminService | ServiceTest, ControllerTest, QS 11–13 |
| FR-017, 018 | TutorMembershipMonitorService | ControllerTest, QS 14–15 |
| FR-019–021 | PaymentIssueService | ServiceTest, QS 9–10 |
| FR-022 | (chờ D-2) | — |
| FR-023 | SecurityConfig | PaymentSecurityTest, QS 17 |
| FR-024 | Trang frontend | QS 19 |
| FR-025 | VnpayProperties | ServiceTest, QS 18 |

## Complexity Tracking

| Ngoại lệ | Lý do | Phương án đơn giản hơn bị loại |
| --- | --- | --- |
| IPN không dùng khung `success/data` | VNPay bắt buộc `RspCode/Message` | Dùng khung chung → VNPay coi là lỗi, gửi lại vô hạn |
| Hai cấu hình VNPay cùng tồn tại (`vnpay.*` cũ, `app.payment.vnpay.*` mới) | Không được sửa code người khác | Sửa khối cũ — vi phạm quy định |

## Phase Outputs and Readiness

- Phase 0: research.md. Phase 1: data-model.md, contracts/, quickstart.md.
- Constitution Check sau thiết kế: đạt, chờ D-1.
- Chưa sửa code, cấu hình hay DB. Bước tiếp theo: `$speckit-tasks`.

## Ghi chú triển khai (2026-11-04)

- **Trang kết quả**: khi gia sư quay về và giao dịch còn chờ, backend tra cứu querydr; nếu cổng không phản hồi hoặc phản hồi không xác thực được chữ ký thì dùng chính tham số trả về **đã kiểm chữ ký HMAC** làm kết quả dự phòng (vẫn qua `PaymentResultProcessor`). Lý do: chạy cục bộ không nhận được IPN, và thứ tự chữ ký querydr chưa đối chiếu được với sandbox. An toàn vì chữ ký do VNPay tạo.
- **Thêm** `core/exception/ServiceUnavailableException` + phương thức 503 trong `GlobalExceptionHandler` (ngoài ConflictException đã dự kiến), vì handler chung trả 500 cho mọi lỗi lạ.
- **Ghi gói** đi qua câu lệnh native có `CAST(:featuresJson AS jsonb)`: entity `MembershipPlan` (không thuộc phạm vi sửa) ánh xạ JSONB như chuỗi thường, Hibernate sẽ lỗi kiểu khi cập nhật qua entity. Đề xuất người tạo entity thêm `@JdbcTypeCode(SqlTypes.JSON)` cho `featuresJson`.
- **Nhật ký** ghi bằng transaction riêng **sau** khi xử lý xong (không ghi trong lúc giữ khóa dòng giao dịch, tránh tự chờ khóa khóa ngoại).
- **Thêm** `frontend/js/membership-format.js` (định dạng dùng chung cho 5 trang FE-09); `authRequest` (FE-03, của mình) đọc thêm `details` của lỗi validation để hiện lỗi theo trường.
- DTO gom thành `MembershipViews` / `MembershipRequests` (record lồng) thay vì một file mỗi lớp.
- **Bổ sung sau nghiệm thu (2026-11-04)**: A6 `POST /payments/{id}/check` (gia sư tự kiểm tra giao dịch chờ với cổng) và A7 `POST /payments/{id}/resume` (mở lại trang thanh toán trong hạn 15 phút, cùng mã đơn), trường `resumable` ở A4; nút "Kiểm tra kết quả" / "Tiếp tục thanh toán" trong bảng lịch sử giao dịch. Lý do: chạy cục bộ không có IPN, giao dịch đã trả tiền nhưng không về được trang kết quả bị kẹt ở "Chờ thanh toán". Đường dẫn trả về mặc định đổi sang cổng 8085 (phiên đăng nhập lưu theo cổng).
- **Sửa lỗi TLS (2026-11-04)**: tra cứu querydr báo `PKIX path building failed` trên JDK 17.0.12 vì chứng chỉ `sandbox.vnpayment.vn` nối lên "Sectigo Public Server Authentication Root R46" không có trong cacerts của bản JDK này (Windows có). Thêm `features/payment/config/VnpayTrust.java`: SSLContext chỉ cho HttpClient gọi VNPay, tin cacerts của JDK + kho `Windows-ROOT` (nếu chạy Windows). Không tắt kiểm tra chứng chỉ, không đổi cấu hình JVM chung. Đã kiểm tra: JDK mặc định → PKIX; qua VnpayTrust → HTTP 200.
