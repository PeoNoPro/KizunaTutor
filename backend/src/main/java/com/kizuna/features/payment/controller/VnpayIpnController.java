package com.kizuna.features.payment.controller;

import com.kizuna.features.payment.service.VnpayIpnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IPN của VNPay (API-EXT-02). Công khai, xác thực bằng chữ ký HMAC-SHA512.
 * Luôn trả HTTP 200 với {"RspCode","Message"} theo định dạng bắt buộc của cổng (ngoại lệ có lý do với BACKEND_RULES 6.2).
 */
@RestController
public class VnpayIpnController {

    private final VnpayIpnService ipnService;

    public VnpayIpnController(VnpayIpnService ipnService) {
        this.ipnService = ipnService;
    }

    @GetMapping("/api/payments/vnpay/ipn")
    public ResponseEntity<VnpayIpnService.IpnReply> ipn(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(ipnService.handle(params));
    }
}
