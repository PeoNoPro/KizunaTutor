# Quickstart: Kiểm chứng FE-13 — Kho học liệu cho cán bộ học vụ

Dữ liệu: [data-model.md](data-model.md); API: [contracts/content-import-api.md](contracts/content-import-api.md).

## Điều kiện tiên quyết

- Java 17, Node.js, DBeaver kết nối Supabase (chỉ có DB dùng chung).
- Không cần migration (FE-13 không đổi schema).
- Tài khoản cán bộ học vụ mẫu có sẵn (xem `docs/guidelines/Tài Khoản - Mật Khẩu.txt`).
- Tệp Excel để thử: tự tạo từ 3 tệp mẫu trong `/database` (giữ nguyên tiêu đề cột). Mục thêm vào để thử nên có nghĩa bắt đầu bằng "(Dữ liệu thử)" để dọn dẹp ở bước 14.

## Chạy

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0'
cd backend; .\mvnw.cmd spring-boot:run      # cổng 8085
node frontend/server.js                      # cổng 3000
```

Nếu IDE đang tự biên dịch trong lúc backend chạy bằng DevTools và gặp lỗi "Unresolved compilation problem": dừng backend, `.\mvnw.cmd compile`, chạy lại.

## Kiểm thử tự động (không chạm DB)

```powershell
cd backend; .\mvnw.cmd test "-Dtest=ContentImport*"
node --test "frontend/tests/*.test.mjs"
```

## Kịch bản nghiệm thu

Mở `http://localhost:3000/academic/content-import` bằng tài khoản cán bộ học vụ.

| # | Bước | Kết quả mong đợi | Truy vết |
| --- | --- | --- | --- |
| 1 | Tab "Danh sách", lần lượt chọn Kanji / Từ vựng / Ngữ pháp | Thẻ loại hiện đúng tổng số mục; bảng có cột theo loại + "Ngày thêm"; khi tải có khung chờ | US1, FR-001, FR-022 |
| 2 | Từ vựng: gõ "gặp", lọc N5, đổi sắp xếp "Mới thêm gần đây" | Danh sách lọc đúng, về trang 1 | FR-002, FR-003 |
| 3 | Gõ từ khóa không tồn tại | Trạng thái rỗng "Không có mục nào phù hợp." | US1-3 |
| 4 | Bấm một mẫu ngữ pháp | Hộp thoại hiện đủ trường (ví dụ mở rộng, ghi chú, ngày thêm) | FR-004 |
| 5 | Tab "Nhập từ Excel", Kanji, tệp chỉ gồm các chữ đã có trong kho (ví dụ 20 dòng đầu của `kanji_vietnamese_full.xlsx`), "Kiểm tra tệp" | 0 thêm mới, mọi dòng "đã có"; không có nút "Nhập vào hệ thống" | US2-1, FR-010 |
| 6 | Chọn loại Từ vựng khi tệp Kanji đang được chọn | Tệp bị bỏ chọn | FR-010 |
| 7 | Từ vựng + tệp Kanji | Báo không tìm thấy dòng tiêu đề, nêu cột bắt buộc, nhắc kiểm tra loại | US2-5 |
| 8 | Kanji + tệp có cả chữ đã có, chữ mới có "Số nét", dòng lặp giống hệt, dòng lặp khác nghĩa, chữ mới thiếu "Số nét", ô có 2 chữ, JLPT "N6", dòng thiếu nghĩa; kiểm tra | Số thêm mới / đã có / trùng / lỗi khớp nội dung tệp; bảng lỗi nêu đúng số dòng và lý do; dòng lặp khác nghĩa là cảnh báo | US2-3, US2-4, FR-013, FR-014 |
| 9 | Bấm "Nhập vào hệ thống" | Toast thành công; thẻ Kanji tăng đúng số chữ mới; kiểm tra lại cùng tệp → 0 thêm mới | US2-2, FR-012 |
| 10 | Tab "Lịch sử nhập" | Có đúng 1 dòng mới (thời gian, tên cán bộ, tệp, kết quả khớp báo cáo nhập); chi tiết liệt kê đúng các chữ vừa thêm | US3, FR-018, FR-020 |
| 11 | Lặp 8–10 với một tệp từ vựng và một tệp ngữ pháp có cả mục đã có, mục mới, dòng trùng và dòng lỗi | Số liệu khớp với nội dung tệp | SC-002 |
| 12 | Kiểm tra 3 tệp mẫu gốc trong `/database` | 0 thêm mới, 0 lỗi | SC-001 |
| 13 | Gọi API bằng tài khoản gia sư | `403` | FR-005 |
| 14 | Chạy câu lệnh dọn dẹp bên dưới trong DBeaver | Dữ liệu thử bị xóa | A-5 |

Câu lệnh dọn dữ liệu thử (chỉ xóa mục có nghĩa bắt đầu bằng "(Dữ liệu thử)"; `vocabulary_kanjis` tự xóa theo ON DELETE CASCADE):

```sql
BEGIN;
DELETE FROM vocabularies WHERE meaning LIKE '(Dữ liệu thử)%';
DELETE FROM grammars WHERE meaning LIKE '(Dữ liệu thử)%';
DELETE FROM kanjis WHERE meaning LIKE '(Dữ liệu thử)%';
DELETE FROM kanji_dictionary WHERE meaning LIKE '(Dữ liệu thử)%';
COMMIT;
```

Lưu ý: Kanji thêm qua FE-13 không có dữ liệu nét viết KanjiVG nên chưa luyện viết được (plan, Complexity Tracking).
