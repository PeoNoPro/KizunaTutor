# QUY CHUẨN THIẾT KẾ & CODE FRONTEND CHUẨN HÓA (DESIGN.MD)
*(Lấy Trang Chủ `pages/home/index.html` làm bản mẫu chuẩn mực cho toàn bộ dự án KizunaTutor)*

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

---

## 1. Mục đích tài liệu

File này là **khuôn mẫu chuẩn mực (Standard Form & Reference)** cho tất cả các thành viên trong nhóm phát triển Frontend của KizunaTutor. Toàn bộ các trang mới dựng (hoặc trang cần refactor) đều phải tuân theo cấu trúc HTML, hệ màu, Typography, quy cách bo góc, bóng đổ và các component từ **Trang Chủ (`frontend/pages/home/index.html`)**.

---

## 2. Bản mẫu HTML chuẩn mực (Standard Page Boilerplate)

Mọi trang HTML trong `frontend/pages/<tên-trang>/index.html` khi dựng mới **bắt buộc** phải sao chép bộ khung chuẩn sau:

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8">
  <meta content="width=device-width, initial-scale=1.0" name="viewport">
  <meta content="web_standard" name="shell-type">
  <title>Tên Trang - KizunaTutor</title>
  
  <!-- 1. Google Fonts Chuẩn (Plus Jakarta Sans + Noto Serif JP cho Kanji) -->
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Noto+Serif+JP:wght@400;600;700&display=swap" rel="stylesheet">
  <link href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" rel="stylesheet">
  
  <!-- 2. CSS Toàn Cục & Tokens -->
  <link rel="stylesheet" href="/styles/global.css">
  <link rel="stylesheet" href="/styles/components.css">
  <link rel="stylesheet" href="/styles/utilities.css">

  <!-- 3. Tailwind CSS CDN & Theme Config Đồng Bộ Với Trang Home -->
  <script src="https://cdn.tailwindcss.com?plugins=forms,container-queries"></script>
  <script id="tailwind-config">
    tailwind.config = {
      darkMode: "class",
      theme: {
        extend: {
          colors: {
            "primary": "#006194",
            "primary-container": "#007bb9",
            "on-primary": "#ffffff",
            "primary-fixed": "#cce5ff",
            "on-primary-fixed-variant": "#004b73",
            "secondary": "#4059aa",
            "tertiary": "#8d4b00",
            "error": "#ba1a1a",
            "background": "#fcfbf7",
            "surface": "#ffffff",
            "surface-dim": "#f2ecde",
            "on-surface": "#0f2545",
            "on-surface-variant": "#3f4850",
            "outline": "#707881",
            "outline-variant": "#ece8de"
          },
          borderRadius: {
            "xl": "0.75rem",
            "2xl": "1rem",
            "3xl": "1.5rem",
            "full": "9999px"
          },
          fontFamily: {
            "body-md": ["Plus Jakarta Sans", "sans-serif"],
            "kanji": ["Noto Serif JP", "serif"]
          }
        }
      }
    };
  </script>
</head>
<body class="bg-[#fcfbf7] font-body-md text-[#0f2545] antialiased selection:bg-[#cce5ff] selection:text-[#006194]">

  <!-- Header Dùng Chung (BẮT BUỘC dùng placeholder, KHÔNG hardcode HTML) -->
  <div id="header-placeholder"></div>

  <!-- NỘI DUNG TRANG CHÍNH -->
  <main class="w-full pt-20 bg-[#fcfbf7] min-h-[calc(100vh-20rem)]">
    <div class="max-w-[1240px] mx-auto px-4 sm:px-6 lg:px-8 py-8 sm:py-10">
      <!-- Toàn bộ nội dung giao diện của trang đặt ở đây -->
    </div>
  </main>

  <!-- Footer Dùng Chung (BẮT BUỘC dùng placeholder, KHÔNG hardcode HTML) -->
  <div id="footer-placeholder"></div>

  <!-- Scripts: Bắt buộc nạp main.js đầu tiên để tự động tải Header, Footer và Auth -->
  <script type="module" src="/js/main.js"></script>
  <script type="module" src="./ten-trang.js"></script>
</body>
</html>
```

---

## 3. Bảng màu & Design Tokens chuẩn (Theo Trang Home)

KizunaTutor sử dụng phong cách **Wabi-Sabi kết hợp EdTech hiện đại**, tạo cảm giác nhẹ nhàng như trang giấy Washi truyền thống kết hợp với màu lam chàm Aizome đáng tin cậy:

| Phân loại | Mã Màu Hex | Lớp Tailwind tương đương | Mục đích sử dụng |
| :--- | :--- | :--- | :--- |
| **Nền trang chính** | `#FCFBF7` | `bg-[#fcfbf7]` | Nền giấy Washi ấm, áp dụng cho toàn bộ `body` và `main` |
| **Nền thẻ nổi bật** | `#FAF8F2` | `bg-[#faf8f2]` | Nền khối tìm kiếm, dropdown, ô input, section xen kẽ |
| **Nền Card / Modal** | `#FFFFFF` | `bg-white` | Nền thẻ thông tin gia sư, modal popup, box nội dung |
| **Viền chuẩn (Border)** | `#ECE8DE` | `border-[#ece8de]` | Đường phân cách, viền Card, viền Header/Footer |
| **Viền Input (Active)** | `#E2D9C6` | `border-[#e2d9c6]` | Viền ô nhập liệu khi ở trạng thái bình thường |
| **Màu thương hiệu (Primary)** | `#006194` | `bg-[#006194]`, `text-[#006194]` | Màu nút chính, liên kết active, tiêu đề quan trọng |
| **Primary Hover** | `#004B73` | `hover:bg-[#004b73]` | Trạng thái hover của nút bấm chính |
| **Primary Soft** | `#E0EFF7` / `#CCE5FF` | `bg-[#e0eff7]`, `bg-[#cce5ff]` | Nền tag đã chọn, badge số lượng, avatar dự phòng |
| **Chữ tiêu đề (Heading)** | `#0F2545` | `text-[#0f2545]` | Tiêu đề H1, H2, H3, họ tên gia sư, độ tương phản cao |
| **Chữ nội dung (Body)** | `#3F4850` | `text-[#3f4850]` | Đoạn văn mô tả, nhãn trường, nội dung chi tiết |
| **Chữ phụ (Caption/Muted)** | `#68737D` | `text-[#68737d]` | Chú thích số nét, ngày tạo, email phụ, phụ đề |
| **Màu điểm nhấn đỏ (Akane)** | `#BA1A1A` | `text-[#ba1a1a]`, `bg-rose-50` | Điểm nhấn chữ Hán `『 絆 』`, cảnh báo lỗi, nút đăng xuất |
| **Màu thành công (Matcha)** | `#166534` | `text-emerald-700`, `bg-emerald-50` | Badge xác minh danh tính, lịch rảnh, đã hoàn thành |

---

## 4. Typography & Phân cấp chữ (Hierarchy)

1. **Font chữ La-tinh & Tiếng Việt**: `Plus Jakarta Sans`, hỗ trợ hiển thị dấu tiếng Việt chuẩn và hiện đại.
2. **Font chữ Hán / Kanji**: `Noto Serif JP` (`font-kanji`), tạo nét trang trọng, bút lông thư pháp.
3. **Bộ Icon**: **Google Material Symbols Outlined** (dùng thẻ `<span class="material-symbols-outlined text-[18px]">tên_icon</span>`).

### Kích thước chuẩn:
- **Tiêu đề trang (H1 / Hero Title)**: `text-3xl sm:text-4xl lg:text-5xl font-extrabold text-[#0f2545] tracking-tight leading-[1.15]`
- **Tiêu đề mục (H2 / Section Title)**: `text-2xl sm:text-3xl font-bold text-[#0f2545] tracking-tight`
- **Tiêu đề khối / Card (H3)**: `text-base sm:text-lg font-bold text-[#0f2545]`
- **Văn bản thân (Body text)**: `text-sm sm:text-base text-[#3f4850] leading-relaxed`
- **Nhãn ngắn / Phụ đề / Caption**: `text-xs text-[#68737d] font-medium`

---

## 5. Thư viện Component chuẩn từ Trang Home (Mẫu Code)

### 5.1. Nút bấm (Button Styles)

#### A. Nút chính (Primary CTA Button)
```html
<button type="button" class="inline-flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl bg-[#006194] hover:bg-[#004B73] text-white font-semibold text-sm shadow-sm transition-all duration-200 cursor-pointer">
  <span class="material-symbols-outlined text-[18px]">calendar_month</span>
  <span>Đặt lịch học thử 1-1</span>
</button>
```

#### B. Nút phụ mềm (Secondary Soft Button)
```html
<button type="button" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-[#E0EFF7] hover:bg-[#CCE5FF] text-[#006194] font-semibold text-sm transition-all duration-200 cursor-pointer">
  <span class="material-symbols-outlined text-[18px]">visibility</span>
  <span>Xem chi tiết hồ sơ</span>
</button>
```

#### C. Nút viền trắng (Outline / Ghost Button)
```html
<button type="button" class="inline-flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-white border border-[#ECE8DE] hover:bg-[#FAF8F2] text-[#3F4850] hover:text-[#006194] font-semibold text-sm transition-all duration-200 cursor-pointer">
  <span class="material-symbols-outlined text-[18px]">arrow_back</span>
  <span>Quay lại</span>
</button>
```

#### D. Nút cảnh báo / Hủy (Danger Button)
```html
<button type="button" class="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-xl bg-rose-50 border border-rose-200 hover:bg-rose-100 text-[#BA1A1A] font-semibold text-xs transition-all duration-200 cursor-pointer">
  <span class="material-symbols-outlined text-[16px]">delete</span>
  <span>Hủy yêu cầu</span>
</button>
```

---

### 5.2. Huy hiệu & Thẻ trạng thái (Badges & Chips)

```html
<!-- Badge Điểm nhấn thương hiệu Wabi-Sabi -->
<div class="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-[#F2ECDE] border border-[#E2D9C5] shadow-xs">
  <span class="font-kanji font-bold text-red-700 text-xs">『 絆 』</span>
  <span class="text-xs font-bold text-[#0F2545] tracking-wide">Nền tảng kết nối tiếng Nhật 1-1</span>
</div>

<!-- Badge Xác minh chứng chỉ (Thành công) -->
<span class="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-800 border border-emerald-200">
  <span class="material-symbols-outlined text-[15px] text-emerald-600">verified</span>
  <span>Đã xác minh JLPT N1</span>
</span>

<!-- Chip Bộ lọc tương tác (Filter Chip) -->
<button type="button" class="px-4 py-2 rounded-xl text-xs font-semibold bg-[#006194] text-white shadow-xs cursor-pointer">
  Tất cả mức giá
</button>
<button type="button" class="px-4 py-2 rounded-xl text-xs font-semibold bg-white border border-[#ECE8DE] text-[#3F4850] hover:bg-[#FAF8F2] transition-colors cursor-pointer">
  Luyện thi JLPT
</button>
```

---

### 5.3. Ô nhập liệu & Dropdown (Form Inputs & Selects)

```html
<!-- Ô Select chuẩn bộ lọc -->
<div class="flex flex-col gap-1.5">
  <label for="filterLevel" class="text-xs font-bold text-[#0F2545]">Trình độ mục tiêu</label>
  <div class="relative">
    <select id="filterLevel" class="w-full h-11 pl-3.5 pr-9 rounded-xl bg-[#FAF8F2] text-xs font-semibold text-[#0F2545] border border-[#E2D9C6] focus:outline-none focus:border-[#006194] focus:bg-white transition-colors appearance-none cursor-pointer">
      <option value="">Tất cả trình độ (N5 – N1)</option>
      <option value="N1">JLPT N1 (Cao cấp)</option>
      <option value="N2">JLPT N2 (Trung cấp)</option>
    </select>
    <span class="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-[#68737D] pointer-events-none text-[18px]">expand_more</span>
  </div>
</div>

<!-- Ô Text Input chuẩn -->
<div class="flex flex-col gap-1.5">
  <label for="inputKeyword" class="text-xs font-bold text-[#0F2545]">Từ khóa tìm kiếm</label>
  <div class="relative">
    <input type="text" id="inputKeyword" placeholder="Nhập tên gia sư, kỹ năng..." class="w-full h-11 pl-10 pr-4 rounded-xl bg-[#FAF8F2] text-xs font-medium text-[#0F2545] placeholder-[#68737D] border border-[#E2D9C6] focus:outline-none focus:border-[#006194] focus:bg-white transition-colors">
    <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-[#68737D] text-[18px]">search</span>
  </div>
</div>
```

---

### 5.4. Khung thẻ thông tin chuẩn (Standard Card Container)

```html
<div class="bg-white rounded-2xl border border-[#ECE8DE] p-5 sm:p-6 shadow-xs hover:shadow-md hover:border-[#006194]/30 transition-all duration-300 flex flex-col justify-between">
  <div>
    <!-- Card Header: Avatar & Info -->
    <div class="flex items-start gap-4 mb-4">
      <div class="w-14 h-14 rounded-full bg-[#E0EFF7] text-[#006194] font-bold text-lg flex items-center justify-center shrink-0 border border-[#ECE8DE]">
        YK
      </div>
      <div class="min-w-0 flex-1">
        <h3 class="text-base font-bold text-[#0F2545] truncate">Yamada Kenji (Gia sư)</h3>
        <p class="text-xs text-[#68737D] mt-0.5">Tokyo, Nhật Bản • 5 năm kinh nghiệm</p>
      </div>
    </div>

    <!-- Card Content -->
    <p class="text-xs text-[#3F4850] line-clamp-3 leading-relaxed mb-4">
      Chuyên sâu luyện Kaiwa phản xạ thương mại và ngữ pháp Đọc hiểu JLPT N1-N2. Giảng dạy nhiệt tình, phát âm chuẩn Tokyo.
    </p>
  </div>

  <!-- Card Footer: Price & Action -->
  <div class="pt-4 border-t border-[#ECE8DE] flex items-center justify-between">
    <div>
      <span class="text-[11px] text-[#68737D] block">Học phí theo giờ</span>
      <span class="text-sm font-bold text-[#006194]">250.000 đ <span class="text-[11px] font-normal text-[#68737D]">/ 60 phút</span></span>
    </div>
    <a href="/tutor-profile" class="px-4 py-2 rounded-xl bg-[#006194] hover:bg-[#004B73] text-white text-xs font-semibold transition-colors">
      Xem hồ sơ
    </a>
  </div>
</div>
```

---

### 5.5. Trạng thái rỗng (Empty State - BẮT BUỘC khi không có dữ liệu)

> ⛔ **Tuyệt đối không chèn mảng giả / mock data khi danh sách trống. Phải hiển thị Empty State:**

```html
<div class="w-full py-16 px-4 flex flex-col items-center justify-center text-center bg-white rounded-2xl border border-[#ECE8DE] shadow-xs">
  <div class="w-16 h-16 rounded-full bg-[#FAF8F2] border border-[#ECE8DE] flex items-center justify-center text-[#68737D] mb-4">
    <span class="material-symbols-outlined text-[32px]">search_off</span>
  </div>
  <h3 class="text-base font-bold text-[#0F2545] mb-1.5">Không tìm thấy kết quả phù hợp</h3>
  <p class="text-xs text-[#68737D] max-w-md mb-6 leading-relaxed">
    Hiện tại hệ thống chưa tìm thấy dữ liệu khớp với điều kiện tìm kiếm của bạn. Hãy thử thay đổi bộ lọc hoặc từ khóa khác.
  </p>
  <button type="button" class="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-[#FAF8F2] border border-[#ECE8DE] text-[#0F2545] hover:bg-[#EAE4D3] text-xs font-semibold transition-colors cursor-pointer">
    <span class="material-symbols-outlined text-[16px]">refresh</span>
    <span>Xóa toàn bộ bộ lọc</span>
  </button>
</div>
```

---

### 5.6. Trạng thái đang tải (Loading Spinner)

```html
<div class="w-full py-20 flex flex-col items-center justify-center gap-3 text-center">
  <div class="w-10 h-10 border-3 border-[#ECE8DE] border-t-[#006194] rounded-full animate-spin"></div>
  <span class="text-xs font-semibold text-[#68737D]">Đang tải dữ liệu từ hệ thống...</span>
</div>
```

---

## 6. Quy chuẩn viết code JavaScript gọi API

Mọi tương tác dữ liệu từ Backend Spring Boot (`http://localhost:8085/api/...`) phải tuân theo cấu trúc mô-đun sau:

```javascript
// File: frontend/pages/<tên-trang>/<tên-trang>.js
import { api } from '/js/api.js';

document.addEventListener('DOMContentLoaded', async () => {
  await loadPageData();
});

async function loadPageData() {
  const container = document.getElementById('dataContainer');
  if (!container) return;

  // 1. Hiển thị Spinner khi đang gọi mạng
  container.innerHTML = `
    <div class="w-full py-16 flex flex-col items-center justify-center gap-3">
      <div class="w-8 h-8 border-2 border-[#ECE8DE] border-t-[#006194] rounded-full animate-spin"></div>
      <p class="text-xs text-[#68737D]">Đang tải dữ liệu...</p>
    </div>
  `;

  try {
    const response = await api.get('/api/endpoint-thuc-te');
    const items = response.data || [];

    // 2. Không có dữ liệu -> Render Empty State
    if (items.length === 0) {
      container.innerHTML = renderEmptyState('Chưa có dữ liệu nào được ghi nhận.');
      return;
    }

    // 3. Có dữ liệu thật -> Render danh sách
    container.innerHTML = items.map(item => renderItemCard(item)).join('');
  } catch (error) {
    console.error('Lỗi khi tải dữ liệu:', error);
    // 4. Lỗi mạng -> Báo lỗi thân thiện, KHÔNG để lộ StackTrace hay 500 Internal Error
    container.innerHTML = `
      <div class="p-6 text-center text-xs text-[#BA1A1A] bg-rose-50 rounded-xl border border-rose-200">
        Không thể kết nối với máy chủ lúc này. Vui lòng thử lại sau giây lát.
      </div>
    `;
  }
}
```

---

## 7. Bảng kiểm tra trước khi hoàn thành màn hình (Frontend Checklist)

Trước khi xác nhận hoàn thành hoặc nộp mã nguồn cho nhóm:
- [ ] Trang đã dùng đúng thẻ `<div id="header-placeholder"></div>` và `<div id="footer-placeholder"></div>`.
- [ ] Không có mã inline style (`style="..."`) hay mã inline onclick (`onclick="..."`).
- [ ] Không có chữ IN HOA TOÀN BỘ trên bất kỳ nút bấm, thông báo hay tiêu đề nào (trừ chữ viết tắt `JLPT`, `AI`, `OTP`, `ID`).
- [ ] Đã có trạng thái đang tải (Loading Spinner) và trạng thái rỗng (Empty State) khi API trả về rỗng.
- [ ] Toàn bộ chữ trên màn hình hiển thị bằng tiếng Việt chuẩn ngữ cảnh giáo dục.
- [ ] Layout hiển thị mượt mà trên cả Mobile (`max-w-full px-4`) và Desktop (`max-w-[1240px] mx-auto`).
- [ ] Không có dữ liệu mock array hay dummy JSON nhúng cố định trong mã chạy thực tế.

### 4. NGUYÃŠN Táº®C Báº®T BUá»˜C KHI THÃŠM Má»šI TÃNH NÄ‚NG / MÃ€N HÃŒNH (MANDATORY INHERITANCE RULE)
> âš ï¸ **Äáº¶C QUYá»€N ÃP Dá»¤NG RIÃŠNG CHO KHá»I "Há»ŒC Táº¬P & RÃˆN LUYá»†N":**  
> Má»—i khi phÃ¡t triá»ƒn hoáº·c thÃªm má»›i báº¥t ká»³ mÃ n hÃ¬nh, thÃ nh pháº§n (component), popup/modal hay tÃ­nh nÄƒng nÃ o thuá»™c khá»‘i **Há»c Táº­p & RÃ¨n Luyá»‡n (Learning & Practice)** cá»§a Há»c viÃªn:
> 
> 1. **Báº¯t buá»™c Ä‘á»c láº¡i vÃ  tuÃ¢n thá»§ tuyá»‡t Ä‘á»‘i cÃ¡c quy táº¯c á»Ÿ trÃªn:**
>    - NhÃºng file CSS dÃ¹ng chung: `<link rel="stylesheet" href="/assets/css/learning-support-common.css">` á»Ÿ Ä‘áº§u tháº» `<head>`.
>    - Sá»­ dá»¥ng Ä‘Ãºng há»‡ thá»‘ng Design Tokens, tháº» bÃ i há»c `.ls-card`, cá»¥m nÃºt báº¥m `.ls-btn` (`.ls-btn-primary`, `.ls-btn-outline`, `.ls-btn-shodo`, `.ls-btn-active`), thanh tÃ¡c vá»¥ `.ls-toolbar`, bá»™ lá»c `.ls-pill`, Ã´ nháº­p `.ls-input` vÃ  huy hiá»‡u `.ls-badge`.
>    - Tuyá»‡t Ä‘á»‘i tuÃ¢n thá»§ nguyÃªn táº¯c WCAG AAA chá»‘ng chá»¯ tÃ ng hÃ¬nh (`--color-text-primary: #0F2545`, `--color-text-secondary: #3F4850`), khÃ´ng bao giá» Ä‘á»ƒ chá»¯ tráº¯ng trÃªn ná»n sÃ¡ng.
>    - ToÃ n bá»™ luá»“ng dá»¯ liá»‡u pháº£i lÃ  Zero-Reload: truyá»n ngáº§m JSON qua `apiClient.js` vÃ  cáº­p nháº­t DOM Ä‘á»™ng, kÃ¨m thÃ´ng bÃ¡o ná»•i Toast/Dialog.
> 
> 2. **Giá»›i háº¡n pháº¡m vi nghiÃªm ngáº·t:**  
>    Quy Ä‘á»‹nh nÃ y **chá»‰ Ã¡p dá»¥ng duy nháº¥t cho cÃ¡c pháº§n trong khá»‘i Há»c Táº­p & RÃ¨n Luyá»‡n** (bao gá»“m: Kana Quiz, Bá»™ tháº» Flashcards, Chi tiáº¿t Flashcard, Luyá»‡n láº­t tháº», PhÃ²ng luyá»‡n viáº¿t Shodo, Tra cá»©u Tá»« Ä‘iá»ƒn chá»¯ HÃ¡n, Tra cá»©u viáº¿t tay AI, Game MÆ°a chá»¯ HÃ¡n, Ngá»¯ phÃ¡p & AI Sensei, Trá»£ lÃ½ há»c táº­p vÃ  cÃ¡c module há»c táº­p má»›i phÃ¡t triá»ƒn thÃªm sau nÃ y). Tuyá»‡t Ä‘á»‘i **khÃ´ng Ã¡p dá»¥ng** sang cÃ¡c phÃ¢n há»‡ ngoÃ i khá»‘i nÃ y (nhÆ° Admin, Manager, Gia sÆ°, CSKH hay cÃ¡c trang Portal cÃ´ng khai).
## Äá»’NG Bá»˜ TOÃ€N DIá»†N KHá»I Há»ŒC Táº¬P & RÃˆN LUYá»†N
- **Chuáº©n tham chiáº¿u gá»‘c (Benchmark):** Giao diá»‡n trang Äáº¥u TrÆ°á»ng GÃµ Kana (/kana).
- **Typography báº¯t buá»™c:** Chá»‰ sá»­ dá»¥ng 2 font: ui-sans-serif (vÄƒn báº£n) vÃ  ui-monospace (input, code, Furigana, badges). Thang Ä‘o: 10px / 12px / 14px / 16px / 18px.
- **Bo gÃ³c chuáº©n:** Cá»‘ Ä‘á»‹nh adius-sm: 6px cho card/button/input vÃ  adius-full: 9999px cho filter pills.
- **MÃ u ná»n bao phá»§:** --color-bg-pink-tint: #FFE5EC káº¿t há»£p tháº» tráº¯ng #FFFFFF vÃ  viá»n #0F172A.
- **NghiÃªm cáº¥m:** KhÃ´ng tá»± viáº¿t class CSS riÃªng láº» trÃªn tá»«ng trang lÃ m phÃ¡ vá»¡ tÃ­nh Ä‘á»“ng nháº¥t.
