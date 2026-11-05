# Quickstart: Kiểm chứng FE-09 — Membership & Payment

## Điều kiện tiên quyết

- Java 17, Node.js, DBeaver kết nối Supabase (chỉ có DB Supabase, không có DB kiểm thử riêng).
- Biến môi trường (đặt trên máy, không ghi vào repo): `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`; tùy chọn `VNPAY_RETURN_URL` (mặc định `http://localhost:8085/tutor/membership/result` (backend phục vụ cả giao diện; phiên đăng nhập lưu theo cổng)).
- Tài khoản: 1 gia sư đã phê duyệt (qua luồng FE-03), 1 gia sư chưa phê duyệt, `manager@kizuna.vn`, `admin@kizuna.vn`.
- Thẻ thử nghiệm VNPay sandbox (ngân hàng NCB) theo tài liệu sandbox VNPay.
- Thử IPN (tùy chọn): dựng tunnel (ngrok/cloudflared) tới cổng 8085 và đăng ký `https://{tunnel}/api/payments/vnpay/ipn` trong trang quản trị merchant sandbox. Không có tunnel: kết quả xác nhận qua tra cứu cổng khi quay về trang kết quả.

## Chạy

```powershell
cd backend; .\mvnw.cmd spring-boot:run      # cổng 8085
node frontend/server.js                      # cổng 3000
```

## Kiểm thử tự động

```powershell
cd backend; .\mvnw.cmd test -Dtest="*Membership*,*Payment*,*Vnpay*"
```

Chỉ unit test và `@WebMvcTest`, không chạm DB. Test tích hợp DB bị bỏ qua (không có DB kiểm thử) — ghi rõ trong báo cáo.

## Kịch bản nghiệm thu

| # | Bước | Kết quả mong đợi | Truy vết |
| --- | --- | --- | --- |
| 1 | Gia sư đã duyệt mở `/tutor/membership` | Thấy gói đang mở bán, giá `199.000 đ`, quyền lợi; "Bạn chưa có gói hội viên" | US1-1, US2-2 |
| 2 | Gia sư chưa duyệt mở trang, bấm mua | MSG34, không tạo giao dịch | US1-7, FR-002 |
| 3 | Chọn gói 1 tháng, xác nhận | Chuyển sang cổng VNPay; DB có giao dịch `PENDING` đúng số tiền | US1-2, FR-003 |
| 4 | Thanh toán bằng thẻ thử, quay về | MSG31; gói `ACTIVE` từ hôm nay đến +1 tháng −1 ngày; `tutor_state = ACTIVE_PAID`; email đến | US1-3, FR-007/008/010 |
| 5 | Tải lại trang kết quả nhiều lần; gửi lại IPN cùng tham số | Chỉ 1 bản ghi hội viên; IPN lần sau trả `02` | US1-5, SC-002 |
| 6 | Sửa 1 ký tự `vnp_Amount` trong URL kết quả / IPN | 400 "Kết quả thanh toán không hợp lệ." / `97`; không đổi dữ liệu | SC-003 |
| 7 | Mua tiếp gói 3 tháng khi gói cũ còn hiệu lực | Gói mới bắt đầu ngày sau ngày hết hạn cũ; trang hiện "Đã gia hạn, bắt đầu từ …" | US1-8, FR-008 |
| 8 | Mua gói rồi bấm "Hủy giao dịch" trên cổng | MSG32; giao dịch `FAILED`; nút thanh toán lại | US1-4, FR-009 |
| 9 | Tạo giao dịch rồi đóng tab, quản trị viên bấm "Kiểm tra với cổng thanh toán" | Trạng thái đồng bộ theo cổng; nhật ký có `QUERYDR` | US5-2, FR-020 |
| 10 | Quản trị viên ghi chú điều tra | Ghi chú hiện trong chi tiết; gia sư không thấy | US5-4, FR-021 |
| 11 | Quản lý đổi giá gói 1 tháng | Gia sư thấy giá mới; giao dịch cũ giữ giá cũ | US3-2, FR-015 |
| 12 | Quản lý nhập giá 0 / bỏ trống tên | Bị chặn, MSG36/MSG02 | US3-3 |
| 13 | Quản lý ngừng bán một gói | Gia sư không còn thấy gói đó | US3-5 |
| 14 | Quản lý mở danh sách hội viên, lọc theo gói, tìm tên không tồn tại | Đúng dữ liệu; MSG01 | US4-1/2 |
| 15 | Quản lý mở chi tiết hội viên | Các lần đăng ký và lịch sử thanh toán | US4-3 |
| 16 | Hết hạn: người phụ trách tạm đổi `end_date` của một gói thử về hôm qua bằng DBeaver, gia sư mở trang | "Đã hết hạn"; `tutor_state` trở về `APPROVED_UNPAID`; trả lại giá trị sau khi thử | US2-3, FR-013 |
| 17 | Học viên gọi API quản lý; gia sư gọi API quản trị; không token | 403; 403; 401 | FR-023, SC-007 |
| 18 | Bỏ biến môi trường VNPay, khởi động lại, bấm mua | Ứng dụng vẫn chạy; MSG37 | FR-025 |
| 19 | Rà soát giao diện (1240px, 375px) | Tiếng Việt, không in hoa toàn bộ, không mã nội bộ; trạng thái tải/rỗng/lỗi + "Thử lại" | FR-024 |

## Dọn dẹp

Giao dịch và hội viên sinh ra khi thử là dữ liệu thật của luồng thanh toán sandbox; người phụ trách quyết định giữ hay xóa bằng DBeaver. AI không xóa dữ liệu.
