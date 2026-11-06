package com.kizuna.features.admin.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public final class StaffAccountRequests {
    private StaffAccountRequests() { }
    public record Create(
        @NotBlank(message="Vui lòng nhập họ tên.") @Size(max=100,message="Họ tên tối đa 100 ký tự.") String fullName,
        @NotBlank(message="Vui lòng nhập email.") @Email(message="Email chưa hợp lệ.") @Size(max=254,message="Email quá dài.") String email,
        @Size(max=20,message="Số điện thoại tối đa 20 ký tự.") String phone,
        @NotNull(message="Vui lòng chọn vai trò.") Integer roleId,
        @Size(max=1500,message="Căn cứ bổ nhiệm tối đa 1500 ký tự.") String appointmentReason) { }
    public record Update(
        @NotBlank(message="Vui lòng nhập họ tên.") @Size(max=100,message="Họ tên tối đa 100 ký tự.") String fullName,
        @NotBlank(message="Vui lòng nhập email.") @Email(message="Email chưa hợp lệ.") @Size(max=254,message="Email quá dài.") String email,
        @Size(max=20,message="Số điện thoại tối đa 20 ký tự.") String phone,
        @NotNull(message="Vui lòng tải lại dữ liệu.") OffsetDateTime expectedUpdatedAt,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }
    public record Resend(@NotNull(message="Vui lòng tải lại dữ liệu.") OffsetDateTime expectedUpdatedAt) { }
    public record Command(String status,
        @NotNull(message="Vui lòng tải lại dữ liệu.") OffsetDateTime expectedUpdatedAt,
        @NotBlank(message="Vui lòng nhập lý do.") @Size(max=2000,message="Lý do tối đa 2000 ký tự.") String reason) { }
}
