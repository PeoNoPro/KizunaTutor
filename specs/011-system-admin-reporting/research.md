# Research: FE-11

Ngày 2026-10-30. Căn cứ: spec với năm quyết định được chốt, constitution 1.0.0 và repo hiện tại. Chưa chạy kiểm thử ứng dụng hoặc kết nối dịch vụ trong lượt này.

## 1. Giữ kiến trúc và sửa khoảng trống mapping

- **Decision**: Giữ Java 17, Spring Boot 3.3.4, Thymeleaf, Spring Security, JPA và PostgreSQL. Tạo service quản trị vận hành theo trách nhiệm, không tạo microservice/frontend mới.
- **Rationale**: pom hiện có stack này. SystemConfiguration chỉ có configKey, dùng tên cột configKey và kế thừa BaseEntity, trong khi schema nguồn là config_key/config_value/data_type/description/updated_at, không có created_at. Hoàn thiện mapping rõ cột, không serialize entity ra người dùng.
- **Alternatives considered**: Dựng lại dashboard/ứng dụng hoặc dùng key-value editor tổng quát làm tăng phạm vi và cho sửa các khóa ngoài hai tham số đã chốt.

## 2. Cấu hình nhất quán và tích hợp đặt học thử

- **Decision**: Giữ khóa system_configurations hiện hữu cho tương thích; thêm bảng phiên bản cấu hình UUID, snapshot hai giá trị, audit và version dùng cho cạnh tranh. Một active revision; đọc/chỉnh trong transaction khóa một hàng revision. Không cache không có cơ chế invalidation. Giữ chỗ chưa thanh toán 15 phút chỉ đọc, không biến thành tham số chỉnh sửa.
- **Rationale**: Có hai giá trị nguồn 45 phút/12 giờ nhưng chưa chứng minh DB đang chạy có giá trị đó. Không tự seed hoặc ghi đè giá trị hiện hữu. Có lịch sử để lịch đã tạo giữ quy tắc cũ.
- **Decision**: Policy evaluator nhận thời điểm server, thời điểm bắt đầu, trả duration/minAdvance/revision và kiểm khoảng đặt trước; lưu snapshot trong cùng transaction tạo booking, tính scheduled_end_time theo duration snapshot, không tính lại booking cũ.
- **Integration gap**: TrialBookingController, TrialBookingServiceImpl và TrialBookingRequest đều là stub. Không tuyên bố US1 hoàn tất chỉ từ kiểm thử evaluator. Khi triển khai phải xác định hoặc bổ sung điểm gọi thật trong luồng tạo booking được dự án cho phép; phần nhận lịch/tutor/ownership/availability nếu chưa có là phụ thuộc của module đặt học thử, phải được ghi blocker trong tasks và validation. Không tự thiết kế thanh toán hoặc toàn bộ đặt lịch trong FE-11 để che phụ thuộc.
- **Alternatives considered**: Chỉ ghi hai dòng cấu hình không chứng minh FR-004; thay mọi booking cũ vi phạm snapshot. Giao dịch đặt booking và đổi config khóa cùng active revision để xác định thứ tự hiệu lực bằng commit, tránh race theo đồng hồ.

## 3. Quan sát có căn cứ

- **Decision**: Không dùng `/api/test/health` hoặc serverStatus hardcode làm bằng chứng sức khỏe. Quan sát ứng dụng là heartbeat của tiến trình đang chạy, DB là query probe qua pool riêng/read-only với timeout, dịch vụ bộ nhớ AI chỉ có probe khi deployment cung cấp hợp đồng health đã kiểm chứng. Nếu thiếu hợp đồng thì UNKNOWN kèm lý do “Chưa có nguồn kiểm tra”, không tự đoán URL hoặc trạng thái khỏe.
- **Rationale**: TestController hiện trả UP cố định; AdminController gán serverStatus “Đang hoạt động”. AgentMemoryService có cấu hình base-url nhưng chưa xác minh health endpoint. Heartbeat không chứng minh website khả dụng từ Internet; UI phải ghi phạm vi “Tiến trình ứng dụng”.
- **Decision**: Collector định kỳ 30 giây, mỗi probe tối đa 2 giây, budget toàn lượt tối đa 4 giây, chạy hữu hạn song song, không chồng lượt. Lưu kết quả mới nhất thành immutable snapshot trên bộ nhớ để đọc sức khỏe không phụ thuộc DB đang lỗi. Endpoint chỉ đọc snapshot, trả nhanh; client refresh 30 giây, đánh dấu quá hạn theo tuổi >60 giây. Refresh thủ công đọc snapshot hiện tại, không spam probe đồng bộ.
- **Rationale**: Có thể trả DB unavailable ngay cả khi DB không phục vụ truy vấn nghiệp vụ. Khi tiến trình web tự chết, chính trang này không thể phục vụ; theo dõi từ bên ngoài không thuộc FE-11 và không được hứa là đã có.
- **Alternatives considered**: Chỉ kiểm JDBC lúc mỗi request làm treo UI và cạn pool; tổng hợp mọi lỗi thành UP tạo dữ liệu giả. Không cần thêm hệ thống metrics ngoài dự án cho ba nguồn đã nêu.

## 4. Sự cố, lịch sử và báo cáo

- **Decision**: Sự cố tạo thủ công, event append-only có seq tăng trong mỗi sự cố, snapshot mức độ/thành phần/phân công/trạng thái tại event. Ticket FE-10 và technical incident độc lập; không tự tạo incident từ probe.
- **Rationale**: Báo cáo quá khứ không thể tính đúng từ trạng thái hiện tại. Ngày dùng Asia/Ho_Chi_Minh với khoảng nửa mở: đầu ngày start tới đầu ngày sau end; ngày hiện tại chốt tại thời điểm báo cáo. Query state as-of cutoff, không dùng updated_at hiện tại để đoán trạng thái quá khứ.
- **Decision**: Mức độ và thành phần ghi lúc tạo được giữ nguyên trong bản này; thay đổi sau chỉ phân công, tiến độ và vòng đời có event. Không thêm quyền sửa/xóa hồ sơ ngoài các lệnh đã đặc tả.
- **Alternatives considered**: Đếm snapshot hiện tại làm sai tồn cuối kỳ và giải quyết nhiều lần; thêm báo cáo doanh thu/usage trái câu trả lời clarify.

## 5. Chính sách đọc từ nguồn được duyệt

- **Decision**: Tài liệu được duyệt nạp qua quy trình quản lý tài liệu/SQL có kiểm soát vào bảng policy_document_versions; không editor/publish API. Query chỉ bản published và có hiệu lực. Không có tài liệu thì empty state. Hiển thị nội dung text đã duyệt, không thực thi HTML không tin cậy.
- **Rationale**: Repo chưa tìm thấy tài liệu terms/policy sẵn để khẳng định có nội dung được duyệt. Cấu trúc lưu có thể được xây và test với fixture riêng, nhưng nghiệm thu nội dung thật phụ thuộc chủ dự án cung cấp bản đã duyệt. Không dùng AI tự viết chính sách thay chủ dự án.
- **Alternatives considered**: CMS mới vượt phạm vi; hardcode điều khoản mẫu vi phạm constitution. Dữ liệu công bố chồng lấn phải fail closed, không chọn ngẫu nhiên.

## 6. Bảo mật, migration và kiểm chứng

- **Decision**: Reload current User/role/account ACTIVE/deletedAt mỗi read/write; admin-only vận hành, learner/tutor-only policy đọc. Cookie JWT cần CSRF cho FE-11 writes; mở rộng scoped matcher, không đổi endpoint ngoài tính năng. DTO chỉ có nhãn tiếng Việt và dữ liệu cần thiết; không raw log/connection string/config key/token.
- **Decision**: Migration SQL tăng dần có preflight, lịch sử phiên bản và khóa migration vì ddl-auto none, không framework migration hiện hữu. Giữ String PK bảng config legacy; các bảng mới dùng UUID như AGENTS. Không chạy DatabasePgAdmin4.txt có DROP CASCADE lên DB hiện hữu.
- **Decision**: JUnit/MockMvc và PostgreSQL test riêng cho lock/enum/timezone/event/report. Load test backend bổ sung đo trình duyệt để đúng SC-006 “hiển thị”, không coi HTTP latency là thời gian hiển thị. Fixture chỉ môi trường test, không seed vận hành.
- **Alternatives considered**: H2 không chứng minh cùng hành vi PostgreSQL; route role đơn thuần không kiểm tài khoản bị thu hồi.

## Readiness

Không còn câu hỏi nghiệp vụ chưa giải quyết chặn thiết kế. Có hai phụ thuộc giao hàng cần giữ rõ: điểm tạo booking thật và nội dung chính sách đã duyệt. Thiếu một trong hai phải ghi phần nghiệm thu tương ứng chưa đạt, không tự đánh dấu tính năng hoàn tất.
