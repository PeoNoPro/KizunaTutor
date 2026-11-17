-- Xóa 8 bảng không còn được code sử dụng (theo chốt của thầy: bỏ sự cố kỹ thuật, bỏ luồng đề xuất đổi vai trò,
-- bỏ xếp ca trực CS). KHÔNG CHẠY TỰ ĐỘNG. Người phụ trách chạy tay trên DB sau khi sao lưu.
-- Điều kiện: đã deploy bản code không còn đọc/ghi các bảng này.
-- Bảng con xóa trước, bảng cha xóa sau. Không dùng CASCADE để lỗi phụ thuộc lộ ra thay vì xóa nhầm đối tượng khác.
BEGIN;

-- Sự cố kỹ thuật
DROP TABLE IF EXISTS technical_incident_events;
DROP TABLE IF EXISTS incident_components;
DROP TABLE IF EXISTS technical_incidents;

-- Đề xuất đổi vai trò
DROP TABLE IF EXISTS staff_role_change_requests;

-- Ca trực của nhân viên hỗ trợ
DROP TABLE IF EXISTS support_shift_events;   -- có trigger append-only đi kèm, bị xóa cùng bảng
DROP FUNCTION IF EXISTS support_shift_events_append_only();
DROP TABLE IF EXISTS support_shifts;

-- Bảng đánh dấu phiên bản của file seed (không còn file seed nào đọc chúng)
DROP TABLE IF EXISTS fe10_schema_versions;
DROP TABLE IF EXISTS operations_schema_versions;

COMMIT;
