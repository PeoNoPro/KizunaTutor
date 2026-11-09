# Data Model: FE-09 — Membership & Payment Management

Nguồn schema: kết quả `information_schema.columns` trên Supabase ngày 2026-11-04 (khớp `DatabasePgAdmin4.txt`). **Không đổi schema cho US1–US5.**

## 1. Gói hội viên → `membership_plans` (entity `MembershipPlan`, có sẵn)

| Cột | Kiểu | Ràng buộc FE-09 |
| --- | --- | --- |
| `plan_id` | UUID PK | |
| `plan_name` | VARCHAR(100) NOT NULL | 1–100 ký tự sau trim, không trùng (không phân biệt hoa thường) |
| `duration_months` | INTEGER NOT NULL, CHECK > 0 | 1–36; không đổi sau khi tạo |
| `price` | NUMERIC(12,2) NOT NULL | 10.000–100.000.000, số nguyên VND |
| `features_json` | JSONB | Mảng chuỗi, 0–20 mục, mỗi mục 1–200 ký tự |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | Mở bán / ngừng bán |
| `created_at` | TIMESTAMPTZ | |

Không xóa gói (FK `tutor_memberships.plan_id` RESTRICT).

## 2. Giao dịch → `payment_transactions` (entity `PaymentTransaction`, cần hoàn thiện — plan D-1)

| Thuộc tính Java | Cột | Kiểu | Ghi chú |
| --- | --- | --- | --- |
| `transactionId` | `transaction_id` | UUID PK | `vnp_TxnRef` = chuỗi hex 32 ký tự của UUID |
| `user` | `user_id` | FK users NOT NULL | Người dùng của gia sư |
| `plan` | `plan_id` | FK membership_plans, nullable | Luôn có với FE-09 |
| `bookingId` | `booking_id` | UUID nullable | Không dùng (FE-05) |
| `amount` | `amount` | NUMERIC(12,2) NOT NULL | Giá gói lúc tạo |
| `paymentGateway` | `payment_gateway` | VARCHAR(50) | `VNPay` |
| `gatewayTransactionId` | `gateway_transaction_id` | VARCHAR(100) | `vnp_TransactionNo` |
| `status` | `status` | `payment_transaction_status_enum` | `PaymentTransactionStatus`, `@JdbcType(PostgreSQLEnumJdbcType)` |
| `transactionTime` | `transaction_time` | TIMESTAMPTZ NOT NULL | Thời điểm tạo; dùng cho `vnp_TransactionDate` khi querydr |

Không kế thừa `BaseEntity` (bảng không có `created_at/updated_at`).

Chuyển trạng thái: `PENDING → SUCCESS` | `PENDING → FAILED`. `SUCCESS`, `FAILED` là trạng thái cuối (GB-01). `REFUNDED` không dùng. "Chưa hoàn tất thanh toán" = `PENDING` và `transaction_time` + 15 phút < hiện tại (chỉ là nhãn hiển thị).

## 3. Hội viên → `tutor_memberships` (entity `TutorMembership`, có sẵn)

| Cột | Giá trị FE-09 ghi khi kích hoạt |
| --- | --- |
| `tutor_id` | Gia sư của người dùng giao dịch |
| `plan_id` | Gói của giao dịch |
| `start_date`, `end_date` | research mục 4 |
| `status` | `ACTIVE` → `EXPIRED` khi đọc và `end_date < today` |
| `amount` | = `payment_transactions.amount` |
| `transaction_ref` | = `transaction_id` (chuỗi UUID) |
| `payment_status` | `SUCCESS` (gán rõ, không dựa mặc định) |
| `payment_gateway` | `VNPay` |

Chỉ tạo khi giao dịch thành công. Mỗi `transaction_ref` có tối đa một bản ghi (bảo đảm bằng khóa dòng giao dịch, research mục 3).

## 4. Nhật ký → `payment_processing_logs` (entity `PaymentProcessingLog`, cần hoàn thiện — plan D-1)

| Thuộc tính | Cột | Ghi chú |
| --- | --- | --- |
| `logId` | `log_id` | UUID PK |
| `transaction` | `transaction_id` | FK NOT NULL |
| `requestPayload` | `request_payload` | JSONB, có trường `type`: `CREATE_PAYMENT_URL`, `IPN`, `RETURN`, `QUERYDR`, `ADMIN_NOTE` |
| `responsePayload` | `response_payload` | JSONB |
| `httpStatusCode` | `http_status_code` | INTEGER nullable |
| `loggedAt` | `logged_at` | TIMESTAMPTZ |

Không ghi hash secret; `vnp_SecureHash` chỉ giữ 8 ký tự đầu. Ghi chú `ADMIN_NOTE` không bao giờ trả ra API phía gia sư.

## 5. Gia sư → `tutors` (entity `core.entity.Tutor`, chỉ cập nhật giá trị, không sửa mã)

| Sự kiện | `tutor_state` | `membership_status` |
| --- | --- | --- |
| Kích hoạt gói | `APPROVED_UNPAID` → `ACTIVE_PAID` (giữ `ACTIVE_PAID` nếu đã có) | `PREMIUM` (giả định A-4) |
| Hết gói, phát hiện khi đọc | `ACTIVE_PAID` → `APPROVED_UNPAID` | `EXPIRED` |

Điều kiện mua: `verification_status = APPROVED` và `users.account_status = ACTIVE`.

## 6. Bài đăng gia sư (US6) — ĐÃ TẠO trên Supabase bằng DBeaver ngày 2026-11-04

Người phụ trách đã chạy câu lệnh dưới đây bằng DBeaver (nhóm quản lý schema trực tiếp trên DB, không dùng file migration). Khóa ngoại đặt tên `fk_tutor_posts_tutors`, `fk_tutor_posts_users`. Bài đăng hiện gồm tiêu đề và nội dung; thêm trường cần câu lệnh `ALTER TABLE` mới.

```sql
DO $$ BEGIN
  CREATE TYPE tutor_post_status_enum AS ENUM ('PENDING_REVIEW', 'PUBLISHED', 'REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE TABLE IF NOT EXISTS tutor_posts (
    post_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tutor_id UUID NOT NULL REFERENCES tutors(tutor_id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    content TEXT NOT NULL,
    status tutor_post_status_enum NOT NULL DEFAULT 'PENDING_REVIEW',
    rejection_reason TEXT,
    reviewed_by UUID REFERENCES users(user_id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tutor_posts_tutor_id ON tutor_posts(tutor_id);
CREATE INDEX IF NOT EXISTS idx_tutor_posts_status ON tutor_posts(status, created_at);
```

**Bổ sung 2026-11-08** (`database/migrations/015_tutor_posts.sql`): thêm `specialties VARCHAR(255)`, `teaching_format VARCHAR(20)` (ONLINE | OFFLINE | BOTH), `hourly_rate NUMERIC(12,2)`. Quy ước: mỗi lần gia sư gửi bài là một dòng; bài hiển thị cho học viên là dòng `PUBLISHED` có `reviewed_at` mới nhất; mỗi gia sư tối đa một dòng `PENDING_REVIEW` (gửi lại khi đang chờ duyệt sẽ sửa dòng đó). Chỉ gia sư có gói hội viên còn hạn mới gửi được bài và mới xuất hiện ở trang Tìm gia sư.

Lý do: BF I.2.2 yêu cầu lưu bài, trạng thái duyệt và lý do từ chối; không bảng nào hiện có đáp ứng. Phương án thay thế: gộp vào `tutors.bio` (không có luồng duyệt riêng, sai BF I.2.2) — không khuyến nghị.
