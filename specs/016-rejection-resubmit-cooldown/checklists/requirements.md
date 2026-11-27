# Specification Quality Checklist: FE-16 — Nộp lại sau 7 ngày bị từ chối

**Purpose**: Kiểm tra chất lượng spec trước khi lập kế hoạch
**Created**: 2026-11-26
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Không lẫn chi tiết cài đặt vào yêu cầu nghiệp vụ (tên trường API chỉ nêu ở FR để khớp hợp đồng)
- [x] Tập trung vào giá trị cho gia sư
- [x] Đủ các mục bắt buộc

## Requirement Completeness

- [x] Không còn mục [NEEDS CLARIFICATION] (đã chốt phiên 2026-11-26)
- [x] Yêu cầu kiểm thử được, không mơ hồ
- [x] Tiêu chí thành công đo được
- [x] Kịch bản chấp nhận đầy đủ cho US1–US3
- [x] Đã nêu trường hợp biên (dữ liệu cũ thiếu thời điểm, đúng mốc 7 ngày, gói hết hạn, từ chối lặp lại)
- [x] Phạm vi rõ ràng (chỉ hồ sơ bị từ chối và bài đầu tiên bị từ chối)
- [x] Đã nêu giả định (7 ngày cố định, không migration)

## Feature Readiness

- [x] Mỗi FR có kịch bản chấp nhận tương ứng
- [x] Giữ nguyên chức năng hiện có (bản cập nhật bị từ chối, yêu cầu cập nhật hồ sơ bị từ chối)
- [x] Không chạm mã của thành viên khác

## Notes

- Thay quy tắc GBR-41 của SRS ("từ chối là cuối cùng"); người phụ trách tự cập nhật SRS theo plan.md.
