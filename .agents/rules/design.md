

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
