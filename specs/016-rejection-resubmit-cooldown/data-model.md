# Data Model: FE-16 — Nộp lại sau 3 ngày bị từ chối

Không thêm bảng, không thêm cột, không migration. Chỉ dùng dữ liệu sẵn có.

## Hồ sơ xác minh

| Dữ liệu | Nguồn | Ghi chú |
| --- | --- | --- |
| Trạng thái | `tutors.verification_status` | `REJECTED` là trạng thái chờ nộp lại |
| Thời điểm từ chối | `tutor_verification_histories.created_at` của dòng `action = REJECT` mới nhất | Xem research R1 |
| Thời điểm được nộp lại (tính) | thời điểm từ chối + 3 ngày | Không lưu; rỗng nếu không có dòng lịch sử (được nộp ngay) |
| Giấy tờ | `tutor_credentials.verified_status` | `REJECTED` không tính khi kiểm tra điều kiện nộp |

### Chuyển trạng thái (bổ sung)

```text
REJECTED --(gia sư nộp lại, now >= rejectedAt + 3 ngày, hồ sơ đủ)--> PENDING
```

Các chuyển trạng thái khác giữ nguyên ([003 data-model](../003-tutor-verification/data-model.md)). Mỗi lần nộp lại ghi một dòng lịch sử `SUBMIT` như hiện tại.

## Bài đăng

| Dữ liệu | Nguồn | Ghi chú |
| --- | --- | --- |
| Bài mới nhất | `tutor_posts` mới nhất theo `created_at` | |
| Thời điểm từ chối | `tutor_posts.reviewed_at` của bài `REJECTED` | Rỗng → được gửi ngay |
| Có bản đã duyệt | dòng `PUBLISHED` của gia sư | Có → gửi bản cập nhật ngay (không đổi) |
| Thời điểm được gửi bài mới (tính) | `reviewed_at` + 3 ngày | Chỉ khi chưa có bản đã duyệt |

### Điều kiện gửi bài (sau khi đổi)

| Bài mới nhất | Có bản đã duyệt | Được gửi |
| --- | --- | --- |
| Chưa có | — | Có |
| `NEEDS_REVISION`, `PUBLISHED` | — | Có |
| `PENDING_REVIEW` | — | Không |
| `REJECTED` | Có | Có (ngay) |
| `REJECTED` | Không | Có khi now ≥ `reviewed_at` + 3 ngày |

Gửi sau khi bị từ chối luôn tạo dòng `tutor_posts` mới; bài bị từ chối giữ nguyên.
