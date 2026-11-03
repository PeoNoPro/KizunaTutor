package com.kizuna.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Địa chỉ email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
