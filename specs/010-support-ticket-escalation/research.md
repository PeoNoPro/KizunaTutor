# Research: FE-10

Ngày: 2026-10-29. Căn cứ: spec với 5 câu trả lời, constitution 1.0.0 và mã nguồn tại thời điểm lập kế hoạch. Đây là quyết định thiết kế, chưa phải kết quả chạy hệ thống.

## 1. Kiến trúc và phạm vi

- Decision: Giữ Java 17, Spring Boot 3.3.4, JPA, PostgreSQL, Thymeleaf; mở rộng service/controller/repository có sẵn.
- Rationale: `pom.xml` xác nhận stack; `SupportTicketController`, `SupportTicketServiceImpl`, `ISupportTicketService` còn khung. Không cần ứng dụng frontend hoặc microservice mới.
- Alternatives: SPA và dịch vụ ticket riêng làm tăng phạm vi không có yêu cầu tương ứng.

## 2. Dữ liệu và thay đổi schema

- Decision: Migration SQL tăng dần có phiên bản tại `src/main/resources/db/changes/`, thực thi có kiểm soát bằng psql; không thêm framework migration cho toàn dự án ở FE-10. Có bảng ghi phiên bản và khóa giao dịch để ngăn chạy lại/đồng thời. Script tự kiểm tra điều kiện đầu vào và dừng khi schema không tương thích.
- Rationale: `application.yml` đặt `ddl-auto: none`, pom chưa có Flyway/Liquibase. `DatabasePgAdmin4.txt` là script dựng lại có DROP CASCADE, không được chạy trên dữ liệu hiện hữu để triển khai tính năng.
- Alternatives: Hibernate tự sửa schema không đủ kiểm soát/backfill; bật Flyway ngay trên schema chưa baseline tác động toàn dự án.
- Gaps: Ticket entity chỉ có ID, mapping `ticketId` không khớp `ticket_id`; TicketMessage có vấn đề tương tự và kế thừa BaseEntity không khớp bảng chỉ có `sent_at`. Cần mapping rõ, không dựa vào naming strategy.
- Decision: Dùng escalation riêng và audit append-only, không chỉ ghi đè `escalation_notes`; giữ cột legacy để chuyển đổi tương thích.

## 3. Phân quyền

- Decision: Một policy service dùng User hiện tại, đọc lại vai trò/trạng thái từ DB mỗi request; kiểm tra chủ phiếu, CS, quản lý nhận ngay trong service và khi tải tệp. Không serialize entity trực tiếp; DTO công khai và nội bộ tách biệt.
- Rationale: SecurityConfig hiện chỉ yêu cầu authenticated cho `/api/v1/tickets`; JwtAuthenticationFilter nhận role từ token và chưa bảo đảm tài khoản còn hoạt động. SecurityContextUtil hỗ trợ lấy User nhưng không thay thế kiểm tra nghiệp vụ.
- Alternatives: Chỉ kiểm tra route hoặc ẩn nút không chặn truy cập ngang và tải tệp trực tiếp.
- Decision: CS xem mọi phiếu nhưng chỉ assignee thực hiện nghiệp vụ, mọi CS được phân công lại. Manager chỉ current recipient; giữ recipient sau trả kết quả cho tới lần chuyển tiếp theo. Đổi recipient thu hồi quyền người cũ. Chủ phiếu mới có quyền đóng.

## 4. Bảo vệ request có cookie

- Decision: Bổ sung CSRF cho các thao tác ghi FE-10, gồm multipart, không thay chính sách endpoint ngoài FE-10; UI lấy token được gắn vào trang và gửi header. Bảo vệ mọi POST ticket, kể cả khi dùng JWT cookie; kiểm thử token thiếu/sai và đúng. Session token phục vụ CSRF không thay cơ chế JWT đăng nhập.
- Rationale: Cookie `access_token` được dùng để xác thực nhưng SecurityConfig đang disable CSRF toàn cục. FE-10 phải xử lý khoảng trống ở chính route được bổ sung.
- Alternatives: Chỉ SameSite hoặc kiểm tra role không đủ thay thế xác nhận request hợp lệ.

## 5. Tệp và tính nguyên tử

- Decision: Lưu metadata và bytes (`bytea`, truy cập JDBC riêng) trong PostgreSQL cùng transaction với phiếu/trao đổi/audit. Không đọc bytes trong list/detail; tải từng tệp qua controller kiểm tra quyền. Không dùng public URL hoặc static upload folder.
- Rationale: Spec yêu cầu toàn bộ lần gửi thành công hoặc không lưu gì; tối đa 50 MB/lần gửi. Cùng DB tránh trạng thái DB commit nhưng file ngoài bị mất. Khối lượng 1.000 phiếu chưa có yêu cầu kho đối tượng riêng.
- Alternatives: Filesystem cần staging/cleanup và xử lý crash; object storage cần vận hành và nhất quán nhiều hệ thống. Xem xét lại khi kích thước lưu trữ vượt khả năng backup hiện có.
- Decision: Streaming multipart sang vùng tạm do servlet quản lý; kiểm tra tối đa 5 tệp, mỗi tệp 10.000.000 byte; giới hạn request 52.000.000 byte để đủ 50 MB cộng metadata. Không load cả request vào RAM. Dùng ImageIO đọc ảnh và Apache PDFBox parser cho PDF, giới hạn tài nguyên parser; bổ sung dependency parser lúc implement sau khi xác minh bản tương thích. Không chỉ tin đuôi/MIME/magic bytes. Trả attachment download, nosniff; tên lưu không bao giờ là filesystem path.
- Rationale: Chỉ header `%PDF` không phát hiện file hỏng. Định dạng hợp lệ không đồng nghĩa đã quét mã độc; không tuyên bố có antivirus nếu chưa triển khai.

## 6. Cạnh tranh, liên kết và kiểm chứng

- Decision: `@Version` cho ticket, expectedVersion bắt buộc mọi lệnh cập nhật; khóa ticket để serialize quyết định trạng thái, role và recipient. Unique partial index bảo đảm chỉ một escalation pending; audit chung transaction.
- Rationale: Tránh hai lần nhận, quyết định cũ được dùng sau đổi quản lý, hoặc phản hồi đồng thời với đóng. Các lệnh stale trả 409 để tải lại.
- Decision: Related-context adapter dùng projection SQL rõ cột theo schema, không phụ thuộc entity TrialBooking/PaymentTransaction đang thiếu ownership. Kiểm tra learner/tutor của booking, tutor sở hữu membership, user sở hữu giao dịch có plan hội viên. Chỉ đọc, không tác động nghiệp vụ bên kia.
- Alternatives: Tin ID client hoặc dùng stub entity không đủ kiểm tra quyền.
- Decision: JUnit/MockMvc cho hợp đồng và phân quyền; PostgreSQL kiểm thử riêng cho enum, rollback, bytea, cạnh tranh và migration. Không dùng DB vận hành hoặc seed dữ liệu giả vào UI.

## Kết luận

Không còn câu hỏi nghiệp vụ chặn thiết kế. Phiên bản thư viện PDF parser và khả năng schema triển khai thực tế là kiểm tra phụ thuộc tại bước triển khai, không thay đổi hợp đồng chức năng. Không chạy migration, test ứng dụng hoặc đổi cấu hình trong bước lập kế hoạch.
