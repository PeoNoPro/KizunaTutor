package com.kizuna.features.auth.dto;


import com.kizuna.core.entity.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tiếp nhận thông tin yêu cầu đăng nhập.
 * Hỗ trợ đăng nhập bằng Email hoặc Username.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email hoặc tên đăng nhập không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
