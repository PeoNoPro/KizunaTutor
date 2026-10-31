# Implementation Plan: FE-04 — Tutor Discovery & Search (Learner)

**Branch**: `haipt` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Đặc tả phân hệ Tutor Discovery (UC-29, UC-30, UC-31, UC-77) và bản thiết kế frontend HTML/Tailwind CSS chuẩn từ Figma do người dùng cung cấp.

---

## Summary

Triển khai hoàn chỉnh phân hệ Tìm kiếm & Khám phá gia sư tiếng Nhật dành cho Học viên (Learner) và Khách vãng lai. Giữ nguyên 100% phong cách thiết kế, phông chữ và bảng màu thương hiệu chuẩn (`FRONTEND_RULES.md`). Xây dựng module điều khiển JavaScript ES6+ hỗ trợ tìm kiếm trực tiếp (Live Filtering với debounce 300ms), kết hợp đa tầng 5 tiêu chí lọc (Mục tiêu, Trình độ JLPT, Hình thức học, Học phí, Lịch rảnh), sắp xếp linh hoạt và trạng thái rỗng thân thiện (Empty State - MSG01). Toàn bộ dữ liệu vận hành trên mô hình Mock Data chuẩn độc lập trước khi tích hợp API backend.

---

## Technical Context

**Language/Version**: HTML5, CSS3, JavaScript ES6+ (Browser native).

**Primary Dependencies**: Tailwind CSS CDN, Google Fonts (Plus Jakarta Sans, Noto Serif JP), Google Material Symbols Outlined.

**Storage**: Mock Data tĩnh lưu tại `frontend/js/data/tutors.mock.js`. Cấu trúc đối tượng khớp 100% với Entity Backend (`User`, `Tutor`, `TutorProfile`, `TutorReview`).

**Testing**: Kiểm thử thủ công trên trình duyệt (Desktop 1240px, Tablet 768px, Mobile 375px), kiểm tra Console không có lỗi JavaScript, xác minh logic lọc và sắp xếp.

**Target Platform**: Web Browser (Chrome, Edge, Firefox, Safari).

**Project Type**: Frontend Client-side Web Module (Monorepo `frontend/`).

**Performance Goals**: Live filtering phản hồi tức thì dưới 100ms; debounce nhập liệu 300ms; chuyển trang và sắp xếp không giật lag.

**Constraints**: Tuân thủ quy định bất biến trong `AGENTS.md` (100% Tiếng Việt thân thiện, không viết IN HOA TOÀN BỘ, không để lộ mã code nội bộ); thiết kế bám sát `FRONTEND_RULES.md`.

**Scale/Scope**: Bao gồm Trang danh sách & lọc gia sư (`tutors.html`), module mock data (`tutors.mock.js`), controller logic (`tutors.js`), và liên kết điều hướng từ trang chủ (`home.html`).

---

## Constitution Check

*GATE: Kiểm tra trước thiết kế và sau khi xây dựng.*

| Nguyên tắc | Đánh giá | Bằng chứng kiểm tra |
| :--- | :--- | :--- |
| **I. Tiếng Việt, không lộ kỹ thuật** | **ĐẠT** | 100% nhãn, placeholder, badge, thông báo trạng thái đều bằng tiếng Việt tự nhiên; không để lộ tên biến, enum thô (`ROLE_TUTOR`, `user_id`). |
| **II. Dữ liệu thật & Empty State** | **ĐẠT** | Cấu trúc dữ liệu mock phản ánh trung thực hồ sơ gia sư thực tế; khi không khớp kết quả, hiển thị màn hình rỗng chuẩn `MSG01` và nút xóa bộ lọc. |
| **III. Giữ kiến trúc & quy ước** | **ĐẠT** | Đặt file đúng cấu trúc `frontend/` (mục 9 của `FRONTEND_RULES.md`), không đưa React/Vue hay thư viện bên ngoài không cần thiết. |
| **IV. Thiết kế theo phân hệ** | **ĐẠT** | Sử dụng đúng bảng màu Aizome (`#006194`), Navy (`#0F2545`), Washi Background (`#FCFBF7`) và phông chữ `Plus Jakarta Sans`. |
| **V. Phạm vi & kiểm chứng** | **ĐẠT** | Bám sát đúng 4 User Stories của FE-04; kiểm thử trực quan trên server local cổng 3000. |

---

## Project Structure

```text
frontend/
├── home.html                      # Trang chủ KizunaTutor (liên kết bộ lọc nhanh sang tutors.html)
├── tutors.html                    # Trang Danh sách & Bộ lọc khám phá gia sư chi tiết (FE-04)
├── FRONTEND_RULES.md              # Bộ quy chuẩn phát triển frontend
├── css/
│   └── global.css                 # Hệ thống CSS Variables & Design Tokens dùng chung
└── js/
    ├── config/
    │   └── api.config.js          # Cấu hình API endpoints tập trung
    ├── data/
    │   └── tutors.mock.js         # Dữ liệu mẫu (Mock data) cho danh sách gia sư
    └── pages/
        └── tutors.js              # Controller xử lý tìm kiếm, lọc, sắp xếp, phân trang
```

---

## Implementation Sequence

1. **Chuẩn hóa Giao diện HTML (`frontend/tutors.html`)**:
   - Tích hợp mã giao diện Figma do người dùng cung cấp.
   - Thêm ID cho các input tìm kiếm, dropdown bộ lọc, vùng hiển thị thẻ gia sư (`#tutor-grid`), màn hình rỗng (`#empty-state`) và thanh phân trang.
   - Cập nhật liên kết điều hướng đồng bộ giữa `home.html` và `tutors.html`.

2. **Xây dựng Dữ liệu mẫu (`frontend/js/data/tutors.mock.js`)**:
   - Định nghĩa mảng `MOCK_TUTORS` với 6 hồ sơ gia sư tiêu biểu và đầy đủ thuộc tính: họ tên, avatar, trình độ JLPT (N1–N3), số năm kinh nghiệm, học phí, chuyên môn, hình thức dạy (online/offline), lịch rảnh và điểm đánh giá.

3. **Hiện thực Logic Điều khiển (`frontend/js/pages/tutors.js`)**:
   - Quản lý trạng thái tìm kiếm (State Management: keyword, level, goal, format, price, schedule, sortBy).
   - Hàm lọc đa tiêu chí (AND logic) kết hợp tìm kiếm từ khóa với debounce 300ms.
   - Hàm sắp xếp (Rating cao nhất, Học phí tăng/giảm, Kinh nghiệm).
   - Render động các thẻ gia sư và chip bộ lọc đang áp dụng kèm nút xóa nhanh từng chip.
   - Xử lý trạng thái rỗng (**Empty State**) khi không tìm thấy kết quả.

4. **Kiểm thử & Nghiệm thu trên Dev Server**:
   - Kiểm tra trực tiếp trên trình duyệt tại `http://localhost:3000/tutors.html`.

