package com.kizuna.features.admin.dto;


import com.kizuna.core.entity.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO khi Admin bật/tắt trạng thái hoạt động (Khóa / Mở khóa).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {

    @NotBlank(message = "Trạng thái tài khoản không được để trống")
    private String status;
}
