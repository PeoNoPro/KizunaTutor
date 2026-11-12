# Data Model: FE-13 — Kho học liệu cho cán bộ học vụ

**Không tạo bảng, không đổi schema, không migration.** FE-13 chỉ đọc và **thêm** (INSERT) bản ghi vào các bảng đã có; không UPDATE, không DELETE dữ liệu học liệu.

## 1. Bảng học liệu (đã có)

### 1.1 `kanji_dictionary` — Mục Kanji (tra cứu, luyện viết)

| Cột | Kiểu | Nguồn khi thêm | Ràng buộc / kiểm tra |
| --- | --- | --- | --- |
| `kanji_id` | UUID | `gen_random_uuid()` | PK |
| `kanji_character` | CHAR(1) | cột "Kanji" | UNIQUE; đúng 1 chữ Hán (`Character.isIdeographic`) — **khóa trùng** |
| `unicode_hex` | CHAR(5) | tính: mã Unicode, 5 ký tự hex thường | UNIQUE |
| `stroke_count` | SMALLINT | cột "Số nét" | NOT NULL; 1–84; bắt buộc với chữ mới |
| `han_viet` | VARCHAR(50) | "Hán Việt" | ≤ 50 |
| `onyomi` | VARCHAR(150) | "Onyomi" | ≤ 150 |
| `kunyomi` | VARCHAR(300) | "Kunyomi" | ≤ 300 |
| `meaning` | VARCHAR(200) | "Nghĩa tiếng Việt" | bắt buộc; ≤ 200 |
| `jlpt_level` | `jlpt_level_enum` | "JLPT" | N1–N5 hoặc trống |
| `svg_data`, `stroke_order_svg_url`, `radical` | | không ghi | chỉ đến từ KanjiVG |
| `created_at` | TIMESTAMPTZ | mặc định | dùng cho "Ngày thêm" |

### 1.2 `kanjis` — Kanji dùng để liên kết từ vựng

Thêm cùng lúc với Kanji mới: `character`, `han_viet`, `onyomi`, `kunyomi`, `meaning`, `jlpt_level`. `ON CONFLICT ("character") DO NOTHING` — chữ đã có giữ nguyên.

### 1.3 `vocabularies` — Mục từ vựng

| Cột | Nguồn | Kiểm tra |
| --- | --- | --- |
| `id` | BIGSERIAL | PK |
| `word` | "Từ vựng" | bắt buộc; ≤ 255 |
| `reading` | "Cách đọc" | ≤ 255; trống → bằng `word`. **Khóa trùng = (`word`, `reading`)** (không có ràng buộc DB) |
| `romaji`, `han_viet` | "Phiên âm Romaji", "Âm Hán Việt" | ≤ 255 |
| `part_of_speech` | "Từ loại" | ≤ 100 |
| `meaning` | "Nghĩa tiếng Việt" | bắt buộc (TEXT) |
| `example_sentence`, `example_furigana`, `example_meaning` | ba cột ví dụ | TEXT |
| `jmdict_id` | "Mã JMdict ID" | ≤ 50 (không duy nhất) |
| `jlpt_level` | "Cấp độ JLPT" | N1–N5 hoặc trống |

### 1.4 `vocabulary_kanjis` — Liên kết từ vựng ↔ Kanji

Sau khi thêm từ mới: với mỗi ký tự của `word` có trong `kanjis.character`, thêm (`vocabulary_id`, `kanji_id`), `ON CONFLICT DO NOTHING`. Chỉ áp dụng cho đúng các cặp (`word`, `reading`) vừa thêm (ghép theo từng dòng, không ghép chéo từ của dòng này với cách đọc của dòng khác).

### 1.5 `grammars` — Mẫu ngữ pháp

| Cột | Nguồn | Kiểm tra |
| --- | --- | --- |
| `grammar_code` | "Mã định danh (ID)" | bắt buộc; ≤ 50; UNIQUE — **khóa trùng, không phân biệt hoa thường** |
| `pattern` | "Mẫu cấu trúc ngữ pháp" | bắt buộc; ≤ 255; không được thuộc mã khác |
| `romaji` | "Phiên âm Romaji" | ≤ 255 |
| `meaning` | "Ý nghĩa tiếng Việt" | bắt buộc |
| `formation`, `notes`, `example_sentence`, `example_furigana`, `example_meaning`, `extended_examples` | các cột tương ứng | TEXT |
| `tags` | "Thẻ phân loại (Tags)" | ≤ 255 |
| `jlpt_level` | "Cấp độ JLPT" | N1–N5 hoặc trống |

## 2. Lần nhập học liệu → `system_audit_logs` (đã có)

| Cột | Giá trị |
| --- | --- |
| `log_id` | `gen_random_uuid()` |
| `actor_user_id` | cán bộ học vụ đang đăng nhập |
| `action_type` | `LEARNING_CONTENT_IMPORT` |
| `target_entity` | `kanji_dictionary` / `vocabularies` / `grammars` |
| `details` (JSONB) | `{ type, fileName, sheets[], totalRows, created, skippedExisting, duplicatesInFile, invalidRows, addedItems[] (≤ 2.000), addedTruncated }` |
| `created_at` | mặc định |

Mục trong `addedItems`: Kanji = chữ; Từ vựng = `từ` hoặc `từ (cách đọc)` khi khác nhau; Ngữ pháp = `mã · mẫu`.

## 3. Quy tắc xử lý một dòng (theo thứ tự)

1. Dòng trống → bỏ qua (không đếm).
2. Thiếu cột bắt buộc / JLPT sai / quá độ dài / Kanji không phải 1 chữ Hán / số nét sai → **dòng lỗi**.
3. Khóa đã xuất hiện trước trong tệp → **trùng trong tệp** (khác nội dung → cảnh báo).
4. Khóa đã có trong kho → **đã có, giữ nguyên**.
5. Ngữ pháp: mẫu đã thuộc mã khác → **dòng lỗi**.
6. Kanji mới thiếu "Số nét" → **dòng lỗi**.
7. Còn lại → **thêm mới**.

## 4. Giao dịch

Nhập thật: một giao dịch = khóa theo loại → đọc khóa hiện có → lập kế hoạch → thêm theo lô → (từ vựng) liên kết Kanji → ghi lịch sử. Lỗi ở bất kỳ bước nào → rollback toàn bộ.
