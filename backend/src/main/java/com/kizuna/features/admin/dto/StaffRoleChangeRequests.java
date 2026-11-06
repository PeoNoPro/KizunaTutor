package com.kizuna.features.admin.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Lý do và ghi chú giới hạn để nội dung ghép vào nhật ký không vượt cột 2000 ký tự. */
public final class StaffRoleChangeRequests {
    private StaffRoleChangeRequests() { }
    public record Move(
        @NotNull(message="Vui lòng tải lại dữ liệu.") OffsetDateTime expectedUpdatedAt,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=1500,message="Lý do tối đa 1500 ký tự.") String reason) { }
    public record Propose(
        @NotNull(message="Vui lòng chọn nhân viên.") UUID targetUserId,
        @NotNull(message="Vui lòng tải lại dữ liệu.") OffsetDateTime expectedUpdatedAt,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=1500,message="Lý do tối đa 1500 ký tự.") String reason) { }
    public record Decide(@Size(max=300,message="Ghi chú tối đa 300 ký tự.") String note) { }
}
