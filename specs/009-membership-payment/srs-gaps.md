# FE-09 — Các điểm SRS còn thiếu hoặc mâu thuẫn (cần bổ sung vào SRS)

Danh sách cho người phụ trách SRS cập nhật `Report-3_Software Requirement Specification.docx`. Mỗi mục ghi đề xuất đang dùng tạm trong spec FE-09.

| Mã | Vị trí SRS | Vấn đề | Đề xuất đang dùng trong FE-09 | Cần làm |
| --- | --- | --- | --- | --- |
| G-01 | BF I.2.2 | Luồng "Create Post → Send for Review → Review Post → Publish / Notify Rejection" không có UC, màn hình, trường dữ liệu, thông điệp; ERD không có thực thể bài đăng | US6 + FR-022; thực thể "Bài đăng gia sư" (data-model mục 6): tiêu đề ≤ 150, nội dung 20–5.000 ký tự; trạng thái Chờ duyệt / Đã công bố / Bị từ chối; chỉ công khai khi gói còn hiệu lực | Thêm UC: *Create Tutor Post* (Tutor), *Update and Resubmit Tutor Post* (Tutor), *Review Tutor Post* (Academic Officer), *View Tutor Posts* (Learner/Guest); thêm thực thể vào ERD; duyệt bảng mới |
| G-02 | UC-104, NFR, ASM-05, OOS-09 | UC-104 và actor ghi VNPay; NFR/ASM/OOS ghi SePay/VietQR | Dùng VNPay | Sửa NFR 1, ASM-05, OOS-09 thành VNPay sandbox |
| G-03 | UC-97, UC-104 | Không quy định mua gói khi gói hiện tại còn hiệu lực | Cộng nối: gói mới bắt đầu ngày sau ngày hết hạn xa nhất | Thêm quy tắc vào UC-104 (Postconditions) hoặc Business Rules |
| G-04 | UC-97 AF-2, 5.3 Background Job | Không có job đổi gói sang "Expired" | Cập nhật khi đọc dữ liệu; trạng thái gia sư trở về "Chờ kích hoạt gói" khi hết gói | Ghi rõ trong UC-97 hoặc thêm JOB-04 nếu nhóm muốn chạy định kỳ |
| G-05 | V.2 System Messages | Không có thông điệp cho thanh toán, gói, cấu hình giá | MSG31–MSG37 (spec mục Thông điệp) | Thêm MSG31–MSG37 vào V.2 |
| G-06 | Entity, DB `tutor_membership_status_enum` | Enum có FREE/STANDARD/PREMIUM/EXPIRED nhưng gói chỉ khác số tháng | Có gói = PREMIUM, hết gói = EXPIRED, STANDARD không dùng | Nhóm xác nhận ý nghĩa enum |
| G-07 | 3.2 Entity PaymentTransaction | SRS gắn với `period_id` (học phí theo kỳ), trạng thái có Processing; DB gắn `plan_id`, có REFUNDED | Theo DB: PENDING/SUCCESS/FAILED | Sửa mô tả thực thể cho khớp DB và phạm vi chỉ thu phí hội viên |
| G-08 | 4.4 Permission Matrix | Cột là HR Manager/Interviewer/Candidate (mẫu cũ); không có dòng Membership, Payment | Phân quyền theo actor từng UC | Làm lại ma trận với 6 vai trò, thêm dòng Membership/Payment |
| G-09 | UC-99 PRE-3, UC-100 | Không nói rõ được tạo/ngừng bán gói, có được đổi số tháng | Được tạo, ngừng/mở bán; số tháng cố định sau khi tạo; không xóa | Bổ sung bước vào UC-99 |
| G-10 | UC-98 bước 6 | "Escalating the issue" không có đích chuyển tiếp | Chỉ ghi kết quả điều tra | Ghi rõ đích chuyển tiếp hoặc bỏ |
| G-11 | UC-104 | Không nói thời hạn thanh toán và xử lý giao dịch bỏ dở | Hạn 15 phút; giao dịch chỉ đổi trạng thái khi cổng báo kết quả | Bổ sung AF "Payment timeout" |
| G-12 | Mục lục II.9.8–9.16 | Các UC bài tập, tài liệu xếp nhầm vào chương 9 | Loại khỏi FE-09 | Chuyển về chương 6–8 |
| G-13 | ASM-02 | Ghi ReactJS, Java 21 | Theo repo: HTML/JS thuần, Java 17 | Sửa ASM-02 |
