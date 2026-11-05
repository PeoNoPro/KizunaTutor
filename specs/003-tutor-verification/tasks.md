---
description: "Danh sách công việc triển khai FE-03 — Tutor Verification"
---

# Tasks: FE-03 — Tutor Verification

**Input**: Tài liệu thiết kế trong `specs/003-tutor-verification/` — [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/verification-api.md](contracts/verification-api.md), [quickstart.md](quickstart.md).

**Tests**: Có. Constitution Nguyên tắc V yêu cầu kiểm thử cho thay đổi nghiệp vụ, schema và hợp đồng API.

**Môi trường DB (đã chọn 2026-11-03 — phương án 3)**: Chỉ có DB Supabase dùng chung, không có DB kiểm thử riêng. Do đó:
- Test tự động chỉ gồm unit test (Mockito) và `@WebMvcTest` (không chạm DB) — chạy được mọi lúc.
- Test tích hợp cần DB (`*IT.java`) vẫn được viết nhưng tự bỏ qua khi thiếu biến môi trường `VERIFICATION_TEST_DB_URL`; báo cáo phải ghi "chưa kiểm chứng trên DB" cho các mục này.
- Migration do người phụ trách tự chạy bằng DBeaver sau khi đồng ý; agent không tự áp lên Supabase.
- Nghiệm thu thủ công trên Supabase chỉ dùng tài khoản thử nghiệm tạo qua chức năng đăng ký thật; không chèn dữ liệu sinh sẵn; không chạy đo tải.

**Organization**: Nhóm theo user story để mỗi story triển khai và kiểm thử độc lập.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Chạy song song được (khác file, không phụ thuộc task chưa xong)
- **[Story]**: US1…US5 theo spec.md
- Đường dẫn tính từ gốc repo. Viết tắt: `BE` = `backend/src/main/java/com/kizuna`, `BT` = `backend/src/test/java/com/kizuna`, `VER` = `BE/features/verification`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Migration, cấu hình và khung thư mục.

- [X] T001 (Đã xóa file sau khi áp dụng — câu lệnh lưu tại data-model.md §3) Tạo migration cộng thêm, idempotent (`ADD COLUMN IF NOT EXISTS feedback TEXT`, `reason_code VARCHAR(50)`, `CREATE INDEX IF NOT EXISTS idx_verif_hist_action_created ON tutor_verification_histories(action, created_at DESC)`) tại `database/migrations/20261103_fe03_verification_history_feedback.sql`
- [X] T002 Đồng bộ khối `CREATE TABLE tutor_verification_histories` (thêm `feedback`, `reason_code`) và index mới trong `database/DatabasePgAdmin4.txt`; không sửa phần khác của file
- [X] T003 [P] Thêm `spring.servlet.multipart.max-file-size=10MB`, `spring.servlet.multipart.max-request-size=11MB`, `app.verification.storage-dir=${VERIFICATION_STORAGE_DIR:${user.home}/kizuna-data/verification-docs}` vào `backend/src/main/resources/application.properties`
- [X] T004 [P] Thêm profile kiểm thử dành cho test tích hợp tùy chọn, đọc DB từ biến môi trường (`VERIFICATION_TEST_DB_URL`, `…_USER`, `…_PASSWORD`; không có giá trị mặc định trỏ tới Supabase), thư mục lưu tệp tạm, `app.seeder.kanji.enabled=false` tại `backend/src/test/resources/application-verification-test.properties`
- [X] T005 [P] Thêm mục loại trừ `kizuna-data/` vào `.gitignore` ở gốc repo (phòng trường hợp thư mục lưu tệp đặt trong repo)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entity, repository, xác thực, lỗi, nhãn, lưu tệp, state machine — mọi story đều cần.

**⚠️ CRITICAL**: Không bắt đầu user story trước khi phase này xong.

### Dữ liệu

- [X] T006 [P] Tạo enum `CredentialType` (`IDENTITY_DOCUMENT`, `JLPT_CERTIFICATE`, `OTHER_JAPANESE_CERTIFICATE`, `DEGREE`, `TEACHING_CERTIFICATE`, `OTHER`) kèm phương thức nhóm bắt buộc (định danh / năng lực tiếng Nhật) tại `VER/entity/CredentialType.java`
- [X] T007 [P] Tạo enum `RejectionReason` (`INSUFFICIENT_QUALIFICATION`, `INVALID_OR_FORGED_DOCUMENT`, `INFORMATION_MISMATCH`, `OTHER`) tại `VER/entity/RejectionReason.java`
- [X] T008 [P] Hoàn thiện `TutorCredential` theo data-model §2: bỏ `extends BaseEntity` và các import thừa; map `credential_id`, `tutor_id` (`@ManyToOne(LAZY)`), `credential_type` (`EnumType.STRING`), `title`, `file_url` → `storageKey`, `verified_status` (`@JdbcType(PostgreSQLEnumJdbcType.class)`, `columnDefinition = "tutor_credential_status_enum"`), `rejection_reason`, `uploaded_at` tại `VER/entity/TutorCredential.java`
- [X] T009 [P] Hoàn thiện `TutorVerificationHistory` theo data-model §3: bỏ `extends BaseEntity`; map `history_id`, `tutor_id`, `officer_id` (`@ManyToOne(LAZY)` → `User`, nullable), `action` (`tutor_verif_action_enum`), `notes` → `internalNote`, `feedback`, `reason_code` → `RejectionReason`, `created_at` (`@CreationTimestamp`) tại `VER/entity/TutorVerificationHistory.java`
- [X] T010 [P] Tạo `TutorCredentialRepository` (`findByTutor_TutorIdOrderByUploadedAtAsc`, `findByCredentialIdAndTutor_TutorId`) tại `VER/repository/TutorCredentialRepository.java`
- [X] T011 [P] Tạo `TutorVerificationHistoryRepository` (`findByTutor_TutorIdOrderByCreatedAtAsc`, `countByActionAndCreatedAtBetween`, truy vấn N mục mới nhất có action ∈ {APPROVE, REJECT, REQUEST_INFO} kèm `fetch join` tutor.user và officer) tại `VER/repository/TutorVerificationHistoryRepository.java`
- [X] T012 Tạo `VerificationTutorRepository extends JpaRepository<Tutor, UUID>` với `@Lock(PESSIMISTIC_WRITE)` `findLockedByTutorId`, `findLockedByUserId`; không sửa `core/repository/TutorRepository.java`, tại `VER/repository/VerificationTutorRepository.java`
- [X] T013 **Người phụ trách thực hiện**: sao lưu hoặc ghi nhận cấu trúc bảng `tutor_verification_histories` hiện tại, rồi chạy migration T001 trên Supabase bằng DBeaver (chạy lại lần 2 để xác nhận idempotent). Sau đó agent khởi động backend như bình thường để xác nhận Hibernate nạp mapping T008–T009 không lỗi; ghi kết quả vào "Kết quả kiểm chứng". Chưa chạy migration thì chặn T034, T049 trở đi khi chạy thật (code vẫn viết được)

### Xác thực và phân quyền

- [X] T014 [P] Tạo `RestAuthErrorHandlers` (`AuthenticationEntryPoint` → 401 `UNAUTHENTICATED`, `AccessDeniedHandler` → 403 `FORBIDDEN`, JSON UTF-8 với message tiếng Việt theo contract) tại `BE/core/security/RestAuthErrorHandlers.java`
- [X] T015 Tạo `JwtAuthenticationFilter` (`OncePerRequestFilter`): đọc `Authorization: Bearer`, `JwtTokenProvider.validateToken`, lấy email, nạp `User` qua `UserRepository`, bỏ qua nếu `deletedAt != null` hoặc `accountStatus != ACTIVE`, đặt `UsernamePasswordAuthenticationToken` với authority = `role.roleName` từ DB; không token thì cho đi tiếp, tại `BE/core/security/JwtAuthenticationFilter.java`
- [X] T016 Sửa `SecurityConfig`: thêm filter T015 trước `UsernamePasswordAuthenticationFilter`; `requestMatchers("/api/tutor/verification/**").hasAuthority("ROLE_TUTOR")`, `requestMatchers("/api/academic/verifications/**").hasAuthority("ROLE_ACADEMIC_OFFICER")` đặt trước `anyRequest().permitAll()`; gắn handler T014; giữ nguyên mọi quy tắc khác tại `BE/core/security/SecurityConfig.java`
- [X] T017 Tạo `CurrentUserProvider` (lấy `User` hiện tại từ `SecurityContext`, ném lỗi 401 nếu thiếu) tại `VER/service/CurrentUserProvider.java`

### Lỗi, nhãn, DTO dùng chung

- [X] T018 [P] Tạo `VerificationException` (mã lỗi + HTTP status + message tiếng Việt + `fields` tùy chọn) và các factory theo bảng lỗi contract (`locked`, `alreadyProcessed`, `incomplete`, `invalidFile`, `notFound`, `validation`, `commitmentRequired`) tại `VER/service/VerificationException.java`
- [X] T019 [P] Tạo `ApiErrorResponse` (`code`, `message`, `fields`, `missingDocuments`) tại `VER/dto/response/ApiErrorResponse.java`
- [X] T020 Tạo `VerificationExceptionHandler` (`@RestControllerAdvice(basePackages = "com.kizuna.features.verification.controller")`, đặt `@Order` cao hơn `core/exception/GlobalExceptionHandler`): ánh xạ `VerificationException`, `MethodArgumentNotValidException` → 400, `MaxUploadSizeExceededException` → 413 MSG13, lỗi khác → 500 "Hệ thống đang bận, bạn vui lòng thử lại sau." và log nội bộ, tại `VER/controller/VerificationExceptionHandler.java`
- [X] T021 [P] Tạo `VerificationLabels` ánh xạ mã → nhãn tiếng Việt cho trạng thái hồ sơ, trạng thái giấy tờ, loại giấy tờ, hành động lịch sử, lý do từ chối (theo spec và contract) tại `VER/service/VerificationLabels.java`

### Lưu tệp và state machine

- [X] T022 Tạo `VerificationDocumentStorage`: `store(MultipartFile)` kiểm tra kích thước ≤ 10.485.760 byte, đuôi, chữ ký byte, `ImageIO.read` cho ảnh, `%%EOF` cho PDF, ghi tên `UUID + đuôi` vào `app.verification.storage-dir`, đăng ký xóa tệp mới khi rollback; `scheduleDeleteAfterCommit(key)`; `open(key)` từ chối khóa chứa `/`, `\`, `..` và kiểm tra đường dẫn chuẩn hóa nằm trong thư mục gốc; `contentType(key)` tại `VER/service/VerificationDocumentStorage.java`
- [X] T023 Tạo `VerificationStateMachine`: bảng chuyển hợp lệ theo data-model §4; `isEditable(status)`; `transition(tutor, action, officer, feedback, reasonCode, internalNote)` cập nhật `verification_status`, `tutor_state` (giữ `ACTIVE_PAID` khi phê duyệt), `submitted_at`/`verified_at`/`verification_feedback`, trạng thái giấy tờ, và lưu đúng một `TutorVerificationHistory`; trạng thái nguồn sai → `VerificationException` tại `VER/service/VerificationStateMachine.java`
- [ ] T024 [P] ~~Tạo `AsyncConfig` với `@EnableAsync`~~ — Hủy khi triển khai: `EmailServiceImpl` (OTP, FE-01) đã có `@Async` nhưng đang chạy đồng bộ vì dự án chưa bật `@EnableAsync`; bật toàn cục sẽ đổi hành vi gửi OTP (ngoài phạm vi). Thay bằng executor riêng do `VerificationNotificationService` tự tạo và đóng (T047)

### Test nền tảng

- [X] T025 [P] Unit test `VerificationStateMachine`: mọi chuyển hợp lệ, mọi chuyển cấm, đồng bộ `tutor_state`, đúng một lịch sử mỗi chuyển (SC-006) tại `BT/features/verification/VerificationStateMachineTest.java`
- [X] T026 [P] Unit test `VerificationDocumentStorage`: tệp quá 10MB, `.exe` đổi đuôi `.pdf`, PNG hỏng, PDF thiếu `%%EOF`, khóa có `..`, tệp hợp lệ (dùng `@TempDir`) tại `BT/features/verification/VerificationDocumentStorageTest.java`
- [X] T027 [P] Test `JwtAuthenticationFilter` + `SecurityConfig` bằng MockMvc: không token → 401 trên route FE-03; vai trò sai → 403; tài khoản `SUSPENDED` → 401; route ngoài FE-03 (ví dụ `/api/tutors`) vẫn truy cập được không token, tại `BT/features/verification/VerificationSecurityTest.java`

**Checkpoint**: Nền tảng xong — các user story có thể bắt đầu.

---

## Phase 3: User Story 1 — Gia sư chuẩn bị và nộp hồ sơ xác minh (Priority: P1) 🎯 MVP

**Goal**: Gia sư xem trạng thái, nhập thông tin, quản lý giấy tờ, lưu nháp và nộp hồ sơ.

**Independent Test**: Quickstart kịch bản 1–6.

### Tests for User Story 1

- [X] T028 [P] [US1] Unit test `TutorVerificationServiceImpl`: lưu nháp khi có thể chỉnh sửa; 409 khi `PENDING`/`APPROVED`; nộp thiếu trường/thiếu nhóm giấy tờ → `SUBMISSION_INCOMPLETE` có `fields`/`missingDocuments`; thiếu cam kết; nộp thành công ghi `SUBMIT` tại `BT/features/verification/TutorVerificationServiceTest.java`
- [X] T029 [P] [US1] `@WebMvcTest` hợp đồng JSON của `TutorVerificationController` (GET, PUT, POST/PUT/DELETE giấy tờ, submit, tải tệp) theo contract; phản hồi không chứa `storageKey` tại `BT/features/verification/TutorVerificationControllerTest.java`
- [X] T030 [US1] Test tích hợp tùy chọn (profile `verification-test`, `@EnabledIfEnvironmentVariable(named = "VERIFICATION_TEST_DB_URL", …)` — hiện sẽ bị bỏ qua theo phương án DB 3): hai request nộp song song chỉ tạo một `SUBMIT` (FR-008) tại `BT/features/verification/TutorSubmissionConcurrencyIT.java`; bổ sung unit test xác nhận service luôn gọi `findLockedByUserId` (không gọi bản không khóa) trước khi đổi trạng thái

### Implementation for User Story 1

- [X] T031 [P] [US1] Tạo request DTO `UpdateVerificationProfileRequest` (validation định dạng theo data-model §1, mọi trường tùy chọn), `SubmitVerificationRequest` (`commitment`) tại `VER/dto/request/`
- [X] T032 [P] [US1] Tạo response DTO `TutorVerificationResponse`, `VerificationProfileDto`, `VerificationDocumentResponse`, `LatestFeedbackDto` theo contract (không có trường ghi chú nội bộ) tại `VER/dto/response/`
- [X] T033 [US1] Tạo interface `TutorVerificationService` (`getMyVerification`, `updateProfile`, `addDocument`, `updateDocument`, `removeDocument`, `loadMyDocumentFile`, `submit`) tại `VER/service/TutorVerificationService.java`
- [X] T034 [US1] Hiện thực `TutorVerificationServiceImpl`: khóa tutor bằng `findLockedByUserId`; kiểm tra `isEditable`; cập nhật `users.full_name` và các cột `tutors`; giấy tờ mới/thay tệp → `PENDING`; xóa tệp cũ sau commit; tính `missingRequirements`; `submit` kiểm tra đủ trường bắt buộc + 2 nhóm giấy tờ có tệp tồn tại + cam kết rồi gọi state machine; `fileAvailable` theo sự tồn tại tệp tại `VER/service/impl/TutorVerificationServiceImpl.java`
- [X] T035 [US1] Tạo `TutorVerificationController` tại `/api/tutor/verification` với 7 endpoint phía gia sư theo contract; endpoint tải tệp đặt `Content-Disposition: inline`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` tại `VER/controller/TutorVerificationController.java`
- [X] T036 [P] [US1] Bổ sung endpoint FE-03 vào `frontend/js/config/api.config.js`
- [X] T037 [US1] Bổ sung vào `frontend/js/api.js` helper gọi API kèm `Authorization: Bearer <getAuthToken()>` cho JSON, multipart và tải blob; 401 → xóa phiên và chuyển `/login`; trả `message` tiếng Việt từ `ApiErrorResponse`; không sửa hành vi các hàm hiện có
- [X] T038 [P] [US1] Thêm alias `/tutor/verification` → `/pages/tutor-verification/index.html` trong `frontend/server.js`
- [X] T039 [US1] Tạo giao diện trang xác minh gia sư theo `DESIGN.md` mục 7 (bo 5px, viền đen 1px, bóng cứng, CTA tím `#7A2CA8`, font Itim/Segoe UI): thẻ trạng thái, khối "Bắt đầu xác minh" khi chưa nộp, form thông tin, danh sách giấy tờ + vùng tải lên, ô cam kết, nút "Lưu thay đổi" (outlined) và "Nộp hồ sơ" (CTA chính), vùng tải/rỗng/lỗi + "Thử lại" tại `frontend/pages/tutor-verification/index.html`
- [X] T040 [US1] Hiện thực logic trang: tải trạng thái, chế độ chỉ đọc khi không thể chỉnh sửa, lưu nháp, kiểm tra tệp phía client (đuôi, 10MB) trước khi gửi, đánh dấu trường lỗi từ `fields`, hộp xác nhận nộp, hiển thị MSG12/MSG13, chống bấm nộp hai lần (vô hiệu nút khi đang gửi), xem tệp qua `URL.createObjectURL` tại `frontend/pages/tutor-verification/tutor-verification.js`

**Checkpoint**: US1 chạy độc lập — gia sư nộp được hồ sơ.

---

## Phase 4: User Story 2 — Cán bộ học vụ thẩm định và ra quyết định (Priority: P1)

**Goal**: Bảng điều khiển, hàng đợi, chi tiết, xem giấy tờ, phê duyệt/từ chối, email.

**Independent Test**: Quickstart kịch bản 7–9, 12, 13, 15, 17.

### Tests for User Story 2

- [X] T041 [P] [US2] Unit test `VerificationReviewServiceImpl`: phê duyệt/từ chối khi `PENDING`; 409 `ALREADY_PROCESSED` khi trạng thái khác; từ chối thiếu `reasonCode`/`internalNote` → 400; phê duyệt giữ `ACTIVE_PAID`; email chỉ đăng ký sau commit tại `BT/features/verification/VerificationReviewServiceTest.java`
- [X] T042 [P] [US2] `@WebMvcTest` hợp đồng `VerificationReviewController` (summary, danh sách có `group`/`keyword`/khoảng ngày/phân trang, chi tiết, tải tệp sai `tutorId` → 404, approve, reject) tại `BT/features/verification/VerificationReviewControllerTest.java`
- [ ] T043 [US2] Test tích hợp tùy chọn (profile `verification-test`, bỏ qua khi thiếu biến môi trường như T030): hai quyết định đồng thời trên một hồ sơ chỉ một thành công, một lịch sử (SC-005); truy vấn tóm tắt theo tháng/ngày đúng múi giờ `Asia/Ho_Chi_Minh` tại `BT/features/verification/ReviewDecisionConcurrencyIT.java`; bổ sung unit test xác nhận `approve`/`reject`/`requestInfo` dùng `findLockedByTutorId` và từ chối khi trạng thái đã đổi

### Implementation for User Story 2

- [X] T044 [P] [US2] Tạo request DTO `RejectVerificationRequest` (`reasonCode` bắt buộc, `feedback` ≤ 2.000, `internalNote` 1–2.000) và query `VerificationQueueQuery` (`group`, `keyword` ≤ 100, `submittedFrom`, `submittedTo`, `page`, `size` ≤ 50) tại `VER/dto/request/`
- [X] T045 [P] [US2] Tạo response DTO `VerificationSummaryResponse`, `VerificationQueueItemResponse`, `PageResponse<T>`, `VerificationDetailResponse`, `DecisionResultResponse` theo contract tại `VER/dto/response/`
- [X] T046 [US2] Tạo `VerificationQueueSpecification` (JPA Specification: lọc nhóm trạng thái, `lower(full_name)`/`lower(email)` LIKE có escape `%`/`_`, khoảng `submitted_at` theo ngày `Asia/Ho_Chi_Minh`, sắp xếp `submitted_at ASC NULLS LAST`) và cho `VerificationTutorRepository` kế thừa `JpaSpecificationExecutor` tại `VER/repository/VerificationQueueSpecification.java`
- [X] T047 [US2] Tạo `VerificationNotificationService`: `@Async("verificationMailExecutor")` gửi email tiếng Việt cho phê duyệt và từ chối (kèm nhãn lý do + phản hồi, không kèm ghi chú nội bộ) bằng `JavaMailSender`; bắt mọi lỗi và log nội bộ (FR-022); phương thức `afterCommit(...)` đăng ký `TransactionSynchronization` tại `VER/service/VerificationNotificationService.java`
- [X] T048 [US2] Tạo interface `VerificationReviewService` (`getSummary`, `getQueue`, `getDetail`, `loadDocumentFile`, `approve`, `reject`) tại `VER/service/VerificationReviewService.java`
- [X] T049 [US2] Hiện thực `VerificationReviewServiceImpl`: tóm tắt theo data-model §5; hàng đợi qua T046; chi tiết (hồ sơ `UNVERIFIED` → 404); tải tệp kiểm tra giấy tờ thuộc `tutorId`; `approve`/`reject` khóa dòng bằng `findLockedByTutorId`, gọi state machine với officer hiện tại, đăng ký email sau commit tại `VER/service/impl/VerificationReviewServiceImpl.java`
- [X] T050 [US2] Tạo `VerificationReviewController` tại `/api/academic/verifications` với endpoint `summary`, danh sách, chi tiết, tải tệp, `approve`, `reject` theo contract tại `VER/controller/VerificationReviewController.java`
- [X] T051 [P] [US2] Thêm alias `/academic/verifications`, `/academic/dashboard` → `/pages/academic-verifications/index.html` và `/academic/verifications/detail` → `/pages/academic-verification-detail/index.html` trong `frontend/server.js`
- [X] T052 [P] [US2] Tạo giao diện bảng điều khiển thẩm định theo bảng màu `FRONTEND_RULES.md` mục 5: thẻ tóm tắt (đang chờ + mới hôm nay, cần bổ sung, đã duyệt/từ chối trong tháng), tab nhóm, ô tìm kiếm, lọc khoảng ngày, bảng hàng đợi, phân trang, vùng tải/rỗng/lỗi tại `frontend/pages/academic-verifications/index.html`
- [X] T053 [US2] Hiện thực logic bảng điều khiển: gọi summary + danh sách, debounce tìm kiếm 300ms, định dạng `dd/MM/yyyy`, MSG01 khi lọc không có kết quả, "Hiện không còn hồ sơ nào chờ thẩm định." khi nhóm chờ rỗng, liên kết sang trang chi tiết tại `frontend/pages/academic-verifications/academic-verifications.js`
- [X] T054 [P] [US2] Tạo giao diện chi tiết hồ sơ: thông tin cá nhân, giới thiệu, kinh nghiệm, JLPT, danh sách giấy tờ với khung xem PDF/ảnh, cụm nút "Phê duyệt" (CTA chính), "Yêu cầu bổ sung", "Từ chối" (outlined), hộp thoại xác nhận phê duyệt và hộp thoại từ chối (chọn lý do, phản hồi, ghi chú nội bộ) tại `frontend/pages/academic-verification-detail/index.html`
- [X] T055 [US2] Hiện thực logic chi tiết: đọc `tutorId` từ query, tải chi tiết, tải tệp qua blob và hiển thị "Không thể hiển thị tệp này. Tệp có thể bị lỗi hoặc không được hỗ trợ." khi lỗi, gửi phê duyệt/từ chối, kiểm tra phía client thông điệp UC-28 AF-1, xử lý 409 bằng thông điệp GB-01 và nút tải lại, ẩn cụm quyết định khi hồ sơ không còn `PENDING` tại `frontend/pages/academic-verification-detail/academic-verification-detail.js`

**Checkpoint**: US1 + US2 cho vòng nộp → phê duyệt/từ chối hoàn chỉnh.

---

## Phase 5: User Story 3 — Yêu cầu bổ sung và gia sư nộp lại (Priority: P2)

**Goal**: Cán bộ trả hồ sơ kèm hướng dẫn; gia sư thấy phản hồi, sửa và nộp lại (kể cả sau khi bị từ chối).

**Independent Test**: Quickstart kịch bản 10, 11, 13.

- [X] T056 [P] [US3] Bổ sung test: `requestInfo` rỗng → 400 với thông điệp UC-26 AF-1; hợp lệ → `ADDITIONAL_INFO_REQUIRED`, lịch sử `REQUEST_INFO`, email; gia sư nộp lại từ `ADDITIONAL_INFO_REQUIRED` và `REJECTED` → `PENDING` với mục `SUBMIT` mới, trong `BT/features/verification/VerificationReviewServiceTest.java` và `BT/features/verification/TutorVerificationServiceTest.java`
- [X] T057 [P] [US3] Tạo `RequestInfoRequest` (`message` 1–2.000 sau trim) tại `VER/dto/request/RequestInfoRequest.java`
- [X] T058 [US3] Thêm `requestInfo` vào `VerificationReviewService` và `VerificationReviewServiceImpl` (khóa dòng, state machine, email yêu cầu bổ sung kèm nội dung) tại `VER/service/`
- [X] T059 [US3] Thêm email yêu cầu bổ sung vào `VER/service/VerificationNotificationService.java`
- [X] T060 [US3] Thêm endpoint `POST /{tutorId}/request-info` trả "Đã gửi yêu cầu bổ sung thông tin." vào `VER/controller/VerificationReviewController.java`
- [X] T061 [US3] Thêm nút và hộp thoại "Yêu cầu bổ sung" (ô nội dung bắt buộc, đếm ký tự 2.000) vào `frontend/pages/academic-verification-detail/index.html` và logic gửi vào `frontend/pages/academic-verification-detail/academic-verification-detail.js`
- [X] T062 [US3] Trang gia sư: khối phản hồi hiển thị nội dung yêu cầu bổ sung hoặc lý do từ chối kèm lối tắt cuộn tới form cập nhật; mở khóa form ở `ADDITIONAL_INFO_REQUIRED`/`REJECTED`; nhãn nút "Nộp lại hồ sơ", trong `frontend/pages/tutor-verification/index.html` và `frontend/pages/tutor-verification/tutor-verification.js`

**Checkpoint**: Vòng bổ sung/nộp lại hoạt động độc lập trên nền US1 + US2.

---

## Phase 6: User Story 4 — Gia sư theo dõi trạng thái và phản hồi (Priority: P2)

**Goal**: Minh bạch trạng thái, ngày nộp, bản tóm tắt chỉ đọc, phản hồi.

**Independent Test**: Mở trang ở từng trạng thái trong 5 trạng thái; kiểm tra nhãn, ngày, phản hồi, rỗng, lỗi (quickstart 1, 11, 18).

- [X] T063 [P] [US4] Bổ sung test `TutorVerificationControllerTest`: mỗi trạng thái trả `statusLabel` đúng bảng ánh xạ; `latestFeedback` null khi chưa có phản hồi; JSON không có khóa `internalNote`/`notes` trong mọi trạng thái (GB-02, SC-004) tại `BT/features/verification/TutorVerificationControllerTest.java`
- [X] T064 [US4] Hoàn thiện khối trạng thái trên trang gia sư: nhãn + màu theo trạng thái (Đã phê duyệt dùng xanh `#1E8B3F`, Bị từ chối dùng `#BF360C`), ngày nộp/ngày duyệt `dd/MM/yyyy`, bản tóm tắt chỉ đọc khi không thể chỉnh sửa, "Hiện chưa có phản hồi nào cho hồ sơ của bạn." khi chưa có phản hồi, thông báo lỗi thân thiện + nút "Thử lại" tại `frontend/pages/tutor-verification/tutor-verification.js` và `frontend/pages/tutor-verification/index.html`

**Checkpoint**: Gia sư theo dõi được mọi trạng thái.

---

## Phase 7: User Story 5 — Cán bộ học vụ xem lịch sử thẩm định (Priority: P3)

**Goal**: Lịch sử gần đây trên bảng điều khiển và lịch sử đầy đủ của một hồ sơ.

**Independent Test**: Quickstart kịch bản 14.

- [ ] T065 [P] [US5] Bổ sung test: `history` sắp xếp tăng dần, có `officerName`, `feedback`, `internalNote`, `reasonLabel`; `recent-decisions` chỉ gồm APPROVE/REJECT/REQUEST_INFO, tối đa `limit` trong `BT/features/verification/VerificationReviewControllerTest.java`
- [ ] T066 [P] [US5] Tạo `VerificationHistoryItemResponse`, `RecentDecisionResponse` tại `VER/dto/response/`
- [X] T067 [US5] Thêm `getHistory(tutorId)`, `getRecentDecisions(limit)` vào `VerificationReviewService` và `VerificationReviewServiceImpl` tại `VER/service/`
- [X] T068 [US5] Thêm endpoint `GET /{tutorId}/history` và `GET /recent-decisions?limit=10` (limit 1–50) vào `VER/controller/VerificationReviewController.java`
- [ ] T069 [US5] Thêm khối "Lịch sử thẩm định gần đây" (tên gia sư, quyết định + lý do, ngày, người thẩm định, liên kết "Xem toàn bộ lịch sử") vào `frontend/pages/academic-verifications/index.html` và `frontend/pages/academic-verifications/academic-verifications.js`
- [X] T070 [US5] Thêm khối dòng thời gian lịch sử đầy đủ (ngày, hành động, phản hồi, ghi chú nội bộ, cán bộ; "Gia sư" cho mục nộp; "Chưa có lịch sử thẩm định." khi rỗng; mở bằng neo `#history` từ liên kết của T069) vào `frontend/pages/academic-verification-detail/index.html` và `frontend/pages/academic-verification-detail/academic-verification-detail.js`

**Checkpoint**: Mọi user story hoạt động độc lập.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [ ] T071 [P] Rà soát chuỗi giao diện 3 trang FE-03: không in hoa toàn bộ, không lộ mã enum/HTTP, sentence case cho thông báo (FR-021, SC-007); rà soát file mới là UTF-8 không BOM
- [ ] T072 [P] Kiểm tra responsive 1240px / 768px / 375px, không cuộn ngang, console không lỗi trên 3 trang
- [ ] T073 Chạy `cd backend; .\mvnw.cmd test -Dtest="*Verification*"` (unit + `@WebMvcTest`, không chạm DB); ghi kết quả thực tế và liệt kê test `*IT` bị bỏ qua vào "Kết quả kiểm chứng"
- [ ] T074 Nghiệm thu thủ công kịch bản 1–18 trong [quickstart.md](quickstart.md) trên Supabase bằng tài khoản thử nghiệm; bấm giờ lần nộp đầu tiên của một người chưa từng dùng trang để đối chiếu SC-001 (< 10 phút); ghi kết quả từng kịch bản và danh sách tài khoản/hồ sơ thử nghiệm đã tạo để nhóm dọn dẹp
- [ ] T075 Ghi SC-003 (p95 ≤ 2 giây với 1.000 gia sư, 100 người dùng đồng thời) là **chưa đo** theo phương án DB 3 (không sinh 1.000 gia sư vào DB dùng chung — Nguyên tắc II); chỉ ghi thời gian phản hồi quan sát được khi nghiệm thu thủ công, nói rõ không phải số đo tải
- [ ] T076 Ghi giới hạn còn lại vào "Kết quả kiểm chứng": khóa dòng/cạnh tranh (SC-005, FR-008) và SC-003 chưa kiểm chứng trên DB do không có DB kiểm thử riêng; tệp mồ côi khi tiến trình chết giữa chừng; phụ thuộc FE-01 (đăng ký đang đặt `APPROVED`, A-11); `Tutor` Java mặc định lệch DDL; không quét mã độc

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: không phụ thuộc.
- **Foundational (Phase 2)**: phụ thuộc Phase 1 (T013 cần T001 và người phụ trách chạy migration trên Supabase). Chặn mọi user story.
- **US1 (Phase 3)**: sau Phase 2.
- **US2 (Phase 4)**: sau Phase 2; về mặt dữ liệu cần hồ sơ `PENDING` — có thể tạo bằng API của US1 hoặc trực tiếp trên DB kiểm thử, nên lập trình song song với US1 được. `frontend/js/api.js` (T037) phải xong trước T053, T055.
- **US3 (Phase 5)**: sau US1 + US2 (dùng chung service, controller, trang của cả hai).
- **US4 (Phase 6)**: sau US1; nên làm sau US3 để có dữ liệu phản hồi thật.
- **US5 (Phase 7)**: sau US2.
- **Polish (Phase 8)**: sau các story muốn phát hành.

### Within Each User Story

- Test viết trước và phải thất bại trước khi hiện thực.
- DTO → service interface → service impl → controller → trang HTML → logic JS.
- Các task sửa cùng một file (ví dụ `VerificationReviewController.java`, `academic-verification-detail.js`) chạy tuần tự giữa các story.

### Parallel Opportunities

- Phase 1: T003, T004, T005 song song.
- Phase 2: T006–T011 song song; T014, T018, T019, T021, T024 song song; T025–T027 song song sau khi có lớp được test.
- US1: T028, T029 song song; T031, T032, T036, T038 song song.
- US2: T041, T042 song song; T044, T045, T051, T052, T054 song song.
- US1 và US2 có thể do hai người làm song song sau Phase 2.

---

## Parallel Example: User Story 1

```text
# Test trước:
T028 TutorVerificationServiceTest.java
T029 TutorVerificationControllerTest.java

# DTO, cấu hình frontend cùng lúc:
T031 dto/request/*   T032 dto/response/*   T036 api.config.js   T038 server.js
```

## Parallel Example: User Story 2

```text
T041 VerificationReviewServiceTest.java   T042 VerificationReviewControllerTest.java
T044 dto/request/*   T045 dto/response/*   T051 server.js   T052 academic-verifications/index.html   T054 academic-verification-detail/index.html
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Phase 1 → Phase 2 (T013: người phụ trách chạy migration bằng DBeaver, agent xác nhận mapping khi khởi động backend).
2. US1 → kiểm thử quickstart 1–6.
3. US2 → kiểm thử quickstart 7–9, 12–13, 15–17. Hai story P1 này tạo vòng onboarding tối thiểu (nộp → quyết định); chỉ US1 thì chưa có giá trị nghiệp vụ.
4. Dừng, nghiệm thu, demo.

### Incremental Delivery

US3 (bổ sung/nộp lại) → US4 (theo dõi) → US5 (lịch sử) → Polish. Mỗi bước kiểm thử theo kịch bản tương ứng trong quickstart trước khi sang bước sau.

---

## Kết quả kiểm chứng

_Điền khi triển khai (T013, T073–T076). Không đánh dấu hoàn tất khi chưa chạy._

### 2026-11-03 — Phase 1–2

- **Test nền tảng** (`mvnw -o test -Dtest="Verification*Test"`, không chạm DB): `VerificationStateMachineTest` 16/16, `VerificationDocumentStorageTest` 8/8, `VerificationSecurityTest` 7/7 — đạt.
- **T013 — migration**: người phụ trách chạy script trên Supabase bằng DBeaver; truy vấn `information_schema.columns` cho thấy `feedback text NULL` và `reason_code character varying NULL` trong `tutor_verification_histories`, các cột cũ giữ nguyên.
- **T013 — khởi động backend** (`mvnw -o spring-boot:run` trên Supabase): `Started KizunaBackendApplication in 10.238 seconds`, không lỗi tạo bean; các truy vấn JPQL `@Query` của repository FE-03 được Spring Data kiểm tra khi khởi động và hợp lệ. Giới hạn: `ddl-auto=none` nên Hibernate không đối chiếu từng cột; việc đọc thật các cột của `tutor_credentials`/`tutor_verification_histories` sẽ được xác nhận ở lần gọi `GET /api/tutor/verification` đầu tiên (US1).

### 2026-11-03 — US1 (gia sư nộp hồ sơ)

- **Test tự động** (`mvnw -o test -Dtest="Verification*Test,TutorVerification*Test"`, không chạm DB): 54/54 đạt — thêm `TutorVerificationServiceTest` 12/12, `TutorVerificationControllerTest` 11/11.
- **T030** `TutorSubmissionConcurrencyIT`: đã viết và biên dịch; **chưa chạy** (phương án DB 3, thiếu `VERIFICATION_TEST_DB_URL`). FR-008 trên DB thật chưa kiểm chứng; unit test xác nhận service luôn dùng `findLockedByUserId`.
- **Chạy thử chỉ đọc trên Supabase** (đăng nhập `tutor@kizuna.vn`, `GET /api/tutor/verification`): trả 200, nhãn tiếng Việt, đọc đúng cột của `tutors`/`tutor_credentials` → hoàn tất phần còn lại của T013. Tài khoản gia sư gọi `/api/academic/verifications/summary` → 403; `/api/tutors/search` vẫn 200 không cần token; preflight CORS từ `http://localhost:3000` → 200.
- **Phát hiện khi chạy thử**: `tutor_credentials` trên Supabase có dữ liệu nhập trước FE-03, `file_url` là URL ảnh bên ngoài (không có trong repo). Đã xử lý: `CredentialTypeConverter` đọc giá trị loại giấy tờ lạ thành `OTHER`; tên tệp hiển thị không ghép đuôi từ URL; các giấy tờ này hiện `fileAvailable=false` và thông điệp "Không thể hiển thị tệp này…".
- **Chưa kiểm chứng**: giao diện `/tutor/verification` chưa chạy trên trình duyệt (cần nghiệm thu thủ công T074 với tài khoản gia sư ở trạng thái chưa nộp); thao tác ghi (lưu nháp, tải tệp, nộp) chưa chạy trên Supabase.

### 2026-11-03 — Bổ sung theo yêu cầu người dùng (ngoài đặc tả FE-03)

- **Sửa lỗi**: trang xác minh mở qua alias bị kẹt ở trạng thái tải vì script dùng đường dẫn tương đối (`./tutor-verification.js` → `/tutor/tutor-verification.js` 404); đổi sang đường dẫn tuyệt đối.
- **`GlobalExceptionHandler`**: `NoResourceFoundException` trả 404 "Không tìm thấy trang bạn yêu cầu" thay vì 500.
- **Dashboard gia sư** `/tutor/dashboard` (`frontend/pages/tutor-dashboard/`): chỉ dùng dữ liệu thật từ `GET /api/tutor/verification` (họ tên, trạng thái, JLPT, kinh nghiệm, số giấy tờ, ngày nộp, phản hồi). Lịch dạy/lớp học/học viên và gói hội viên chưa có API → ghi rõ "đang được phát triển", không hiển thị số liệu. Thêm `styles/tutor-retro.css` và `components/tutor-layout/tutor-header.js` dùng chung cho khu vực gia sư; route `/tutor`, `/tutor/dashboard`, `/tutor/verification` ở cả `WebMvcConfig` và `frontend/server.js`.
- **Kiểm tra**: các trang và tệp tĩnh trả 200 qua backend 8085; chưa kiểm tra hiển thị trên trình duyệt bởi agent.
- **Giao diện theo thiết kế Stitch** (người dùng cung cấp `code.html` "Tutor Dashboard"): dashboard dựng lại theo bố cục Stitch (sidebar 10 mục đúng thứ tự, thanh trên, thẻ chào, 4 thẻ số liệu, thao tác nhanh, lưới 7/5 cột, dải hướng dẫn, footer). Theo chỉ đạo "không cần đúng số liệu, chỉ cần giống sidebar": số liệu giả trong bản thiết kế không được chép — 4 thẻ dùng dữ liệu xác minh thật; lịch dạy/bài tập/yêu cầu kết nối/học viên hiển thị "Chưa có dữ liệu"; "Hoạt động gần đây" dựng từ mốc xác minh thật; mục sidebar chưa có trang hiện toast "đang được phát triển". "Hồ sơ gia sư" → `/tutor/verification`. Trang xác minh dùng chung sidebar/thanh trên và theme mới. Thay `tutor-retro.css`/`tutor-header.js` bằng `styles/tutor-theme.css`, `components/tutor-layout/{tailwind-theme.js,tutor-layout.js}`. Ngoại lệ Nguyên tắc IV ghi trong plan.md.

### 2026-11-03 — US2 + US3 (yêu cầu bổ sung) + lịch sử US5, giao diện theo Stitch "Cổng học vụ"

- **Backend**: `VerificationReviewController` (`/api/academic/verifications`: summary, hàng đợi, chi tiết, tải tệp, lịch sử, approve, reject, request-info), `VerificationReviewServiceImpl` (khóa dòng mọi quyết định), `VerificationQueueSpecification`, `VerificationNotificationService` (email sau commit, executor riêng, lỗi chỉ ghi log). Hợp đồng hàng đợi đổi `group` → `status` + `jlptLevel` + `sort`; summary bổ sung số liệu cho thiết kế (đã cập nhật contracts).
- **Test** (`mvnw -o test -Dtest="Verification*Test,TutorVerification*Test"`, không chạm DB): 73/73 đạt — thêm `VerificationReviewServiceTest` 12/12, `VerificationReviewControllerTest` 7/7.
- **Chạy thử chỉ đọc trên Supabase** (`academic@kizuna.vn`): summary, hàng đợi (lọc từ khóa/JLPT/khoảng ngày, `jlptLevel=N9` → 400), chi tiết, lịch sử đều trả đúng; các trang `/academic/dashboard`, `/academic/verifications`, `/academic/verifications/detail` trả 200. **Chưa chạy** phê duyệt/từ chối/yêu cầu bổ sung trên DB thật (ghi dữ liệu) và chưa kiểm chứng gửi email thật.
- **Giao diện**: theo thiết kế Stitch người dùng gửi; số liệu giả trong bản thiết kế (SLA 48h, ca làm việc, mã hồ sơ KZ-TUT, thời gian duyệt trung bình, tỷ lệ đạt vòng 1, "cần quyết định cuối cùng/phỏng vấn") được thay bằng chỉ số thật tương ứng hoặc bỏ (A-2, A-3, OOS-03). Nhãn trạng thái tiếng Anh trong bộ lọc của thiết kế được bỏ.
- **Chưa làm**: T043 (IT cạnh tranh — phương án DB 3), T065/T066/T069 (API + khối "lịch sử thẩm định gần đây" trên bảng điều khiển; mục sidebar "Lịch sử xét duyệt" đang báo "đang được phát triển").

### 2026-11-03 — Sửa đổi quy tắc: "Bị từ chối" là quyết định cuối cùng

- **Quyết định của người phụ trách** (khi nghiệm thu): hồ sơ bị từ chối không được chỉnh sửa/nộp lại; chỉ "Cần bổ sung thông tin" mới nộp lại được. Thay câu trả lời Q1 cũ; khớp UC-28 POST-2. Đã cập nhật spec (Clarifications, US3-5, edge cases, bảng trạng thái, chuyển trạng thái), data-model §4, contracts (lỗi 409 `VERIFICATION_REJECTED`), quickstart kịch bản 13–14.
- **Code**: `VerificationStateMachine.EDITABLE` bỏ `REJECTED`; `requireEditable` trả `VERIFICATION_REJECTED` với thông điệp riêng; email từ chối bỏ câu mời nộp lại; giao diện gia sư (chỉ đọc + thông điệp cuối cùng, nút "Nộp lại" chỉ cho "Cần bổ sung"), dashboard gia sư, dashboard học vụ, hộp thoại từ chối có cảnh báo "quyết định cuối cùng, dùng Yêu cầu bổ sung nếu chỉ thiếu thông tin".
- **Test**: 74/74 đạt (thêm `rejectedProfileIsFinalAndCannotBeResubmitted`, `rejectedProfileCannotBeEditedOrResubmitted`; chuỗi lịch sử thay "từ chối → nộp lại" bằng "yêu cầu bổ sung → nộp lại").
