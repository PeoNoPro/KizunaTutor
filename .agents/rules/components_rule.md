# QUY CHUẨN BẤT BIẾN VỀ COMPONENT DÙNG CHUNG (COMPONENTS RULE)
*(Dự án KizunaTutor — Dành cho AI Assistant và toàn bộ lập trình viên)*

<!-- ===================================================================== -->
<!-- [IMMUTABLE COMPONENT GUARDRAILS] STRICT COMPLIANCE REQUIRED           -->
<!-- KHỐI QUY TẮC BẤT BIẾN: BẮT BUỘC TUÂN THỦ 100%, KHÔNG NGOẠI LỆ        -->
<!-- ===================================================================== -->

> ⛔ **NGUYÊN TẮC TỐI THƯỢNG VỀ COMPONENT (STRICT GUARDRAILS):**
> 
> 1. **TUYỆT ĐỐI CẤM HARDCODE VÀO TRANG (`pages/*/index.html`):**
>    - Mọi thành phần giao diện dùng chung (Header, Footer, Modal dùng chung) **BẮT BUỘC** phải nằm riêng biệt trong thư mục `frontend/components/`.
>    - Các trang trong `frontend/pages/*/index.html` (như Trang chủ, Tìm gia sư, Chi tiết gia sư...) **TUYỆT ĐỐI KHÔNG ĐƯỢC CHỨA** khối mã HTML trực tiếp của `<header>...</header>` hay `<footer>...</footer>`.
>    - Các trang con **CHỈ ĐƯỢC PHÉP** khai báo thẻ giữ chỗ (placeholder) duy nhất:
>      ```html
>      <!-- Header dùng chung (Nạp tự động từ /components/header/header.html) -->
>      <div id="header-placeholder"></div>
> 
>      <!-- Footer dùng chung (Nạp tự động từ /components/footer/footer.html) -->
>      <div id="footer-placeholder"></div>
>      ```
> 
> 2. **SỬA ĐÚNG NƠI QUY ĐỊNH (`frontend/components/`):**
>    - Khi người dùng hoặc task yêu cầu chỉnh sửa Header (thay đổi logo, slogan, menu, nút bấm, style, trạng thái):
>      ➔ **CHỈ ĐƯỢC PHÉP SỬA TRONG `frontend/components/header/header.html`**.
>    - Khi có yêu cầu sửa Footer:
>      ➔ **CHỈ ĐƯỢC PHÉP SỬA TRONG `frontend/components/footer/footer.html`**.
>    - Tuyệt đối không được sửa hay chèn đè code Header/Footer vào file `index.html` của bất kỳ trang nào.
> 
> 3. **DUY NHẤT MỘT NGUỒN CHÂN LÝ (SINGLE SOURCE OF TRUTH):**
>    - Không được tạo các biến thể phân mảnh như `header-guest.html`, `header-learner.html`, `header-home.html`.
>    - Toàn bộ ứng dụng KizunaTutor dùng **DUY NHẤT 1 file Header** (`components/header/header.html`) và **DUY NHẤT 1 file Footer** (`components/footer/footer.html`). Mọi thay đổi tại file component này phải có hiệu lực đồng thời trên toàn bộ các trang.

<!-- ===================================================================== -->

---

## 1. Cấu trúc thư mục Component chuẩn

```text
frontend/components/
├── header/
│   └── header.html          # File HTML duy nhất chứa mã thanh điều hướng Header
├── footer/
│   └── footer.html          # File HTML duy nhất chứa mã chân trang Footer
└── modal/
    └── modal.js             # Logic mở/đóng modal popup dùng chung
```

---

## 2. Cách các trang `pages/*/index.html` tích hợp Component

Mọi trang HTML trong `frontend/pages/` khi dựng mới hoặc chỉnh sửa **BẮT BUỘC** tuân thủ cấu trúc sau:

```html
<!DOCTYPE html>
<html lang="vi">
<head>
  <!-- Các thẻ meta, font, css... -->
</head>
<body class="bg-[#FCFBF7] text-[#0F2545] ...">

  <!-- 1. HEADER CHỈ GỌI QUA PLACEHOLDER -->
  <div id="header-placeholder"></div>

  <!-- 2. NỘI DUNG CHÍNH CỦA RIÊNG TRANG ĐÓ -->
  <main class="pt-20 ...">
    ...
  </main>

  <!-- 3. FOOTER CHỈ GỌI QUA PLACEHOLDER -->
  <div id="footer-placeholder"></div>

  <!-- 4. SCRIPTS (main.js bắt buộc để tự động nạp component) -->
  <script type="module" src="/js/main.js"></script>
  <script type="module" src="./trang-cua-ban.js"></script>
</body>
</html>
```

---

## 3. Cơ chế hoạt động của Component Loader

Component được nạp tự động qua hàm `loadSharedComponents()` nằm trong `frontend/js/main.js`:

1. Khi sự kiện `DOMContentLoaded` kích hoạt, `main.js` tìm kiếm phần tử `#header-placeholder` và `#footer-placeholder`.
2. Gửi request `fetch('/components/header/header.html')` và `fetch('/components/footer/footer.html')`.
3. Thay thế nội dung bằng `outerHTML`, giữ nguyên cấu trúc DOM chuẩn.
4. Tự động kích hoạt hàm `initNavigation()` để:
   - Nhận diện URL hiện tại và bôi màu xanh (`bg-[#E0EFF7] text-[#006194]`) cho tab đang active (`/tutors`, `/home`...).
   - Gắn sự kiện click mở/đóng cho nút Menu trên thiết bị di động (`#mobileMenuBtn` và `#mobileMenu`).

---

## 4. Bảng kiểm tra trước khi xác nhận hoàn thành (Checklist)

Trước khi báo cáo với người dùng hoặc đánh dấu `[x]` task liên quan đến giao diện:

- [ ] File `frontend/pages/*/index.html` của trang đang làm **KHÔNG** chứa thẻ `<header>` hay `<footer>` tự viết.
- [ ] File `frontend/pages/*/index.html` đã có `<div id="header-placeholder"></div>` và `<div id="footer-placeholder"></div>`.
- [ ] Đã nhúng `<script type="module" src="/js/main.js"></script>`.
- [ ] Các chỉnh sửa về giao diện, nút bấm, menu Header đều được thực hiện trực tiếp tại `frontend/components/header/header.html`.
- [ ] Mở cả `/home` và `/tutors` trên trình duyệt để xác nhận Header hiển thị đồng bộ 100%.

