# KizunaTutor Design System (DESIGN.md)

<!-- ===================================================================== -->
<!-- [IMMUTABLE CORE GUARDRAILS] DO NOT MODIFY, OVERWRITE OR DELETE BELOW  -->
<!-- KHỐI QUY TẮC BẤT BIẾN: NGHIÊM CẤM TỰ Ý SỬA ĐỔI HOẶC XÓA BỎ DƯỚI MỌI HÌNH THỨC -->
<!-- ===================================================================== -->

> ⛔ **CẢNH BÁO BẢO VỆ HỆ THỐNG (STRICT NON-NEGOTIABLE GUARDRAILS):**  
> Đây là 3 nguyên tắc nền tảng tối thượng của hệ thống KizunaTutor. Mọi tác vụ lập trình (Backend & Frontend) và cập nhật tài liệu BẮT BUỘC phải tuân thủ nghiêm ngặt:
>
> 1. **KHÔNG BAO GIỜ HIỂN THỊ NỘI DUNG BACKEND LÊN FRONTEND (ZERO BACKEND LEAKS):**
>    - Tuyệt đối cấm để lộ các mã nội bộ, tên enum, tên Exception, mã HTTP raw hay cấu trúc dữ liệu Backend ra giao diện (ví dụ: `ROLE_ADMIN`, `ROLE_LEARNER`, `BadCredentialsException`, `NullPointerException`, `500 INTERNAL_SERVER_ERROR`, `user_id`, `is_active`).
>    - Toàn bộ thông báo, nhãn, badge và trạng thái gửi tới người dùng BẮT BUỘC phải được chuyển hóa 100% sang ngôn ngữ người dùng tự nhiên (Human-centric / User-friendly Vietnamese).
>
> 2. **KHÔNG BAO GIỜ VIẾT IN HOA TOÀN BỘ TRÊN GIAO DIỆN (NO ALL-CAPS UI TEXT):**
>    - Tuyệt đối cấm sử dụng chữ IN HOA TOÀN BỘ (ALL-CAPS) cho các dòng thông báo, tiêu đề Modal, Alert, Toast, Empty States hay nội dung giải thích (ví dụ: cấm `LỖI ĐĂNG NHẬP!`, `ĐĂNG KÝ THÀNH CÔNG!`, `KHÔNG TÌM THẤY DỮ LIỆU!`).
>    - Chỉ áp dụng viết hoa chữ cái đầu câu (Sentence case) hoặc chữ cái đầu mỗi từ chính (Title Case cho nhãn nút/tiêu đề ngắn). Chữ in hoa chỉ được dùng duy nhất cho các từ viết tắt chuyên môn đã định nghĩa (ví dụ: `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`).
>
> 3. **KHÔNG BAO GIỜ CẬP NHẬT DỮ LIỆU ẢO ĐỂ HIỂN THỊ HỆ THỐNG (NO MOCK / FAKE DATA INJECTION):**
>    - Tuyệt đối không tự ý chèn dữ liệu ảo (hardcoded mock data, dummy JSON, dữ liệu bịa đặt) vào mã nguồn hoặc cơ sở dữ liệu để đối phó hiển thị khi chưa có API thực tế.
>    - Mọi dữ liệu hiển thị trên màn hình của 6 Actor (Admin, Manager, CS Staff, Academic Officer, Tutor, Learner) BẮT BUỘC phải được kết nối từ Endpoint API thực tế hoặc hiển thị màn hình rỗng chuẩn (Empty State) nếu cơ sở dữ liệu chưa có dữ liệu.

<!-- ===================================================================== -->
<!-- [END IMMUTABLE CORE GUARDRAILS] - SAFE TO APPEND EXTENSIONS BELOW     -->
<!-- ===================================================================== -->

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

---

## QUY CHUẨN HIỂN THỊ VĂN BẢN TRÊN GIAO DIỆN (UI COPYWRITING STANDARDS)

### 1. Bảng chuyển ngữ thuật ngữ Backend sang Giao diện
Tất cả các phản hồi từ API hoặc xử lý giao diện phải ánh xạ theo bảng sau trước khi hiển thị:

| Mã Backend / Kỹ thuật | Hiển thị chuẩn trên UI | Cấm hiển thị |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | Quản trị viên | ROLE_ADMIN, Admin role |
| `ROLE_MANAGER` | Quản lý vận hành | ROLE_MANAGER |
| `ROLE_CS_STAFF` | Chăm sóc khách hàng | ROLE_CS_STAFF, CS Staff |
| `ROLE_ACADEMIC_OFFICER` | Cán bộ học vụ | ROLE_ACADEMIC_OFFICER |
| `ROLE_TUTOR` | Gia sư | ROLE_TUTOR, Tutor role |
| `ROLE_LEARNER` / `ROLE_STUDENT` | Học viên | ROLE_LEARNER, STUDENT |
| `BadCredentialsException` | Email hoặc mật khẩu không chính xác | Bad credentials |
| `UserAlreadyExistsException` | Email này đã được đăng ký trong hệ thống | User already exists |
| `InternalServerError` | Hệ thống đang bận, vui lòng thử lại sau ít phút | 500 Server Error |
| `NullPointerException` | Đã có lỗi xảy ra trong quá trình xử lý | Null pointer exception |

### 2. Quy chuẩn viết hoa (Casing Rules)
- **Thông báo Toast / Alert:** Dùng Sentence case. 
  - Đúng: `✅ Đăng ký tài khoản thành công. Vui lòng kiểm tra email để lấy mã OTP.`
  - Sai: `✅ ĐĂNG KÝ TÀI KHOẢN THÀNH CÔNG!`
- **Tiêu đề Modal / Tiêu đề Card:** Dùng Title Case hoặc Sentence case trang nhã.
  - Đúng: `Xác thực mã OTP`, `Khởi tạo tài khoản nhân viên`
  - Sai: `XÁC THỰC MÃ OTP`, `KHỞI TẠO TÀI KHOẢN NHÂN VIÊN`
- **Thông báo lỗi (Error messages):** Giọng văn trung tính, lịch sự, không dùng dấu chấm than dồn dập (`!!!`).
  - Đúng: `Mã OTP không chính xác. Bạn còn 3 lần thử lại.`
  - Sai: `LỖI! MÃ OTP SAI! HÃY NHẬP LẠI NGAY!`
- **Ngoại lệ:** Chỉ viết hoa toàn bộ đối với các từ viết tắt chuyên môn / chuẩn quốc tế đã được quy ước (như `OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `JWT`).

---

## 7. NHAITOPIK.COM RETRO HARD-SHADOW DESIGN SYSTEM (TUTOR & LEARNER VIEWS)

Áp dụng cho toàn bộ các màn hình của phân hệ **Học viên (Learner)** và **Gia sư (Tutor)**:

### 1. Visual Language:
- **Bo góc cố định:** Duy nhất `border-radius: 5px` (`rounded-[5px]`). Cấm dùng bo tròn lớn mềm (`rounded-xl`, `rounded-2xl`).
- **Đổ bóng khối cứng (Hard/Retro Shadow):** `shadow-sm: 4px 4px 0px 0px #000000` (`shadow-retro`).
- **Viền ngoài:** Viền đen mảnh sắc sảo `border: 1px solid #000000`.
- **Micro-interactions:** 
  - Hover: `transform: translate(-1px, -1px); box-shadow: 5px 5px 0px 0px #000000;`
  - Active: `transform: translate(2px, 2px); box-shadow: 1px 1px 0px 0px #000000;`
  - Transition: `0.15s cubic-bezier(0.4, 0, 0.2, 1)`.

### 2. Bảng Color Tokens:
- `color-1` (`#000000`): Màu chữ chính, viền vi tính, shadow khối cứng.
- `color-2` (`#2A5A1A` - Rêu đậm): Nhấn khu vực Học viên (Learner Primary CTA).
- `color-3` (`#7A2CA8` - Tím trầm): Nhấn khu vực Gia sư (Tutor Primary CTA).
- `color-4` (`#BF360C` - Đỏ đất / Terracotta): Cảnh báo, Deadline, nút hành động nổi bật cần lưu ý.
- `color-5` (`#1F6FB4` - Xanh biển): Liên kết, thẻ bài tập.
- `color-6` (`#1E8B3F` - Xanh lục tươi): Hoàn thành, Duyệt, Điểm cao.
- `color-10` (`#E76F51` - Cam san hô): Trạng thái Đang học, Thẻ Flashcard.
- `color-11` (`#FFFFFF`): Nền thẻ card trắng, chữ trên nền đậm.

### 3. Font Stack & Typography Scale:
- **Heading / Tiêu đề:** `Itim`, cursive (cỡ 30px cho H1, 20px cho H2, 16px cho H3).
- **Body / Nội dung:** `Segoe UI`, sans-serif (14px).
- **Nhãn, Badge, Meta:** `ui-sans-serif`, system-ui (11px, 7px).
- **Nguyên tắc hành động (UX Rule):** Chỉ có DUY NHẤT 1 nút bấm Primary CTA trên mỗi màn hình/khối nhìn. Các nút khác dùng dạng Outlined (nền trắng viền đen) hoặc Secondary.



---

## 📦 QUY CHUẨN THỰC THI CHO KHỐI "HỖ TRỢ HỌC TẬP" (LEARNING SUPPORT ARCHITECTURE)

### 1. BẮT BUỘC NHÚNG CSS DÙNG CHUNG (MANDATORY INCLUSION)
Tất cả các màn hình thuộc phân hệ Học Tập & Rèn Luyện của Học viên:
- **Ngữ pháp & AI Sensei:** `/learner/grammar` & `/learner/grammar-detail`
- **Phòng luyện viết Shodo:** `/learner/practice-canvas` (hoặc `/shodo`)
- **Bộ thẻ Flashcard:** `/learner/flashcards` & `/learner/deck-detail`
- **Tra cứu Từ Điển Chữ Hán:** `/learner/dictionary` (hoặc `/kanji-dictionary`)
- **Đấu trường gõ Kana:** `/learner/kana`

**BẮT BUỘC** phải import file CSS dùng chung ở thẻ `<head>` trước các CSS riêng lẻ:
```html
<link rel="stylesheet" href="/assets/css/learning-support-common.css">
```

### 2. ÁNH XẠ NGUYÊN TẮC THIẾT KẾ (DESIGN RULE MAPPING)

| Thành phần UI | Class chuẩn sử dụng | Mô tả & Quy tắc bắt buộc |
| :--- | :--- | :--- |
| **Nền trang chính** | `<body class="learning-support-body">` | Sử dụng màu `--color-bg-surface` (`#FAF8F2`) dịu mắt, ổn định thanh cuộn `scrollbar-gutter: stable`. |
| **Khối nội dung / Thẻ bài học** | `.ls-card` / `.ls-card-interactive` | Viền `--color-border-strong` (`#DED7C5`), bo góc `radius-md` (8px), bóng đổ `shadow-sm`. |
| **Thẻ mang phong cách Retro** | `.ls-card-retro` | Viền đen `#0F172A`, bóng đổ cứng `3px 3px 0px 0px #0F172A`. |
| **Nút bấm chính (Action)** | `.ls-btn.ls-btn-primary` | Nền xanh Sensei `#006194`, chữ trắng rõ nét. |
| **Nút Shodo nét bút** | `.ls-btn.ls-btn-shodo` | Nền xanh lá `#2A5A1A`, chữ trắng. |
| **Tab / Nút đang chọn** | `.ls-btn-active` / `.ls-pill.is-active` | Nền hồng Hiragana `--color-accent-pink` (`#E0399A`), chữ trắng. |
| **Nút phụ / Viền mảnh** | `.ls-btn.ls-btn-outline` | Nền trắng, viền `#DED7C5`, chữ `--color-text-primary` (`#0F2545`). |
| **Thanh điều hướng / Tác vụ** | `.ls-toolbar` | Đặt ở đầu trang chi tiết hoặc thanh tìm kiếm, bọc cụm nút bấm và bộ lọc. |
| **Bộ lọc dạng Pill** | `.ls-pill` | Bo tròn viên thuốc `radius-full`, chuyển `active` khi chọn tab JLPT. |
| **Ô nhập liệu** | `.ls-input` | Viền `#E2D9C6`, bắt focus highlight xanh Sensei `#006194`. |
| **Huy hiệu cấp độ** | `.ls-badge.ls-badge-jlpt` | Nền xanh nhạt `#E0F2FE`, chữ `#006194`, font monospace. |

### 3. QUY TẮC ĐỘ TƯƠNG PHẢN & NỘI DUNG (WCAG AAA SAFETY)
- **Tuyệt đối chống chữ tàng hình:** Tất cả văn bản (giải nghĩa tiếng Việt, câu ví dụ, âm Hán Việt) bắt buộc nhận token `--color-text-primary` (`#0F2545`) hoặc `--color-text-secondary` (`#3F4850`). Không bao giờ áp dụng màu chữ trắng (`#FFFFFF`) lên các vùng thẻ có nền sáng.
- **Xóa bỏ thành phần dư thừa:** Tuyệt đối không để dải breadcrumb lặp lại (`school Không gian học viên...`) hoặc banner từ điển tĩnh xuất hiện làm choán diện tích trên các trang học tập.
- **Luồng dữ liệu (Zero-Reload):** Toàn bộ thao tác chuyển tab bài học, lật thẻ Flashcard, nộp nét vẽ Shodo phải truyền ngầm bằng gói tin JSON qua `apiClient.js` và cập nhật DOM động, không reload trang.


### 4. NGUYÊN TẮC BẮT BUỘC KHI THÊM MỚI TÍNH NĂNG / MÀN HÌNH (MANDATORY INHERITANCE RULE)
> ⚠️ **ĐẶC QUYỀN ÁP DỤNG RIÊNG CHO KHỐI "HỌC TẬP & RÈN LUYỆN":**  
> Mỗi khi phát triển hoặc thêm mới bất kỳ màn hình, thành phần (component), popup/modal hay tính năng nào thuộc khối **Học Tập & Rèn Luyện (Learning & Practice)** của Học viên:
> 
> 1. **Bắt buộc đọc lại và tuân thủ tuyệt đối các quy tắc ở trên:**
>    - Nhúng file CSS dùng chung: `<link rel="stylesheet" href="/assets/css/learning-support-common.css">` ở đầu thẻ `<head>`.
>    - Sử dụng đúng hệ thống Design Tokens, thẻ bài học `.ls-card`, cụm nút bấm `.ls-btn` (`.ls-btn-primary`, `.ls-btn-outline`, `.ls-btn-shodo`, `.ls-btn-active`), thanh tác vụ `.ls-toolbar`, bộ lọc `.ls-pill`, ô nhập `.ls-input` và huy hiệu `.ls-badge`.
>    - Tuyệt đối tuân thủ nguyên tắc WCAG AAA chống chữ tàng hình (`--color-text-primary: #0F2545`, `--color-text-secondary: #3F4850`), không bao giờ để chữ trắng trên nền sáng.
>    - Toàn bộ luồng dữ liệu phải là Zero-Reload: truyền ngầm JSON qua `apiClient.js` và cập nhật DOM động, kèm thông báo nổi Toast/Dialog.
> 
> 2. **Giới hạn phạm vi nghiêm ngặt:**  
>    Quy định này **chỉ áp dụng duy nhất cho các phần trong khối Học Tập & Rèn Luyện** (bao gồm: Kana Quiz, Bộ thẻ Flashcards, Chi tiết Flashcard, Luyện lật thẻ, Phòng luyện viết Shodo, Tra cứu Từ điển chữ Hán, Tra cứu viết tay AI, Game Mưa chữ Hán, Ngữ pháp & AI Sensei, Trợ lý học tập và các module học tập mới phát triển thêm sau này). Tuyệt đối **không áp dụng** sang các phân hệ ngoài khối này (như Admin, Manager, Gia sư, CSKH hay các trang Portal công khai).
