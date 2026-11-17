# Research: FE-13 — Kho học liệu cho cán bộ học vụ

Ngày: 2026-11-10. Căn cứ: [spec.md](spec.md), constitution 1.1.0, `.agents/AGENTS.md`, `backend/BACKEND_RULES.md`, `frontend/FRONTEND_RULES.md`, ba tệp mẫu trong `/database` và schema Supabase đọc trực tiếp (chỉ đọc) ngày 2026-11-10.

## 0. Hiện trạng liên quan

| Thành phần | Hiện trạng | Hệ quả |
| --- | --- | --- |
| Bảng `kanji_dictionary` | 6.701 dòng; khóa `kanji_id` UUID; `kanji_character` CHAR(1) UNIQUE, `unicode_hex` CHAR(5) UNIQUE, `stroke_count` NOT NULL (≥ 0), `jlpt_level` enum `jlpt_level_enum`; `svg_data` có ở mọi dòng (từ KanjiVG). | Khóa trùng = chữ; Kanji mới cần số nét; không ghi `svg_data`. |
| Bảng `kanjis` | 6.611 dòng; `character` VARCHAR(10) UNIQUE; `meaning` NOT NULL. Dùng cho liên kết `vocabulary_kanjis`. | Kanji mới thêm cả vào đây (`ON CONFLICT DO NOTHING`). |
| Bảng `vocabularies` | 7.811 dòng; khóa `id` BIGSERIAL; **không có ràng buộc duy nhất**; `jmdict_id` không duy nhất (7.687 giá trị khác nhau); cặp (`word`, `reading`) hiện không trùng. | Khóa trùng = (từ, cách đọc), kiểm tra ở tầng ứng dụng + khóa giao dịch. |
| Bảng `grammars` | 526 dòng; `grammar_code` UNIQUE (giá trị là cột "Mã định danh (ID)" của tệp, ví dụ `wa`); `pattern` hiện không trùng. | Khóa trùng = mã (không phân biệt hoa thường); chặn mẫu thuộc mã khác. |
| Bảng `system_audit_logs` | Có sẵn: `log_id` UUID, `actor_user_id`, `action_type`, `target_entity`, `target_id`, `ip_address`, `details` JSONB, `created_at`. Chưa mã nào ghi vào. Entity `admin/entity/SystemAuditLog` chỉ có khóa (mapping chưa hoàn chỉnh), không có repository / service. | Dùng làm lịch sử nhập, không tạo bảng mới. |
| `learning_support` (FE-08, thành viên khác) | Có `KanjiExcelSeeder`, `VocabExcelSeeder` (đọc cùng tệp mẫu theo vị trí cột khi khởi động, bật bằng cấu hình); `KanjiDictionaryRepository`; không có entity / service cho từ vựng, ngữ pháp. | Không gọi, không sửa. Tham khảo cách liên kết `vocabulary_kanjis` của `VocabExcelSeeder`. |
| Ba tệp mẫu | Kanji: 1 sheet, 6 cột, không có số nét. Từ vựng: 6 sheet ("Tất cả" + N5…N1 lặp lại), 12 cột. Ngữ pháp: 6 sheet, 13 cột. | Đọc mọi sheet, gộp trùng; nhận cột theo tên. |
| SRS | Không có UC nhập / xem kho học liệu cho cán bộ học vụ. | Bổ sung ngoài SRS (spec A-6). |

## 1. Nhận cột theo tên tiêu đề

- Decision: Chuẩn hóa tên tiêu đề (bỏ phần trong ngoặc, bỏ dấu tiếng Việt, đổi "đ" → "d", chữ thường, chỉ giữ chữ và số) rồi so khớp đúng với danh sách tên gọi khác của từng cột; tìm dòng tiêu đề trong 5 dòng đầu; đọc mọi sheet có đủ cột bắt buộc.
- Rationale: "Định dạng tương tự" cho phép đổi thứ tự cột, thêm cột, đổi chữ hoa/dấu; so khớp đúng (không "chứa") tránh nhầm "Nghĩa tiếng Việt" với "Dịch nghĩa câu ví dụ (Tiếng Việt)".
- Alternatives considered: Đọc theo vị trí cột như seeder hiện có — gãy khi tệp đổi thứ tự cột; so khớp "chứa chuỗi" — nhầm cột.

## 2. Tầng truy cập dữ liệu

- Decision: `JdbcTemplate` + `batchUpdate` (lô 500) trong repository của FE-13; giao dịch bằng `TransactionTemplate`; khóa `pg_advisory_xact_lock(hashtext('content-import:<LOẠI>'))`.
- Rationale: Không có entity cho `vocabularies`, `grammars`, `kanjis`; entity `KanjiDictionary` thuộc FE-08. Không tạo entity trùng bảng của feature khác. Đọc tệp trước khi mở giao dịch để không giữ kết nối trong lúc phân tích. `vocabularies` không có ràng buộc duy nhất nên cần khóa để hai lần nhập song song không thêm trùng.
- Alternatives considered: Entity JPA riêng cho từng bảng — trùng mapping với FE-08; `INSERT ... ON CONFLICT` — không dùng được cho `vocabularies` (không có ràng buộc duy nhất) và không cho biết mục nào đã có để báo cáo.

## 3. Xử lý trùng lặp

- Decision: Khóa — Kanji: chữ; Từ vựng: (từ, cách đọc); Ngữ pháp: mã (chữ thường). Trùng trong tệp: giữ lần đầu, lặp giống hệt chỉ đếm, khác nội dung thì cảnh báo. Đã có trong kho: luôn giữ nguyên (quyết định 2026-11-10). Ngữ pháp: mẫu thuộc mã khác là lỗi.
- Rationale: Đúng yêu cầu "giữ nguyên mục đã có, chỉ thêm mục mới"; tệp mẫu có sheet theo cấp độ lặp lại sheet "Tất cả" nên lặp giống hệt là bình thường.
- Alternatives considered: Lựa chọn "cập nhật mục đã có" — đã có trong bản đầu, người phụ trách yêu cầu bỏ.

## 4. Hai bước kiểm tra → nhập

- Decision: Cùng một endpoint với tham số `dryRun`; bước nhập gửi lại tệp, hệ thống đọc và lập kế hoạch lại trong giao dịch có khóa.
- Rationale: Không phải lưu tệp tạm trên máy chủ; kết quả nhập luôn dựa trên dữ liệu mới nhất (có thể khác lần kiểm tra nếu người khác vừa nhập).
- Alternatives considered: Lưu kết quả kiểm tra theo mã phiên — thêm trạng thái phía máy chủ và việc dọn dẹp.

## 5. Lịch sử nhập

- Decision: Một dòng `system_audit_logs` cho mỗi lần nhập thật: `action_type = 'LEARNING_CONTENT_IMPORT'`, `actor_user_id`, `target_entity` = bảng đích, `details` JSONB chứa loại, tên tệp, sheet, số liệu và tối đa 2.000 mục đã thêm. Ghi trong cùng giao dịch với lệnh thêm.
- Rationale: Bảng nhật ký có sẵn, đúng mục đích; người phụ trách không muốn tạo bảng mới khi đã có bảng dùng được (quyết định trước đó ở FE-03).
- Alternatives considered: Bảng `content_import_logs` mới — cần migration; chỉ ghi log ứng dụng — không xem được trên giao diện.

## 6. Kanji mới

- Decision: Cho phép, bắt buộc cột "Số nét" (1–84) với chữ chưa có; `unicode_hex` tính từ mã Unicode; không ghi `svg_data`.
- Rationale: Người phụ trách giữ chức năng hiện có (spec Clarifications). `stroke_count` NOT NULL nên không thể thiếu số nét.
- Alternatives considered: Chỉ xem Kanji — đúng constitution II.5 nhưng mất chức năng đã nghiệm thu (ghi ở Complexity Tracking để nhóm quyết định lại nếu cần).

## 7. Kiểm thử khi chỉ có DB dùng chung

- Decision: Test tự động không chạm DB (reader, planner, chạy trên bộ tệp thử). Kiểm chứng SQL bằng chương trình chạy trong giao dịch rồi rollback. Nghiệm thu nhập thật dùng dữ liệu có nhãn "(Dữ liệu thử)" + câu lệnh dọn dẹp.
- Rationale: Theo phương án DB đã chọn cho dự án (chỉ có Supabase dùng chung).
- Alternatives considered: Test tích hợp `*IntegrationTest` với DB riêng — dự án chưa có DB test.
