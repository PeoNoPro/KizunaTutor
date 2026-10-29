<!--
Sync Impact Report (ghi chú tạm thời để rà soát; xóa trước khi commit):
- Version: scaffold chưa có phiên bản -> 1.0.0 (ban hành lần đầu).
- Modified principles: thay năm nguyên tắc mẫu bằng năm nguyên tắc KizunaTutor.
- Added sections: Kiến trúc và ràng buộc kỹ thuật; Quy trình phát triển và kiểm chứng;
  quản trị, thứ tự áp dụng tài liệu và phạm vi thiết kế theo phân hệ.
- Removed sections: không loại bỏ quy định dự án; chỉ thay nội dung mẫu.
- Deferred TODOs: không có. Ngày ban hành là ngày thiết lập constitution lần đầu.
- Dependent templates/commands: không sửa; đọc constitution khi thực thi.
-->
# KizunaTutor Constitution

## Core Principles

### I. Giao diện tiếng Việt, không lộ nội dung kỹ thuật

Mọi tiêu đề, nút, nhãn, hướng dẫn, thông báo và trạng thái người dùng nhìn thấy
PHẢI dùng tiếng Việt tự nhiên, lịch sự và có tính sư phạm. Ngoại lệ nội dung học tập
là Kanji, Kana, Romaji và cấp độ JLPT N5–N1; nhãn giải thích vẫn PHẢI bằng tiếng Việt.
KHÔNG hiển thị tên biến, trường dữ liệu, enum vai trò, exception, stack trace hoặc
mã HTTP thô. PHẢI ánh xạ lỗi và trạng thái sang thông điệp thân thiện theo
`UI_LOCALIZATION_RULES.md`; chi tiết chẩn đoán chỉ dùng nội bộ.
Thông báo dùng sentence case; nút và tiêu đề ngắn được dùng title case.
KHÔNG viết hoa toàn bộ, trừ từ viết tắt đã quy ước. Ngoại lệ viết hoa không cho phép
hiển thị thuật ngữ kỹ thuật bị cấm. Mục đích là giúp người dùng hiểu và thực hiện tác vụ.

### II. Dữ liệu thật và trạng thái rỗng trung thực

Dữ liệu nghiệp vụ trên màn hình của cả sáu vai trò PHẢI lấy từ nguồn dữ liệu thực
qua backend/API của dự án. KHÔNG chèn dữ liệu hardcode, JSON giả hay bản ghi bịa đặt
vào mã nguồn hoặc cơ sở dữ liệu để làm đầy giao diện khi thiếu API hay dữ liệu.
Khi chưa có dữ liệu, PHẢI hiển thị trạng thái rỗng phù hợp; khi tải lỗi, PHẢI báo lỗi
thân thiện, không giả vờ tải thành công. Dữ liệu Kanji PHẢI theo nguồn KanjiVG và
cơ chế `KanjiDataSeeder` hiện có. Mục đích là phản ánh đúng trạng thái nghiệp vụ thực tế.

### III. Giữ kiến trúc hiện có và quy ước mã nguồn

PHẢI phát triển trên Spring Boot, Spring Data JPA, PostgreSQL và HTML/Thymeleaf hiện có.
Thay đổi kiến trúc hoặc thêm nền tảng PHẢI ghi lý do, tác động và phương án tương thích
trong kế hoạch. PHẢI giữ trách nhiệm controller, service, repository, entity và DTO
trong cấu trúc `com.kizuna`; không đưa truy cập cơ sở dữ liệu vào view.
Theo `CONVENTIONS.md`: package viết thường, lớp/interface PascalCase, phương thức/biến
camelCase, hằng/enum UPPER_SNAKE_CASE; file view và lớp CSS kebab-case, HTML ID camelCase,
thuộc tính `data-*` kebab-case. Entity mới PHẢI dùng annotation JPA chuẩn và khóa UUID
đúng `AGENTS.md`. Thay đổi schema PHẢI đối chiếu mapping và dữ liệu hiện hữu.
Mục đích là bổ sung tính năng mà không tạo kiến trúc song song hoặc phá tương thích.

### IV. Thiết kế nhất quán theo phân hệ

Giao diện bị thay đổi PHẢI tuân thủ `DESIGN.md` và `UI_LOCALIZATION_RULES.md`.
Thiết kế chung dùng bảng màu Aizome, Sakura, Matcha, Washi và Sumi đã định nghĩa.
Riêng màn hình học viên và gia sư PHẢI theo mục 7 của `DESIGN.md`: bo góc 5px,
viền đen 1px, bóng khối cứng, bảng màu/font theo phân hệ và một hành động chính
trên mỗi màn hình hoặc khối nhìn. Quy định chuyên biệt này thay các thông số chung
 tương ứng trong đúng phạm vi học viên và gia sư.
KHÔNG dùng gradient tím rập khuôn hoặc ba thẻ giống nhau thiếu phân cấp;
màu tím trầm dành cho gia sư theo mục 7 vẫn hợp lệ.
Nội dung tiếng Nhật PHẢI dùng font hỗ trợ glyph Nhật. Luyện viết Kanji PHẢI giữ
hệ tọa độ KanjiVG `0 0 109 109`, kể cả khi phóng to hoặc thu nhỏ khung hiển thị.
Mục đích là giữ bản sắc và trải nghiệm học tập phù hợp từng phân hệ.

### V. Thay đổi có phạm vi và kiểm chứng được

Tính năng triển khai theo Spec Kit PHẢI có yêu cầu, tiêu chí chấp nhận, kế hoạch và
công việc cụ thể; thay đổi PHẢI truy được về yêu cầu. PHẢI giới hạn phạm vi nhiệm vụ,
tái sử dụng thành phần phù hợp và giải thích độ phức tạp bổ sung trong kế hoạch.
Thay đổi nghiệp vụ PHẢI có kiểm thử phù hợp; thay đổi schema, hợp đồng API hoặc
liên lạc dịch vụ PHẢI kiểm tra tích hợp liên quan. Thay đổi UI PHẢI kiểm tra ngôn ngữ,
viết hoa, dữ liệu thật, trạng thái rỗng/lỗi và thiết kế phân hệ.
PHẢI ghi kết quả đã kiểm tra và giới hạn còn lại; KHÔNG tuyên bố kiểm thử thành công
khi chưa chạy. Kiểm tra bị chặn PHẢI ghi nguyên nhân và phần chưa xác minh.
Chỉnh tài liệu chỉ cần kiểm tra nội dung tương ứng. Mục đích là có bằng chứng hoàn tất
với mức kiểm chứng tương xứng thay đổi.

## Kiến trúc và ràng buộc kỹ thuật

- Nguồn schema: `DatabasePgAdmin4.txt` và `Database_Minigame_KanjiFolder.sql`.
  PHẢI đối chiếu mapping và code trước khi sửa; ghi chênh lệch giữa các nguồn trong kế hoạch.
- Tích hợp bộ nhớ AI PHẢI qua `com.kizuna.service.ai.AgentMemoryService` đến dịch vụ
  `agent-memory` tại cổng 8081 theo cấu hình hiện có; thay đổi phải có lý do trong kế hoạch.
- Nguồn KanjiVG là `data/kanjivg/kanji`; PHẢI giữ tương thích dữ liệu nét, bộ nạp và UI.
- File mã nguồn, cấu hình và dữ liệu văn bản liên quan PHẢI đọc/ghi UTF-8 không BOM.
  PHẢI giữ `server.servlet.encoding.charset=UTF-8` và
  `server.servlet.encoding.force=true` theo quy định bản địa hóa.
- KHÔNG sửa, ghi đè hoặc xóa các khối quy tắc bất biến trong tài liệu dự án.

## Quy trình phát triển và kiểm chứng

1. Đọc `AGENTS.md`, `CONVENTIONS.md`, `DESIGN.md`, `UI_LOCALIZATION_RULES.md`
   và hướng dẫn áp dụng tại khu vực mã nguồn trước khi lập kế hoạch.
2. Dùng `$speckit-specify` xác định hành vi và tiêu chí chấp nhận;
   dùng `$speckit-clarify` giải quyết điểm mơ hồ trước khi chốt phần phụ thuộc vào chúng.
3. Dùng `$speckit-plan` ghi kiến trúc, dữ liệu, tác động schema/API, kiểm tra cả năm
   nguyên tắc và xác định thiết kế phân hệ nếu có UI.
4. Dùng `$speckit-tasks` chia việc theo phụ thuộc và `$speckit-analyze` kiểm tra
   nhất quán trước `$speckit-implement`.
5. Đối chiếu tiêu chí chấp nhận, chạy kiểm tra phù hợp và ghi kết quả.
   Dùng `$speckit-converge` đối chiếu triển khai và ghi công việc còn thiếu.
   Tạo tài liệu hoặc danh sách công việc không tự chứng minh tính năng đã hoàn tất.

## Governance

Constitution tổng hợp quy định để Spec Kit áp dụng thống nhất; hướng dẫn chi tiết
vẫn nằm trong tài liệu nguồn. Các khối bất biến trong `AGENTS.md`, `CONVENTIONS.md`,
`DESIGN.md` và `UI_LOCALIZATION_RULES.md` PHẢI được giữ nguyên và tuân thủ.
Quy định chuyên biệt theo phân hệ áp dụng trong phạm vi của nó, quy định chung
áp dụng cho phần còn lại. Nếu còn xung đột thực chất, PHẢI ghi nhận và làm rõ
trước khi triển khai phần phụ thuộc; không tự bỏ quy tắc hoặc sửa nguồn để né tránh.

Mọi sửa đổi constitution PHẢI ghi lý do, phần bị ảnh hưởng, tác động lên công việc
đang làm và phiên bản mới để người phụ trách dự án rà soát; không được vô hiệu hóa
các khối bất biến. Dùng Semantic Versioning: MAJOR cho thay đổi không tương thích
hoặc loại bỏ nguyên tắc; MINOR khi thêm nguyên tắc hay mở rộng nghĩa vụ;
PATCH khi chỉ làm rõ câu chữ. Giữ ngày ban hành gốc và cập nhật ngày sửa đổi theo ISO.
Mỗi kế hoạch và lần rà soát thay đổi PHẢI kiểm tra tuân thủ, ghi nhận và xử lý vi phạm
trước khi tuyên bố hoàn tất.

**Version**: 1.0.0 | **Ratified**: 2026-10-29 | **Last Amended**: 2026-10-29
