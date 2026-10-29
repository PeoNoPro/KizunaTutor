# Quickstart Validation: FE-10

Đây là hướng dẫn chạy sau khi triển khai, không xác nhận code hoặc test dưới đây đã tồn tại/đã chạy.

## Prerequisites

- Java 17, Maven Wrapper, Docker/PostgreSQL 16, môi trường DB kiểm thử tách biệt.
- Đã hoàn tất migration FE-10 và cấu hình ứng dụng trỏ đúng DB. Không chạy migration trên dữ liệu thật để thử nghiệm.
- Các tài khoản test riêng: hai learner, một tutor, hai CS và hai manager còn hoạt động; có cách khóa tài khoản/đổi role để thử thu hồi quyền.
- Dữ liệu booking, membership và payment thật trong môi trường kiểm thử, có cả đối tượng thuộc người khác. Fixture kiểm thử không được đưa vào dữ liệu hiển thị vận hành.
- Tệp JPG, PNG, PDF đọc được; file hỏng/giả mạo; file đúng 10.000.000 byte, vượt một byte, tập 5 và 6 tệp. Bộ tạo fixture phải tạo file thực sự đọc được, không chỉ padding header.

## Setup and commands

Chạy tại thư mục repo trong PowerShell:

```powershell
java -version
.\mvnw.cmd -version
docker compose up -d kizuna-postgres
```

Compose trên dành cho môi trường phát triển, không tự tạo DB kiểm thử. Chuẩn bị DB riêng theo cấu hình test hiện có; không đưa mật khẩu vào lệnh chia sẻ/log.

Khi triển khai, cung cấp script SQL có phiên bản trong `src/main/resources/db/changes/` kèm README về preflight, backup/restore và lệnh psql cụ thể. Chỉ chạy theo hướng dẫn đó trên DB kiểm thử; không dùng `DatabasePgAdmin4.txt` chứa DROP CASCADE để nâng cấp DB hiện hữu. Bước này không tạo migration giả để lệnh chạy được.

Profile `ticket-test` dự kiến nhận `TICKET_TEST_DB_URL`, `TICKET_TEST_DB_USERNAME`, `TICKET_TEST_DB_PASSWORD` từ môi trường. Profile phải fail nếu thiếu hoặc trỏ vào DB không được đánh dấu kiểm thử. Sau khi code/test đã được tạo:

```powershell
.\mvnw.cmd "-Dspring.profiles.active=ticket-test" "-Dtest=SupportTicket*Test" test
.\mvnw.cmd "-Dspring.profiles.active=ticket-test" test
```

Ứng dụng phát triển dùng cấu hình dự án hiện có, địa chỉ mặc định localhost:8080:

```powershell
.\mvnw.cmd spring-boot:run
```

Không khởi động agent-memory cho riêng FE-10 trừ khi cấu hình ứng dụng hiện hữu yêu cầu. Bảo đảm môi trường được dùng không gửi email/thực thi thanh toán thật trong các test liên quan ngoài module.

## End-to-end acceptance

1. **Tạo/theo dõi**: Learner gửi phiếu cho mỗi phạm vi FE-10, kiểm tạo một lần, OPEN/MEDIUM, first message, reload không mất. Tutor thực hiện cùng luồng. Thử text thiếu/trắng/quá dài. Learner khác không được mở phiếu hoặc tệp. Không liên kết dữ liệu vẫn gửi được.
2. **Xử lý**: CS1 nhận, hỏi thêm; owner trả lời; CS2 chuyển assignee sang mình. CS1 mất quyền phản hồi/resolve nhưng vẫn được xem và phân công lại. CS2 đổi ưu tiên, gửi kết luận và resolve.
3. **Đóng**: CS/manager không được đóng RESOLVED. Owner bổ sung làm về IN_PROGRESS; resolve lại rồi owner xác nhận CLOSED. Không tự đóng theo thời gian; sau CLOSED không gửi message/chuyển cấp/đổi assignee.
4. **Chuyển cấp**: CS phụ trách chọn manager1 kèm reason/summary. Manager2 không thấy list/detail/history/tệp của phiếu; manager1 lưu quyết định nhưng owner không thấy. Return đưa về IN_PROGRESS; CS gửi kết luận công khai.
5. **Đổi quản lý**: Chuyển cấp tiếp, lưu decision, đánh dấu người nhận vắng mặt hoặc ngừng hoạt động. Chỉ assignee được đổi manager2 kèm lý do. Manager1 không tải được tệp dù giữ URL; decision cũ chỉ nằm trong lịch sử. Manager2 không return được trước khi tự ghi decision mới. Đổi CS trong khi pending thì return về CS mới.
6. **Context**: Chọn booking/membership/payment đúng chủ; đối tượng người khác bị từ chối. Chỉ payment hội viên được liên kết. Xóa/ngừng cung cấp context trong môi trường test không làm mất phiếu. Trạng thái lịch, tiền, hội viên không đổi do xử lý ticket.
7. **Tệp**: Tạo phiếu/message với 0..5 tệp, thử đúng/vượt giới hạn; thử sai MIME/đuôi/content, file hỏng, PDF không mở được. Một file sai phải làm cả lần gửi thất bại, giữ text để sửa; kiểm DB không còn message/attachment/audit một phần. Người được phép tải đúng bytes, metadata/detail không chứa binary.
8. **Security**: Account inactive/deleted hoặc DB role thay đổi bị từ chối dù token cũ còn hạn. Kiểm CSRF thiếu/sai, role không hợp lệ, owner giả, đoán UUID, truy cập tệp trực tiếp, nội dung script được escaped; owner response không có trường nội bộ.
9. **Concurrency/rollback**: Hai CS claim cùng version chỉ một thành công; update stale trả 409. Race close/message, manager change/decision/return, assignee change/reply không ghi đè. Mô phỏng lỗi lưu attachment/audit để xác nhận toàn bộ transaction rollback.
10. **UI**: Rỗng/lỗi/loading riêng, nhãn tiếng Việt có dấu, không all-caps/enum/exception thô; keyboard và field errors hoạt động; learner/tutor đúng DESIGN mục 7.

## Migration and performance evidence

- Test migration trên schema cũ có dữ liệu: số phiếu/message trước-sau không đổi; map ID đúng; escalation legacy thiếu dữ liệu phải báo rõ thay vì tạo tác giả giả. Lần chạy lại không thêm bảng/dòng dư; rollback triển khai bằng restore bản backup đã thử, không hứa có down migration phá dữ liệu.
- SC-001: 10 người thử tạo/tìm lại phiếu, ghi thời gian và số cần trợ giúp; đạt ≥9 người trong 3 phút.
- SC-005: Dùng công cụ tải của môi trường kiểm thử với 100 phiên xác thực/10 phút, 1.000 phiếu và trao đổi; đo p95 riêng list/detail ≤2 giây, error rate và đối chiếu mutation/audit. Không tính bytes tệp vào list và không gọi mục tiêu là kết quả nếu chưa đo.
- Báo cáo kết quả theo FR-001–021 và SC-001–007; ghi lệnh, môi trường, phiên bản schema, số ca đạt/thất bại/chưa chạy, lỗi và giới hạn. Hợp đồng xem [contracts/tickets.md](contracts/tickets.md), mô hình xem [data-model.md](data-model.md).
