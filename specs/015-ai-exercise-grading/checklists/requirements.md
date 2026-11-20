# Specification Quality Checklist: FE-15 — AI gợi ý chấm câu tự luận

**Purpose**: Kiểm tra chất lượng spec trước khi lập kế hoạch
**Created**: 2026-11-19
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] Không lẫn chi tiết cài đặt vào yêu cầu nghiệp vụ (tên bảng / API chỉ nêu ở FR để khớp hợp đồng)
- [x] Tập trung vào giá trị cho gia sư và học viên
- [x] Đủ các mục bắt buộc

## Requirement Completeness

- [x] Không còn mục [NEEDS CLARIFICATION] (đã làm rõ phiên 2026-11-19)
- [x] Yêu cầu kiểm thử được, không mơ hồ
- [x] Tiêu chí thành công đo được
- [x] Kịch bản chấp nhận đầy đủ cho US1–US3
- [x] Đã nêu trường hợp biên (để trống, AI lỗi, nhiều máy, chưa migration, kẹt xử lý)
- [x] Phạm vi rõ ràng (chỉ câu tự luận không đáp án của bài làm trực tiếp)
- [x] Đã nêu giả định và phụ thuộc (cấu hình AI của Admin, FE-07, FE-12)

## Feature Readiness

- [x] Mỗi FR có kịch bản chấp nhận tương ứng
- [x] Tôn trọng luật 8: AI chỉ gợi ý, gia sư xác nhận điểm
- [x] Không sửa luồng nghiệp vụ của FE-07

## Notes

- SRS không có use case riêng cho AI chấm bài; nguồn là UC-67 + luật 8 AGENTS.md + BACKEND_RULES mục 8 + yêu cầu người phụ trách.
