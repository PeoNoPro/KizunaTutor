# API Contract: FE-13 — Kho học liệu cho cán bộ học vụ

- Tiền tố: `/api/academic/content-imports`. Chỉ `ROLE_ACADEMIC_OFFICER` (cấu hình trong `SecurityConfig`); thiếu token → `401`, sai vai trò → `403`.
- `{type}` ∈ `kanji` | `vocabulary` | `grammar` (sai → `400` "Loại học liệu không hợp lệ.").
- Phản hồi thành công bọc theo BACKEND_RULES §6.2: `{ "success": true, "message": "...", "data": <payload>, "timestamp": "..." }`. Dưới đây chỉ mô tả `data`.
- Lỗi: `{ "success": false, "message": "<tiếng Việt>", "timestamp": "..." }` qua `GlobalExceptionHandler`.
- Trang dữ liệu (`Page<T>`): `{ items: T[], page, size, totalElements, totalPages }` (`core/dto/PageResponse`). `size` ∈ {10, 20, 50}, giá trị khác → 10.

## 1. `GET /summary`

`data`: `{ types: ContentTypeSummary[], maxFileSizeBytes: 15728640 }`

`ContentTypeSummary`: `{ type: "KANJI" | "VOCABULARY" | "GRAMMAR", label: "Kanji" | "Từ vựng" | "Ngữ pháp", total, requiredColumns: string[], optionalColumns: string[] }`

## 2. `GET /{type}/items`

Query: `keyword` (≤ 100 ký tự, tùy chọn), `jlpt` (`N1`…`N5`, trống/`ALL` = mọi cấp; giá trị khác → 400), `sort` (`LEVEL` mặc định: N5 → N1 | `NEWEST`: ngày thêm mới nhất), `page` (từ 0), `size`.

`data`: `Page<Item>`; trường của `Item` theo loại (giá trị trống là `null`; `createdAt` ISO-8601):

| Loại | Trường |
| --- | --- |
| kanji | `character`, `hanViet`, `onyomi`, `kunyomi`, `meaning`, `jlpt`, `strokes`, `createdAt` |
| vocabulary | `id`, `word`, `reading`, `romaji`, `hanViet`, `partOfSpeech`, `meaning`, `exampleSentence`, `exampleFurigana`, `exampleMeaning`, `jmdictId`, `jlpt`, `createdAt` |
| grammar | `id`, `code`, `pattern`, `romaji`, `meaning`, `formation`, `notes`, `exampleSentence`, `exampleFurigana`, `exampleMeaning`, `extendedExamples`, `tags`, `jlpt`, `createdAt` |

## 3. `POST /{type}?dryRun=true|false`

`multipart/form-data`, phần `file` (`.xlsx`, ≤ 15 MB). `dryRun` mặc định `true` (chỉ kiểm tra, không ghi). `dryRun=false`: thêm mục mới và ghi lịch sử trong một giao dịch.

`data`: `ContentImportResponse`

```json
{
  "type": "KANJI", "typeLabel": "Kanji", "fileName": "kanji_3_hon_hop.xlsx", "dryRun": true,
  "sheets": ["Kanji Vietnamese"],
  "totalRows": 15, "created": 3, "skippedExisting": 6, "duplicatesInFile": 2, "invalidRows": 4,
  "issues": [{ "sheet": "Kanji Vietnamese", "row": 13, "level": "ERROR", "message": "Chữ 丆 chưa có trong hệ thống nên cần cột Số nét." }],
  "issuesTruncated": false,
  "message": "Kiểm tra xong: sẽ thêm 3 mục mới."
}
```

- `issues[].level`: `ERROR` (dòng bị bỏ qua) | `WARNING` (trùng khác nội dung); lỗi trước, sau đó theo sheet và số dòng; tối đa 300 mục (`issuesTruncated`).
- `row` đếm từ 1 như Excel.

Lỗi `400`: không có tệp ("Vui lòng chọn tệp Excel cần nhập."), không phải `.xlsx`, quá 15 MB, quá 50.000 dòng, tệp hỏng, không tìm thấy dòng tiêu đề ("Không tìm thấy dòng tiêu đề phù hợp. Tệp <loại> cần có các cột: …. Hãy kiểm tra bạn đã chọn đúng loại học liệu cho tệp này.").

## 4. `GET /history`

Query: `type` (`kanji` | `vocabulary` | `grammar`; trống/`ALL` = mọi loại), `page`, `size`.

`data`: `Page<ImportHistoryResponse>`, mới nhất trước.

`ImportHistoryResponse`: `{ id (UUID), createdAt, actorName, actorEmail, type, typeLabel, fileName, totalRows, created, skippedExisting, duplicatesInFile, invalidRows }`

## 5. `GET /history/{id}`

`data`: `ImportHistoryDetailResponse` = `{ item: ImportHistoryResponse, addedItems: string[], addedTruncated: boolean }`. Không tồn tại → `404` "Không tìm thấy nội dung bạn yêu cầu.".
