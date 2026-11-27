<!--
Sync Impact Report (ghi chú tạm thời để rà soát; xóa trước khi commit):
- Version: 1.0.0 -> 1.1.0 (MINOR: Mở rộng và thống nhất toàn diện các nguyên tắc KizunaTutor theo luật chung AGENTS.md, components_rule.md và design.md).
- Modified principles:
  - Principle I: Khẳng định nguyên tắc 100% tiếng Việt tự nhiên, cấm lộ mã lỗi Backend và cấm viết hoa toàn bộ (No ALL-CAPS).
  - Principle II: Cấm tiêm dữ liệu giả (No Mock Data), bắt buộc trạng thái rỗng trung thực và Skeleton Loading.
  - Principle III: Kiến trúc phân hệ độc lập - giao tiếp chéo tính năng chỉ qua Service công khai, không gọi Repository của feature khác.
  - Principle IV: Chuẩn hóa thiết kế theo Trang Chủ (Wabi-Sabi x Modern EdTech) và duy nhất một nguồn chân lý cho Component dùng chung.
  - Principle V: Ranh giới nghiệp vụ bất biến, kiểm thử trước khi báo hoàn thành và kỷ luật Git tuyệt đối.
- Added sections: Bổ sung ràng buộc công nghệ chuẩn hóa (Spring Boot 3, Tailwind, Vanilla JS ES6+) và quy chuẩn phối hợp nhóm.
- Removed sections: Không loại bỏ quy định; tích hợp và nhất thể hóa các quy tắc rời rạc.
- Deferred TODOs: Không có.
- Dependent templates/commands: Đọc trực tiếp hiến pháp khi chạy.
-->
# KizunaTutor Constitution

## Core Principles

### I. Giao diện tiếng Việt tự nhiên, không lộ mã kỹ thuật & không viết hoa toàn bộ

1. Mọi tiêu đề, nhãn, nút bấm, hướng dẫn, thông báo lỗi, toast và trạng thái trên giao diện của cả sáu vai trò PHẢI hiển thị bằng tiếng Việt tự nhiên, chuẩn mực sư phạm và thân thiện với con người. Ngoại lệ duy nhất về ngôn ngữ là nội dung học tập tiếng Nhật (Kanji, Kana, Romaji) và mã cấp độ năng lực quốc tế (`JLPT N5` – `JLPT N1`).
2. TUYỆT ĐỐI CẤM để lộ các chi tiết nội bộ hệ thống, tên lớp Exception (như `NullPointerException`, `BadCredentialsException`), mã HTTP thô (`500 INTERNAL_SERVER_ERROR`), tên bảng, tên trường cơ sở dữ liệu (`user_id`, `is_active`) hoặc mã enum kỹ thuật (`ROLE_ADMIN`, `ROLE_LEARNER`) lên giao diện người dùng. Mọi mã lỗi và trạng thái PHẢI được tầng xử lý ngoại lệ chuyển hóa thành thông điệp tiếng Việt có ý nghĩa hướng dẫn khắc phục.
3. TUYỆT ĐỐI CẤM viết hoa toàn bộ (ALL-CAPS) cho các đoạn thông báo, alert, toast, tiêu đề modal hay mô tả trạng thái rỗng. BẮT BUỘC áp dụng viết hoa đầu câu (Sentence case) cho nội dung giải thích/thông báo và viết hoa chữ cái đầu (Title Case) cho nhãn nút hoặc tiêu đề ngắn. Chữ in hoa chỉ được dùng cho các từ viết tắt chuẩn mực đã quy ước (`OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `JWT`).

### II. Dữ liệu thật, cấm dữ liệu ảo và sử dụng trạng thái rỗng trung thực

1. Mọi màn hình hiển thị của 6 nhóm người dùng (Học viên, Gia sư, Cán bộ học vụ, CS Staff, Quản lý vận hành, Quản trị viên) BẮT BUỘC phải lấy từ nguồn dữ liệu thật thông qua các Endpoint REST API của hệ thống.
2. TUYỆT ĐỐI CẤM tiêm hoặc hardcode các mảng dữ liệu giả lập (mock array), chuỗi JSON bịa đặt hay bản ghi tĩnh vào Controller, Service hoặc file JavaScript frontend để tạo cảm giác tính năng đã hoàn thiện khi chưa kết nối API.
3. Khi cơ sở dữ liệu chưa có dữ liệu, giao diện PHẢI hiển thị màn hình trạng thái rỗng (Empty State) chỉn chu với thông điệp hướng dẫn rõ ràng; khi gặp sự cố mạng, PHẢI hiển thị thông báo lỗi thân thiện thay vì giả vờ thành công.
4. Trong lúc chờ API phản hồi (0.5s – 1s), PHẢI sử dụng khung xương chờ tải (Skeleton Loading với hiệu ứng `animate-pulse`) hoặc Spinner đang tải chuẩn, TUYỆT ĐỐI KHÔNG để thẻ ảnh hay thông tin tĩnh chớp giật trên màn hình trước khi nạp dữ liệu thật.
5. Dữ liệu chữ Hán Kanji chỉ được trích xuất từ nguồn KanjiVG chuẩn thông qua bộ gieo mầm dữ liệu `KanjiDataSeeder`. Dữ liệu kiểm thử chỉ được lưu trong thư mục `src/test/` hoặc `frontend/tests/`.

### III. Kiến trúc phân hệ độc lập & giao tiếp công khai qua Service Interface

1. Hệ thống backend phát triển trên nền tảng Spring Boot 3.x, Java 17+, Spring Data JPA, PostgreSQL (Supabase) và tuân thủ chặt chẽ cấu trúc phân gói theo tính năng (`com.kizuna.features.<feature>`).
2. Nguyên tắc cô lập phân hệ (Feature Isolation): Một feature tuyệt đối KHÔNG được gọi trực tiếp `Repository` của một feature khác. Mọi tương tác hoặc truy xuất nghiệp vụ giữa các feature BẮT BUỘC phải thông qua `Service` interface công khai do feature đó cung cấp.
3. Thư mục `core/` chỉ dành riêng cho hạ tầng dùng chung (bảo mật, cấu hình, xử lý ngoại lệ toàn cục, tiện ích cơ bản); TUYỆT ĐỐI KHÔNG đưa nghiệp vụ riêng của bất kỳ feature cụ thể nào vào `core/`.
4. Quy ước đặt tên: Mã Java package chữ thường; Lớp/Interface theo `PascalCase`; Phương thức/Biến theo `camelCase`; Hằng số/Enum theo `UPPER_SNAKE_CASE`. Entity JPA phải dùng UUID chuẩn và khai báo mapping an toàn, không phá vỡ tính tương thích dữ liệu.

### IV. Giao diện chuẩn hóa theo Trang Chủ & duy nhất một nguồn chân lý Component

1. Toàn bộ giao diện người dùng web được xây dựng bằng HTML5, CSS3, JavaScript ES6+ thuần (Module) và Tailwind CSS. TUYỆT ĐỐI KHÔNG đưa các thư viện/framework SPA nặng (React, Vue, Angular, TypeScript) vào dự án khi chưa có phê duyệt từ kiến trúc sư trưởng.
2. Mọi trang con BẮT BUỘC lấy mẫu từ Trang Chủ (`frontend/pages/home/index.html`) và quy chuẩn tại `.agents/rules/design.md`:
   - Bảng màu: Nền giấy Washi ấm `#FCFBF7`, nền khối `#FAF8F2`, viền chuẩn `#ECE8DE`, màu thương hiệu Lam Chàm Aizome `#006194`, màu điểm nhấn Son Akane `#BA1A1A`.
   - Typography: Font chữ chính `Plus Jakarta Sans`, font chữ Hán `Noto Serif JP` (`font-kanji`), bộ biểu tượng Google Material Symbols Outlined.
   - Hệ thống bo góc mềm (`rounded-2xl` cho card, `rounded-xl` cho button/input) và khoảng cách theo hệ số 8pt.
3. Nguyên tắc bất biến về Component dùng chung (`.agents/rules/components_rule.md`):
   - Header và Footer BẮT BUỘC có duy nhất 1 nguồn chân lý: `frontend/components/header/header.html` và `frontend/components/footer/footer.html`.
   - Các trang tại `pages/*/index.html` TUYỆT ĐỐI KHÔNG hardcode mã HTML của Header/Footer, CHỈ ĐƯỢC PHÉP đặt thẻ giữ chỗ `<div id="header-placeholder"></div>` và `<div id="footer-placeholder"></div>`.
   - Mọi chỉnh sửa thanh điều hướng hay chân trang CHỈ ĐƯỢC PHÉP thực hiện trực tiếp tại thư mục component tương ứng.
4. Đối với màn hình chuyên sâu của Học viên và Gia sư, tuân thủ phong cách bo góc 5px, viền đen 1px, bóng khối cứng. Lưới luyện viết Kanji phải giữ nguyên vẹn hệ tọa độ KanjiVG `0 0 109 109`.

### V. Ranh giới nghiệp vụ, kiểm chứng nghiêm ngặt & kỷ luật Git tuyệt đối

1. Ranh giới nghiệp vụ nền tảng:
   - KizunaTutor là nền tảng kết nối và hỗ trợ học tập trực tuyến; hệ thống KHÔNG trực tiếp thu học phí các buổi học, KHÔNG ký hợp đồng lao động hay thanh toán tiền lương cho gia sư.
   - Cổng thanh toán trực tuyến (VNPay) DUY NHẤT chỉ áp dụng cho việc mua gói hội viên dịch vụ dành cho gia sư.
   - Tính năng trí tuệ nhân tạo (AI Sensei) chỉ mang tính chất gợi ý và hỗ trợ học tập; gia sư và người dùng luôn là người quyết định và xác nhận điểm số cuối cùng.
2. Kiểm chứng đi liền với mã nguồn:
   - Mọi thay đổi nghiệp vụ bắt buộc phải có unit test hoặc integration test tương ứng; kiểm thử backend qua `.\mvnw.cmd test`, kiểm thử frontend qua `node --test`.
   - Chỉ được báo cáo hoàn thành khi kiểm thử chạy xanh. Nếu bị chặn do thiếu DB hoặc biến môi trường, phải báo cáo rõ nguyên nhân và phạm vi chưa thể xác minh.
3. Kỷ luật mã nguồn và Git:
   - Trí tuệ nhân tạo (AI) và lập trình viên TUYỆT ĐỐI KHÔNG tự ý `git commit`, `git merge`, `git rebase` hay chuyển nhánh khi chưa có sự đồng ý của người phụ trách; và TUYỆT ĐỐI KHÔNG BAO GIỜ tự chạy lệnh `git push`.
   - Chỉ làm việc trên nhánh được phân công (nhánh cá nhân hoặc nhánh feature), không commit thẳng vào `main` hoặc `dev`.
   - Chỉ sửa đổi mã trong phạm vi task được giao; TUYỆT ĐỐI KHÔNG tự ý xóa, tái cấu trúc hoặc dời đổi vị trí các nút/thành phần của thành viên khác.

---

## Kiến trúc và ràng buộc kỹ thuật

1. **Backend & Cơ sở dữ liệu**:
   - Khởi chạy tại cổng `8085`. Gọi API frontend qua tiền tố `/api/...`.
   - Kết nối cơ sở dữ liệu PostgreSQL (Supabase Mumbai instance). Nghiêm cấm chạy các câu lệnh hủy dữ liệu hàng loạt (`DROP DATABASE`, `TRUNCATE`, `DROP TABLE CASCADE`) trên DB chạy thật.
   - Bộ nhớ AI tích hợp qua `com.kizuna.service.ai.AgentMemoryService` kết nối tới microservice `agent-memory` tại cổng `8081`.
2. **Frontend & Giao diện**:
   - Máy chủ node dev phục vụ tại cổng `3000` (`frontend/server.js`), tích hợp proxy ngược `/api/` về cổng `8085`.
   - Tương tác API chỉ thông qua mô-đun dùng chung `frontend/js/api.js` và cấu hình tại `frontend/js/config/api.config.js`. Mọi cuộc gọi `fetch` phải được bọc trong khối `try/catch` với thông báo lỗi thân thiện.
   - Không viết inline styles (`style="..."`) hay inline listeners (`onclick="..."`).
3. **Mã hóa và định dạng tệp**:
   - Mọi file mã nguồn Java, HTML, JS, CSS, SQL và tài liệu Markdown BẮT BUỘC lưu dưới định dạng UTF-8 không BOM (No BOM).
   - Tên file tiếng Anh theo định dạng `kebab-case` cho frontend và `PascalCase`/`camelCase` cho backend theo `CONVENTIONS.md`. Tài liệu và chú thích nghiệp vụ viết bằng tiếng Việt.
4. **Bảo tồn khối bất biến**:
   - Tuyệt đối giữ nguyên vẹn khối cảnh báo bất biến (Immutable Core Guardrails) ở đầu các tài liệu dự án (`AGENTS.md`, `DESIGN.md`, `UI_LOCALIZATION_RULES.md`, `constitution.md`).

---

## Quy trình phát triển và kiểm chứng (Spec Kit Workflow)

1. **Bước 1 - Tiếp nhận & Làm rõ (`$speckit-specify` / `$speckit-clarify`)**:
   - Trích xuất nghiệp vụ gốc từ tài liệu đặc tả SRS (`Report-3_Software Requirement Specification.docx`). Không tự suy đoán hoặc sáng tạo tính năng ngoài phạm vi SRS.
   - Làm rõ mọi điểm mơ hồ trước khi xây dựng kế hoạch.
2. **Bước 2 - Lập kế hoạch (`$speckit-plan`)**:
   - Xác định rõ mô hình dữ liệu (Data Model), hợp đồng API (Contracts) và đối chiếu tuân thủ 5 nguyên tắc cốt lõi của Constitution.
3. **Bước 3 - Phân rã công việc (`$speckit-tasks`)**:
   - Chia nhỏ các task độc lập, sắp xếp theo thứ tự phụ thuộc chặt chẽ (Setup -> Contracts -> Model/DB -> Services -> UI -> Testing).
4. **Bước 4 - Triển khai & Xác minh (`$speckit-implement` / `$speckit-converge`)**:
   - Viết mã và viết test song hành.
   - Rà soát kiểm tra ngôn ngữ hiển thị tiếng Việt, quy tắc viết hoa, kiểm tra trạng thái rỗng và không lộ mã lỗi trước khi bàn giao.
   - Ghi chú đầy đủ các quyết định kỹ thuật và phần cần nhóm xác nhận trong `plan.md`.

---

## Governance

1. **Hiệu lực tối thượng**:
   - Bản Hiến pháp này (`constitution.md`) là văn bản pháp quy kỹ thuật cao nhất trong dự án KizunaTutor. Mọi công cụ AI, lập trình viên và thành viên dự án khi tham gia viết mã hoặc xây dựng tài liệu đều bắt buộc phải tuân thủ tuyệt đối.
   - Trong trường hợp có sự mâu thuẫn giữa các tài liệu, thứ tự ưu tiên áp dụng như sau:  
     `Khối quy tắc bất biến đầu file` ➔ `.specify/memory/constitution.md` ➔ `tasks.md của feature đang làm` ➔ `.agents/AGENTS.md` ➔ `backend/BACKEND_RULES.md` & `frontend/FRONTEND_RULES.md` ➔ `docs/guidelines/`.
2. **Quy trình sửa đổi Hiến pháp (Amendment Procedure)**:
   - Mọi đề xuất thay đổi nguyên tắc trong Hiến pháp phải được thảo luận, thống nhất trong nhóm và được phê duyệt bởi Quản trị viên/Nhóm trưởng.
   - Khi sửa đổi, phải tạo bản báo cáo ảnh hưởng đồng bộ (Sync Impact Report), nêu rõ lý do và cập nhật ngày `Last Amended`.
3. **Chính sách đánh số phiên bản (Versioning Policy)**:
   - Áp dụng chuẩn Semantic Versioning (`MAJOR.MINOR.PATCH`):
     - **MAJOR**: Thay đổi lớn làm thay đổi hoặc hủy bỏ một trong các nguyên tắc cốt lõi.
     - **MINOR**: Bổ sung nguyên tắc mới, mở rộng phạm vi ràng buộc kỹ thuật hoặc quy chuẩn thiết kế.
     - **PATCH**: Hiệu đính từ ngữ, sửa lỗi chính tả hoặc làm rõ câu chữ mà không thay đổi bản chất quy tắc.

---

**Version**: 1.1.0 | **Ratified**: 2026-10-29 | **Last Amended**: 2026-11-04
