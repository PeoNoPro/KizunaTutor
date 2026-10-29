package com.kizuna.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO khi Quản trị viên khởi tạo tài khoản nhân sự / người dùng mới.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Địa chỉ email không đúng định dạng")
    private String email;

    private String phone;

    @NotBlank(message = "Vai trò không được để trống")
    private String role;

    @NotBlank(message = "Mật khẩu khởi tạo không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String temporaryPassword;

    @Builder.Default
    private String status = "ACTIVE";
}
