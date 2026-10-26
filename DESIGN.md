# KizunaTutor Design System (DESIGN.md)

> Built upon **pbakaus/impeccable** and **Leonxlnx/taste-skill** standards.
> Anti-slop, pedagogical, culturally rooted in modern Japanese aesthetics.

---

## 1. Brand Identity & Aesthetic Direction

- **Concept**: *Wabi-Sabi meets Modern EdTech* (Giản dị, tinh tế, học thuật nhưng thân thiện và ấm áp).
- **Voice & Tone**: Chu đáo (Omoiyari - 思いやり), Đáng tin cậy, Khích lệ học viên tiến bộ mỗi ngày.
- **Visual Personality**: Sạch sẽ, nhiều khoảng thở (Ma - 間), phân cấp thông tin rõ ràng, nét chữ Kanji nổi bật trên lưới chuẩn.

---

## 2. Color Palette (Design Tokens)

Bảng màu được trích xuất từ văn hóa truyền thống Nhật Bản:

| Tên Token | Mã Hex | Ý nghĩa văn hóa / Ứng dụng |
| :--- | :--- | :--- |
| `--color-primary-aizome` | `#1B2A4A` | **Lam Chàm (Aizome)**: Màu chủ đạo, thanh điều hướng, nút chính, tiêu đề H1. |
| `--color-secondary-sakura`| `#FAD0C4` | **Hoa Anh Đào (Sakura)**: Màu phụ trợ, điểm nhấn nhẹ nhàng, tag thẻ học thử, badge. |
| `--color-accent-matcha`   | `#88A064` | **Trà Xanh (Matcha)**: Trạng thái thành công, bài tập đạt chuẩn, thanh tiến độ. |
| `--color-alert-akane`     | `#C84B31` | **Đỏ son (Akane)**: Nét vẽ sai, cảnh báo vi phạm hạn mức, nút hủy. |
| `--color-bg-washi`        | `#FAF9F6` | **Giấy Washi**: Nền chính toàn trang, êm mắt khi học viên luyện chữ lâu. |
| `--color-surface`         | `#FFFFFF` | Nền thẻ bài học (Card), bảng từ điển, modal popup. |
| `--color-text-sumi`       | `#1F2421` | **Mực Tàu (Sumi)**: Màu chữ chính và nét vẽ chữ Hán Kanji, độ tương phản cao. |
| `--color-text-muted`      | `#687076` | Chú thích Furigana, Romaji, ngày tạo, số nét. |

---

## 3. Typography Hierarchy

- **Latin Font**: `Inter`, `Plus Jakarta Sans` hoặc system fonts (`-apple-system, BlinkMacSystemFont, "Segoe UI"`).
- **Japanese Kanji & Kana Font**: `"Noto Sans JP"`, `"Zen Kaku Gothic New"`, `"Yu Gothic"`, `sans-serif`.
- **Kanji Practice Font**: `"Klee One"`, `"Noto Serif JP"` (cho phong cách nét bút lông truyền thống).

### Tỷ lệ cỡ chữ:
- **Display (Kanji Focus)**: `48px` - `64px` (Hiển thị nét chữ Hán tập viết trên khung Canvas).
- **H1 (Page Title)**: `32px`, font-weight `700`, line-height `1.2`.
- **H2 (Section Header)**: `24px`, font-weight `600`, line-height `1.3`.
- **H3 (Card Title)**: `18px`, font-weight `600`, line-height `1.4`.
- **Body**: `15px`, font-weight `400`, line-height `1.6`.
- **Furigana / Caption**: `12px`, font-weight `500`.

---

## 4. Kanji Practice Grid Specs (Lưới luyện viết)

- **Kích thước chuẩn**: `109px x 109px` (khớp với viewBox chuẩn của KanjiVG `0 0 109 109`).
- **Lưới phụ trợ**:
  - Khung viền ngoài: `border: 2px solid #1B2A4A`.
  - Trục chữ thập (thập tự giá) và đường chéo mờ chia 4 ô hoặc 8 ô: `stroke: #E2E8F0; stroke-dasharray: 4, 4`.
- **Màu nét chữ**:
  - Nét mẫu: `#1B2A4A` (độ dày nét `3px`).
  - Nét đang vẽ: `#C84B31` hoặc `#88A064` (khi vẽ đúng).
  - Thứ tự số thứ tự nét: `#808080`, cỡ chữ `8px`.

---

## 5. Anti-Slop Principles (Nguyên tắc chống rập khuôn AI)

Tuân thủ nghiêm ngặt theo **Leonxlnx/taste-skill** & **pbakaus/impeccable**:
1. ❌ **Không dùng dải màu tím AI tím lịm (purple gradients)** hoặc bóng đổ neon mờ ảo không có chủ đích.
2. ❌ **Không dùng layout 3 thẻ bài (three identical cards)** đồng dạng nhàm chán.
3. ✅ **Tập trung vào trải nghiệm sư phạm**: Lộ trình bài học rõ ràng, bảng từ điển dễ tra cứu, thẻ gia sư làm nổi bật chứng chỉ N1/N2/Sư phạm.
4. ✅ **Khoảng cách (Spacing)**: Sử dụng hệ số 8pt (`8px`, `16px`, `24px`, `32px`, `48px`).
5. ✅ **Border Radius**: Vừa phải (`6px` - `10px`), tránh bo tròn quá đà làm mất vẻ nghiêm túc của ứng dụng học tập.


---

## 6. Bản Địa Hóa 100% Tiếng Việt & Không Rò Rỉ Mã Kỹ Thuật (Human-First UI)

1. **Ngôn ngữ hiển thị**: Toàn bộ giao diện (tiêu đề, nút bấm, nhãn, cảnh báo, thống kê, hộp thoại modal) **phải 100% bằng Tiếng Việt**.
2. **Không để lộ mã nguồn & cơ sở dữ liệu**: Tuyệt đối không để lọt các từ ngữ kỹ thuật như ID, UUID, entity, session, request, response, DTO, camelCase, snake_case lên màn hình học viên.
3. **Từ ngữ thân thiện sư phạm**:
   - Thay "Deadline" bằng "Vạch ranh giới nguy hiểm".
   - Thay "Game Over" bằng "Kết thúc ván đấu (Chưa vượt qua)".
   - Thay "You Win" bằng "Chiến thắng xuất sắc!".
   - Thay "Accuracy" bằng "Độ chính xác".
   - Thay "Score" bằng "Điểm số tích lũy".
   - Thay "Combo" bằng "Chuỗi gõ liên hoàn".
