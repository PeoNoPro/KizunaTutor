package com.kizuna.features.admin.dto;


import com.kizuna.core.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO khi Admin cập nhật thông tin và thay đổi vai trò (Role).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String fullName;

    private String phone;

    @NotBlank(message = "Vai trò không được để trống")
    private String role;

    private String status;
}
