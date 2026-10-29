package com.kizuna.dto.tutor;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO yêu cầu khởi tạo giao dịch thanh toán gói thành viên.
 * Phục vụ UC 73 (Thanh toán qua cổng VNPay).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotNull(message = "Vui lòng chọn gói thành viên")
    private UUID planId;

    @Builder.Default
    private String paymentMethod = "VNPAY";
}
