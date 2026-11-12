# Data Model: FE-03 — Tutor Verification

Nguồn schema: `database/DatabasePgAdmin4.txt` (bảng `tutors`, `tutor_credentials`, `tutor_verification_histories`, `users`). Quyết định và phương án thay thế: [research.md](research.md) mục 1–2.

## 1. Hồ sơ xác minh (SRS: TutorApplication) → `tutors` + `users`

Entity: `com.kizuna.core.entity.Tutor` (đã có, không đổi mapping).

| Trường nghiệp vụ | Cột | Kiểu | Ràng buộc kiểm tra (FE-03) |
| --- | --- | --- | --- |
| Họ tên hiển thị | `users.full_name` | VARCHAR(100) | Bắt buộc khi nộp; 2–100 ký tự sau khi trim |
| Số điện thoại | `tutors.phone` | VARCHAR(20) | Bắt buộc khi nộp; số Việt Nam 10 chữ số bắt đầu bằng 0 |
| Ngày sinh | `tutors.date_of_birth` | DATE | Bắt buộc khi nộp; đủ 18 tuổi, không ở tương lai |
| Địa chỉ | `tutors.address` | VARCHAR(255) | Bắt buộc khi nộp; ≤ 255 ký tự |
| Giới thiệu ngắn | `tutors.bio` | TEXT | Bắt buộc khi nộp; 20–2.000 ký tự |
| Số năm kinh nghiệm | `tutors.experience_years` | SMALLINT | 0–60 |
| Trình độ JLPT cao nhất | `tutors.highest_jlpt_level` | VARCHAR(10) | Bắt buộc khi nộp; ∈ {N1, N2, N3, N4, N5} |
| Trạng thái xác minh | `tutors.verification_status` | `tutor_verification_status_enum` | Chỉ đổi qua state machine (mục 4) |
| Vòng đời gia sư | `tutors.tutor_state` | VARCHAR(30) | Đồng bộ theo mục 4; không đặt `ACTIVE_PAID` |
| Phản hồi mới nhất cho gia sư | `tutors.verification_feedback` | TEXT | Ghi khi yêu cầu bổ sung/từ chối; xóa khi phê duyệt |
| Thời điểm nộp gần nhất | `tutors.submitted_at` | TIMESTAMPTZ | Ghi mỗi lần nộp/nộp lại |
| Thời điểm duyệt | `tutors.verified_at` | TIMESTAMPTZ | Ghi khi phê duyệt |

Lưu nháp cho phép giá trị rỗng ở các trường bắt buộc; kiểm tra định dạng vẫn áp dụng cho trường có giá trị. Kiểm tra bắt buộc đầy đủ chỉ ở bước nộp (FR-005).

Quan hệ: 1 `users` — 1 `tutors` — N `tutor_credentials` — N `tutor_verification_histories`.

## 2. Giấy tờ xác minh (SRS: TutorApplicationDocument) → `tutor_credentials`

Entity: `com.kizuna.features.verification.entity.TutorCredential` (hoàn thiện; **không** kế thừa `BaseEntity`).

| Thuộc tính Java | Cột | Kiểu | Ghi chú |
| --- | --- | --- | --- |
| `credentialId` | `credential_id` | UUID PK | `GenerationType.UUID` |
| `tutor` | `tutor_id` | FK → `tutors` | `@ManyToOne(LAZY)` |
| `credentialType` | `credential_type` | VARCHAR(50) | Enum Java `CredentialType` lưu `STRING` |
| `title` | `title` | VARCHAR(150) | Bắt buộc, 1–150 ký tự |
| `storageKey` | `file_url` | VARCHAR(500) | Chỉ khóa tệp ngẫu nhiên, không bao giờ trả ra API |
| `verifiedStatus` | `verified_status` | `tutor_credential_status_enum` | `TutorCredentialStatus` có sẵn; `@JdbcType(PostgreSQLEnumJdbcType)` |
| `rejectionReason` | `rejection_reason` | TEXT | Không dùng ở FE-03 v1 (không duyệt từng giấy tờ) |
| `uploadedAt` | `uploaded_at` | TIMESTAMPTZ | Cập nhật khi thay tệp |

Enum mới `CredentialType` (UPPER_SNAKE_CASE, nhãn tiếng Việt chỉ ở tầng hiển thị):

| Mã | Nhãn | Nhóm bắt buộc (A-4) |
| --- | --- | --- |
| `IDENTITY_DOCUMENT` | Giấy tờ tùy thân | Định danh |
| `JLPT_CERTIFICATE` | Chứng chỉ JLPT | Năng lực tiếng Nhật |
| `OTHER_JAPANESE_CERTIFICATE` | Chứng chỉ tiếng Nhật khác | Năng lực tiếng Nhật |
| `DEGREE` | Bằng cấp | — |
| `TEACHING_CERTIFICATE` | Chứng chỉ sư phạm | — |
| `OTHER` | Khác | — |

Điều kiện nộp: ≥ 1 giấy tờ nhóm Định danh **và** ≥ 1 giấy tờ nhóm Năng lực tiếng Nhật, mỗi giấy tờ có tệp tồn tại trong kho lưu.

Trạng thái giấy tờ: tải/thay mới → `PENDING`; hồ sơ được phê duyệt → mọi giấy tờ `PENDING` thành `VERIFIED`; hồ sơ bị từ chối → `REJECTED`; yêu cầu bổ sung → giữ nguyên.

Tệp: kích thước ≤ 10.485.760 byte; đuôi pdf/png/jpg/jpeg khớp chữ ký byte (research mục 4). Gỡ/thay chỉ khi hồ sơ có thể chỉnh sửa; tệp cũ xóa sau commit.

## 3. Mục lịch sử thẩm định → `tutor_verification_histories`

Entity: `com.kizuna.features.verification.entity.TutorVerificationHistory` (hoàn thiện; **không** kế thừa `BaseEntity`; chỉ thêm, không sửa/xóa).

| Thuộc tính Java | Cột | Kiểu | Ghi chú |
| --- | --- | --- | --- |
| `historyId` | `history_id` | UUID PK | |
| `tutor` | `tutor_id` | FK → `tutors` | `@ManyToOne(LAZY)` |
| `officer` | `officer_id` | FK → `users`, nullable | Null với hành động `SUBMIT` |
| `action` | `action` | `tutor_verif_action_enum` | `TutorVerifAction` có sẵn |
| `internalNote` | `notes` | TEXT | Ghi chú nội bộ; **không bao giờ** xuất hiện trong DTO phía gia sư |
| `feedback` | `feedback` *(mới)* | TEXT nullable | Nội dung gửi gia sư tại thời điểm đó |
| `reasonCode` | `reason_code` *(mới)* | VARCHAR(50) nullable | Chỉ với `REJECT` |
| `createdAt` | `created_at` | TIMESTAMPTZ | `@CreationTimestamp` |

Enum mới `RejectionReason`: `INSUFFICIENT_QUALIFICATION` (Chưa đủ trình độ chuyên môn), `INVALID_OR_FORGED_DOCUMENT` (Giấy tờ không hợp lệ hoặc có dấu hiệu giả mạo), `INFORMATION_MISMATCH` (Thông tin không trùng khớp với giấy tờ), `OTHER` (Lý do khác).

### Thay đổi schema (duy nhất) — đã áp dụng trực tiếp trên Supabase

Người phụ trách đã chạy bằng DBeaver ngày 2026-11-03 (nhóm quản lý schema trực tiếp trên DB, không dùng file migration; file `.sql` tạm đã được xóa khỏi repo). Câu lệnh đã chạy:

```sql
ALTER TABLE tutor_verification_histories ADD COLUMN IF NOT EXISTS feedback TEXT;
ALTER TABLE tutor_verification_histories ADD COLUMN IF NOT EXISTS reason_code VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_verif_hist_action_created
    ON tutor_verification_histories(action, created_at DESC);   -- thẻ tóm tắt theo tháng và "lịch sử gần đây"
```

Cộng thêm, nullable, chạy lại an toàn; bản ghi cũ giữ nguyên với `feedback`/`reason_code` null. Đã kiểm tra bằng `information_schema.columns` (xem tasks.md, T013). Khối `CREATE TABLE` trong `DatabasePgAdmin4.txt` cũng được bổ sung tương ứng để tham khảo. DB khác (nếu có) cần chạy đúng 3 câu lệnh trên.

## 4. State machine

```text
UNVERIFIED ──(gia sư nộp)──────────────► PENDING
ADDITIONAL_INFO_REQUIRED ──(nộp lại)───► PENDING
REJECTED: trạng thái cuối — không có chuyển tiếp (sửa đổi 2026-11-03)
PENDING ──(cán bộ yêu cầu bổ sung)─────► ADDITIONAL_INFO_REQUIRED
PENDING ──(cán bộ phê duyệt)───────────► APPROVED
PENDING ──(cán bộ từ chối)─────────────► REJECTED
```

| Chuyển | `tutor_state` sau | Ghi `TutorVerificationHistory` | Trường `tutors` khác | Email |
| --- | --- | --- | --- | --- |
| → PENDING | `PENDING_VERIFICATION` | `SUBMIT`, officer null | `submitted_at = now` | Không |
| → ADDITIONAL_INFO_REQUIRED | `REJECTED` | `REQUEST_INFO`, `feedback` = nội dung yêu cầu (1–2.000 ký tự) | `verification_feedback` = nội dung | Có |
| → APPROVED | `APPROVED_UNPAID` (giữ `ACTIVE_PAID` nếu đang có) | `APPROVE` | `verified_at = now`, `verification_feedback = null` | Có |
| → REJECTED | `REJECTED` | `REJECT`, `reason_code`, `feedback`, `notes` (bắt buộc, 1–2.000 ký tự) | `verification_feedback` = nhãn lý do + phản hồi | Có |

Có thể chỉnh sửa: `UNVERIFIED`, `ADDITIONAL_INFO_REQUIRED`. `REJECTED` là trạng thái cuối: mọi thao tác sửa/nộp trả 409 `VERIFICATION_REJECTED`. Hàng đợi: `PENDING`. Nhóm "Cần bổ sung": `ADDITIONAL_INFO_REQUIRED`. Nhóm "Đã hoàn tất": `APPROVED`, `REJECTED`.

Mọi chuyển trạng thái chạy trong một transaction có khóa dòng `tutors` (`PESSIMISTIC_WRITE`); trạng thái nguồn sai → lỗi 409, không ghi lịch sử.

## 5. Truy vấn dẫn xuất

- Đang chờ: `count(verification_status = PENDING)`; mới trong ngày: thêm `submitted_at ≥ đầu ngày (Asia/Ho_Chi_Minh)`.
- Cần bổ sung (`needsInfoCount`): `count(verification_status = ADDITIONAL_INFO_REQUIRED)`.
- Đã duyệt/từ chối trong tháng: `count(history.action = APPROVE|REJECT AND created_at ∈ tháng hiện tại)` (đếm quyết định, kể cả gia sư sau đó nộp lại).
- Hàng đợi: join `users` để lọc `lower(full_name)`/`lower(email)` LIKE `%keyword%` (≤ 100 ký tự, escape `%`/`_`), lọc `submitted_at` theo khoảng ngày; sắp xếp `submitted_at ASC NULLS LAST`; phân trang 10/trang (tối đa 50).
- Lịch sử gần đây: 10 mục mới nhất có `action ∈ {APPROVE, REJECT, REQUEST_INFO}`.

## Bổ sung 2026-11-10: cập nhật hồ sơ sau khi đã phê duyệt

Quyết định của người phụ trách (chưa có trong SRS, cần nhóm xác nhận). **Không thêm bảng mới**, dùng `tutor_verification_histories` + `tutor_credentials`:

- Hồ sơ `APPROVED` sửa ngay (không thẩm định lại): SĐT, địa chỉ, giới thiệu, số năm kinh nghiệm. Họ tên, ngày sinh giữ nguyên.
- Đổi trình độ JLPT hoặc bổ sung giấy tờ: gia sư gửi **yêu cầu cập nhật**; hồ sơ vẫn `APPROVED` trong lúc chờ.
  - Ghi một dòng `SUBMIT`: `notes` = "Yêu cầu cập nhật hồ sơ" (kèm ". Đề xuất đổi trình độ JLPT: N2 → N1" nếu đổi JLPT), `feedback` = ghi chú của gia sư.
  - Quyết định = dòng `APPROVE` / `REJECT` kế tiếp, `notes` = "Yêu cầu cập nhật hồ sơ", `reason_code` trống; lý do từ chối ở `feedback`.
  - Đang chờ = hồ sơ `APPROVED` và dòng lịch sử mới nhất là `SUBMIT` có `notes` như trên. Thao tác luôn khóa dòng `tutors` nên mỗi gia sư tối đa một yêu cầu chờ.
  - Giấy tờ `VERIFIED` không sửa / gỡ được; giấy tờ mới có `verified_status = PENDING`. Đang có yêu cầu chờ thì khóa thêm / sửa / gỡ giấy tờ.
- Cán bộ học vụ (hàng đợi lọc `status=PROFILE_UPDATE`, trang chi tiết):
  - Chấp nhận: `highest_jlpt_level` = mức đề xuất; giấy tờ `PENDING` → `VERIFIED`.
  - Từ chối (bắt buộc lý do): giấy tờ `PENDING` → `REJECTED`; hồ sơ vẫn `APPROVED`.
- Hệ quả: quyết định cập nhật hiện trong "Lịch sử xét duyệt" (nhãn "Chấp nhận / Từ chối cập nhật hồ sơ") và được tính vào số duyệt / từ chối trong tháng.
- API: `POST /api/tutor/verification/profile-update` `{highestJlptLevel, note}`; `POST /api/academic/verifications/{tutorId}/profile-update/approve`; `POST .../profile-update/reject` `{feedback}`.
