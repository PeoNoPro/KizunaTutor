# FE-11 — Bằng chứng hiệu năng

Thực hiện 2026-10-31 đến 2026-11-01 trên máy phát triển Windows, Java 17.0.9, PostgreSQL 18 riêng, Chrome headless và Node 24. Không chạy trên PostgreSQL 16 hoặc máy triển khai đích.

## Lượt tải 600 giây

- 100 tài khoản quản trị kiểm thử khác nhau, 1.000 sự cố và 2.500 sự kiện trong DB cách ly. Mỗi người nghỉ 3 giây giữa các yêu cầu list/detail/report.
- Bắt đầu 2026-10-31 23:47:19, kết thúc 23:57:19 giờ Việt Nam.
- 19.704 yêu cầu thành công, không ghi nhận lỗi. Không tính yêu cầu thất bại thành số liệu thành công.

| Đường đi | Số yêu cầu HTTP | P95 HTTP | Số navigation trình duyệt | P95 trình duyệt |
| --- | ---: | ---: | ---: | ---: |
| Cấu hình | Không nằm trong vòng HTTP tải | — | 37 | 636,7 ms |
| Sức khỏe | Không nằm trong vòng HTTP tải | — | 37 | 405,2 ms |
| Danh sách sự cố | 6.600 | 126,8 ms | 37 | 561,3 ms |
| Chi tiết sự cố | 6.600 | 125,3 ms | Không lấy mẫu navigation riêng | — |
| Báo cáo | 6.504 | 128,4 ms | 37 | 320,7 ms |

Đo HTTP và trình duyệt riêng. Artifact đầy đủ: `target/fe11-load-results-600s.json`, `target/fe11-load-run.log`. Script trình duyệt chờ nội dung hoàn thành; bản script cuối chờ thêm hai frame trước lấy mốc. Những mẫu đầu của lượt dài lấy mốc DOM hoàn thành, vì vậy không dùng bảng này để tuyên bố đã chốt toàn SC-006.

## Kiểm tra sau thay đổi cuối

- Lượt 15 giây sau bổ sung gate quyền trước transaction: 100 tài khoản, cùng 1.000 sự cố, 500 yêu cầu thành công, không lỗi; p95 HTTP list/detail/report lần lượt 167,3/150,7/157,6 ms.
- Một lượt navigation mỗi trang trong tải ngắn: cấu hình 672,4 ms, sức khỏe 472,1 ms, danh sách 475,5 ms, báo cáo 355,6 ms. Mẫu đơn chỉ là smoke check, không có ý nghĩa nghiệm thu p95 dài hạn.
- Trình duyệt mô phỏng nguồn API không trả lời: thông báo lỗi sau 4.507,7 ms, không thay lỗi bằng số 0. Bàn phím, menu mobile và nội dung nhập được kiểm riêng trong results.json.
- JUnit kiểm timeout probe và identity pool cạn: fail-closed hữu hạn, không bỏ kiểm quyền. Snapshot không đổi observedAt khi đọc; dữ liệu hơn 60 giây bị đánh dấu quá hạn.
- EXPLAIN ANALYZE truy vấn as-of trên bản restore 1.000 sự cố: khoảng 1,49 ms ở lượt đo; dùng index lịch sử hiện có, chưa có bằng chứng cần migration index bổ sung. File `target/fe11-explain.txt`.

## Giới hạn nghiệm thu

Harness dùng service/repository/controller/template/security thật của FE-11 nhưng giả lập JWT provider trong src/test; không đo đăng nhập/chữ ký JWT thật hoặc toàn bộ monolith. Browser chi tiết có smoke chức năng nhưng chưa có mẫu p95 navigation đủ dưới tải. Thay đổi cuối được đối chiếu bằng lượt ngắn, chưa chạy lại toàn 600 giây. SC-006 vì vậy chưa được xác nhận đầy đủ trên bản cuối và môi trường đích.

Backup/restore DB kiểm thử đã thực hiện bằng pg_dump/pg_restore, giữ đủ 1.000 incident, 2.500 event và singleton cấu hình. Không áp migration hoặc restore lên DB dự án.
