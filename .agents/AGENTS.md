# AGENTS.md: luật chung cho mọi người và mọi công cụ AI

<!-- ===================================================================== -->
<!-- [CORE SYSTEM GUARDRAILS] NON-NEGOTIABLE PRINCIPLES                     -->
<!-- ===================================================================== -->

> ⛔ **CRITICAL SYSTEM GUARDRAILS (STRICT COMPLIANCE REQUIRED):**
> 
> 1. **ZERO INTERNAL LEAKS ON USER INTERFACE:**
>    - Never expose internal system details, technical exception names, stack traces, raw HTTP errors, database schema names, or raw enum codes (e.g., `ROLE_ADMIN`, `NullPointerException`, `BadCredentialsException`, `500 INTERNAL_SERVER_ERROR`, `user_id`) to the end-user interface.
>    - All user-facing labels, badges, alerts, toasts, and error messages MUST be presented in human-centric, natural Vietnamese. Technical IDs and status codes are strictly reserved for internal API communication and programmatic frontend logic.
>
> 2. **NO ALL-CAPS TEXT ON USER INTERFACE:**
>    - Never use ALL-CAPS text for alert messages, modal titles, toasts, empty state descriptions, or buttons.
>    - Use Sentence case for alerts/descriptions and Title Case for short buttons/headings. ALL-CAPS is permitted solely for standard international acronyms (e.g., `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `JWT`).
>
> 3. **NO FAKE OR MOCK DATA IN PRODUCTION FLOWS:**
>    - Never inject hardcoded mock arrays, fake JSON structures, or fabricated data into controllers, services, or frontend JavaScript to simulate feature completion when an API is not connected.
>    - If data is absent, the system MUST render a dedicated, user-friendly Empty State. Testing fixtures (in `src/test/`) and explicitly configured database seeders (e.g., `KanjiDataSeeder`) are permitted and must be strictly isolated from application runtime logic.

<!-- ===================================================================== -->

---

Dự án KizunaTutor (mã KZT, nhóm SEP490_G6): ứng dụng web responsive kết nối người học tiếng Nhật với gia sư và hỗ trợ học trực tuyến (luyện Kanji, flashcard, minigame, bài tập, tiến độ). Nhóm làm song song, mỗi người có thể dùng một công cụ AI khác nhau (Claude Code, ChatGPT/Codex, Cursor, Copilot, Gemini, Antigravity…). File này là điểm vào chung: công cụ nào cũng phải đọc file này trước khi viết mã.

Khi các tài liệu khác nhau, thứ tự thắng là: khối quy tắc bất biến ở đầu các file → `.specify/memory/constitution.md` → `tasks.md` của feature đang làm → file này → luật riêng từng phần ([backend/BACKEND_RULES.md](../backend/BACKEND_RULES.md), [frontend/FRONTEND_RULES.md](../frontend/FRONTEND_RULES.md)) → `docs/guidelines/`.

Nếu công cụ của bạn không tự đọc file này, hãy dán câu sau vào đầu cuộc trò chuyện:

> Đọc `.agents/AGENTS.md` và file luật của phần bạn sẽ sửa (`backend/BACKEND_RULES.md` hoặc `frontend/FRONTEND_RULES.md`). Tôi đang làm feature <FE-NN> trên nhánh <tên nhánh>, đặc tả ở `specs/<NNN-ten>/`. Chỉ sửa trong phạm vi feature này. Trước khi làm, tóm tắt cho tôi các task chưa đánh `[x]` trong `tasks.md` của feature.

## 1. Đọc gì, theo thứ tự nào

1. File này, rồi luật của phần sẽ sửa: [backend/BACKEND_RULES.md](../backend/BACKEND_RULES.md) (Spring Boot, cơ sở dữ liệu, API, bảo mật, AI) hoặc [frontend/FRONTEND_RULES.md](../frontend/FRONTEND_RULES.md) (HTML/CSS/JS, cấu trúc trang, gọi API).
2. Khi làm một task theo Spec Kit: `specs/NNN-*/tasks.md` → `plan.md` → `data-model.md` → `contracts/` → `research.md` → `spec.md` → `quickstart.md`. Nguồn nghiệp vụ gốc là tài liệu SRS `Report-3_Software Requirement Specification.docx` mà `spec.md` trích dẫn.
3. Giao diện và chữ trên màn: [docs/guidelines/DESIGN.md](../docs/guidelines/DESIGN.md), [docs/guidelines/UI_LOCALIZATION_RULES.md](../docs/guidelines/UI_LOCALIZATION_RULES.md), bảng chuyển ngữ ở [.agents/rules/README.md](rules/README.md).
4. Đặt tên: [docs/guidelines/CONVENTIONS.md](../docs/guidelines/CONVENTIONS.md); cấu trúc gói backend: [docs/guidelines/BACKEND_FEATURE_ARCHITECTURE.md](../docs/guidelines/BACKEND_FEATURE_ARCHITECTURE.md).

## 2. Mười luật không được vi phạm

1. **Nghiệp vụ chỉ lấy từ SRS và `spec.md` của feature.** Không đoán, không tự thêm tính năng, trạng thái hay quy tắc. Chưa rõ thì dừng và hỏi (hoặc chạy `$speckit-clarify`). Không sửa `spec.md` khi chưa được yêu cầu.
2. **Chỉ sửa phần của mình**: đúng feature, đúng thư mục của task. Không sửa, refactor, xóa mã của thành viên khác ngoài phạm vi task; cần thay đổi ở đó thì báo người phụ trách.
3. **Feature khác chỉ gọi qua Service interface công khai** của feature đó, không gọi thẳng `Repository` của feature khác. `core/` chỉ chứa hạ tầng dùng chung, không chứa nghiệp vụ riêng của feature nào.
4. **Test đi cùng mã**: thay đổi nghiệp vụ phải có test; thay đổi schema, hợp đồng API phải kiểm tra tích hợp liên quan. Chỉ báo "xong" và đánh `[x]` task khi đã chạy test và thấy xanh; test bị chặn (thiếu DB, thiếu biến môi trường) thì ghi rõ nguyên nhân và phần chưa xác minh.
5. **Hợp đồng API là luật**: chỉ cài đúng endpoint, trường trong `specs/NNN-*/contracts/`. Không thêm, bớt, đổi tên endpoint hay trường. Cần đổi thì sửa hợp đồng trước và báo người dùng.
6. **Giao diện tiếng Việt, không lộ nội dung backend, không IN HOA toàn bộ** (khối bất biến ở đầu file). Mã nguồn đặt tên tiếng Anh; tài liệu, comment, chữ trên màn tiếng Việt. File văn bản đọc/ghi UTF-8 không BOM.
7. **Không dữ liệu giả**: màn hình của cả 6 vai trò lấy dữ liệu từ API thật; chưa có dữ liệu thì hiện trạng thái rỗng, lỗi tải thì báo lỗi thân thiện. Dữ liệu Kanji chỉ từ KanjiVG qua `KanjiDataSeeder`. Fixture test chỉ nằm trong `src/test/` hoặc `frontend/tests/`.
8. **Ranh giới nghiệp vụ**: nền tảng không thu học phí, không lập hợp đồng học phí điện tử, không trả lương gia sư; thanh toán chỉ dành cho gói hội viên gia sư (VNPay). AI chỉ gợi ý, gia sư luôn là người chấm và xác nhận điểm cuối cùng.
9. **Không viết mã thừa**: chỉ đủ cho task đang làm; không thêm tính năng, lớp trừu tượng, cấu hình, framework (React, Vue, Angular…) mà tasks/plan không yêu cầu; không sửa định dạng, mã kế bên. Lệch kỹ thuật so với plan thì sửa `plan.md`, `tasks.md` cùng lúc và ghi lý do.
10. **Git**: AI không tự commit, merge, rebase, đổi nhánh khi người dùng chưa đồng ý, và **không bao giờ tự `git push`**: mọi lần đẩy lên remote do người dùng tự làm sau khi xem lại thay đổi. Làm trên nhánh được giao (nhánh theo tên thành viên hoặc nhánh feature); không commit thẳng `main` hay `dev`.

## 3. Định nghĩa "xong" một task hoặc một phase

- [ ] Test của phần vừa làm đã viết và chạy xanh (nếu không chạy được thì đã ghi lý do)
- [ ] `backend`: `.\mvnw.cmd test-compile` qua; chạy test liên quan `.\mvnw.cmd test -Dtest=<TenTest>` (Linux/macOS: `./mvnw`)
- [ ] `frontend`: `node --test frontend/tests/` qua; đã mở trang qua `node frontend/server.js` và kiểm tra ngôn ngữ, viết hoa, trạng thái rỗng, trạng thái lỗi
- [ ] Đánh `[x]` task trong `tasks.md`; ghi chú triển khai, lệch kỹ thuật ở `plan.md`
- [ ] Thuật ngữ mới hoặc trạng thái mới có bản chuyển ngữ tiếng Việt cho giao diện
- [ ] Báo cáo cho người dùng: file đã tạo hoặc sửa, kết quả lệnh kiểm tra, quyết định kỹ thuật cần nhóm xác nhận

## 4. Backend (`backend/`)

Toàn bộ luật backend ở [backend/BACKEND_RULES.md](../backend/BACKEND_RULES.md): công nghệ, cấu trúc gói theo feature, trách nhiệm từng tầng, đặt tên Java, cơ sở dữ liệu, migration, chuẩn API, bảo mật, tích hợp AI, kiểm thử. Ai sửa mã trong `backend/` hoặc `database/` phải đọc file đó trước.

## 5. Frontend (`frontend/`)

Theo [frontend/FRONTEND_RULES.md](../frontend/FRONTEND_RULES.md): HTML5, CSS3, JavaScript ES6+ thuần (module), Tailwind; không React, Vue, Angular, TypeScript. Trang theo feature ở `frontend/pages/<ten-trang>/`, phần dùng chung ở `components/`, `styles/`, `js/`. Gọi API chỉ qua `js/api.js` với cấu hình ở `js/config/api.config.js` (backend `http://localhost:8085/api/...`, frontend chạy cổng 3000). Không viết `style="..."` hay `onclick="..."` nội tuyến; mặc định dùng `const`, không dùng `var`; bọc mọi `fetch()` bằng xử lý lỗi.

Đặt tên frontend:

| Thành phần | Quy ước | Ví dụ đúng |
| :--- | :--- | :--- |
| File HTML, CSS, JS | kebab-case | `tutor-profile.html`, `booking-service.js` |
| Lớp CSS, thuộc tính `data-*` | kebab-case | `.tutor-card`, `data-tutor-id` |
| HTML ID | camelCase | `id="bookingModal"` |
| Hàm, biến JS | camelCase | `calculateScore()`, `tutorList` |
| Hằng JS | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Lớp JS | PascalCase | `KanjiDrawer` |

Màn học viên và gia sư theo mục 7 của DESIGN.md (bo góc 5px, viền đen 1px, bóng khối cứng). Luyện viết Kanji giữ hệ tọa độ KanjiVG `0 0 109 109`.

## 6. Vai trò và phân hệ

Sáu vai trò (mã nội bộ → tên trên giao diện): `ROLE_LEARNER` Học viên, `ROLE_TUTOR` Gia sư, `ROLE_ACADEMIC_OFFICER` Cán bộ học vụ, `ROLE_CS_STAFF` Chăm sóc khách hàng, `ROLE_MANAGER` Quản lý vận hành, `ROLE_ADMIN` Quản trị viên. Mã nội bộ không bao giờ hiện trên màn.

Các feature trong `com.kizuna.features`: `auth`, `profile`, `verification`, `tutor_search`, `booking`, `classroom`, `progress`, `learning_support`, `payment`, `ticket`, `admin`, `notification`.

## 7. AI không được làm

- Không bịa dữ liệu người dùng, gia sư, quy tắc nghiệp vụ, quyền; không chèn mảng giả, JSON giả vào mã chạy thật.
- Không ghi mật khẩu, khóa API, JWT secret vào kho mã; dùng biến môi trường. Không đưa thông tin đăng nhập vào log hay chat.
- Không tắt, bỏ qua, xóa test cho xanh; không sửa test của người khác cho khớp mã của mình.
- Không sửa, ghi đè, xóa các khối quy tắc bất biến trong tài liệu dự án; khi sửa file `.md` phải giữ nguyên khối đó ở đầu file.
- Không đổi `pom.xml`, `docker-compose.yml`, `application.properties`, thư mục `core/` khi task không yêu cầu; cần đổi thì hỏi người dùng trước.
- Không chạy lệnh phá dữ liệu trên cơ sở dữ liệu thật (`DROP ... CASCADE`, `TRUNCATE`, xóa hàng loạt); không chạy `DatabasePgAdmin4.txt`.
- Không sửa nội dung submodule (`database/kanjivg`, `backend/services/agent-memory`, `docs/design/impeccable`, `.skills/taste-skill`).

## 8. Quyết định kỹ thuật còn chờ nhóm xác nhận

1. Cách làm style frontend: CSS tự viết, Tailwind CLI hay Tailwind CDN.
2. Cơ sở dữ liệu cho test: PostgreSQL trong Docker hay một instance test riêng.
3. Nhà cung cấp AI (Gemini hoặc OpenAI), tên model chính thức và hạn mức token.
4. Tham số sandbox VNPay và URL callback IPN.
5. Nơi lưu file tải lên (ảnh đại diện, bài tập): `bytea` trong PostgreSQL, ổ đĩa máy chủ hay dịch vụ lưu trữ đám mây.
