# Research: FE-03 — Tutor Verification

Ngày: 2026-11-03. Căn cứ: [spec.md](spec.md) (3 câu trả lời làm rõ, A-1…A-11), constitution 1.0.0 và mã nguồn trên nhánh `han` tại thời điểm lập kế hoạch. Đây là quyết định thiết kế, chưa phải kết quả chạy hệ thống.

## 0. Hiện trạng mã nguồn liên quan

| Thành phần | Hiện trạng | Hệ quả cho FE-03 |
| --- | --- | --- |
| `core/entity/Tutor` | Đã map `verification_status`, `tutor_state`, `phone`, `date_of_birth`, `address`, `verification_feedback`, `submitted_at`, `verified_at`. `@Builder.Default` của `verificationStatus` là `APPROVED` (DB mặc định `UNVERIFIED`). | Dùng làm "hồ sơ xác minh" (A-1). Không đổi giá trị mặc định (thuộc FE-01, A-11). |
| `features/verification/entity/TutorCredential` | Chỉ có khóa, `@Column(name = "credentialId")` sai tên cột, kế thừa `BaseEntity` trong khi bảng không có `created_at`/`updated_at`. | Hoàn thiện mapping theo `tutor_credentials`; bỏ kế thừa `BaseEntity`. |
| `features/verification/entity/TutorVerificationHistory` | Tương tự: sai tên cột, kế thừa `BaseEntity`. | Hoàn thiện mapping theo `tutor_verification_histories`; bỏ kế thừa `BaseEntity`. |
| `features/verification/controller`, `repository` | Thư mục rỗng; chưa có `service`, `dto`. | Tạo mới trong đúng feature `verification`. |
| `core/security/SecurityConfig` | `csrf` tắt, `anyRequest().permitAll()`; không có filter JWT. `JwtTokenProvider` đã sinh token chứa `userId`, `email`, `role`. | FE-03 phải bổ sung xác thực theo token (mục 3). |
| `features/auth/controller/AuthController`, `AuthServiceImpl` | Tạo `Tutor` với `verificationStatus(APPROVED)` khi đăng ký. | Ngoài phạm vi; ghi nhận phụ thuộc FE-01 (A-11). |
| `tutor_search/.../TutorSearchSpecification` | Lọc `verificationStatus = APPROVED` (GB-06). | FE-03 cung cấp trạng thái; không sửa tìm kiếm. |
| `frontend/` | Trang tĩnh ES module, dev server Node cổng 3000; `js/api.js` lưu token ở `localStorage` (`kizuna_token`) nhưng wrapper fetch chưa gắn header `Authorization`. Chưa có trang gia sư/cán bộ học vụ; login chuyển cán bộ học vụ tới `/academic/dashboard` chưa tồn tại. | Thêm helper gọi API có token; tạo trang mới. |
| Kiểm thử | JUnit 5, spring-security-test có sẵn; hai test hiện có chạy `@SpringBootTest` vào DB cấu hình chung (Supabase). | Test FE-03 không được ghi vào DB dùng chung (mục 7). |

Chênh lệch nguồn schema: constitution coi `DatabasePgAdmin4.txt` là nguồn schema; Java entity `Tutor` lệch giá trị mặc định so với DDL (`APPROVED`/`PREMIUM` so với `UNVERIFIED`/`FREE`). FE-03 không sửa vì thuộc FE-01/FE-09, chỉ ghi nhận.

Chênh lệch constitution ↔ thực tế: Nguyên tắc III nêu HTML/Thymeleaf, nhưng các feature gần đây (FE-04) dùng trang tĩnh trong `frontend/` gọi REST `/api/...` theo `FRONTEND_RULES.md`. FE-03 theo thực tế này để không tạo kiến trúc song song.

## 1. Nguồn trạng thái xác minh

- Decision: `tutors.verification_status` là nguồn duy nhất của trạng thái FE-03. Ánh xạ: `UNVERIFIED` = Chưa nộp hồ sơ; `PENDING` = Đang chờ thẩm định; `ADDITIONAL_INFO_REQUIRED` = Cần bổ sung thông tin; `APPROVED` = Đã phê duyệt; `REJECTED` = Bị từ chối.
- Decision: Đồng bộ `tutors.tutor_state` (vòng đời rộng hơn, gồm cả gói hội viên) khi chuyển trạng thái: nộp → `PENDING_VERIFICATION`; yêu cầu bổ sung hoặc từ chối → `REJECTED`; phê duyệt → `APPROVED_UNPAID` trừ khi đang `ACTIVE_PAID` (giữ nguyên). FE-03 không bao giờ đặt `ACTIVE_PAID` (FE-09).
- Rationale: Tìm kiếm (GB-06) đọc `verification_status`; enum DB có đủ 5 giá trị khớp bảng ánh xạ của spec. `tutor_state` mô tả cùng vòng đời nên phải đồng bộ để không mâu thuẫn, nhưng không đủ chi tiết (gộp "từ chối" và "cần bổ sung") để làm nguồn chính.
- Alternatives: Chỉ dùng `tutor_state` mất phân biệt Rejected/Needs info; bảng `TutorApplication` riêng bị loại theo A-1.

## 2. Dữ liệu và thay đổi schema

- Decision: Không tạo bảng mới. Hoàn thiện mapping cho `tutor_credentials`, `tutor_verification_histories`.
- Decision (thay đổi schema duy nhất, cộng thêm): bổ sung hai cột nullable vào `tutor_verification_histories`: `feedback TEXT` (nội dung gửi gia sư tại thời điểm đó) và `reason_code VARCHAR(50)` (mã lý do từ chối). `notes` giữ nghĩa ghi chú nội bộ. Script tăng dần, idempotent (`ADD COLUMN IF NOT EXISTS`) tại `database/migrations/`; cập nhật đồng bộ khối `CREATE TABLE` trong `DatabasePgAdmin4.txt`. Không xóa hoặc đổi kiểu cột nào.
- Rationale: FR-018/UC-84 yêu cầu mỗi mục lịch sử có cả "phản hồi" và "ghi chú nội bộ"; FR-009/GB-02 cấm lộ ghi chú nội bộ cho gia sư. Một cột `notes` duy nhất buộc phải trộn hai loại nội dung (mã hóa JSON/tiền tố trong văn bản), dễ rò rỉ và khó kiểm thử. `tutors.verification_feedback` chỉ giữ phản hồi mới nhất (A-10) nên không thay được lịch sử.
- Alternatives: (a) Lưu JSON `{feedback, internalNote}` vào `notes` — không đổi schema nhưng phá nghĩa cột, bản ghi cũ không theo định dạng; (b) Chỉ hiển thị phản hồi mới nhất — không đạt FR-018. Nếu nhóm không chấp thuận thay đổi schema, dùng (a) với bộ phân tích chịu được văn bản thuần, và ghi giới hạn vào kết quả kiểm chứng.
- Decision: Thông tin xác minh lưu tại: họ tên → `users.full_name`; điện thoại → `tutors.phone`; ngày sinh, địa chỉ → `tutors.date_of_birth`, `tutors.address`; giới thiệu ngắn → `tutors.bio`; kinh nghiệm → `tutors.experience_years`; JLPT → `tutors.highest_jlpt_level`. `bio`/`experience_years`/`highest_jlpt_level` cũng là dữ liệu hồ sơ giảng dạy của FE-02 — FE-03 chỉ cho sửa khi hồ sơ ở trạng thái có thể chỉnh sửa; ghi chú phối hợp với FE-02 trong plan.
- Decision: `tutors.certificate_urls` (legacy) không dùng; nguồn giấy tờ là `tutor_credentials`.

## 3. Xác thực và phân quyền

- Decision: Thêm `core/security/JwtAuthenticationFilter` (OncePerRequestFilter): nếu có `Authorization: Bearer <token>` hợp lệ, nạp lại `User` từ DB theo `userId`/email, bỏ qua nếu `deleted_at` khác null hoặc `account_status` khác `ACTIVE`, rồi đặt `Authentication` với quyền là `role_name` **đọc từ DB** (không tin claim). Không có token thì không làm gì.
- Decision: Trong `SecurityConfig`, chỉ siết hai nhóm route của FE-03: `/api/tutor/verification/**` → `ROLE_TUTOR`; `/api/academic/verifications/**` → `ROLE_ACADEMIC_OFFICER`. Mọi route khác giữ `permitAll` như hiện tại. Trả JSON 401/403 tiếng Việt qua `AuthenticationEntryPoint`/`AccessDeniedHandler` chỉ cho hai nhóm route này.
- Rationale: FR-020, GB-05 yêu cầu kiểm tra vai trò và trạng thái tài khoản phía máy chủ mỗi request. Siết riêng route FE-03 tránh làm hỏng các feature đang dựa vào `permitAll` (Nguyên tắc V: không can thiệp phần khác).
- Decision: Kiểm tra quyền sở hữu ở service: gia sư chỉ thao tác trên `Tutor` có `user_id` = người dùng hiện tại; đường dẫn gia sư không nhận `tutorId` từ client. Tải tệp phía gia sư kiểm tra giấy tờ thuộc tutor của mình; phía cán bộ kiểm tra giấy tờ thuộc `tutorId` trên đường dẫn.
- CSRF: Token nằm trong `localStorage` và gửi qua header, không qua cookie, nên request giả mạo từ site khác không mang được xác thực; giữ `csrf` tắt. Ghi nhận: nếu sau này chuyển sang cookie phải bổ sung CSRF cho các route ghi.
- Alternatives: `@PreAuthorize` rải trong controller vẫn cần filter; bật `authenticated()` toàn cục phá các feature khác.

## 4. Lưu trữ tệp giấy tờ

- Decision: Thư mục cấu hình `app.verification.storage-dir` (mặc định `${user.home}/kizuna-data/verification-docs`), nằm ngoài `frontend/` và `src/main/resources/static`. Tên lưu `UUID.randomUUID() + đuôi chuẩn hóa`; `tutor_credentials.file_url` chỉ lưu khóa này (không phải URL/đường dẫn tuyệt đối). Khi đọc, chuẩn hóa và từ chối mọi khóa chứa ký tự phân cách đường dẫn.
- Decision: Giới hạn: `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=11MB`. Kiểm tra trong service: kích thước ≤ 10.485.760 byte, đuôi ∈ {pdf, png, jpg, jpeg}, chữ ký byte đầu (PDF `%PDF-`, PNG `89 50 4E 47`, JPEG `FF D8 FF`) khớp đuôi, ảnh phải giải mã được bằng `ImageIO`, PDF phải có `%%EOF` ở 1.024 byte cuối. Không thêm thư viện PDF. Vượt giới hạn multipart được ánh xạ sang MSG13.
- Decision (nhất quán DB ↔ tệp): Ghi tệp mới trước khi lưu bản ghi; đăng ký `TransactionSynchronization`: rollback → xóa tệp mới; commit → xóa tệp cũ (khi thay/gỡ). Tệp mồ côi chỉ còn khả năng xảy ra khi tiến trình chết giữa chừng — chấp nhận, ghi nhận giới hạn.
- Decision: Tải tệp trả `Content-Type` theo đuôi đã kiểm tra, `Content-Disposition: inline` với tên an toàn, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store`. Frontend tải bằng `fetch` có header rồi hiển thị qua `URL.createObjectURL` (thẻ `<img>`/`<iframe>` không gửi được header).
- Rationale: Theo câu trả lời làm rõ Q2. Không phải quét mã độc; không tuyên bố có antivirus.
- Alternatives: `bytea` như FE-10 bị loại vì nhóm đã chọn lưu trên máy chủ ứng dụng.

## 5. Cạnh tranh và tính một chiều (GB-01, FR-008, FR-017)

- Decision: Mọi lệnh thay đổi trạng thái (nộp, yêu cầu bổ sung, phê duyệt, từ chối) và mọi lệnh sửa thông tin/giấy tờ khóa dòng `tutors` bằng `@Lock(PESSIMISTIC_WRITE)` (`SELECT … FOR UPDATE`) trong cùng transaction, rồi kiểm tra trạng thái nguồn hợp lệ theo state machine trước khi ghi. Lệnh cán bộ trên hồ sơ không còn `PENDING` → 409 với thông điệp GB-01; lệnh nộp trên hồ sơ đã `PENDING`/`APPROVED` → 409 với thông điệp UC-23 AF-1.
- Rationale: Bảng `tutors` không có cột version; khóa dòng không cần đổi schema và bảo đảm SC-005 (hai quyết định đồng thời) cũng như chống bấm nộp hai lần.
- Alternatives: `UPDATE … WHERE verification_status = 'PENDING'` cũng đạt nhưng tách rời việc ghi lịch sử; `@Version` cần thêm cột.
- Decision: Mỗi chuyển trạng thái ghi đúng một `TutorVerificationHistory` trong cùng transaction (SC-006). Lưu nháp không ghi lịch sử (enum không có hành động tương ứng).

## 6. Email thông báo

- Decision: `VerificationNotificationService` trong feature `verification` dùng `JavaMailSender` có sẵn, gửi sau khi commit (`TransactionSynchronization.afterCommit`) trên luồng nền bằng executor riêng do service tự tạo và đóng. Không bật `@EnableAsync` toàn cục: `EmailServiceImpl` (OTP) đã gắn `@Async` nhưng đang chạy đồng bộ, bật lên sẽ đổi hành vi FE-01 (phát hiện khi triển khai 2026-11-03). Lỗi gửi chỉ ghi log nội bộ (FR-022). Nội dung tiếng Việt cho: yêu cầu bổ sung (kèm nội dung), phê duyệt (chào mừng), từ chối (kèm lý do và phản hồi). Không gửi ghi chú nội bộ.
- Rationale: SMTP có timeout 10 giây; gửi đồng bộ làm chậm thao tác cán bộ và có thể khiến lỗi email làm hỏng transaction.
- Alternatives: Gọi `EmailService` của feature `auth` — interface hiện chỉ phục vụ OTP; mở rộng nó làm feature auth gánh nghiệp vụ xác minh.

## 7. Kiểm thử

- Decision: (a) Unit test JUnit/Mockito cho state machine, kiểm tra bắt buộc khi nộp, kiểm tra tệp (chữ ký byte, kích thước, đuôi), ánh xạ lỗi; (b) `@WebMvcTest` + spring-security-test cho ma trận phân quyền (không token, sai vai trò, tài khoản không ACTIVE, gia sư truy cập giấy tờ người khác) và hợp đồng JSON (không có trường `internalNote` trong phản hồi phía gia sư); (c) kiểm thử tích hợp DB (khóa dòng, migration, lịch sử) chỉ chạy với PostgreSQL kiểm thử riêng qua profile `verification-test` cấu hình bằng biến môi trường, bị bỏ qua khi biến không có.
- Rationale: `application.properties` trỏ vào DB Supabase dùng chung; kiểm thử ghi dữ liệu không được chạy vào đó (Nguyên tắc II, V).
- Decision (2026-11-03, phương án DB 3): nhóm chỉ dùng Supabase, không dựng DB kiểm thử riêng. Nhóm (c) được viết nhưng tự bỏ qua; khóa dòng được kiểm gián tiếp bằng unit test (service luôn gọi phương thức có khóa); SC-003 và SC-005 trên DB ghi là chưa kiểm chứng. Có thể bật lại nhóm (c) sau này bằng `kizuna-postgres` trong `docker-compose.yml` mà không sửa code.
- Kiểm thử thủ công UI theo [quickstart.md](quickstart.md).

## 8. Giao diện

- Decision: Trang gia sư theo `DESIGN.md` mục 7 (retro hard-shadow, bo 5px, viền đen 1px, CTA tím trầm `#7A2CA8`, một CTA chính mỗi khối). Trang cán bộ học vụ theo bảng màu chung trong `FRONTEND_RULES.md` mục 5 (Primary `#006194`, nền Washi). Nhãn trạng thái và thông báo theo bảng ánh xạ của spec; không hiển thị mã enum.
- Decision: Alias `/academic/dashboard` → trang bảng điều khiển thẩm định (đây là "Application Review Dashboard" III.3.1.1 và là màn hình cán bộ học vụ duy nhất hiện có); khi FE khác bổ sung dashboard tổng của cán bộ học vụ, alias được chuyển và trang FE-03 giữ `/academic/verifications`.

## Kết luận

Không còn NEEDS CLARIFICATION chặn thiết kế. Một điểm cần nhóm xác nhận khi review plan: thêm 2 cột nullable vào `tutor_verification_histories` (mục 2) — có phương án dự phòng không đổi schema. Không chạy migration, test hay đổi cấu hình trong bước lập kế hoạch.
