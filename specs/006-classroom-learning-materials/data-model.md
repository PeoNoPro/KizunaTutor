# Data Model: FE-06

Thay đổi schema duy nhất: thêm `learning_materials.description TEXT NULL` (đã được duyệt — Q3). Không đổi bảng, cột, kiểu, khóa, ràng buộc, index nào khác.

## learning_spaces (sẵn có — không đổi schema)

| Cột | Kiểu | Ghi chú mapping |
|---|---|---|
| space_id | UUID PK | `spaceId` |
| tutor_id | UUID FK tutors (RESTRICT) | `@ManyToOne Tutor` |
| learner_id | UUID FK learners (RESTRICT) | `@ManyToOne Learner` |
| trial_booking_id | UUID FK trial_bookings NULL | Không map (FE-06 không dùng; insert để NULL) |
| title | VARCHAR(150) NOT NULL | 1–150 ký tự, trim |
| status | learning_space_status_enum | `ACTIVE` khi tạo |
| created_at / updated_at | TIMESTAMPTZ | `BaseEntity` |

Entity `LearningSpace.java` hiện đã khớp; không sửa.

## learning_materials (sửa mapping entity + 1 cột mới)

| Cột | Kiểu | Mapping mới |
|---|---|---|
| material_id | UUID PK | `@Column(name="material_id") UUID materialId` |
| space_id | UUID FK learning_spaces (CASCADE) NOT NULL | `@ManyToOne(fetch=LAZY) LearningSpace space` |
| title | VARCHAR(150) NOT NULL | `String title` |
| **description** | **TEXT NULL (mới)** | `String description` (≤ 1000, kiểm ở service) |
| file_url | VARCHAR(500) NOT NULL | `String fileUrl` — đường dẫn đối tượng trong bucket |
| file_type | VARCHAR(50) NULL | `String fileType` — PDF/PNG/JPG/MP3 |
| uploaded_by | UUID FK users (SET NULL) | `@ManyToOne(fetch=LAZY) User uploadedBy` |
| created_at | TIMESTAMPTZ NOT NULL | `@CreationTimestamp OffsetDateTime createdAt`, `updatable=false` |

Không kế thừa `BaseEntity` (bảng không có `updated_at`).

## connection_requests (chỉ đọc, native query)

Dùng `tutor_id`, `learner_id`, `status = 'ACCEPTED'`. Không map entity mới.

## Quy tắc trạng thái

- Lớp "có thể quản lý" ⇔ `space.status = ACTIVE` ∧ `tutor.membershipStatus ≠ EXPIRED`.
- Ngược lại: chế độ chỉ xem (`readOnly = true` trong response).
- Chặn trùng: không tạo nếu tồn tại space cùng tutor+learner với status ∈ {ACTIVE, PAUSED}.

## Nhãn trạng thái tiếng Việt (UI)

ACTIVE → "Đang hoạt động", PAUSED → "Tạm dừng", COMPLETED → "Đã hoàn thành", CLOSED → "Đã đóng".
