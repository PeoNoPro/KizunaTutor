# Dokkai Guide Modal Overlay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển đổi khung hướng dẫn 3 bước học nhanh (#readingGuideModal) từ khối chiếm diện tích trang thành cửa sổ Modal Overlay nổi chính giữa màn hình với nền mờ, loại bỏ hoàn toàn việc xô lệch bố cục trang khi bấm chữ "i".

**Architecture:** Đưa thẻ `#readingGuideModal` ra ngoài flow của `#readingInputScreen`, bọc trong container `fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-xs hidden`. Thêm các cơ chế đóng linh hoạt: bấm `✕`, bấm nút "Đã hiểu", click ra ngoài nền tối (click outside), và nhấn phím `Esc`.

**Tech Stack:** HTML5, Tailwind CSS, Vanilla JavaScript (DOM event listeners).

**Spec:** Short design approved during brainstorming on 2026-11-24.

## Global Constraints
- Không làm xô lệch bất kỳ phần tử nào của trang khi mở/đóng modal (0% layout shift).
- Hỗ trợ tốt trên cả màn hình Desktop và Mobile (`max-w-lg w-full`).
- Tương thích với SPA Router của KizunaTutor (`kizuna:page-mounted`, detached DOM handling).

## Review Focus
- Bấm vào chữ "i" phải mở modal ngay lập tức mà không cuộn trang hay giật khung.
- Bấm vào vùng xám mờ bên ngoài hộp thoại (backdrop) phải đóng modal.
- Bấm vào nút `✕` hoặc nút "Đã hiểu, bắt đầu ngay" phải đóng modal.
- Nhấn phím `Esc` khi modal đang mở phải đóng modal.
- Không để sự kiện click bên trong hộp thoại (dialog box) bị nổi bọt (stop propagation) làm đóng nhầm modal.

---

### Task 1: Refactor Guide Modal HTML in `index.html`

**Files:**
- Modify: `frontend/pages/ai-reading/index.html`

**Interfaces:**
- Produces: `#readingGuideModal` with fixed overlay structure, dialog card `#readingGuideDialog`, close button `#btnCloseGuide`, and submit button `#btnAckGuide`.

- [x] **Step 1: Update `#readingGuideModal` markup in `frontend/pages/ai-reading/index.html`**
Chuyển `#readingGuideModal` thành `fixed inset-0 z-50 bg-slate-900/50 backdrop-blur-xs hidden flex items-center justify-center p-4`, bên trong chứa card `max-w-lg w-full bg-white rounded-2xl border-2 border-[#0F172A] shadow-[4px_4px_0px_0px_#0F172A]` với đầy đủ Header, 3 Bước học, nút `✕` và Footer nút "Đã hiểu, bắt đầu ngay".

- [x] **Step 2: Verify HTML markup consistency**
Kiểm tra đảm bảo các ID `#readingGuideModal`, `#btnToggleGuide`, `#btnCloseGuide`, `#btnAckGuide` không bị trùng lặp và đặt ở vị trí phù hợp trong `index.html`.

---

### Task 2: Update Guide Modal Logic & Keyboard Event in `ai-reading.js`

**Files:**
- Modify: `frontend/pages/ai-reading/ai-reading.js`

**Interfaces:**
- Consumes: `#btnToggleGuide`, `#readingGuideModal`, `#btnCloseGuide`, `#btnAckGuide`.
- Produces: Click listeners for opening/closing, click-outside dismissal, and Escape key listener.

- [x] **Step 1: Implement open/close modal functions and backdrop click in `ai-reading.js`**
Trong `setupEventListeners()`:
- Bấm `#btnToggleGuide`: Bỏ class `hidden` khỏi `#readingGuideModal`.
- Bấm `#btnCloseGuide` hoặc `#btnAckGuide`: Thêm class `hidden` vào `#readingGuideModal`.
- Bấm ra vùng backdrop (e.target === readingGuideModalEl): Đóng modal.
- Lắng nghe sự kiện `keydown` phím `Escape`: Nếu modal đang mở thì đóng lại.

- [x] **Step 2: Verify interactive behavior manually and check console for errors**
Kiểm tra mở đóng modal, phím Esc, click outside, đảm bảo không có lỗi script.
