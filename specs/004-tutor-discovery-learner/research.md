# Research: Filter Tìm kiếm, Đồng bộ URL (URL Synchronization) & Dynamic Routing

**Feature**: FE-04 — Tutor Discovery & Search (Learner)  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Bối cảnh & Vấn đề cần giải quyết

Hiện tại trang tìm kiếm gia sư (`frontend/pages/tutor-discovery/`):
- Chỉ đọc `window.location.search` một lần duy nhất khi nạp trang lần đầu.
- Khi người dùng tương tác với các bộ lọc (chọn JLPT chip, chọn dropdown, gõ từ khóa, sắp xếp, phân trang), thanh địa chỉ trình duyệt không hề thay đổi.
- **Hệ quả**:
  1. Người dùng nhấn `F5` (reload) thì toàn bộ lựa chọn biến mất, quay về mặc định.
  2. Người dùng không thể copy link chia sẻ kết quả tìm kiếm cho bạn bè (Shareable URL).
  3. Khi bấm nút "Quay lại" (Back) hoặc "Tiến lên" (Forward) trên trình duyệt, trang không phản hồi theo lịch sử tìm kiếm.
  4. Đường dẫn hiện tại dài dòng (`/pages/tutor-discovery/index.html`) thay vì định tuyến ngắn gọn, thân thiện (`/tutors`).

---

## 2. Quyết định kỹ thuật (Technical Decisions)

### Quyết định 1: Cơ chế đồng bộ URL hai chiều (Two-Way URL Synchronization)

- **Nguyên lý hoạt động**:
  ```
  [URL Search Params]
         ▲ │
         │ │ (Khởi động trang hoặc bấm Back/Forward: URL -> State -> DOM)
         │ ▼
  [Trạng thái ứng dụng: state] ──(Render/API)──> [Backend API & Danh sách thẻ]
         ▲
         │ (Người dùng click/chọn/gõ: DOM -> State -> URL)
  [Giao diện điều khiển: Inputs, Dropdowns, Pills, Chips]
  ```
- **Thực thi**:
  - Hàm `updateUrlFromState(shouldPushHistory = true)`:
    - Chuyển đổi đối tượng `state` thành `URLSearchParams`.
    - **Làm sạch URL (Clean URL)**: Loại bỏ các giá trị mặc định (`jlpt: 'all'`, `sortBy: 'rating'`, `page: 1`, chuỗi rỗng) để URL luôn ngắn gọn, tinh tế.
    - Dùng `window.history.pushState({}, '', newUrl)` khi người dùng chủ động click chọn bộ lọc hoặc đổi trang.
    - Dùng `window.history.replaceState({}, '', newUrl)` khi người dùng đang gõ tìm kiếm (tránh spam lịch sử duyệt web cho mỗi ký tự).
  - Hàm `syncStateFromUrl()`:
    - Bóc tách `window.location.search` thành các trường trong `state`.
    - Gán lại giá trị cho các input, select, active classes trên pill JLPT, active filter chips.
  - Lắng nghe sự kiện trình duyệt:
    ```javascript
    window.addEventListener('popstate', () => {
      syncStateFromUrl();
      loadTutors(false); // false: không ghi đè lại URL
    });
    ```

### Quyết định 2: Chuẩn hóa tham số Query Parameters

| Tiêu chí | Tên tham số URL | Ví dụ giá trị | Giá trị mặc định (bỏ khỏi URL) |
| :--- | :--- | :--- | :--- |
| Từ khóa tìm kiếm | `q` | `q=kenji` | Rỗng |
| Trình độ JLPT | `jlpt` | `jlpt=N2` | `all` hoặc không có |
| Mục tiêu học tập | `goal` | `goal=kaiwa` | Rỗng |
| Hình thức học | `format` | `format=online` | Rỗng |
| Mức học phí | `price` | `price=150-250` | Rỗng |
| Lịch rảnh | `schedule` | `schedule=weekend`| Rỗng |
| Sắp xếp | `sort` | `sort=price_asc` | `rating` |
| Trang phân trang | `page` | `page=2` | `1` |

Ví dụ URL thực tế sau khi áp dụng:
`http://localhost:3000/tutors?jlpt=N2&goal=kaiwa&format=online&sort=price_asc&page=1`

### Quyết định 3: Kết hợp Dynamic Routing (Định tuyến động)

1. **Route trang tìm kiếm gọn gàng**:
   - URL thân thiện: `/tutors` thay vì `/pages/tutor-discovery/index.html`.
   - Cấu hình trong `frontend/server.js`: Ánh xạ `/tutors` và `/tutors/` về file tĩnh `frontend/pages/tutor-discovery/index.html`.
2. **Route xem chi tiết gia sư theo mã tự tăng**:
   - URL: `/gs/:tutorCode` (ví dụ `/gs/10001`).
   - Mỗi thẻ gia sư trong kết quả tìm kiếm kết xuất liên kết trực tiếp `<a href="/gs/${tutor.tutorCode}">`.
3. **Deep Linking từ Trang chủ (Home Page)**:
   - Các nút bấm nhanh trên Home (ví dụ: Pill JLPT "N2", Thẻ "Giao tiếp Kaiwa") điều hướng thẳng sang `/tutors?jlpt=N2` hoặc `/tutors?goal=kaiwa`.
   - Trang `/tutors` ngay lập tức tự động áp dụng bộ lọc và tải đúng danh sách mà không cần xử lý thủ công.

---

## 3. Rủi ro & Giải pháp phòng ngừa

1. **Rủi ro vòng lặp vô tận (Infinite Update Loop)**:
   - *Nguy cơ*: `State đổi` ➔ `Cập nhật URL` ➔ `popstate kích hoạt` ➔ `State đổi`...
   - *Giải pháp*: `pushState` / `replaceState` không kích hoạt sự kiện `popstate`. Sự kiện `popstate` chỉ kích hoạt khi người dùng bấm nút Back/Forward của trình duyệt. Cờ `shouldUpdateUrl` được truyền vào hàm gọi API để phân định rõ ràng.
2. **Rủi ro độ trễ khi gõ từ khóa (Debounce conflict)**:
   - *Nguy cơ*: URL thay đổi liên tục từng ký tự làm trình duyệt chậm hoặc kẹt lịch sử.
   - *Giải pháp*: Sử dụng Debounce 300ms kết hợp `history.replaceState` riêng cho ô nhập từ khóa.
3. **Rủi ro đường dẫn tĩnh trên các route ảo**:
   - *Nguy cơ*: Giống sự cố `/gs/10001` trước đây, các script nạp dạng relative `./tutor-discovery.js` sẽ bị 404 khi truy cập qua route ngắn.
   - *Giải pháp*: Toàn bộ thẻ script và CSS trong HTML sử dụng đường dẫn tuyệt đối bắt đầu bằng dấu `/` (ví dụ: `/pages/tutor-discovery/tutor-discovery.js`, `/styles/global.css`).

