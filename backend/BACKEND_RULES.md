# BACKEND_RULES.md: luật riêng cho backend và cơ sở dữ liệu

<!-- ===================================================================== -->
<!-- [IMMUTABLE CORE GUARDRAILS] DO NOT MODIFY, OVERWRITE OR DELETE BELOW  -->
<!-- KHỐI QUY TẮC BẤT BIẾN: NGHIÊM CẤM TỰ Ý SỬA ĐỔI HOẶC XÓA BỎ DƯỚI MỌI HÌNH THỨC -->
<!-- ===================================================================== -->

> ⛔ **CẢNH BÁO BẢO VỆ HỆ THỐNG (STRICT NON-NEGOTIABLE GUARDRAILS):**  
> Đây là 3 nguyên tắc nền tảng tối thượng của hệ thống KizunaTutor. Mọi tác vụ lập trình (Backend & Frontend) và cập nhật tài liệu BẮT BUỘC phải tuân thủ nghiêm ngặt:
>
> 1. **KHÔNG BAO GIỜ HIỂN THỊ NỘI DUNG BACKEND LÊN FRONTEND (ZERO BACKEND LEAKS):**
>    - Tuyệt đối cấm để lộ các mã nội bộ, tên enum, tên Exception, mã HTTP raw hay cấu trúc dữ liệu Backend ra giao diện (ví dụ: `ROLE_ADMIN`, `ROLE_LEARNER`, `BadCredentialsException`, `NullPointerException`, `500 INTERNAL_SERVER_ERROR`, `user_id`, `is_active`).
>    - Toàn bộ thông báo, nhãn, badge và trạng thái gửi tới người dùng BẮT BUỘC phải được chuyển hóa 100% sang ngôn ngữ người dùng tự nhiên (Human-centric / User-friendly Vietnamese).
>
> 2. **KHÔNG BAO GIỜ VIẾT IN HOA TOÀN BỘ TRÊN GIAO DIỆN (NO ALL-CAPS UI TEXT):**
>    - Tuyệt đối cấm sử dụng chữ IN HOA TOÀN BỘ (ALL-CAPS) cho các dòng thông báo, tiêu đề Modal, Alert, Toast, Empty States hay nội dung giải thích (ví dụ: cấm `LỖI ĐĂNG NHẬP!`, `ĐĂNG KÝ THÀNH CÔNG!`, `KHÔNG TÌM THẤY DỮ LIỆU!`).
>    - Chỉ áp dụng viết hoa chữ cái đầu câu (Sentence case) hoặc chữ cái đầu mỗi từ chính (Title Case cho nhãn nút/tiêu đề ngắn). Chữ in hoa chỉ được dùng duy nhất cho các từ viết tắt chuyên môn đã định nghĩa (ví dụ: `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`).
>
> 3. **KHÔNG BAO GIỜ CẬP NHẬT DỮ LIỆU ẢO ĐỂ HIỂN THỊ HỆ THỐNG (NO MOCK / FAKE DATA INJECTION):**
>    - Tuyệt đối không tự ý chèn dữ liệu ảo (hardcoded mock data, dummy JSON, dữ liệu bịa đặt) vào mã nguồn hoặc cơ sở dữ liệu để đối phó hiển thị khi chưa có API thực tế.
>    - Mọi dữ liệu hiển thị trên màn hình của 6 Actor (Admin, Manager, CS Staff, Academic Officer, Tutor, Learner) BẮT BUỘC phải được kết nối từ Endpoint API thực tế hoặc hiển thị màn hình rỗng chuẩn (Empty State) nếu cơ sở dữ liệu chưa có dữ liệu.

<!-- ===================================================================== -->
<!-- [END IMMUTABLE CORE GUARDRAILS] - SAFE TO APPEND EXTENSIONS BELOW     -->
<!-- ===================================================================== -->

---

File này gom mọi luật dành cho `backend/` và `database/`. Luật chung (thứ tự đọc tài liệu, mười luật, định nghĩa "xong", git) ở [.agents/AGENTS.md](../.agents/AGENTS.md); khi khác nhau thì AGENTS.md thắng.

## 1. Công nghệ

- Java 17, Spring Boot 3.3.4, Maven wrapper (`mvnw`, `mvnw.cmd`).
- Spring Web, Spring Security 6 (JWT qua `jjwt`, đăng nhập Google OAuth2), Spring Data JPA, Validation, Mail (gửi OTP), AOP, Lombok, Apache POI.
- PostgreSQL (khóa UUID, enum gốc của PostgreSQL). Không dùng H2.
- Backend chạy cổng **8085**, mọi API có tiền tố `/api/...`. Frontend tách riêng ở `frontend/` (cổng 3000), backend không phục vụ giao diện cho các trang mới.
- Không thêm thư viện, framework, microservice mới khi plan không yêu cầu; cần thêm thì ghi lý do, tác động và phương án tương thích trong `plan.md` rồi hỏi nhóm.

## 2. Cấu trúc gói theo feature

Gói gốc `com.kizuna`. Không đặt lớp tự do ở gốc, trừ `KizunaBackendApplication.java`.

```text
com.kizuna
├── core/            # Hạ tầng dùng chung, không chứa nghiệp vụ của feature nào
│   ├── config/      # Cấu hình Spring
│   ├── security/    # SecurityConfig, xác thực JWT
│   ├── exception/   # Xử lý lỗi chung, exception gốc
│   ├── entity/      # BaseEntity, User, Role, Learner, Tutor
│   ├── repository/  # Repository của entity dùng chung
│   ├── dto/         # Bao bọc phản hồi dùng chung
│   ├── mapper/      # Mapper của entity dùng chung
│   └── util/        # Tiện ích ngày giờ, ngữ cảnh bảo mật
└── features/        # auth, profile, verification, tutor_search, booking, classroom,
                     # progress, learning_support, payment, ticket, admin, notification
```

Trong mỗi feature (`com.kizuna.features.<feature>`):

- Bắt buộc: `controller/`, `dto/`, `entity/`, `repository/`, `service/` (interface `*Service`, cài đặt ở `service/impl/*ServiceImpl`).
- Tùy chọn khi cần: `mapper/`, `util/`, `seeder/`, `config/` (chỉ bean riêng của feature, ví dụ `admin/config/OperationsConfig`; cấu hình dùng chung vẫn ở `core/`).
- Không tạo gói con lặp lại các tầng bên trong một feature (ví dụ `features/admin/operations/{dto, entity, repository, service}` song song với `features/admin/{dto, entity, repository, service}`). Lớp của chức năng con đặt thẳng vào tầng tương ứng của feature.
- Thứ dùng chung cho mọi feature đặt ở `core/`, không tạo bản riêng trong feature: bao bọc phản hồi (`ApiResponse`, `PageResponse`) ở `core/dto/`, xử lý lỗi chung ở `core/exception/`, cấu hình Spring/Web ở `core/config/`, cấu hình bảo mật và JWT ở `core/security/` (chỉ một `SecurityConfig`). Cần thêm hoặc sửa trong `core/` thì báo nhóm trước vì mọi người cùng dùng.
- Khai báo gói: `package com.kizuna.features.<feature>.<subpackage>;`. Tên feature viết thường, snake_case, không đánh số (`tutor_search`, không phải `fe04_` hay `tutorSearch`).
- Không để `.gitkeep` trong gói đã có lớp Java; gói chưa có mã thì chưa tạo.
- Feature khác chỉ được gọi qua Service interface công khai; không gọi `Repository` của feature khác.

## 3. Trách nhiệm từng tầng

1. **Controller**: nhận request, kiểm tra đầu vào bằng `@Valid`, gọi service, trả DTO phản hồi. Không có nghiệp vụ, không gọi repository, không tính toán.
2. **Service**: chứa toàn bộ quy tắc nghiệp vụ, ranh giới giao dịch (`@Transactional` cho thao tác ghi, rollback khi lỗi runtime), điều phối giữa các feature.
3. **Repository**: truy cập dữ liệu qua Spring Data JPA. Dùng `JOIN FETCH` / `LEFT JOIN FETCH` khi cần nạp quan hệ `@ManyToOne`, `@OneToOne` để tránh N+1 và `LazyInitializationException`.
4. **Entity**: mô hình miền ánh xạ bảng, kế thừa `BaseEntity` để có `createdAt`, `updatedAt`.
5. **DTO**: request dùng Jakarta Validation (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Min`, `@Max`); response không bao giờ chứa mật khẩu, hash, token nội bộ.

Giữ mỗi lớp, mỗi phương thức một trách nhiệm (phương thức dưới khoảng 40 dòng). Xóa import thừa, mã chú thích bỏ đi, lệnh in debug trước khi bàn giao.

## 4. Đặt tên Java

| Thành phần | Quy ước | Ví dụ đúng | Ví dụ sai |
| :--- | :--- | :--- | :--- |
| Gói | chữ thường, snake_case cho feature | `com.kizuna.features.tutor_search.service` | `com.kizuna.features.tutorSearch` |
| Lớp, interface | PascalCase | `TrialBookingService` | `trialBookingService` |
| Phương thức | camelCase, bắt đầu bằng động từ | `findTutorsBySkill()` | `FindTutors()` |
| Biến, trường | camelCase | `hourlyRate` | `hourly_rate` |
| Hằng | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` | `defaultPageSize` |
| Enum | Kiểu PascalCase, giá trị UPPER_SNAKE_CASE | `TicketPriority.HIGH` | `TicketPriority.High` |
| Entity | PascalCase, danh từ số ít | `TrialBooking` | `TrialBookings`, `tbl_booking` |
| DTO | `*Request`, `*Response` | `CreateBookingRequest`, `BookingDetailResponse` | `BookingReq`, `TutorDataDTO` |
| Controller, Service, Repository | hậu tố tương ứng, cài đặt là `*ServiceImpl` | `TrialBookingController`, `TrialBookingServiceImpl` | `BookingManager`, `IBookingService` |
| Exception | hậu tố `Exception` | `ResourceNotFoundException` | `NotFoundExc` |
| Cấu hình | hậu tố `Config` | `SecurityConfig` | `SecurityConfigurationHelper` |

## 5. Cơ sở dữ liệu

### 5.1 Đặt tên (PostgreSQL)

| Thành phần | Quy ước | Ví dụ |
| :--- | :--- | :--- |
| Bảng | snake_case, số nhiều | `trial_bookings`, `support_tickets` |
| Cột | snake_case | `created_at`, `hourly_rate` |
| Khóa chính | `<bảng_số_ít>_id`, kiểu UUID | `booking_id` |
| Khóa ngoại | `<bảng_tham_chiếu_số_ít>_id` | `tutor_id`, `learner_id` |
| Index | `idx_<bảng>_<cột>` | `idx_users_email` |
| Ràng buộc duy nhất | `uq_<bảng>_<cột>` | `uq_users_email` |
| Ràng buộc khóa ngoại | `fk_<bảng>_<bảng_tham_chiếu>` | `fk_bookings_tutors` |

### 5.2 Quy định bảng và entity

- Khóa chính UUID: `UUID PRIMARY KEY DEFAULT gen_random_uuid()`.
- Mọi bảng nghiệp vụ có `created_at` và `updated_at` kiểu `TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`.
- Khóa ngoại khai báo rõ ràng, cột khóa ngoại có index.
- Ánh xạ entity:

  ```java
  @Entity
  @Table(name = "trial_bookings")
  @Getter
  @Setter
  @NoArgsConstructor
  public class TrialBooking extends BaseEntity {
      @Id
      @GeneratedValue(strategy = GenerationType.UUID)
      @Column(name = "booking_id", updatable = false, nullable = false)
      private UUID bookingId;
  }
  ```

- Enum gốc của PostgreSQL ánh xạ bằng `@JdbcType` của Hibernate 6; không so sánh chuỗi thô với cột enum trong JPQL `@Query` (gây `PSQLException: type does not exist`):

  ```java
  @Enumerated(EnumType.STRING)
  @Column(name = "booking_status", columnDefinition = "booking_status_enum", nullable = false)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  private BookingStatus status;
  ```

- `spring.jpa.hibernate.ddl-auto=none`: Hibernate không tự sửa schema. Thay đổi schema phải đối chiếu mapping entity và dữ liệu đang có.

### 5.3 Migration

- Mọi thay đổi schema đi qua script SQL tăng dần trong `database/migrations/`, đặt tên `fe<NN>_<nnn>_<ten>.sql` (mẫu: `fe11_001_operations_schema.sql`), kèm README hướng dẫn chạy nếu cần backup hoặc preflight.
- Không sửa migration đã áp dụng; sửa bằng migration mới.
- Không chạy `DROP ... CASCADE`, `TRUNCATE` hay lệnh phá dữ liệu trên cơ sở dữ liệu thật. Không chạy `DatabasePgAdmin4.txt` (có `DROP CASCADE`).
- Không tự seed giá trị cấu hình nghiệp vụ để migration chạy qua; thiếu giá trị thì dừng và hỏi người phụ trách.
- Dữ liệu Kanji chỉ nạp từ KanjiVG qua `KanjiDataSeeder` (bật tắt bằng `app.seeder.kanji.enabled`).

## 6. Chuẩn API

### 6.1 Đường dẫn

| Quy tắc | Đúng | Sai |
| :--- | :--- | :--- |
| Tập tài nguyên: chữ thường, số nhiều, kebab-case | `GET /api/trial-bookings` | `GET /api/getTrialBookings` |
| Một tài nguyên | `GET /api/trial-bookings/{bookingId}` | `GET /api/trial-bookings?id=123` |
| Chuyển trạng thái | `POST /api/support-tickets/{id}/resolve` | `POST /api/set-status-resolve` |
| Tài nguyên con | `GET /api/tutors/{tutorId}/reviews` | `GET /api/tutor-reviews/{tutorId}` |
| Tham số truy vấn camelCase | `?jlptLevel=N2&minRating=4` | `?jlpt_level=N2` |

Endpoint, trường phải đúng `specs/NNN-*/contracts/` của feature; không tự thêm, bớt, đổi tên.

### 6.2 Phản hồi thành công

```json
{
  "success": true,
  "message": "Đặt lịch học thử thành công.",
  "data": { "bookingId": "c3b8a1e2-9d3b-4c2d-8e4f-5a6b7c8d9e0f", "status": "PENDING" },
  "timestamp": "2026-10-31T18:00:00+07:00"
}
```

`frontend/js/api.js` đọc `success` và `data` từ phản hồi này; đổi cấu trúc phải sửa cả hai phía và test `frontend/tests/`.

### 6.3 Phản hồi lỗi

```json
{
  "success": false,
  "message": "Dữ liệu gửi lên không hợp lệ.",
  "fieldErrors": [{ "field": "lessonTime", "message": "Thời gian học phải diễn ra trong tương lai." }],
  "timestamp": "2026-10-31T18:00:00+07:00"
}
```

- Lỗi được bắt tập trung bằng một `@RestControllerAdvice` dùng chung ở `core/exception/` (`GlobalExceptionHandler`); feature không tự viết handler riêng. Không để tên exception, stack trace, mã HTTP thô lọt vào `message`.
- `message` và `fieldErrors[].message` là tiếng Việt tự nhiên, sentence case, theo bảng chuyển ngữ ở `.agents/rules/README.md` (ví dụ `BadCredentialsException` → "Email hoặc mật khẩu không chính xác").

### 6.4 Mã HTTP

`200` thành công · `201` đã tạo · `204` thành công không có nội dung · `400` dữ liệu không hợp lệ · `401` chưa đăng nhập hoặc token hết hạn · `403` không đủ quyền · `404` không tồn tại · `409` xung đột trạng thái nghiệp vụ hoặc khóa lạc quan · `500` lỗi chưa xử lý (vẫn trả thông điệp thân thiện).

## 7. Bảo mật

1. Không tin kiểm tra phía client: backend tự kiểm tra lại mọi đầu vào.
2. Mọi endpoint nhạy cảm phân quyền theo vai trò (`@PreAuthorize("hasRole('TUTOR')")` hoặc cấu hình trong `SecurityConfig`). Ẩn nút trên giao diện không phải là phân quyền: API phải tự trả `403`.
3. Mật khẩu băm bằng BCrypt; không ghi log, không lưu mật khẩu dạng rõ.
4. Lệnh ghi (POST, PUT, PATCH, DELETE) yêu cầu CSRF token (header mặc định `X-CSRF-TOKEN`); frontend lấy token và tên header từ endpoint `/csrf` của backend, không tự đặt cứng.
5. Bí mật (mật khẩu DB, JWT secret, khóa Gemini/OpenAI, Google client secret, VNPay hash secret, mật khẩu Gmail) chỉ đọc từ biến môi trường dạng `${TEN_BIEN}` trong `application.properties`; không ghi giá trị thật vào kho mã, log hay chat.
6. Giữ `server.servlet.encoding.charset=UTF-8` và `server.servlet.encoding.force=true`.

## 8. Tích hợp AI

1. Mọi lời gọi model AI chỉ chạy ở backend; JavaScript phía trình duyệt không gọi API AI, không giữ khóa AI.
2. Phản hồi AI phải được kiểm tra theo schema (cấu trúc rubric, điểm trong khoảng 0–100) trước khi lưu; sai định dạng hoặc ngoài khoảng thì loại bỏ.
3. Gợi ý AI lưu ở cột riêng (`ai_suggested_score`, `ai_feedback`), đánh giá của gia sư ở cột riêng (`final_score`, `tutor_feedback`); không ghi đè bản ghi gia sư đã xác nhận bằng dữ liệu AI.
4. AI lỗi, hết thời gian chờ hoặc hết hạn mức: ghi log nội bộ, báo thông điệp tiếng Việt thân thiện và cho gia sư chấm tay; lỗi AI không được chặn tiến độ của học viên.
5. Nhà cung cấp và model đọc từ `app.ai.*`. Bộ nhớ AI gọi dịch vụ `agent-memory` qua `app.agent-memory.base-url` (cổng 8081); không thêm model hay microservice AI khác khi nhóm chưa duyệt.

## 9. Kiểm thử

- Test ở `backend/src/test/java/com/kizuna/`, gom theo feature (mẫu: `com.kizuna.operations`).
- Đặt tên: `*Test` cho test đơn vị và test hợp đồng; test cần cơ sở dữ liệu thật đặt tên `*IntegrationTest` và kế thừa lớp nền của feature (mẫu: `OperationsIntegrationBase`).
- Test tích hợp dùng profile và biến môi trường riêng, không đọc `application.properties` thật. Mẫu của FE-11: `application-operations-test.properties` cùng `OPERATIONS_TEST_DB_URL`, `OPERATIONS_TEST_DB_USERNAME`, `OPERATIONS_TEST_DB_PASSWORD`; schema test phải tách riêng (tên kết thúc `operations_test`), không chạy trên DB Supabase thật.
- Fixture SQL đặt ở `backend/src/test/resources/`.
- Lệnh (Windows dùng `.\mvnw.cmd`, Linux/macOS dùng `./mvnw`):
  - Biên dịch cả mã và test: `.\mvnw.cmd test-compile`
  - Chạy một lớp test: `.\mvnw.cmd test -Dtest=<TenTest>`
  - Chạy toàn bộ: `.\mvnw.cmd test`
- Thiếu biến môi trường hoặc DB test khiến test bị bỏ qua hay lỗi thì báo rõ cho người dùng, không ghi là đã xanh.

## 10. Quy trình khi nhận task backend

1. Đọc `.agents/AGENTS.md`, file này và `tasks.md` → `plan.md` → `data-model.md` → `contracts/` của feature.
2. Xem mã hiện có của feature; không giả định phần chưa xây đã tồn tại.
3. Xác định đúng feature và tập file nhỏ nhất cần tạo hoặc sửa.
4. Viết test cho hành vi cần làm, rồi cài đặt.
5. Kiểm tra đặt tên (mục 4, 5.1, 6.1), xử lý lỗi tiếng Việt (mục 6.3), phân quyền (mục 7).
6. Chạy `test-compile` và test liên quan; ghi kết quả.
7. Báo cáo file đã tạo hoặc sửa, kết quả lệnh, quyết định cần nhóm xác nhận.
