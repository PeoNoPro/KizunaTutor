# KIZUNATUTOR DESIGN SYSTEM (DESIGN_SYSTEM.md)
**Tài liệu Nguồn Chân Lý Duy Nhất (Single Source of Truth - SSOT) cho Toàn Bộ Giao Diện Hệ Thống**

<!-- ===================================================================== -->
<!-- [IMMUTABLE CORE GUARDRAILS] DO NOT MODIFY, OVERWRITE OR DELETE BELOW  -->
<!-- KHỐI QUY TẮC BẤT BIẾN: NGHIÊM CẤM TỰ Ý SỬA ĐỔI HOẶC XÓA BỎ DƯỚI MỌI HÌNH THỨC -->
<!-- ===================================================================== -->

> ⛔ **CẢNH BÁO BẢO VỆ HỆ THỐNG (STRICT NON-NEGOTIABLE GUARDRAILS):**  
> Đây là 3 nguyên tắc nền tảng tối thượng của hệ thống KizunaTutor. Mọi tác vụ lập trình (Backend & Frontend) và sinh mã giao diện bằng AI BẮT BUỘC phải tuân thủ nghiêm ngặt:
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

---

## 1. Triết Lý Thiết Kế & Bản Sắc Thị Giác (Brand Identity & Aesthetic)

KizunaTutor kết hợp tinh hoa giữa **Nghệ thuật Wabi-Sabi Nhật Bản** (tĩnh lặng, mộc mạc, nhiều khoảng thở) và **Phong cách Retro-Brutalist Hard-Shadow** (đường nét dứt khoát, viền đen sắc sảo, bóng đổ khối cứng cáp theo chuẩn nhaitopik.com):
- **Cảm giác thị giác (Vibe):** Hiện đại, thân thiện, mang đậm chất giáo dục sư phạm, gợi cảm hứng học tập chữ Hán và tiếng Nhật mỗi ngày.
- **Micro-interactions:** Phản hồi xúc giác tức thì, nút bấm khi hover nổi lên nhẹ (`translate(-1px, -1px)`), khi click lún xuống (`translate(1px, 1px)`), tạo cảm giác bấm máy bấm retro cơ học.
- **Khoảng thở (Ma - 間):** Không nhồi nhét thông tin, bố cục thoáng đãng, phân cấp thị giác rõ ràng qua kích thước chữ và độ đậm nhạt.

---

## 2. Design Tokens & Theme System

### 2.1. Bảng Màu Ngữ Nghĩa (Semantic Color Palette)

Toàn bộ hệ thống giao diện được kiểm soát qua các token màu sắc ngữ nghĩa có độ tương phản cao, hỗ trợ chuẩn WCAG AA:

| Nhóm Token | Tên Biến CSS | Mã HEX | Tailwind Class Tương Ứng | Mục Đích Sử Dụng |
| :--- | :--- | :--- | :--- | :--- |
| **Primary (Brand)** | `--color-primary` | `#E0399A` | `bg-[#E0399A]`, `text-[#E0399A]` | Màu thương hiệu Kizuna Sakura, tab đang chọn, nút CTA nổi bật |
| **Primary (Learner)** | `--color-learner-cta` | `#15803D` | `bg-[#15803D]`, `text-[#15803D]` | Nút hành động chính phân hệ Học viên (Lưu, Bắt đầu học, Luyện Shodo) |
| **Primary (Tutor)** | `--color-tutor-cta` | `#7C3AED` | `bg-[#7C3AED]`, `text-[#7C3AED]` | Nút hành động chính phân hệ Gia sư (Tạo ca dạy, Duyệt lịch, AI Sensei) |
| **Surface (Card/Modal)** | `--color-surface` | `#FFFFFF` | `bg-white` | Nền thẻ bài học (Card), bảng biểu, hộp thoại Modal, thanh nhập liệu |
| **Canvas (Background)** | `--color-bg-canvas` | `#F8FAFC` | `bg-[#F8FAFC]` | Nền toàn trang Slate-50, dịu mắt khi nhìn lâu |
| **Washi Canvas** | `--color-bg-washi` | `#FAF9F6` | `bg-[#FAF9F6]` | Nền khu vực luyện viết nét chữ Hán Shodo, khung Furigana |
| **Text Heading** | `--color-text-heading` | `#000000` | `text-black` | Tiêu đề trang H1, H2, H3, chữ Kanji lớn |
| **Text Body** | `--color-text-body` | `#0F172A` | `text-[#0F172A]`, `text-slate-800` | Nội dung văn bản chính, câu ví dụ tiếng Nhật, bản dịch tiếng Việt |
| **Text Muted** | `--color-text-muted` | `#64748B` | `text-slate-500` | Chú thích Furigana, Romaji, nhãn phụ, ngày tháng |
| **Border Solid** | `--color-border-solid` | `#000000` | `border-black` | Viền ngoài 1px chuẩn Retro-Brutalist trên mọi component |
| **Border Subdued** | `--color-border-subdued` | `#CBD5E1` | `border-[#CBD5E1]` | Viền ngăn cách nhẹ giữa các hàng, breadcrumb |
| **Shadow Token** | `--shadow-hard` | `#0F172A` | `shadow-[3px_3px_0px_0px_#0F172A]` | Đổ bóng khối cứng đặc trưng |
| **Status: Thành công** | `--color-success-bg` | `#DCFCE7` | `bg-[#DCFCE7] text-[#14532D]` | Thông báo lưu thành công, đạt bài kiểm tra, badge hoàn thành |
| **Status: Cảnh báo** | `--color-warning-bg` | `#FEF3C7` | `bg-[#FEF3C7] text-[#78350F]` | Thông báo nhắc nhở, gần hết hạn, vạch ranh giới nguy hiểm |
| **Status: Lỗi / Xóa** | `--color-error-bg` | `#FEE2E2` | `bg-[#FEE2E2] text-[#7F1D1D]` | Thông báo lỗi, nút hủy/xóa, nét viết sai |
| **Status: Thông tin** | `--color-info-bg` | `#E0F2FE` | `bg-[#E0F2FE] text-[#0C4A6E]` | Hướng dẫn sử dụng, mẹo học tập của Sensei |

---

### 2.2. Hệ Thống Typography (Font Stacks & Scale)

#### Font Families
- **Font Tiêu đề & Cảm xúc (Heading / Display):** `'Itim', cursive` (ấm áp, thân thiện, đậm nét viết tay sư phạm).
- **Font Nội dung & Giao diện (Body / UI):** `'Segoe UI', 'Inter', -apple-system, sans-serif` (rõ ràng, sắc sảo, chống mỏi mắt).
- **Font Chữ Hán & Tiếng Nhật (Kanji & Kana):** `'Noto Sans JP', 'Noto Serif JP', 'Zen Kaku Gothic New', sans-serif`.

#### Phân Cấp Cỡ Chữ (Type Scale)

| Cấp Bậc | Cỡ Chữ (px / rem) | Font Family | Font Weight | Line Height | Ứng Dụng Thực Tế |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Kanji Focus** | `48px - 64px` | Noto Serif JP | 700 / 900 | 1.1 | Chữ Hán hiển thị lớn trên Shodo Canvas |
| **H1 (Page Title)** | `24px - 28px (1.5 - 1.75rem)` | Itim | 700 (Bold) | 1.25 | Tiêu đề chính màn hình (Bàn học, Cẩm nang ngữ pháp) |
| **H2 (Section Header)** | `18px - 20px (1.125 - 1.25rem)` | Itim | 700 (Bold) | 1.3 | Tiêu đề phân khu, tiêu đề Modal |
| **H3 (Card Title)** | `15px - 16px (0.937 - 1rem)` | Itim | 600 / 700 | 1.4 | Tiêu đề thẻ Card, tên bộ thẻ Flashcard |
| **Body Large** | `15px (0.9375rem)` | Segoe UI / Inter | 500 / 600 | 1.5 | Câu đàm thoại mẫu, lời khuyên Sensei |
| **Body (Default)** | `13px - 14px (0.8125 - 0.875rem)` | Segoe UI / Inter | 400 (Regular) | 1.6 | Văn bản thông thường, mô tả bài học, bản dịch |
| **Body Small** | `12px (0.75rem)` | Segoe UI / Inter | 500 / 600 | 1.5 | Nhãn nút phụ, tiêu đề bảng, thông tin phụ |
| **Caption / Badge** | `10px - 11px (0.625 - 0.6875rem)`| System UI / Inter | 700 (Bold) | 1.4 | Badge cấp độ JLPT, số lượng, Furigana `<rt>` |

---

### 2.3. Lưới Không Gian, Bo Góc & Đổ Bóng (Spacing, Radius & Shadows)

#### Lưới 8pt Grid
- `space-1` = `4px` (Khoảng cách giữa icon và chữ)
- `space-2` = `8px` (Khoảng cách giữa các chip, badge, gap nhỏ)
- `space-3` = `12px` (Padding nút bấm nhỏ, khoảng cách dòng)
- `space-4` = `16px` (Padding chuẩn của Card, gap lưới phần tử)
- `space-6` = `24px` (Padding màn hình, khoảng cách giữa các Section)
- `space-8` = `32px` (Khoảng cách phân đoạn lớn)

#### Quy Chuẩn Bo Góc Duy Nhất (Strict Fixed Border Radius)
> ⚠️ **ĐIỀU KHOẢN BẤT BIẾN:** Toàn bộ component hình chữ nhật (Nút bấm, Card, Input, Modal, Badge, Toast, Tab) trên hệ thống **DUY NHẤT dùng `rounded-[5px]`** (tương đương `border-radius: 5px;`).  
> Tuyệt đối cấm sử dụng bo góc lớn như `rounded-xl`, `rounded-2xl`, `rounded-3xl` gây phá vỡ phong cách Retro-Brutalist. Chỉ cho phép dùng `rounded-full` cho ảnh Avatar đại diện hoặc nút tròn icon 1:1.

#### Hệ Thống Đổ Bóng Khối Cứng (Retro Hard-Shadow Tokens)
```css
/* Tokens bóng đổ khối cứng (Hard Shadow) */
--shadow-retro-xs: 1.5px 1.5px 0px 0px #0F172A; /* Ô input, nút nhỏ trong table */
--shadow-retro-sm: 2px 2px 0px 0px #0F172A;     /* Nút phụ, tab level, pill bar */
--shadow-retro-md: 3px 3px 0px 0px #0F172A;     /* Thẻ Card, Slim Hero banner, Toast */
--shadow-retro-lg: 4px 4px 0px 0px #0F172A;     /* Card khi hover, khối thống kê */
--shadow-retro-xl: 6px 6px 0px 0px #0F172A;     /* Modal Dialog, Drawer */
```

---

## 3. Quy Chuẩn Thành Phần Giao Diện (Component Specifications)

### 3.1. Nút Bấm (Buttons)

Mọi nút bấm phải có hiệu ứng nhún Retro cơ học:
- **Normal:** Viền đen `border border-black`, bo góc `rounded-[5px]`, bóng đổ `shadow-[2px_2px_0px_0px_#0F172A]`.
- **Hover:** Dịch lên nhẹ `hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[3px_3px_0px_0px_#0F172A]`.
- **Active / Pressed:** Lún xuống `active:translate-x-[1px] active:translate-y-[1px] active:shadow-[1px_1px_0px_0px_#0F172A]`.
- **Disabled:** Mờ và vô hiệu chuột `disabled:opacity-40 disabled:cursor-not-allowed disabled:pointer-events-none`.

```html
<!-- 1. Primary CTA Button (Học viên) -->
<button type="button" class="px-4 py-2 bg-[#15803D] hover:bg-[#166534] text-white font-itim text-xs font-bold rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition active:scale-95 flex items-center gap-1.5 cursor-pointer">
    <span>🌸</span>
    <span>Lưu Vào Bộ Thẻ</span>
</button>

<!-- 2. AI Sensei Special Button (Tím Retro) -->
<button type="button" class="px-3.5 py-1.5 bg-[#7C3AED] hover:bg-[#6D28D9] text-white font-itim text-xs font-bold rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition active:scale-95 flex items-center gap-1.5 cursor-pointer">
    <span>🤖</span>
    <span>AI Đặt Câu</span>
</button>

<!-- 3. Secondary / Outlined Button (Nền trắng viền đen) -->
<button type="button" class="px-3.5 py-1.5 bg-white hover:bg-slate-50 text-black font-itim text-xs font-bold rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition active:scale-95 flex items-center gap-1.5 cursor-pointer">
    <span>📁</span>
    <span>Bộ Thẻ Cá Nhân</span>
</button>

<!-- 4. Danger Button (Hủy / Xóa) -->
<button type="button" class="px-3 py-1.5 bg-[#FEE2E2] hover:bg-[#FECACA] text-[#7F1D1D] font-itim text-xs font-bold rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] transition active:scale-95 flex items-center gap-1 cursor-pointer">
    <span>🗑️</span>
    <span>Xóa Tạm</span>
</button>
```

---

### 3.2. Biểu Mẫu Nhập Liệu (Form Inputs)

Mọi ô input phải tuân thủ chuẩn:
- Nhãn (Label): Viết hoa chữ cái đầu (Sentence case), font-bold `text-[11px] font-meta text-black`.
- Khung nhập: `rounded-[5px] border border-black text-xs font-body shadow-[1.5px_1.5px_0px_0px_#0F172A] outline-none focus:ring-1 focus:ring-black`.
- Gợi ý (Placeholder): Dùng tiếng Việt lịch sự kèm ví dụ thực tế.

```html
<!-- Text Input Chuẩn -->
<div class="space-y-1">
    <label for="deckTitleInput" class="block text-[11px] font-bold text-black font-meta">
        Tên bộ thẻ học tập:
    </label>
    <input type="text"
           id="deckTitleInput"
           placeholder="Ví dụ: Từ vựng giao tiếp N5, Chữ Hán công sở..."
           class="w-full px-3 py-2 text-xs rounded-[5px] border border-black text-black bg-white shadow-[1.5px_1.5px_0px_0px_#0F172A] outline-none focus:ring-1 focus:ring-black transition">
    <p class="text-[10px] text-slate-500 font-meta">Đặt tên ngắn gọn để dễ tra cứu khi ôn tập hàng ngày.</p>
</div>

<!-- Ô Nhập Số Nét (Quy chuẩn kích thước cố định w-20) -->
<input type="number"
       placeholder="Số nét"
       class="w-20 px-2 py-1.5 text-xs rounded-[5px] border border-black text-black bg-white shadow-[1.5px_1.5px_0px_0px_#0F172A] outline-none">
```

---

### 3.3. Thẻ Chứa Nội Dung & Quy Tắc Cố Định Khung (Card & Fixed Frame Containment)

> ⛔ **QUY TẮC CHỐNG CO DÃN & TRỒI SỤT BỐ CỤC (ZERO LAYOUT SHIFT):**
> 1. Mọi thẻ card trên lưới Flex/Grid bắt buộc có chiều cao cố định đồng nhất (ví dụ: `h-[360px] max-h-[360px]` cho thẻ Flashcard, `h-full min-h-[220px]` cho thẻ ngữ pháp).
> 2. Khung nội dung con thay đổi (danh sách Hán tự, Empty State, ví dụ) bắt buộc bọc trong ô box cố định kèm cuộn nội bộ: `h-[155px] max-h-[155px] overflow-y-auto`.
> 3. Menu thao tác 3 chấm [⋮] bắt buộc định vị tuyệt đối `position: absolute; z-index: 50;` nổi trên bề mặt, cấm làm co kéo các nút lân cận.

```html
<!-- Cấu trúc Card Chuẩn Retro-Brutalist -->
<div class="card-retro bg-white rounded-[5px] border border-black p-4 shadow-[3px_3px_0px_0px_#0F172A] flex flex-col justify-between hover:translate-x-[-1px] hover:translate-y-[-1px] hover:shadow-[4px_4px_0px_0px_#0F172A] transition duration-150">
    <!-- Header Thẻ -->
    <div class="flex items-start justify-between gap-2 pb-2.5 border-b border-black/10">
        <div>
            <h3 class="text-xl font-bold font-serif text-black leading-snug">〜てください</h3>
            <div class="text-[11px] text-slate-500 font-meta italic">te kudasai</div>
        </div>
        <span class="px-2 py-0.5 text-[10px] font-bold font-meta border border-black rounded-[4px] bg-emerald-100 text-emerald-800">
            🌸 N5
        </span>
    </div>

    <!-- Body Cố Định Chiều Cao -->
    <div class="py-2.5 flex-1">
        <div class="p-2 bg-[#FAF9F6] border border-black/15 rounded-[4px] text-xs font-mono mb-2">
            <span class="text-slate-400 font-bold text-[10px]">Công thức:</span>
            <span class="text-indigo-900 font-bold ml-1">Động từ thể て + ください</span>
        </div>
        <p class="text-xs font-bold text-[#14532D] leading-relaxed">
            👉 Xin hãy, vui lòng làm... (yêu cầu lịch sự)
        </p>
    </div>

    <!-- Footer Thao Tác -->
    <div class="pt-2.5 border-t border-black/10 grid grid-cols-2 gap-2">
        <button type="button" class="btn-retro text-xs py-1.5 px-2 bg-[#7C3AED] text-white rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold flex items-center justify-center gap-1">
            <span>🤖</span><span>AI Đặt Câu</span>
        </button>
        <button type="button" class="btn-retro text-xs py-1.5 px-2 bg-white text-black rounded-[5px] border border-black shadow-[2px_2px_0px_0px_#0F172A] font-itim font-bold flex items-center justify-center gap-1">
            <span>📁</span><span>Cất Vào Thẻ</span>
        </button>
    </div>
</div>
```

---

### 3.4. Hệ Thống Thông Báo Nổi Toàn Cục (`KizunaToast`)

- **Vị trí cố định:** Góc dưới bên phải `fixed bottom-5 right-5 z-[9999]`.
- **Cơ chế:** Nổi thụ động, không chặn click (`pointer-events-none` container, `pointer-events-auto` toast item), tự ẩn sau 3.5 giây.
- **Tuyệt đối cấm:** Chèn ô Alert tĩnh (`<div class="alert">`) làm xô lệch lưới phần tử bên dưới.

```javascript
// Gọi Toast từ JavaScript
KizunaToast.success('Đã lưu cấu trúc ngữ pháp vào bộ thẻ cá nhân! 🌸');
KizunaToast.warning('Vui lòng chọn hoặc nhập hoàn cảnh giao tiếp bạn nhé.');
KizunaToast.danger('Có lỗi khi kết nối máy chủ. Đang tải cấu trúc an toàn.');
KizunaToast.info('Mẫu câu này có 3 chữ Hán tương ứng để luyện viết Shodo.');
```

---

### 3.5. Hộp Thoại Tương Tác (`KizunaDialog` / Modal)

- **Backdrop:** Phủ đen mờ nhẹ `fixed inset-0 bg-black/60 backdrop-blur-xs z-50 flex items-center justify-center p-4`.
- **Hộp thoại chính:** `bg-white rounded-[5px] border-2 border-black p-5 shadow-[6px_6px_0px_0px_#0F172A] max-w-lg w-full max-h-[90vh] flex flex-col`.
- **Tiêu đề:** Font `Itim`, chữ thường trang nhã (Sentence case), có nút đóng `✕`.

---

### 3.6. Trạng Thái Rỗng Thân Thiện (Empty States)

Khi cơ sở dữ liệu chưa có bản ghi, hiển thị hộp rỗng thân thiện, tuyệt đối không dùng mảng giả hardcoded:
```html
<div class="p-8 text-center rounded-[5px] border-2 border-dashed border-black bg-white shadow-[3px_3px_0px_0px_#0F172A]">
    <div class="text-3xl mb-2">🍃</div>
    <h3 class="font-itim text-base font-bold text-black">Chưa tìm thấy dữ liệu bài học!</h3>
    <p class="text-xs text-slate-500 font-body mt-1">Hãy thử tìm kiếm với từ khóa khác hoặc chuyển sang chủ đề khác bạn nhé.</p>
</div>
```

---

## 4. Bố Cục Đáp Ứng & Breakpoints (Responsive Patterns)

| Breakpoint | Chiều Rộng Màn Hình | Hành Vi Bố Cục Thanh Điều Hướng | Lưới Thẻ Card |
| :--- | :--- | :--- | :--- |
| **Mobile (`sm`)** | `< 640px` | Sidebar chuyển thành Drawer trượt ẩn, có nút hamburger mở menu | `grid-cols-1` (1 cột chiếm trọn chiều ngang) |
| **Tablet (`md`)** | `640px - 1023px` | Header cố định trên cùng, thanh menu tab cho phép cuộn ngang | `grid-cols-2` (2 cột cân đối) |
| **Desktop (`lg` / `xl`)**| `>= 1024px` | Sidebar cố định bên trái (`w-64`), vùng nội dung cuộn độc lập (`overflow-y-auto`) | `grid-cols-3` hoặc `grid-cols-4` |

---

## 5. Quy Chuẩn Tiếp Cận (Accessibility - A11y) & Nguyên Tắc Lập Trình AI

### 5.1. Tiêu Chuẩn Tiếp Cận (A11y)
1. **Độ tương phản màu (Color Contrast):** Văn bản chính trên nền trắng/washi đạt tỷ lệ tối thiểu `4.5:1` (chuẩn WCAG AA). Không dùng chữ xám mờ trên nền trắng.
2. **Trạng thái Focus Bàn Phím:** Tất cả các phần tử có thể click (`<a>`, `<button>`, `<input>`, `<select>`) phải có chỉ báo focus rõ ràng: `focus-visible:ring-2 focus-visible:ring-black focus-visible:outline-none`.
3. **Thẻ ngữ nghĩa (Semantic HTML5):** Luôn dùng đúng `<header>`, `<main>`, `<nav>`, `<aside>`, `<section>`, `<article>`, `<ruby>`, `<rt>`.
4. **Phát âm Web Speech API:** Mọi nút loa phát âm âm thanh tiếng Nhật phải gắn thuộc tính `title="Nghe phát âm chuẩn giọng Nhật"` và `aria-label`.

### 5.2. Danh Sách Kiểm Tra Khi AI Sinh Mã Nguồn (AI Pre-Flight Checklist)
Trước khi xuất bất kỳ đoạn mã giao diện nào, AI phải tự kiểm tra 5 điều kiện sau:
- [ ] **1. Không có từ tiếng Anh trên UI:** Toàn bộ nhãn, nút bấm, placeholder, thông báo lỗi đã được dịch 100% sang tiếng Việt tự nhiên.
- [ ] **2. Không có chữ ALL-CAPS:** Không viết hoa toàn bộ câu (chỉ dùng Sentence case / Title case).
- [ ] **3. Bo góc chuẩn:** Duy nhất dùng `rounded-[5px]` cho các component hình chữ nhật.
- [ ] **4. Đổ bóng chuẩn:** Sử dụng bóng cứng `shadow-[..._#0F172A]` kèm viền đen `border border-black`.
- [ ] **5. Không chèn dữ liệu ảo:** Dùng API thực tế hoặc hiển thị Empty State chuẩn.

---
*Tài liệu này là nguồn chân lý thiết kế bất biến của KizunaTutor. Mọi màn hình mới và component phát sinh bắt buộc phải đối chiếu các Design Tokens quy định tại văn bản này.*
