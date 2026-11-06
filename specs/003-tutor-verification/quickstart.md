# Quickstart: Kiểm chứng FE-03 — Tutor Verification

Hướng dẫn chạy và nghiệm thu đầu-cuối. Chi tiết dữ liệu: [data-model.md](data-model.md); API: [contracts/verification-api.md](contracts/verification-api.md).

## Điều kiện tiên quyết

- Java 17, Node.js, DBeaver kết nối Supabase (phương án DB 3: không có DB kiểm thử riêng).
- Người phụ trách đã chạy 3 câu lệnh thay đổi schema tại [data-model.md](data-model.md) §3 trên Supabase bằng DBeaver (chạy hai lần để xác nhận idempotent; bản ghi lịch sử cũ giữ nguyên).
- SMTP Gmail đang cấu hình. Thư mục lưu tệp `app.verification.storage-dir` tồn tại, có quyền ghi, nằm ngoài `frontend/`.
- Tài khoản thử nghiệm tạo qua chức năng đăng ký thật (không chèn bản ghi bằng SQL): 1 cán bộ học vụ (`ROLE_ACADEMIC_OFFICER`, dùng tài khoản mẫu có sẵn), 2 gia sư A và B, 1 học viên. Vì đăng ký hiện đặt sẵn "Đã phê duyệt" (A-11), người phụ trách đổi `verification_status` của A và B về `UNVERIFIED` bằng DBeaver. Kịch bản tài khoản bị tạm ngưng: tạm đổi `account_status` của B sang `SUSPENDED` rồi trả lại sau khi thử. Ghi danh sách tài khoản thử nghiệm để dọn dẹp.

## Chạy

```powershell
cd backend; .\mvnw.cmd spring-boot:run      # cổng 8085
node frontend/server.js                      # cổng 3000
```

## Kiểm thử tự động

```powershell
cd backend; .\mvnw.cmd test -Dtest="*Verification*"
```

Lệnh trên chỉ chạy unit test và `@WebMvcTest`, không chạm DB. Bộ test tích hợp DB (`*IT`) tự bỏ qua khi thiếu `VERIFICATION_TEST_DB_URL` — với phương án DB 3 chúng luôn bị bỏ qua và phải được ghi rõ trong báo cáo. Không đặt biến này trỏ tới Supabase dùng chung.

## Kịch bản nghiệm thu

| # | Bước | Kết quả mong đợi | Truy vết |
| --- | --- | --- | --- |
| 1 | Gia sư A mở `/tutor/verification` | "Chưa nộp hồ sơ", nút "Bắt đầu xác minh" | US1-1, FR-001 |
| 2 | Nhập thông tin, bấm "Lưu thay đổi" | Thông báo đã cập nhật; vẫn sửa được; DB không có lịch sử mới | US1-2 |
| 3 | Tải tệp 11MB, rồi tệp `.exe` đổi đuôi `.pdf` | MSG13, trường lỗi được đánh dấu, không có tệp mới trong thư mục lưu | US1-3, FR-004 |
| 4 | Chỉ tải chứng chỉ JLPT, bấm "Nộp hồ sơ" | Thông báo không thể nộp, liệt kê thiếu "Giấy tờ tùy thân" | US1-4, FR-005 |
| 5 | Tải giấy tờ tùy thân, nộp mà không tích cam kết | Bị chặn | FR-006 |
| 6 | Tích cam kết, nộp; bấm nộp lần 2 nhanh (hoặc gửi 2 request song song) | MSG12, "Đang chờ thẩm định", form chỉ đọc; đúng 1 mục lịch sử `SUBMIT` | US1-5/6, FR-007/008, SC-006 |
| 7 | Cán bộ mở `/academic/verifications` | Thẻ tóm tắt đúng số; hồ sơ A trong hàng đợi, ngày `dd/MM/yyyy`, cũ nhất trước | US2-1, FR-010/011, SC-002 |
| 8 | Tìm "khongtontai"; xóa lọc nhóm PENDING khi rỗng | MSG01; "Hiện không còn hồ sơ nào chờ thẩm định." | US2-2, FR-012 |
| 9 | Mở chi tiết A, xem PDF và ảnh; xóa tệp trên đĩa rồi tải lại | Xem trực tiếp; tệp mất hiển thị thông điệp UC-27 AF-1, trang không hỏng | US2-3, FR-013 |
| 10 | Gửi yêu cầu bổ sung rỗng, rồi hợp lệ | Chặn với thông điệp UC-26 AF-1; sau đó "Đã gửi yêu cầu bổ sung thông tin.", email đến A | US3-1/2, FR-014 |
| 11 | Gia sư A mở trang | "Cần bổ sung thông tin", thấy nội dung yêu cầu, không thấy ghi chú nội bộ; form mở khóa; sửa và nộp lại | US3-3/4, FR-009 |
| 12 | Cán bộ bấm "Từ chối" thiếu lý do/ghi chú; rồi từ chối đủ thông tin | Thông điệp UC-28 AF-1; sau đó "Bị từ chối", email kèm lý do | US2-5/6, FR-016 |
| 13 | Gia sư B (bị từ chối ở kịch bản 12 — dùng hồ sơ B) mở trang; thử lưu, tải giấy tờ, nộp | Thấy lý do từ chối, biểu mẫu chỉ đọc; mọi thao tác bị chặn với thông điệp "đã bị từ chối… không thể chỉnh sửa hoặc nộp lại"; không có mục lịch sử mới. Gia sư A (cần bổ sung) nộp lại; cán bộ phê duyệt → "Đã phê duyệt", email chào mừng, A xuất hiện trong tìm kiếm gia sư (FE-04) | US3-5, US2-4, FR-015, A-9 |
| 14 | Mở lịch sử của A | Theo thời gian: Nộp → Yêu cầu bổ sung → Nộp → Phê duyệt, có tên cán bộ, phản hồi, ghi chú nội bộ | US5-2, FR-018 |
| 15 | Hai tab cán bộ mở hồ sơ B đang chờ; tab 1 phê duyệt, tab 2 từ chối | Tab 2 nhận thông điệp GB-01; chỉ 1 quyết định trong lịch sử | US2-7, FR-017, SC-005 |
| 16 | Gia sư B gọi `GET /api/tutor/verification/documents/{id của A}/file`; học viên gọi `/api/academic/verifications`; không token; gia sư bị `SUSPENDED` | 404; 403; 401; 401/403 — không lộ dữ liệu | FR-019/020, SC-004 |
| 17 | Tắt SMTP rồi phê duyệt một hồ sơ | Quyết định vẫn lưu; log nội bộ ghi lỗi gửi email | FR-022 |
| 18 | Rà soát mọi màn hình FE-03 (desktop 1240px, mobile 375px) | Không chữ in hoa toàn bộ, không mã enum/HTTP; có trạng thái tải/rỗng/lỗi + "Thử lại"; trang gia sư theo DESIGN mục 7 | FR-021, SC-007 |

## Hiệu năng (SC-003)

Chưa đo theo phương án DB 3: đo tải cần 1.000 gia sư, không được sinh vào DB dùng chung (Nguyên tắc II). Chỉ ghi thời gian phản hồi quan sát được trong DevTools khi nghiệm thu, nói rõ không phải số đo tải. Có thể đo sau trên `kizuna-postgres` (`docker-compose.yml`).

## Ghi kết quả

Ghi vào báo cáo triển khai: các kịch bản đã chạy, kết quả, test bị bỏ qua và nguyên nhân, giới hạn còn lại (tệp mồ côi khi tiến trình chết, phụ thuộc FE-01 về trạng thái ban đầu).
