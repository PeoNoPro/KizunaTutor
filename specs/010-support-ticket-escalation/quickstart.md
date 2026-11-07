# Quickstart Validation: FE-10

Implementation status and evidence: [validation.md](validation.md). Current paths supersede the original SSR/root-package paths.

## Run locally without unfinished FE-01 login

From `D:\do an 2\KizunaTutor`:

```powershell
.\scripts\start-support-dev.ps1 -AccountRole Cs
```

Open http://localhost:3000/cs/dashboard once the backend is ready; choose “Yêu cầu hỗ trợ” for the full queue at `/cs/support/tickets`. The script restarts only a verified KizunaTutor Java process whose working directory is this repository's backend, starts frontend if missing, applies the additive migration to the private local development DB, and uses an existing active account of the selected role. It does not create fictional tickets or users and never enables the bypass on a shared/remote DB. Missing accounts produce an explicit error.

Switch roles by rerunning with `Manager`, `Learner` or `Tutor`, then use the corresponding `/manager/support/tickets`, `/learner/support/tickets` or `/tutor/support/tickets` route. One local backend uses one development identity at a time. Preserve an open terminal for the backend. Restore the admin session with `.\scripts\start-support-dev.ps1 -AccountRole Admin`; this uses the existing FE-11 admin identity and grants no ticket permissions.

FE-10 is for requesters, CS and the manager receiving escalation. Admin and academic staff do not automatically gain access to private support conversations. The root admin dashboard remains FE-11.

## Database and automated tests

Java 17, Maven wrapper, Node 24, PostgreSQL and a separate database ending `_ticket_test` are required. Verified here on PostgreSQL 18.6; PostgreSQL 16 has not been independently verified. Follow [migration/backup instructions](../../database/migrations/README-fe10.md). Never execute the destructive baseline script to upgrade a database.

Set `TICKET_TEST_DB_URL`, `TICKET_TEST_DB_USERNAME`, `TICKET_TEST_DB_PASSWORD` explicitly; the test datasource refuses missing variables or a non-test DB. Apply the single consolidated `database/seeds/fe10_support_ticket.sql` with psql `-v ON_ERROR_STOP=1` before the integration suite; it includes shift scheduling. The integration context also applies this idempotent schema. No email/payment execution is part of ticket handling.

```powershell
Set-Location backend
.\mvnw.cmd "-Dtest=SupportTicket*Test" test
```

For the complete repository regression, also configure the existing isolated `OPERATIONS_TEST_DB_*` variables and set `SPRING_PROFILES_ACTIVE=ticket-test` so legacy SpringBoot tests use the ticket test database, then run `.\mvnw.cmd test`. Neither test harness may fall back to operational configuration.

Frontend transport tests, from repository root:

```powershell
node --test --test-isolation=none frontend/tests/operations-api.test.mjs frontend/tests/support-api.test.mjs
```

Real browser harness: `frontend/tests/support-tickets.browser.mjs` uses a separate localhost:8090 backend on `*_ticket_test`, a dedicated Chrome profile/CDP port 9340 and the isolated test key in `TICKET_BROWSER_SECRET`. It switches only the test tab's API configuration, exercises four roles and takes screenshots outside the repository. No response data is mocked.

## Attachment runtime

Windows parser launcher enforces 256,000,000 bytes whole-process committed memory, ten seconds per file and thirty seconds per submitted batch. PDFBox/ImageIO run only in the child process. JPG/PNG/PDF, five files maximum, 10,000,000 bytes/file, 52,000,000 bytes/request. Unsupported OS isolation, missing worker dependencies, invalid files or process failure reject the whole submission.

Maven development launch supplies the worker classpath automatically. For packaged fat-jar deployment, configure `app.ticket.parser.classpath` with exploded application classes plus the dependency classpath. Verify a valid upload and the resource-limit probes before accepting uploads on another environment. The server never substitutes in-process parsing.

## Load and human acceptance

[Load harness instructions](../../backend/src/test/load/README.md). SC-005 requires 100 real browser sessions for ten minutes over at least 1,000 populated tickets, with separate list/detail render p95 ≤2 seconds. HTTP/SQL timings are diagnostic only. SC-001 requires ten human participants; automated browser checks do not replace them.

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

## Cập nhật thiết kế và nghiệm thu FE-10

US1 chỉ nghiệm thu tạo phiếu kèm tệp, list/detail, GET hội thoại và tải tệp có phân quyền. POST bổ sung của owner, phản hồi CS và tệp trong hội thoại thuộc US2; chỉ kết luận toàn bộ FR-020 hoàn tất khi checkpoint US1 và US2 đều đạt.

Hội thoại trả sequenceNo và sắp xếp tăng dần theo trường này; sentAt chỉ hiển thị. sequence_no được cấp dưới khóa ticket, UNIQUE(ticket_id, sequence_no). Hai lệnh cùng expectedVersion chỉ một thành công; lệnh stale tải version mới trước khi gửi lại. Kiểm hai message cạnh tranh không trùng số và hiển thị đúng thứ tự ngay khi timestamp trùng.

Ảnh tối đa 20.000.000 pixel; PDF tối đa 100 trang, từ chối bản mã hóa không đọc được. Kiểm tra cấu trúc bằng ImageIO/PDFBox trong tiến trình con riêng: giới hạn cứng 256 MB RAM (256.000.000 byte) bằng cơ chế hệ điều hành, 10 giây/tệp và tổng 30 giây/lần gửi. -Xmx không thay thế giới hạn RAM toàn tiến trình. Hết hạn/vượt bộ nhớ phải dừng tiến trình, dọn tệp tạm và từ chối toàn bộ lần gửi, không lưu phiếu/message/tệp/audit một phần. Không kiểm tra trong JVM ứng dụng; nếu môi trường chưa áp được cách ly và giới hạn thì từ chối tiếp nhận tệp. Thực hiện kiểm tra tệp trước transaction ghi ngắn, rồi kiểm lại quyền/version/trạng thái dưới khóa trước commit.

SC-005: 100 người dùng đồng thời trong 10 phút, 1.000 phiếu có trao đổi; đo riêng p95 danh sách và chi tiết từ thao tác mở trang trên trình duyệt thật đến khi render hoàn tất, mỗi loại ≤2 giây. Báo cáo HTTP timing riêng để chẩn đoán; không dùng HTTP thay phép đo render.

Menu learner và tutor có mục “Yêu cầu hỗ trợ”, dẫn lần lượt tới /learner/support/tickets và /tutor/support/tickets; danh sách cá nhân có nút tạo phiếu. Kiểm thử điều hướng và quyền cả hai vai trò, kể cả truy cập trực tiếp/đổi route.
