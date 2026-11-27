# BÁO CÁO GIẢI TRÌNH KIẾN TRÚC & PHƯƠNG ÁN TỰ CHỦ DỮ LIỆU
## Dự án: KizunaTutor - Hệ Thống Hỗ Trợ Học Tập Tiếng Nhật Thông Minh

---

### Kính gửi: Giảng viên hướng dẫn / Hội đồng đánh giá

Báo cáo này giải trình chi tiết về **Mức độ độc lập dữ liệu**, **Khả năng tự chủ hệ thống (Self-hosted & Offline-first)** và **Phương án bảo vệ dữ liệu dự phòng** khi các kho lưu trữ (repository) bên ngoài bị đóng, xóa hoặc mất kết nối.

---

### 1. Hiện trạng Kiến trúc Dữ liệu của KizunaTutor

Toàn bộ hệ thống KizunaTutor được thiết kế theo nguyên tắc **Nội địa hóa dữ liệu (Local Data Persistence)**, cụ thể:

1. **Kho Từ vựng JLPT (7.811 bản ghi N5 - N1):**
   - **Nguồn tham chiếu ban đầu:** OpenJLPT / JMdict.
   - **Hiện trạng:** Đã được xử lý, làm sạch, bổ sung âm Hán Việt, Furigana và **lưu trữ 100% trong PostgreSQL nội bộ** thông qua file `database/seeds/vocabularies_seed.sql` và file Excel gốc `database/Kho_Tu_Vung_OpenJLPT_Full_Tieng_Viet.xlsx`.
   - **Mức độ phụ thuộc mạng ngoài:** **0%** (Không gọi bất kỳ API bên thứ ba nào khi tra cứu từ vựng).

2. **Kho Ngữ pháp JLPT (526 mẫu câu N5 - N1):**
   - **Nguồn tham chiếu ban đầu:** OpenJLPT Grammar.
   - **Hiện trạng:** Đã được dịch nghĩa, tổng hợp công thức, phân loại nhóm chủ đề và **lưu trữ hoàn toàn trong bảng `grammars`** của PostgreSQL (`database/seeds/grammars_seed.sql` và `database/Kho_Ngu_Phap_OpenJLPT_Tieng_Viet.xlsx`).
   - **Mức độ phụ thuộc mạng ngoài:** **0%** (Hệ thống truy vấn trực tiếp từ bảng cơ sở dữ liệu của KizunaTutor).

3. **Từ điển Chữ Hán Kanji (6.611 Hán tự):**
   - **Nguồn tham chiếu ban đầu:** Kanjidic / Joyo Kanji.
   - **Hiện trạng:** Đã được gắn đầy đủ bộ thủ (Radical), số nét, On/Kun, âm Hán Việt và nghĩa tiếng Việt trong bảng `kanji_dictionary` (`database/seeds/kanji_dictionary_seed.sql`).
   - **Mức độ phụ thuộc mạng ngoài:** **0%**.

4. **Dữ liệu Thứ tự nét bút Shodo (Kanji Stroke Order SVG):**
   - **Nguồn tham chiếu:** KanjiVG Project (Dự án mã nguồn mở theo giấy phép Creative Commons).
   - **Hiện trạng khắc phục:** Dự án **đã tải trọn bộ 11.941 file vector SVG** về lưu cục bộ tại thư mục `frontend/kanjivg/` và `database/kanjivg/`.
   - **Cơ chế nạp:** 
     - **Ưu tiên 1:** Nạp trực tiếp từ ổ cứng / tài nguyên tĩnh cục bộ `/kanjivg/{hex}.svg`.
     - **Ưu tiên 2:** Nạp từ API Backend PostgreSQL lưu trữ cột `svg_data`.
     - **Dự phòng cuối cùng:** CDN ngoài (chỉ kích hoạt nếu file local bị thiếu).
   - **Đánh giá rủi ro:** Ngay cả khi tổ chức KanjiVG xóa vĩnh viễn repository trên GitHub, tính năng luyện viết Shodo của KizunaTutor **vẫn tiếp tục hoạt động 100% bình thường**.

5. **Phát âm AI (TTS) & Minigames:**
   - Sử dụng **VOICEVOX Engine** chạy trên Docker/Localhost (`127.0.0.1:50021`) kết hợp Web Speech API tích hợp sẵn của trình duyệt.
   - Trò chơi Mưa chữ Hán & Ghép thẻ: Tính toán và render trực tiếp trên Client Canvas/HTML5, không phụ thuộc máy chủ game bên ngoài.

---

### 2. Phương Án Phòng Vệ Rủi Ro (Disaster Recovery & Backup Strategy)

Để đảm bảo hệ thống không bao giờ bị vô hiệu hóa dù đối mặt với sự cố mạng diện rộng hoặc xóa mã nguồn mở:

1. **Lưu trữ phiên bản gốc (Version-Controlled Seeds):**
   - Toàn bộ 3 tệp SQL Seed chuẩn (`vocabularies_seed.sql`, `grammars_seed.sql`, `kanji_dictionary_seed.sql`) và 3 tệp Excel gốc được cam kết trực tiếp trong Git repository của dự án.
   - Bất kỳ máy chủ mới nào chỉ cần chạy lệnh nạp seed là có toàn bộ cơ sở tri thức sau 5 giây mà không cần Internet.

2. **Tự động hóa sao lưu cơ sở dữ liệu:**
   - Dự án đã tích hợp công cụ tự động sao lưu định kỳ `database/backup_database.ps1`.
   - Hỗ trợ xuất tự động toàn bộ schema và dữ liệu PostgreSQL ra tệp `.sql` lưu trữ trên hệ thống lưu trữ dự phòng của trường / máy chủ quản trị.

---

### 3. Kết Luận

KizunaTutor hoàn toàn **không phụ thuộc thời gian thực (zero runtime dependency)** vào các repository công cộng bên ngoài cho các nghiệp vụ cốt lõi (Từ vựng, Ngữ pháp, Chữ Hán, Flashcards, Minigames). Hệ thống đạt tiêu chuẩn **tự chủ công nghệ, bảo mật dữ liệu và sẵn sàng vận hành độc lập (Offline-ready)**.
