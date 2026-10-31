# KIZUNATUTOR – QUY CHUẨN PHÁT TRIỂN FRONTEND TOÀN DỰ ÁN
*(KizunaTutor Frontend Development Architecture & Coding Standards)*

> **MỤC ĐÍCH TÀI LIỆU:**  
> Tài liệu này là **Kim chỉ nam duy nhất (Single Source of Truth)** cho toàn bộ thành viên trong nhóm phát triển Frontend của KizunaTutor. Mọi thành viên khi thêm trang mới, sửa component hoặc tích hợp API **BẮT BUỘC ĐỌC VÀ LÀM THEO 100%** để đảm bảo code đồng bộ, giao diện nhất quán, chuẩn nhận diện thương hiệu và dễ bảo trì.

---

## ⛔ 3 NGUYÊN TẮC BẤT BIẾN (STRICT NON-NEGOTIABLE GUARDRAILS)

Toàn bộ thành viên lập trình Frontend BẮT BUỘC tuân thủ nghiêm ngặt 3 quy tắc sau trong mọi trường hợp:

1. **KHÔNG BAO GIỜ ĐỂ LỘ MÃ BACKEND RA GIAO DIỆN (ZERO BACKEND LEAKS):**
   - Tuyệt đối cấm hiển thị: tên biến, enum, Exception, mã HTTP raw hay cấu trúc DB ra UI (ví dụ: cấm `ROLE_TUTOR`, `ROLE_LEARNER`, `BadCredentialsException`, `NullPointerException`, `500 INTERNAL_SERVER_ERROR`, `user_id`, `is_active`).
   - 100% thông báo, nhãn, badge, modal BẮT BUỘC phải được dịch sang **Tiếng Việt tự nhiên, lịch sự, thân thiện với người dùng**.
2. **KHÔNG BAO GIỜ VIẾT IN HOA TOÀN BỘ TRÊN GIAO DIỆN (NO ALL-CAPS UI TEXT):**
   - Tuyệt đối cấm dùng chữ IN HOA TOÀN BỘ cho thông báo Toast, Alert, Modal title, Empty States (ví dụ: cấm `LỖI ĐĂNG KÝ!`, `THÀNH CÔNG!`, `KHÔNG TÌM THẤY GIA SƯ!`).
   - Chỉ áp dụng viết hoa chữ cái đầu câu (Sentence case) hoặc đầu từ chính (Title Case). In hoa toàn bộ chỉ dùng cho từ viết tắt chuẩn quốc tế (`OTP`, `JLPT`, `AI`, `SVG`, `UI/UX`, `ID`, `VND`).
3. **KHÔNG BAO GIỜ DÙNG DỮ LIỆU ẢO ĐỂ ĐỐI PHÓ (NO MOCK / FAKE DATA INJECTION):**
   - Mọi dữ liệu hiển thị (danh sách gia sư, hồ sơ, ca rảnh, yêu cầu kết nối) **BẮT BUỘC lấy từ API Backend thực tế** nối với Supabase PostgreSQL.
   - Khi cơ sở dữ liệu chưa có dữ liệu, **BẮT BUỘC hiển thị Màn hình rỗng chuẩn (Empty State)** với thông điệp hướng dẫn lịch sự, tuyệt đối không bịa dummy JSON vào màn hình chính.

---

## 1. CÔNG NGHỆ & MÔI TRƯỜNG CHẠY (TECH STACK & RUNTIME)

- **Frontend Tech Stack**: HTML5, CSS3, JavaScript ES6+ (Native Modules `type="module"`), Tailwind CSS (Config CDN / PostCSS).
- **KHÔNG sử dụng**: Không dùng React, Vue, Angular, Next.js hay TypeScript (đảm bảo độ nhẹ, tương thích tối đa và chạy ngay trên mọi trình duyệt).
- **Cổng phân định chuẩn (Port Standards)**:
  - **Backend Spring Boot REST API**: `http://localhost:8085` (Prefix API: `/api/...`)
  - **Frontend Dev Server**: `http://localhost:3000` (Khởi chạy bằng `node frontend/server.js`)

---

## 2. CẤU TRÚC THƯ MỤC CHUẨN CỦA DỰ ÁN (PROJECT DIRECTORY STRUCTURE)

Hệ thống Frontend được tổ chức theo kiến trúc **Modular Feature-Driven Pages & Shared Assets**:

```text
frontend/
│
├── index.html                       # Entry point gốc (tự động chuyển hướng sang /pages/home/)
├── server.js                        # Node.js dev server tĩnh chạy cổng 3000 (hỗ trợ routing sạch)
├── FRONTEND_RULES.md                # Tài liệu chuẩn mực này
│
├── assets/                          # Tài nguyên tĩnh tập trung
│   ├── logos/
│   │   └── kizuna-logo.png          # Logo chính thức DUY NHẤT (dùng chung toàn hệ thống)
│   ├── images/                      # Ảnh minh họa, banner, minh họa trống (empty state)
│   ├── icons/                       # Icon SVG tuỳ biến
│   └── fonts/                       # Phông chữ địa phương hóa (nếu có)
│
├── styles/                          # CSS chuẩn hóa & biến hệ thống
│   ├── variables.css                # CSS Variables Design Tokens (:root)
│   ├── global.css                   # Global styles, reset & typography căn bản
│   ├── components.css               # Quy chuẩn style cho Buttons, Cards, Inputs, Modals
│   └── utilities.css                # Các utility classes bổ trợ cho Tailwind
│
├── components/                      # Các thành phần giao diện tái sử dụng
│   ├── header/
│   │   └── header.html              # Thanh Header chuẩn đồng bộ toàn bộ dự án
│   ├── footer/
│   │   └── footer.html              # Thanh Footer chuẩn đồng bộ toàn bộ dự án
│   └── modal/
│       └── modal.js                 # Helper mở / đóng modal popup dùng chung
│
├── pages/                           # Mỗi màn hình/chức năng là một thư mục riêng biệt
│   ├── home/                        # Trang chủ KizunaTutor
│   │   ├── index.html
│   │   └── home.js
│   ├── tutor-discovery/             # Trang Khám phá & Tìm kiếm gia sư
│   │   ├── index.html
│   │   └── tutor-discovery.js
│   ├── tutor-profile/               # Trang Xem chi tiết hồ sơ & lịch rảnh gia sư
│   │   ├── index.html
│   │   └── tutor-profile.js
│   └── connection-requests/         # Trang Quản lý yêu cầu kết nối học thử của học viên
│       ├── index.html
│       └── connection-requests.js
│
└── js/                              # Mã nguồn JavaScript cốt lõi dùng chung
    ├── api.js                       # Module gọi REST API tập trung (Fetch API wrapper)
    ├── main.js                      # Tiện ích chung: formatVND, showToast, Mobile Menu toggle
    ├── config/
    │   └── api.config.js            # Địa chỉ Base URL API Backend (http://localhost:8085/api)
    ├── data/
    │   └── tutors.mock.js           # Dữ liệu mẫu tham khảo cấu trúc (CHỈ DÙNG TEST CỤC BỘ)
    └── pages/
        └── tutors.js                # Helper render card gia sư dùng chung
```

---

## 3. QUY CHUẨN THANH ĐIỀU HƯỚNG HEADER (SYNCHRONIZED HEADER)

Tất cả các trang trong `pages/` **BẮT BUỘC dùng chung một cấu trúc Header** theo mẫu tại `components/header/header.html`:

### 3.1. Nhận diện Logo
- Sử dụng logo chính thức: `<img src="/assets/logos/kizuna-logo.png" alt="Kizuna Logo" class="h-9 w-9 object-contain rounded-full shadow-sm">`.
- Brand Name: `Kizuna` (`#0f2545`) + `Tutor` (`#006194`).
- Slogan bên dưới: `Kết nối song ngữ Việt – Nhật` (font-size: 10px, màu `#68737D`).

### 3.2. Menu điều hướng (Desktop & Mobile Drawer)
Gồm đúng 5 liên kết chuẩn:
1. `Tìm gia sư` ➔ `/pages/tutor-discovery/`
2. `Cách hoạt động` ➔ `/pages/home/#cach-hoat-dong`
3. `Trở thành gia sư` ➔ `/pages/home/#tro-thanh-gia-su`
4. `Yêu cầu đã gửi` ➔ `/pages/connection-requests/`
5. `Công cụ học` ➔ `/pages/home/#cong-cu-hoc`

### 3.3. Trạng thái Active Menu
Khi người dùng đang ở trang nào, mục menu đó được kích hoạt dạng nút xanh bo tròn:
```html
<a aria-current="page" class="font-label-md text-label-md px-3.5 py-2 rounded-lg bg-[#006194] text-white font-semibold shadow-sm transition-colors" href="/pages/tutor-discovery/">Tìm gia sư</a>
```

### 3.4. Cụm nút hành động bên phải (Strict Action Buttons)
- **Nút 1: Vào Dashboard học tập**:  
  Viền mỏng trang nhã `border border-[#006194]/20 text-[#006194]`, có biểu tượng `<span class="material-symbols-outlined text-[16px]">school</span>`.
- **Nút 2: Đăng ký**:  
  Nút màu xanh thương hiệu `bg-[#006194] text-white hover:bg-[#004b73]`, có icon mũi tên `<span class="material-symbols-outlined text-[16px]">arrow_forward</span>`.
- ⛔ **QUY TẮC BẮT BUỘC: KHÔNG ĐƯỢC CÓ NÚT "ĐĂNG NHẬP" TRÊN HEADER**:
  Theo thiết kế đã duyệt, trên Header **CHỈ CÓ NÚT ĐĂNG KÝ**, tuyệt đối không thêm lại nút Đăng nhập vào Header.

---

## 4. QUY CHUẨN CHÂN TRANG FOOTER (SYNCHRONIZED FOOTER)

Tất cả các trang **BẮT BUỘC sử dụng Footer 4 cột nền tối** theo mẫu tại `components/footer/footer.html`:

- **Màu nền**: Xanh than tối `bg-[#131d2e]`, viền trên `border-[#1f2d45]`, chữ sáng `#e3e8f3`.
- **Cấu trúc 4 cột**:
  1. **Thương hiệu & Triết lý**: Logo Kizuna + Slogan + Triết lý gắn kết bền vững (Kizuna) + Badge `verified` *"Bảo chứng hồ sơ & Năng lực sư phạm chuẩn JLPT"*.
  2. **Dành cho học viên**: 5 link (Tìm gia sư theo trình độ N5-N1, Gia sư bản xứ Tokyo, Khóa chuyên đề, Chính sách học thử 0đ, Tra cứu yêu cầu đã gửi).
  3. **Dành cho gia sư**: 5 link (Đăng ký gia sư mới, Không gian quản lý, Bộ quy chuẩn xác thực, Chính sách không cắt phế, Cộng đồng Senpai).
  4. **Hỗ trợ & Pháp lý**: 4 link (Điều khoản, Bảo mật, Khiếu nại, Trung tâm trợ giúp) + Khung nổi bật: `Hotline: 1900 6886 (8:00 – 22:00)`.
- **Dải bản quyền chân trang**:
  - `© 2024 KizunaTutor. Tất cả quyền được bảo lưu. Giữ trọn tinh thần kết nối Nhật – Việt 『絆』.`
  - Cụm bên phải: Biểu tượng `lock` *"Bảo mật SSL 256-bit"* và biểu tượng `translate` *"Hỗ trợ Song ngữ Tiếng Việt • 日本語"*.

---

## 5. HỆ THỐNG MÀU SẮC & DESIGN TOKENS CHUẨN

Toàn bộ nhóm phải dùng bảng màu đã định nghĩa trong `styles/variables.css`:

| Tên Token | Mã HEX | Mục đích sử dụng |
|---|---|---|
| **Primary** | `#006194` | Nút bấm chính, liên kết chính, trạng thái active |
| **Primary Hover** | `#004b73` / `#007bb9` | Hiệu ứng rê chuột trên nút chính |
| **Navy Brand** | `#0F2545` | Chữ Kizuna, tiêu đề lớn, thanh điều hướng nổi bật |
| **Warm Page Background** | `#FCFBF7` | Màu nền chính toàn trang (ấm áp, êm mắt phong cách Washi) |
| **Card / Surface** | `#FFFFFF` | Nền thẻ gia sư, bảng dữ liệu, form nhập liệu |
| **Warm Surface** | `#FAF8F2` | Nền ô input, nền phụ hỗ trợ |
| **Borders** | `#ECE8DE` / `#E5DECB` | Đường phân cách, viền thẻ card |
| **Text Primary** | `#0F2545` | Tiêu đề chính, tên gia sư, điểm số |
| **Text Secondary** | `#3F4850` | Nội dung mô tả, tiêu chí chuyên môn |
| **Text Muted** | `#68737D` | Nhãn phụ, phụ đề slogan, ghi chú nhỏ |
| **Success / Matcha** | `#059669` / `#88A064` | Trạng thái rảnh, đã duyệt, đã xác thực CCCD |
| **Warning / Amber** | `#D97706` | Đánh giá sao, trạng thái chờ duyệt |
| **Error / Crimson** | `#BA1A1A` | Thông báo lỗi, từ chối, xóa |

---

## 6. QUY TẮC VIẾT CODE TRANG MỚI (PAGE CREATION RULES)

Khi một thành viên trong nhóm cần tạo một trang mới (ví dụ: `pages/my-new-page/`):

1. **Tạo thư mục**: `frontend/pages/my-new-page/`.
2. **Tạo 2 file cơ bản**:
   - `index.html`: File giao diện HTML.
   - `my-new-page.js`: File xử lý logic JavaScript cho trang.
3. **Cấu trúc khung HTML chuẩn**:
   ```html
   <!DOCTYPE html>
   <html lang="vi">
   <head>
     <meta charset="utf-8">
     <meta name="viewport" content="width=device-width, initial-scale=1.0">
     <title>Tiêu đề trang - KizunaTutor</title>
     <link rel="icon" type="image/png" href="/assets/logos/kizuna-logo.png">
     <!-- Fonts & Icons -->
     <link rel="preconnect" href="https://fonts.googleapis.com">
     <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
     <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=Noto+Serif+JP:wght@400;600;700&display=swap" rel="stylesheet">
     <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200" />
     <!-- Tailwind & Project CSS -->
     <script src="https://cdn.tailwindcss.com"></script>
     <link rel="stylesheet" href="/styles/variables.css">
     <link rel="stylesheet" href="/styles/global.css">
     <link rel="stylesheet" href="/styles/components.css">
     <link rel="stylesheet" href="/styles/utilities.css">
   </head>
   <body class="bg-[#FCFBF7] text-[#131B2E] min-h-screen flex flex-col font-sans antialiased">
     
     <!-- 1. Header (Copy nguyên khối từ components/header/header.html) -->
     <header> ... </header>

     <!-- 2. Main Content -->
     <main class="w-full pt-28 pb-16 flex-1">
       <div class="max-w-[1240px] mx-auto px-4 md:px-8">
         <!-- Nội dung trang tại đây -->
       </div>
     </main>

     <!-- 3. Footer (Copy nguyên khối từ components/footer/footer.html) -->
     <footer class="mt-auto"> ... </footer>

     <!-- 4. Scripts -->
     <script type="module" src="/js/main.js"></script>
     <script type="module" src="./my-new-page.js"></script>
   </body>
   </html>
   ```
4. **Khai báo route trong `frontend/server.js`**: Nếu cần alias đẹp (ví dụ `/my-new-page`), hãy thêm vào mảng ánh xạ trong `frontend/server.js`.

---

## 7. QUY CHUẨN GỌI API & XỬ LÝ DỮ LIỆU (API INTEGRATION)

### 7.1. Cấu hình Endpoint
- Sử dụng file tập trung: `frontend/js/config/api.config.js`.
- Tuyệt đối không hardcode URL `http://localhost:8085` rải rác trong các file giao diện.
- Gọi qua wrapper hàm `fetchAPI` hoặc `apiService` trong `frontend/js/api.js`.

### 7.2. Xử lý 4 trạng thái giao diện bắt buộc:
Mọi màn hình gọi API BẮT BUỘC xử lý đủ 4 trạng thái sau:
1. **Loading State**: Hiển thị hiệu ứng tải mượt mà (Skeleton loading hoặc Spinner) để người dùng không cảm giác bị đơ màn hình.
2. **Success State**: Render danh sách hoặc dữ liệu chi tiết thực tế từ DB.
3. **Empty State (Trạng thái rỗng)**:
   - Khi API trả về mảng rỗng `[]`, hiển thị Card rỗng lịch sự:
   ```html
   <div class="text-center py-16 bg-white rounded-2xl border border-[#ECE8DE] p-8">
     <span class="material-symbols-outlined text-5xl text-[#68737D] mb-3">sentiment_dissatisfied</span>
     <h3 class="text-base font-bold text-[#0F2545]">Chưa tìm thấy gia sư phù hợp</h3>
     <p class="text-sm text-[#68737D] mt-1">Vui lòng thử điều chỉnh lại bộ lọc tìm kiếm hoặc quay lại sau ít phút.</p>
   </div>
   ```
4. **Error State**: Bắt lỗi `catch`, hiển thị thông báo nhẹ nhàng qua hàm `showToast('Không thể kết nối đến máy chủ. Vui lòng thử lại sau.', 'error')`.

---

## 8. BẢNG ÁNH XẠ CHUYỂN NGỮ BẮT BUỘC (MÃ BACKEND SANG TIẾNG VIỆT)

Trước khi hiển thị bất kỳ giá trị nào từ API Backend lên giao diện, **phải ánh xạ qua bảng sau**:

| Giá trị từ Backend / Exception | Hiển thị chuẩn trên Giao diện Frontend | CẤM HIỂN THỊ |
| :--- | :--- | :--- |
| `ROLE_ADMIN` | Quản trị viên hệ thống | ROLE_ADMIN, Admin |
| `ROLE_TUTOR` | Gia sư tiếng Nhật | ROLE_TUTOR, Tutor |
| `ROLE_LEARNER` / `STUDENT` | Học viên | ROLE_LEARNER, STUDENT |
| `ROLE_CS_STAFF` | Chăm sóc khách hàng | ROLE_CS_STAFF |
| `ROLE_ACADEMIC_OFFICER` | Cán bộ học vụ | ROLE_ACADEMIC_OFFICER |
| `BadCredentialsException` | Email hoặc mật khẩu không chính xác | Bad credentials |
| `UserAlreadyExistsException` | Email này đã được đăng ký trên hệ thống | User already exists |
| `500 Internal Server Error` | Hệ thống đang bận, bạn vui lòng thử lại sau | 500 Internal error |
| `NullPointerException` | Đã có lỗi xảy ra trong quá trình xử lý | Null pointer exception |
| `PENDING` | Chờ phản hồi | PENDING, pending |
| `ACCEPTED` | Đã chấp nhận kết nối | ACCEPTED, accepted |
| `REJECTED` | Đã từ chối kết nối | REJECTED, rejected |
| `CANCELLED` | Đã hủy yêu cầu | CANCELLED, cancelled |

---

## 9. QUY TẮC QUẢN TRỊ GIT (GIT COLLABORATION RULES)

1. **TUYỆT ĐỐI KHÔNG TỰ Ý CHẠY `git push` LÊN REMOTE:**  
   Mọi commit, kiểm thử chỉ được thực hiện trên máy cục bộ (Local Repository). Việc push code lên GitHub/GitLab do trưởng nhóm hoặc từng thành viên tự thực hiện thủ công sau khi đã kiểm tra giao diện chạy ổn định.
2. **Kiểm tra trạng thái trước khi commit:** Luôn gõ `git status` để xem các file bị ảnh hưởng, không commit đè lên code của người khác.
3. **Commit message rõ ràng:** Ví dụ: `feat(discovery): them bo loc trinh do N5-N1`, `fix(header): dong bo thanh dieu huong khong nut dang nhap`.

---

## 10. CHECKLIST TRƯỚC KHI BÀN GIAO MÃ NGUỒN

Trước khi hoàn thành bất kỳ task Frontend nào, hãy tự kiểm tra lại các mục sau:
- [ ] Trang đã sử dụng đúng Logo tại `/assets/logos/kizuna-logo.png` chưa?
- [ ] Header đã đồng bộ đúng 5 menu và **KHÔNG có nút Đăng nhập** chưa?
- [ ] Header có nút "Đăng ký" kèm biểu tượng `arrow_forward` chưa?
- [ ] Footer đã dùng bản 4 cột nền tối `#131d2e` kèm Hotline `1900 6886` chưa?
- [ ] Có bị lộ chữ tiếng Anh kỹ thuật hoặc mã Backend (`ROLE_...`, `Exception`) nào không?
- [ ] Có dòng thông báo nào bị viết IN HOA TOÀN BỘ không?
- [ ] Đã kiểm tra giao diện co giãn hiển thị tốt trên Mobile / Tablet / Desktop chưa?
- [ ] Mở Console trình duyệt (F12) kiểm tra không còn lỗi đỏ JavaScript nào.
