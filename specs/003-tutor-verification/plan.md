# Implementation Plan: FE-03 — Tutor Verification

**Branch**: `han` (thư mục đặc tả `specs/003-tutor-verification`) | **Date**: 2026-11-03 | **Spec**: [spec.md](spec.md)

**Input**: `specs/003-tutor-verification/spec.md` — 10 use case (UC-22…28, UC-82…84), 24 yêu cầu chức năng, 3 câu trả lời làm rõ phiên 2026-11-03.

## Summary

Hoàn thiện feature `verification` đang là khung: gia sư lập, lưu nháp, tải giấy tờ và nộp hồ sơ xác minh; cán bộ học vụ xem bảng điều khiển/hàng đợi, xem chi tiết và giấy tờ, yêu cầu bổ sung, phê duyệt hoặc từ chối; hai bên xem trạng thái, phản hồi và lịch sử. Hồ sơ xác minh nằm trên `tutors` (A-1); `verification_status` là nguồn trạng thái, đồng bộ `tutor_state`. Giấy tờ ở `tutor_credentials`, tệp lưu trên máy chủ ngoài vùng công khai với tên ngẫu nhiên. Lịch sử append-only ở `tutor_verification_histories`, bổ sung 2 cột nullable để tách phản hồi khỏi ghi chú nội bộ. Bổ sung filter JWT và chỉ siết quyền trên route FE-03. Chuyển trạng thái chạy trong transaction có khóa dòng để đạt GB-01. Email gửi sau commit, lỗi không hoàn tác quyết định.

## Technical Context

**Language/Version**: Java 17, Spring Boot 3.3.4 (backend); HTML5 + JavaScript ES module + Tailwind CDN (frontend).

**Primary Dependencies**: Spring Web, Security, Data JPA, Validation, Mail, jjwt 0.11 — đều có sẵn trong `backend/pom.xml`. Không thêm dependency (kiểm tra PDF/ảnh bằng chữ ký byte và `ImageIO`).

**Storage**: PostgreSQL (`ddl-auto=none`): `tutors`, `users`, `tutor_credentials`, `tutor_verification_histories`. Một thay đổi schema cộng thêm (2 cột nullable + 1 index), áp trực tiếp trên Supabase bằng DBeaver — câu lệnh ghi tại data-model.md §3. Tệp giấy tờ trên đĩa máy chủ tại `app.verification.storage-dir`.

**Testing**: JUnit 5, Mockito, `@WebMvcTest` + spring-security-test (có sẵn), không chạm DB. Nhóm chỉ có DB Supabase dùng chung (phương án DB 3, chọn 2026-11-03): test tích hợp DB được viết nhưng tự bỏ qua khi thiếu `VERIFICATION_TEST_DB_URL`; migration do người phụ trách chạy bằng DBeaver; nghiệm thu thủ công trên Supabase bằng tài khoản thử nghiệm; không đo tải SC-003. Xem [quickstart.md](quickstart.md).

**Target Platform**: Backend JVM cổng 8085; frontend dev server Node cổng 3000; trình duyệt desktop/mobile.

**Project Type**: Ứng dụng web — REST backend nguyên khối + trang tĩnh `frontend/`.

**Performance Goals**: SC-003 — p95 ≤ 2 giây cho bảng điều khiển và chi tiết với 1.000 gia sư, 100 người dùng đồng thời. Danh sách không tải tệp; tệp tải riêng theo yêu cầu.

**Constraints**: Tệp PDF/PNG/JPG ≤ 10MB; chỉ chủ hồ sơ và cán bộ học vụ xem tệp; kiểm tra vai trò + trạng thái tài khoản từ DB mỗi request; ghi chú nội bộ không bao giờ tới gia sư; mọi chuyển trạng thái có đúng một mục lịch sử; UTF-8 không BOM; giao diện tiếng Việt, không in hoa toàn bộ.

**Scale/Scope**: 5 user story, 10 UC, 2 vai trò (gia sư, cán bộ học vụ), 16 endpoint REST, 3 trang frontend. Ngoài phạm vi: FE-01 (trạng thái ban đầu khi đăng ký), FE-02, FE-09, GB-06 tại FE-04/05, duyệt từng giấy tờ, giao việc "In Review".

## Constitution Check

*GATE: Kiểm tra trước research và sau thiết kế Phase 1.*

| Nguyên tắc | Trước thiết kế | Sau thiết kế và bằng chứng |
| --- | --- | --- |
| I. Tiếng Việt, không lộ kỹ thuật | Đạt | Mọi mã trả về kèm `…Label`; bảng lỗi tiếng Việt trong contract; handler 401/403 riêng cho route FE-03; không trả `storageKey`/exception |
| II. Dữ liệu thật | Đạt | Dữ liệu từ repository thật; trạng thái tải/rỗng/lỗi + "Thử lại" trên 3 trang; dữ liệu 1.000 gia sư chỉ sinh trên DB kiểm thử |
| III. Kiến trúc/quy ước | Đạt có ghi chú | Giữ `com.kizuna.features.verification` theo `BACKEND_FEATURE_ARCHITECTURE.md`; UUID; JPA chuẩn; migration cộng thêm có lý do (research §2). Ghi chú: constitution nêu Thymeleaf nhưng dự án thực tế dùng trang tĩnh `frontend/` gọi REST (FE-04) — FE-03 theo thực tế, không tạo kiến trúc song song |
| IV. Thiết kế theo phân hệ | Ngoại lệ có ghi nhận | Ban đầu theo `DESIGN.md` mục 7. **2026-11-03: người phụ trách cung cấp thiết kế Google Stitch "Tutor Dashboard" và yêu cầu khu vực gia sư làm theo thiết kế này** (Plus Jakarta Sans, bo góc mềm, màu chính `#006194`, sidebar 10 mục) thay cho phong cách retro của mục 7. Áp dụng cho `/tutor/dashboard` và `/tutor/verification` qua `components/tutor-layout/` và `styles/tutor-theme.css`; vẫn giữ 1 CTA chính mỗi khối, tiếng Việt, không in hoa toàn bộ (nhãn tiếng Nhật/tagline in hoa trong bản Stitch được chuyển về sentence case). `DESIGN.md` không bị sửa. Trang cán bộ học vụ theo `FRONTEND_RULES.md` mục 5 |
| V. Phạm vi/kiểm chứng | Đạt | Ma trận FR bên dưới; chỉ siết route FE-03 trong `SecurityConfig`; không sửa đăng ký (FE-01), tìm kiếm (FE-04), mặc định entity `Tutor` |

Chênh lệch nguồn được ghi nhận (không sửa trong FE-03): `Tutor` Java mặc định `APPROVED`/`PREMIUM` khác DDL `UNVERIFIED`/`FREE`; đăng ký đặt `APPROVED` (A-11).

## Project Structure

### Documentation (this feature)

```text
specs/003-tutor-verification/
  spec.md
  plan.md                       # file này
  research.md                   # Phase 0
  data-model.md                 # Phase 1
  contracts/verification-api.md # Phase 1
  quickstart.md                 # Phase 1
  checklists/requirements.md
```

`tasks.md` tạo sau bằng `$speckit-tasks`.

### Source Code (repository root)

Đường dẫn dự kiến; (M) = sửa file có sẵn, còn lại là file mới.

```text
backend/src/main/java/com/kizuna/
  core/security/
    SecurityConfig.java                       (M) thêm filter; siết /api/tutor/verification/**, /api/academic/verifications/**
    JwtAuthenticationFilter.java              Bearer → nạp User từ DB, chỉ ACTIVE, quyền từ role DB
    JwtTokenProvider.java                     (M) thêm đọc claim userId nếu cần
    RestAuthErrorHandlers.java                401/403 JSON tiếng Việt
  features/verification/
    entity/TutorCredential.java               (M) mapping đầy đủ, bỏ BaseEntity
    entity/TutorVerificationHistory.java      (M) mapping đầy đủ + feedback/reason_code, bỏ BaseEntity
    entity/CredentialType.java, RejectionReason.java
    repository/TutorCredentialRepository.java
    repository/TutorVerificationHistoryRepository.java
    repository/VerificationTutorRepository.java   khóa PESSIMISTIC_WRITE, truy vấn hàng đợi/tóm tắt
    service/TutorVerificationService.java + impl/  phía gia sư (UC-22..24, 82, 83)
    service/VerificationReviewService.java + impl/ phía cán bộ (UC-25..28, 84)
    service/VerificationStateMachine.java          chuyển trạng thái + đồng bộ tutor_state + lịch sử
    service/VerificationDocumentStorage.java       lưu/đọc/xóa tệp, kiểm tra định dạng, đồng bộ transaction
    service/VerificationNotificationService.java   email sau commit, @Async
    service/VerificationLabels.java                ánh xạ mã → nhãn tiếng Việt
    controller/TutorVerificationController.java    /api/tutor/verification
    controller/VerificationReviewController.java   /api/academic/verifications
    controller/VerificationExceptionHandler.java   @RestControllerAdvice giới hạn 2 controller trên
    dto/request/, dto/response/
backend/src/main/resources/application.properties (M) multipart 10MB, app.verification.storage-dir
backend/src/test/java/com/kizuna/features/verification/
(thay đổi schema: chạy trực tiếp trên Supabase, xem data-model.md §3 — không có file migration trong repo)
database/DatabasePgAdmin4.txt                     (M) đồng bộ CREATE TABLE tutor_verification_histories
frontend/
  js/api.js                                      (M) helper gọi API kèm Bearer, multipart, tải blob
  js/config/api.config.js                        (M) endpoint FE-03
  server.js                                      (M) alias /tutor/verification, /academic/verifications, /academic/dashboard
  pages/tutor-verification/{index.html, tutor-verification.js}
  pages/academic-verifications/{index.html, academic-verifications.js}
  pages/academic-verification-detail/{index.html, academic-verification-detail.js}
.gitignore                                       (M) loại thư mục lưu tệp nếu đặt trong repo
```

**Structure Decision**: Toàn bộ nghiệp vụ trong `features/verification` (vertical slice). Truy cập `Tutor`/`User` qua repository của `core` được phép vì là entity lõi. Hạ tầng xác thực đặt ở `core/security` vì dùng chung, nhưng chỉ áp quyền bắt buộc cho route FE-03.

## Implementation Sequence

1. **Nền tảng dữ liệu**: migration cộng thêm (chạy thử 2 lần trên DB kiểm thử); hoàn thiện 2 entity + enum mới; repository có khóa dòng. Kiểm tra Hibernate khởi động với `ddl-auto=none` không lỗi mapping.
2. **Xác thực**: `JwtAuthenticationFilter`, handler 401/403, siết 2 nhóm route; test ma trận vai trò/trạng thái tài khoản; xác nhận route khác vẫn `permitAll`.
3. **US1 (P1)**: lưu trữ tệp + kiểm tra định dạng; API gia sư xem/lưu nháp/giấy tờ/nộp; state machine `→ PENDING`; trang `tutor-verification` (form, tải tệp, cam kết, chỉ đọc).
4. **US2 (P1)**: API tóm tắt/hàng đợi/chi tiết/tải tệp/phê duyệt/từ chối; email sau commit; trang `academic-verifications` và `academic-verification-detail` (xem PDF/ảnh qua blob, hộp thoại quyết định).
5. **US3 (P2)**: yêu cầu bổ sung; mở khóa và nộp lại từ `ADDITIONAL_INFO_REQUIRED`/`REJECTED`; hiển thị phản hồi cho gia sư.
6. **US4 (P2)**: hoàn thiện hiển thị trạng thái/phản hồi/ngày, trạng thái rỗng và lỗi + "Thử lại" phía gia sư.
7. **US5 (P3)**: khối "Lịch sử thẩm định gần đây" và trang/khối lịch sử đầy đủ.
8. **Kiểm chứng**: chạy unit test và `@WebMvcTest`, nghiệm thu thủ công [quickstart.md](quickstart.md) trên Supabase; ghi SC-003 và kiểm thử cạnh tranh trên DB là chưa kiểm chứng; ghi kết quả thật và giới hạn.

## Validation and Requirement Coverage

| Yêu cầu | Thiết kế / kiểm chứng |
| --- | --- |
| FR-001, FR-009, FR-021 | `GET /tutor/verification` có `statusLabel`, `latestFeedback` không có ghi chú nội bộ; test JSON không chứa `internalNote`; rà soát UI (QS 1, 11, 18) |
| FR-002, FR-003 | `PUT /tutor/verification`, CRUD giấy tờ, chặn khi không thể chỉnh sửa (409); QS 2 |
| FR-004, FR-024 | `VerificationDocumentStorage`: giới hạn multipart + chữ ký byte + `ImageIO`; tên UUID ngoài vùng công khai; xóa tệp cũ sau commit; unit test; QS 3, 9 |
| FR-005, FR-006 | Kiểm tra trường bắt buộc + 2 nhóm giấy tờ + cam kết ở bước nộp; unit test; QS 4–5 |
| FR-007, FR-008, FR-017 | State machine trong transaction có `PESSIMISTIC_WRITE`; test tích hợp song song; QS 6, 15 |
| FR-010–FR-012 | Truy vấn tóm tắt/hàng đợi (data-model §5); test lọc/phân trang; QS 7–8 |
| FR-013 | Chi tiết + tải tệp blob; xử lý tệp mất; QS 9 |
| FR-014–FR-016 | Endpoint request-info/approve/reject với validation; lịch sử + email; QS 10, 12, 13 |
| FR-018 | `GET …/history`, `recent-decisions`; cột `feedback`/`reason_code`; QS 14 |
| FR-019, FR-020 | Filter JWT nạp User từ DB, chỉ ACTIVE; kiểm tra sở hữu ở service; test MockMvc; QS 16 |
| FR-022 | Gửi email `afterCommit` + `@Async`, try/catch ghi log; QS 17 |
| FR-023 | FE-03 coi `UNVERIFIED` là "Chưa nộp hồ sơ"; trạng thái ban đầu khi đăng ký thuộc FE-01 (A-11) — ghi trong báo cáo |
| SC-001–SC-007 | Nghiệm thu quickstart (bấm giờ SC-001); SC-004/006 bằng test tự động; SC-005 bằng unit test về khóa dòng + IT tùy chọn; SC-003 chưa đo (không có DB kiểm thử riêng) |

## Complexity Tracking

| Bổ sung | Lý do cần | Phương án đơn giản hơn bị loại vì |
| --- | --- | --- |
| Filter JWT + siết route trong `SecurityConfig` (core) | FR-019/020, GB-05: hiện mọi route `permitAll`, không xác định được người dùng | Nhận `userId` qua query như vài controller hiện có cho phép giả mạo người khác |
| 2 cột nullable ở `tutor_verification_histories` | FR-018 cần phản hồi và ghi chú nội bộ riêng cho từng mục; GB-02 | Trộn vào `notes` dễ rò rỉ ghi chú nội bộ; phương án dự phòng ghi ở research §2 nếu nhóm không duyệt đổi schema |
| Lưu tệp trên đĩa + đồng bộ theo transaction | Theo câu trả lời làm rõ Q2 | — (quyết định của nhóm) — **đã thay, xem mục dưới** |

## Thay đổi sau nghiệm thu (2026-11-06): lưu giấy tờ trong DB

- **Vấn đề**: tệp nằm trên ổ cứng của máy đã chạy backend lúc tải lên; cả nhóm dùng chung DB Supabase nhưng mỗi người chạy backend riêng, nên máy khác thấy bản ghi mà không có tệp ("Không thể hiển thị tệp này") và gia sư còn bị chặn nộp hồ sơ vì giấy tờ bị coi là thiếu tệp.
- **Giải pháp** (cùng cách FE-06 `learning_materials.file_data`, FE-07): nội dung tệp lưu ở cột mới `tutor_credentials.file_data BYTEA` — migration `database/migrations/010_fe03_tutor_credentials_file_data.sql`, người phụ trách FE-03 tự chạy trên Supabase. `file_url` vẫn giữ khóa `<uuid>.<pdf|png|jpg>` để xác định loại tệp.
- `file_data` không ánh xạ vào entity; đọc/ghi qua truy vấn riêng ở `TutorCredentialRepository` (danh sách không tải dữ liệu nhị phân; kiểm tra "có tệp" bằng một truy vấn `file_data IS NOT NULL` cho cả danh sách). Bản ghi và nội dung ghi trong cùng transaction.
- **Chuyển tiếp**: bản ghi cũ có `file_data = NULL` vẫn đọc từ `app.verification.storage-dir` nếu máy hiện tại còn tệp; thay hoặc xóa giấy tờ thì xóa tệp cũ trên đĩa sau commit.
- API và frontend không đổi. Kiểm tra định dạng/dung lượng (FR-024) giữ nguyên.

## Phase Outputs and Readiness

- Phase 0: [research.md](research.md) — không còn NEEDS CLARIFICATION.
- Phase 1: [data-model.md](data-model.md), [contracts/verification-api.md](contracts/verification-api.md), [quickstart.md](quickstart.md).
- Constitution Check sau thiết kế: đạt, một ghi chú ở Nguyên tắc III (Thymeleaf ↔ trang tĩnh thực tế).
- Cần nhóm xác nhận khi review: thay đổi schema cộng thêm (research §2); alias `/academic/dashboard` (research §8).
- Chưa tạo migration, code, cấu hình hay chạy test trong bước lập kế hoạch. Bước tiếp theo: `$speckit-tasks`.
