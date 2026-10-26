# QUY TẮC HIỂN THỊ GIAO DIỆN & BẢN ĐỊA HÓA (UI LOCALIZATION GUIDELINES)
**Dự án KizunaTutor - Hệ Thống Kết Nối Gia Sư & Luyện Chữ Hán Thông Minh**

> ⚠️ **ĐIỀU KHOẢN BẮT BUỘC (STRICT PROTOCOL):**
> Mọi trợ lý AI (Antigravity, Cursor, Claude Code, Copilot, ChatGPT), lập trình viên và người bảo trì hệ thống **BẮT BUỘC** phải tuân thủ nghiêm ngặt các nguyên tắc dưới đây trong mọi file HTML, Template Thymeleaf, JavaScript, CSS, Thông báo API và Dữ liệu trả về cho người dùng.

---

## 1. NGUYÊN TẮC "100% TIẾNG VIỆT THÂN THIỆN CHO HỌC VIÊN"
- Toàn bộ giao diện người dùng (User Interface) bao gồm: tiêu đề trang, nhãn (labels), nút bấm (buttons), gợi ý (placeholders), hộp thoại (modals/dialogs), bảng biểu, tooltip, cảnh báo (alerts), thống kê phân tích và thông điệp trạng thái **PHẢI HOÀN TOÀN BẰNG TIẾNG VIỆT**.
- Giọng văn: Chu đáo, sư phạm, tôn trọng người học, chuẩn văn hóa giáo dục Nhật - Việt.
- **Ngoại lệ duy nhất:** Mặt chữ Hán (Kanji), cách đọc Kana (Hiragana/Katakana), Romaji và cấp độ JLPT (N5 - N1) thuộc nội dung học tập chuyên môn. Các nhãn giải thích, tiêu đề đi kèm bắt buộc phải dùng Tiếng Việt.

---

## 2. TUYỆT ĐỐI KHÔNG ĐỂ LỘ THUẬT NGỮ KỸ THUẬT & CODE RA NGOÀI GIAO DIỆN
- **Cấm hoàn toàn:** Không để lộ tên biến code, tên hàm, tên class, cấu trúc bảng database, phương thức HTTP, định dạng camelCase hay snake_case ra ngoài màn hình:
  - ❌ *Không dùng:* `id`, `uuid`, `userId`, `kanjiId`, `folder_items`, `session`, `play-data`, `request`, `response`, `DTO`, `entity`, `API`, `endpoint`...
  - ✅ *Chuyển thành:* `Mã nhận diện`, `Bộ thẻ học tập`, `Phiên luyện tập`, `Dữ liệu bài học`, `Thông tin chi tiết`...
- Thông báo lỗi hiển thị cho học viên phải là ngôn ngữ tự nhiên:
  - ❌ *Không dùng:* `500 Internal Server Error`, `Failed to fetch`, `NullPointerException`
  - ✅ *Chuyển thành:* `Hệ thống đang bận hoặc có lỗi kết nối, bạn vui lòng thử lại sau giây lát nhé!`

---

## 3. BẢNG QUY ĐỔI THUẬT NGỮ TIẾNG ANH $\rightarrow$ TIẾNG VIỆT CHUẨN KIZUNA

| Thuật ngữ tiếng Anh | Tuyệt đối KHÔNG hiển thị | Hiển thị 100% TIẾNG VIỆT Chuẩn Sư Phạm |
| :--- | :--- | :--- |
| **Kanji Rain Typing** | Rain Typing / Minigame | **Mưa Chữ Kanji / Trò Chơi Luyện Phản Xạ Chữ Hán** |
| **Deadline** | DEADLINE | **VẠCH RANH GIỚI NGUY HIỂM (Chạm đáy sẽ dừng ván)** |
| **Game Over** | GAME OVER | **KẾT THÚC VÁN ĐẤU (Chưa vượt qua thử thách)** |
| **You Win** | YOU WIN / VICTORY | **CHIẾN THẮNG XUẤT SẮC!** |
| **Accuracy** | Accuracy % | **Độ Chính Xác (%)** |
| **Words Typed** | Words Typed Count | **Số Từ Bắn Trúng / Số Chữ Hoàn Thành** |
| **Score** | Score | **Điểm Số Đạt Được / Điểm Tích Lũy** |
| **Combo** | Combo Streak | **Chuỗi Gõ Liên Hoàn** |
| **Timer / Duration** | Timer / Duration | **Thời Gian Còn Lại / Thời Lượng Ván Đấu** |
| **Speed Multiplier** | Speed: x1.10 | **Nhịp Rơi: Mức 1.1x (Cứ 20 giây tăng 10%)** |
| **Live Typing / Echo** | Live Echo Bar | **Ký Tự Bạn Đang Nhập** |
| **Pre-Game Config** | Game Config Modal | **Thiết Lập Ván Học Tập** |
| **Folder Manager** | Folder Manager | **Quản Lý Bộ Thẻ Học Tập** |
| **Practice / Library** | Practice / Library | **Luyện Viết Nét / Danh Sách Chữ Hán** |
| **Rank Titles** | Kanji Master, Samurai | **Bậc Thầy Chữ Hán 🏆, Cao Thủ Gõ Phím ⚔️, Học Giả Tiến Bộ 📜, Tân Binh Luyện Chữ 🌸** |

---

## 4. BẢO ĐẢM MÃ HÓA UTF-8 TUYỆT ĐỐI (CHỐNG LỖI FONT MOJIBAKE)
- Mọi tệp `.html`, `.js`, `.css`, `.properties`, `.java`, `.sql`, `.json` đều phải được lưu trữ và đọc dưới chuẩn mã hóa **UTF-8 (không có BOM)**.
- Khi biên dịch Maven và chạy Spring Boot, thuộc tính `server.servlet.encoding.charset=UTF-8` và `server.servlet.encoding.force=true` luôn được kích hoạt.
- Tuyệt đối không dùng các script PowerShell với mã hóa mặc định ANSI/Windows-1252 để ghi đè chuỗi tiếng Việt.

---
*Văn bản này được ban hành cho toàn bộ các dịch vụ và thành phần trong hệ sinh thái KizunaTutor.*
