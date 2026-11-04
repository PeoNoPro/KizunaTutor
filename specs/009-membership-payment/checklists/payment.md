# Payment Requirements Quality Checklist: FE-09

**Purpose**: Kiểm tra chất lượng yêu cầu thanh toán và hội viên (viết đủ, rõ, nhất quán, đo được) trước khi cài đặt
**Created**: 2026-11-04
**Feature**: [spec.md](../spec.md)

## Đầy đủ

- [x] CHK001 Có yêu cầu cho mọi kết quả từ cổng: thành công, thất bại/hủy, chưa xác nhận? [Spec FR-007, FR-009, MSG33]
- [x] CHK002 Có yêu cầu cho kết quả lặp và hai đường xử lý đồng thời (IPN + tra cứu)? [Spec FR-006, US1-5]
- [x] CHK003 Có yêu cầu cho trường hợp đã thu tiền nhưng kích hoạt lỗi (UC-104 AF-1)? [Spec FR-007, US1-6]
- [x] CHK004 Có quy định thời hạn thanh toán và cách xử lý giao dịch bỏ dở? [Spec FR-003, Edge Cases; srs-gaps G-11]
- [x] CHK005 Có quy định mua gói khi đang còn gói (cộng nối)? [Spec FR-008, Clarifications]
- [x] CHK006 Có quy định khi nào gói chuyển "Đã hết hạn" và ảnh hưởng tới trạng thái gia sư? [Spec FR-013]
- [x] CHK007 Có yêu cầu ghi nhật ký mọi lần trao đổi với cổng? [Spec FR-011]
- [ ] CHK008 Có yêu cầu cho bài đăng gia sư (BF I.2.2) đủ để cài đặt? [Gap — chờ UC, srs-gaps G-01]

## Rõ ràng

- [x] CHK009 Ngày hết hạn được định nghĩa bằng công thức đo được? [Spec FR-008]
- [x] CHK010 Giới hạn giá, số tháng, số quyền lợi có con số cụ thể? [Spec FR-014]
- [x] CHK011 Thứ tự kiểm tra kết quả IPN và mã phản hồi từng trường hợp được nêu rõ? [Spec FR-004, Contract B1]
- [x] CHK012 "Giá mới áp dụng" được định nghĩa theo thời điểm tạo giao dịch? [Spec FR-015]
- [x] CHK013 Ý nghĩa `membership_status` (FREE/STANDARD/PREMIUM/EXPIRED) đã được nhóm xác nhận? [Assumption A-4, srs-gaps G-06]

## Nhất quán

- [x] CHK014 Trạng thái giao dịch trong spec, data-model, contract dùng cùng tập giá trị? [A-3, data-model §2, Contract]
- [x] CHK015 Cổng thanh toán thống nhất là VNPay trong mọi tài liệu FE-09? [Clarifications; srs-gaps G-02 ghi nhận mâu thuẫn SRS]
- [x] CHK016 Vai trò của từng UC khớp giữa spec, contract và SecurityConfig dự kiến? [Spec Phạm vi, Contract A–D, Tasks T008]

## Bảo mật và dữ liệu

- [x] CHK017 Có yêu cầu bí mật cổng chỉ từ biến môi trường và không lộ trong log? [Spec FR-011, FR-025, SC-006]
- [x] CHK018 Gia sư chỉ truy cập giao dịch của chính mình, kể cả ở trang kết quả? [Spec FR-023, Contract A5]
- [x] CHK019 Ghi chú điều tra của quản trị viên bị cấm hiển thị cho gia sư? [Spec FR-021, GB-02]
- [x] CHK020 Kết quả có chữ ký sai hoặc số tiền lệch bị từ chối và đo được? [Spec SC-003]

## Đo được

- [x] CHK021 Tiêu chí kích hoạt đúng một lần có điều kiện thử cụ thể? [Spec SC-002]
- [~] CHK022 Hiệu năng SC-005 kiểm chứng được? [Chỉ quan sát thủ công — không có DB kiểm thử để đo tải; ghi trong T047]

## Notes

- CHK008, CHK013 chưa đạt do phụ thuộc quyết định của nhóm; không chặn US1–US5.
- CHK022 đạt một phần, ghi nhận giới hạn môi trường.
