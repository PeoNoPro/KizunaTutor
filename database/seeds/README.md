# KIZUNATUTOR – DỮ LIỆU SEED TỪ ĐIỂN KANJI, TỪ VỰNG & NGỮ PHÁP JLPT
*(Kanji, Vocabulary & Grammar Seed Data Import Guide)*

Thư mục này chứa file dữ liệu mẫu (Seed) và hướng dẫn nạp (import/upsert) toàn bộ dữ liệu học tập từ các file Excel vào cơ sở dữ liệu KizunaTutor.

> ⛔ **NGUYÊN TẮC BẤT BIẾN:**  
> - Toàn bộ các bảng `kanji_dictionary`, `vocabularies`, `grammars`, `kanjis`, `vocabulary_kanjis` **ĐÃ CÓ SẴN** trong Database.  
> - **TUYỆT ĐỐI KHÔNG TẠO BẢNG MỚI** (Không dùng bất kỳ lệnh `CREATE TABLE` hay `DROP TABLE` nào).  
> - Toàn bộ dữ liệu được nhập an toàn qua cơ chế `INSERT` và `ON CONFLICT` để bảo toàn dữ liệu và các liên kết khóa ngoại.

---

## 1. DANH SÁCH FILE SEED TRONG THƯ MỤC

| Tên File | Nguồn Dữ Liệu | Số lượng bản ghi | Mục tiêu nạp |
| :--- | :--- | :--- | :--- |
| [`kanji_dictionary_seed.sql`](kanji_dictionary_seed.sql) | `kanji_vietnamese_full.xlsx` | 6.611 Chữ Hán | Bảng `kanji_dictionary` (Âm Hán Việt, Onyomi, Kunyomi, nghĩa tiếng Việt, cấp độ JLPT N5 - N1). |
| [`vocabularies_seed.sql`](vocabularies_seed.sql) | `Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx` | 7.811 Từ vựng | Bảng `vocabularies` (Từ vựng, cách đọc Hiragana, Romaji, Hán Việt, từ loại, nghĩa tiếng Việt, ví dụ câu tiếng Nhật kèm Furigana và dịch nghĩa). |
| [`grammars_seed.sql`](grammars_seed.sql) | `Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx` | 526 Ngữ pháp | Bảng `grammars` (Cấu trúc mẫu câu, phiên âm Romaji, ý nghĩa, công thức kết hợp, ghi chú và các câu ví dụ tiêu biểu). |

---

## 2. HƯỚNG DẪN 2 CÁCH NẠP DỮ LIỆU VÀO DATABASE

### CÁCH 1 (Khuyên dùng - Nhanh nhất & Đơn giản nhất): Chạy trực tiếp file SQL
Các file `.sql` trong thư mục này là SQL chuẩn PostgreSQL, thuần `INSERT` và không tạo bảng:

1. **Dùng pgAdmin 4 hoặc DBeaver**:
   - Kết nối vào Database dự án.
   - Mở **Query Tool**.
   - Mở file `database/seeds/vocabularies_seed.sql` (hoặc `kanji_dictionary_seed.sql`, `grammars_seed.sql`).
   - Nhấn **Execute (F5)** để nạp dữ liệu (Thời gian chạy chỉ khoảng 2 – 5 giây cho toàn bộ 7.811 từ vựng).

2. **Dùng Supabase Dashboard**:
   - Truy cập trang quản trị Supabase của dự án KizunaTutor.
   - Vào mục **SQL Editor** -> **New query**.
   - Dán nội dung file SQL và bấm **Run**.

3. **Dùng dòng lệnh `psql`**:
   ```bash
   psql -h aws-0-ap-south-1.pooler.supabase.com -p 5432 -U postgres.dyafiyzmwzmbowkjcnwb -d postgres -f database/seeds/vocabularies_seed.sql
   ```

---

### CÁCH 2: Tự động nạp qua Spring Boot Seeder (Dùng file Excel)
Dự án đã tích hợp sẵn các class Seeder tối ưu bằng Apache POI và `JdbcTemplate`:
- Chữ Kanji: [`KanjiExcelSeeder.java`](file:///d:/KizunaTutor/backend/src/main/java/com/kizuna/features/learning_support/seeder/KanjiExcelSeeder.java)
- Từ vựng & Liên kết Hán tự: [`VocabExcelSeeder.java`](file:///d:/KizunaTutor/backend/src/main/java/com/kizuna/features/learning_support/seeder/VocabExcelSeeder.java)

**Cách chạy:**
1. Mở file `backend/src/main/resources/application.properties`, đổi cấu hình:
   ```properties
   # Bật tính năng nạp từ vựng từ Excel khi khởi động ứng dụng
   app.seeder.excel-vocab.enabled=true
   ```
2. Chạy ứng dụng Spring Boot (qua IntelliJ IDEA hoặc lệnh terminal `cd backend && .\mvnw.cmd spring-boot:run`).
3. Seeder sẽ tự động nạp 7.811 từ vựng và tự động quét các chữ Hán trong từ để tạo liên kết trung gian vào bảng `vocabulary_kanjis`.
4. Sau khi nạp xong, đổi lại thành `false` để tránh nạp lặp lại ở các lần khởi động tiếp theo.

---

## 3. CÁC CỘT DỮ LIỆU ĐƯỢC NẠP VÀO BẢNG `vocabularies`

| Cột trong Excel | Cột trong Database | Kiểu dữ liệu | Ví dụ thực tế |
| :--- | :--- | :--- | :--- |
| `Mã JMdict ID` | `jmdict_id` | `VARCHAR(50)` | `1198180` |
| `Cấp độ JLPT` | `jlpt_level` | `VARCHAR(10)` | `N5` |
| `Từ vựng (Kanji/Kana)` | `word` | `VARCHAR(255)` | `会う` |
| `Cách đọc (Hiragana)` | `reading` | `VARCHAR(255)` | `あう` |
| `Phiên âm Romaji` | `romaji` | `VARCHAR(255)` | `au` |
| `Âm Hán Việt` | `han_viet` | `VARCHAR(255)` | `HỘI` |
| `Từ loại (Tiếng Việt)` | `part_of_speech` | `VARCHAR(255)` | `Động từ nhóm 1 (Ngũ đoạn)...` |
| `Nghĩa tiếng Việt` | `meaning` | `TEXT` | `Gặp, gặp gỡ` |
| `Câu ví dụ (Tiếng Nhật)` | `example_sentence` | `TEXT` | `もうスミスさんに会われましたね。` |
| `Câu ví dụ Furigana` | `example_furigana` | `TEXT` | `もうスミスさんに{会\|あ}われましたね。` |
| `Dịch nghĩa câu ví dụ` | `example_meaning` | `TEXT` | `Bạn đã gặp ông Smith rồi phải không?` |
