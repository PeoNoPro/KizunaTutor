package com.kizuna.features.payment.controller;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Địa chỉ IP người dùng gửi kèm yêu cầu thanh toán VNPay (vnp_IpAddr).
 */
final class ClientIp {

    private ClientIp() {
    }

    static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
