# Implementation Plan: FE-12 — Notification Management

**Branch**: `huyendtt` thực tế; feature context `012-notification-management` do setup-plan báo | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: 16 FR, 7 SC, bốn story và năm quyết định clarify trong spec.

## Summary

Hoàn thiện module thông báo trong monolith hiện có: inbox/read/delete theo người, preferences, admin broadcast ngay/hẹn giờ/all-users, lịch sử riêng từng kênh và event port cho bốn nguồn. PostgreSQL giữ outbox/dispatch/recipient/delivery để restart không mất tác vụ. Email không có bảo đảm exactly-once; timeout không xác định được ghi UNKNOWN, không giả báo thành công.

## Technical Context

**Language/Version**: Java17, Spring Boot3.3.4 theo pom dự án, không dùng stack demo AI.

**Primary Dependencies**: Spring Web/Thymeleaf/Security/Data JPA/Validation/Mail, PostgreSQL driver hiện có; scheduler và bounded executor. Không cần broker/websocket/provider mới.

**Storage**: PostgreSQL16; mở rộng notifications/user_notifications, thêm preference state, broadcast, outbox, frozen cohort, per-channel deliveries. History tách khỏi user soft-delete.

**Testing**: JUnit5/MockMvc/spring-security-test và PostgreSQL riêng, fake clock, test SMTP adapter; profile dự kiến notification-test, không gửi thư thật hoặc seed UI vận hành.

**Target Platform**: Web SSR/responsive, Windows/PowerShell dev và JVM server; timezone nghiệp vụ Asia/Ho_Chi_Minh, timestamp TIMESTAMPTZ.

**Project Type**: Monolith web + internal event port + background worker.

**Performance Goals**: SC-006:100người/10phút,10.000personal notifications, p95browser list/detail≤2s và enqueue≤2s, request failure≤5s từ thao tác yêu cầu. Poll5s, batch100, đến hạn bắt đầu≤60s khi nguồn/worker khả dụng. Đo không giả định provider gửi hết trong thời gian enqueue.

**Constraints**: Title1..150/content1..10.000 Unicode code points không trắng; six roles, five categories; preference default in-app=true/email=false; urgent all-users chỉ override in-app; cohort frozen lúc đến hạn. Source/owner checks, CSRF, immutable confirmed content, no raw enum/secret/log; UTF-8 không BOM.

**Scale/Scope**: Bảy UC-70–76, bốn story; read/inbox, preference, group scheduling/history, urgent-all. Không editor email HTML tùy ý, SMS/push, attachments, cancel/reschedule/retry UI, tracking pixels, finance report hoặc thay OTP.

## Constitution Check

*Gate trước Phase0 và tái kiểm sau Phase1.*

| Nguyên tắc | Trước | Sau thiết kế |
| --- | --- | --- |
| I. Tiếng Việt/không lộ backend | Đạt | Label DTO, escaped body/email, không internal ticket notes/stack/raw roles |
| II. Dữ liệu thật | Đạt | Durable state, queue khác accept/deliver/read, UNKNOWN không fake success, missing source không fixture vận hành |
| III. Kiến trúc/quy ước | Đạt | Spring monolith/controller-service-repository; UUID/mapping rõ; incremental SQL |
| IV. Thiết kế phân hệ | Đạt | Reuse six shells; learner/tutor DESIGN mục7, staff chung; menu/badge có dữ liệu thật |
| V. Phạm vi/kiểm chứng | Đạt | FR/SC coverage, concurrency/SMTP/restart tests; dependencies ghi riêng |

Không có ngoại lệ constitution; không sửa khối bất biến hoặc luồng AI/Kanji.

## Project Structure

### Documentation (this feature)

```text
specs/012-notification-management/
  spec.md
  plan.md
  research.md
  data-model.md
  contracts/notifications.md
  quickstart.md
  checklists/requirements.md
```

tasks.md do bước `$speckit-tasks` tạo sau.

### Source Code (repository root)

Đường dẫn dự kiến, không khẳng định file mới đã triển khai:

```text
src/main/java/com/kizuna/
  controller/notifications/NotificationController.java
  controller/notifications/NotificationPageController.java
  controller/admin/NotificationBroadcastController.java
  service/INotificationService.java
  service/impl/NotificationServiceImpl.java
  service/notifications/        # policy, preferences, cohort, dispatch, delivery, event port
  service/notifications/mail/NotificationMailAdapter.java
  entity/                      # complete Notification/UserNotification, new persisted models
  repository/                  # scoped query, claim/lease/dedup counters
  dto/request/notifications/
  dto/response/notifications/
  config/SecurityConfig.java
  exception/NotificationExceptionHandler.java
src/main/resources/
  db/changes/                  # versioned incremental migrations
  templates/fragments/notifications/
  templates/{admin,manager,cs,academic,tutor,learner}/notifications/
  static/js/notifications.js
  static/css/notifications.css
src/test/java/com/kizuna/notifications/
src/test/resources/application-notification-test.properties
src/test/load/notifications.js
```

**Structure Decision**: Reuse existing stubs, mail transport và shells. Adapter mail riêng trả outcome đồng bộ trong worker, không reuse @Async void OTP để báo broadcast sent. Không direct JPA entity JSON hoặc public event ingestion API.

## Execution Phases and Dependencies

1. Preflight live/test schema/duplicates, security/current account, DB test/migration, mail-safe test profile. Chỉ SQL tăng dần, không DROP CASCADE.
2. Inbox/scoped reads, idempotent read, soft-delete, unread count và UI auto-read sau load thành công; preference state/version/defaults.
3. Broadcast/immutable content/durable enqueue/submission dedup, scheduled cohort snapshot, worker lease/outcomes và admin history; urgent-all confirmation và override in-app.
   Worker claim tối đa min(100,số slot rảnh đã giữ), lease60s/token riêng, kiểm/gia hạn60s trước phát và guard khi finalize; mất lease không gửi hoặc ghi đè kết quả. Email known-unsent transient tối đa4calls tổng cộng (lần đầu+3retry sau30/120/300s), UNKNOWN không retry. Read sau personal delete trả404; DELETE lặp của owner trả200.
4. Event outbox intake cùng transaction từng module thật, recipient authorization và safe template. Có source thiếu thì ghi integration chưa đạt, không thay bằng emit test.
5. Restart/race/SMTP outcomes, browser/load và nghiệm thu; tất cả tasks/tracking phản ánh evidence thật.

## Validation and Requirement Coverage

| Requirement | Thiết kế/kiểm chứng |
| --- | --- |
| FR-001–004 | Owner read/delete/read-mark/current-role, multi-user isolation, badges/count race |
| FR-005–006 | Whole-matrix preference version, defaults, recheck before send, urgent-only in-app override |
| FR-007–010 | Input limits/preview/confirmation, role cohort due-time, durable schedule+request dedup/restart |
| FR-011–013 | Per-channel outcome, UNKNOWN, aggregation/filter/history, SMTP accepted khác delivered |
| FR-014–015 | Source transaction outbox + current authorization, scoped recipients, event IDs và source dependencies |
| FR-016 | Vietnamese/escaped rendering, six shells, loading/empty/error, no fake data |
| SC-001–007 | MockMvc/PostgreSQL/clock/SMTP integration,100user browser/load,10 người thử inbox |

## Complexity Tracking

Không có ngoại lệ. Persisted outbox/cohort/delivery cần thiết cho restart, lịch gửi và idempotency; queue RAM đơn giản hơn nhưng không đáp ứng spec. SMTP ambiguity được biểu diễn thay vì thêm cam kết không thể chứng minh.

## Readiness

Phase0 research và Phase1 model/contracts/quickstart hoàn tất. Sẵn sàng chia tasks; hoàn tất FE-12 vẫn phụ thuộc nguồn giao dịch thật của học thử/bài tập/thanh toán hội viên/hỗ trợ và SMTP dùng được. Chưa sửa source ứng dụng, gửi email, chạy migration/test hoặc commit/push.
