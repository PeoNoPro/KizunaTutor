# Research: FE-16 — Nộp lại sau 3 ngày bị từ chối

## R1. Lấy thời điểm từ chối hồ sơ từ đâu?

- **Quyết định**: dòng lịch sử `tutor_verification_histories` hành động `REJECT` mới nhất của gia sư (`created_at`), qua `TutorVerificationHistoryRepository.findFirstByTutor_TutorIdAndActionInOrderByCreatedAtDesc` (đã có).
- **Lý do**: hồ sơ ở trạng thái `REJECTED` thì không thể có yêu cầu cập nhật hồ sơ (chỉ hồ sơ `APPROVED` mới gửi được), nên dòng `REJECT` mới nhất chắc chắn là lần từ chối hồ sơ. Không cần thêm cột.
- **Phương án loại**: thêm cột `rejected_at` vào `tutors` — bảng thuộc `core/` (cần hỏi người phụ trách, cần migration) trong khi dữ liệu đã có sẵn trong lịch sử.
- **Dữ liệu cũ không có dòng lịch sử**: coi như hết thời gian chờ (cho nộp lại ngay) — không có mốc để tính, và khóa vĩnh viễn là đúng điều người phụ trách muốn bỏ.

## R2. Lấy thời điểm từ chối bài đăng từ đâu?

- **Quyết định**: `tutor_posts.reviewed_at` của bài mới nhất (bài `REJECTED`), đã được `markReviewed` ghi khi từ chối.
- `reviewed_at` rỗng (dữ liệu cũ): coi như hết thời gian chờ.

## R3. Đặt quy tắc ở đâu?

- **Quyết định**: hằng `RESUBMIT_COOLDOWN = Duration.ofDays(3)` và hàm tính đặt riêng trong mỗi feature: `VerificationStateMachine` (verification) và `TutorPostService` (tutor_post).
- **Lý do**: luật 3 AGENTS.md — feature không gọi thẳng nội bộ của nhau; hai feature hiện độc lập. Lặp một hằng số chấp nhận được hơn tạo phụ thuộc chéo.
- Hàm tính nhận tham số `now` để test được mốc biên mà không cần giả lập đồng hồ.

## R4. Giữ chữ ký hàm cũ

- `VerificationStateMachine.isEditable(status)` / `requireEditable(tutor)` là hàm tĩnh, đang được test và mã khác dùng. **Quyết định**: giữ nguyên (vẫn coi `REJECTED` là khóa) và thêm overload có tham số thời điểm được nộp lại; chỗ gọi trong service / assembler chuyển sang overload mới. Không đổi constructor nào (test của nhóm dựng tay `VerificationViewAssembler`, `VerificationStateMachine`).

## R5. Giấy tờ "Không được chấp nhận" khi nộp lại

- **Quyết định**: `VerificationViewAssembler.check` bỏ qua giấy tờ `REJECTED`.
- **Lý do**: khi duyệt, chỉ giấy tờ `PENDING` được chuyển `VERIFIED`; nếu cho nộp lại bằng giấy tờ đã bị từ chối thì hồ sơ được duyệt mà không có giấy tờ đã xác minh (trái GBR-45). Ngoài luồng nộp lại, giấy tờ `REJECTED` chỉ xuất hiện ở hồ sơ đã duyệt (từ chối yêu cầu cập nhật) — nơi `check` không được dùng để nộp, nên không ảnh hưởng luồng khác.

## R6. Giao diện bài đăng

- `isUpdateMode` (frontend) hiện coi mọi bài `REJECTED` là "chế độ cập nhật" (đúng vì trước đây chỉ bài có bản đã duyệt mới sửa được). **Quyết định**: chỉ coi là cập nhật khi có `post.published`; bài đầu tiên bị từ chối đã hết chờ thì mở biểu mẫu ngay với nút "Gửi lại".

## R7. Định dạng thời điểm trong lời nhắn backend

- `HH:mm 'ngày' dd/MM/yyyy`, múi giờ `Asia/Ho_Chi_Minh`, ví dụ "14:30 ngày 03/12/2026".
