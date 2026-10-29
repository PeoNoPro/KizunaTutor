package com.kizuna.features.admin.controller;


import com.kizuna.core.entity.*;
import com.kizuna.features.auth.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller kiểm tra chẩn đoán kết nối SMTP Gmail độc lập:
 * - Chạy đồng bộ (Synchronous, không dùng @Async) để bắt trọn vẹn lỗi SMTP
 * - Trả về mã lỗi, thông điệp Exception và hướng dẫn khắc phục chi tiết ngay trên màn hình.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDiagnosticEmailController {

    private final IEmailService emailService;

    @Value("${spring.mail.username:kizunatutor@gmail.com}")
    private String configuredSender;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String configuredHost;

    @Value("${spring.mail.port:587}")
    private int configuredPort;

    /**
     * API Test Gửi Thư Trực Tiếp (Diagnostic Health-Check API)
     * Hỗ trợ cả POST và GET để test nhanh trên trình duyệt.
     */
    @RequestMapping(value = "/test-email", method = {RequestMethod.POST, RequestMethod.GET})
    public ResponseEntity<Map<String, Object>> testSmtpConnection(
            @RequestParam(defaultValue = "vuh2623832@gmail.com") String to) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("smtpHost", configuredHost);
        response.put("smtpPort", configuredPort);
        response.put("sender", configuredSender);
        response.put("recipient", to);

        try {
            log.info("[ADMIN DIAGNOSTIC] Đang kích hoạt gửi thử nghiệm email đồng bộ tới [{}]...", to);
            emailService.sendDirectDiagnosticTestEmail(to);

            response.put("success", true);
            response.put("status", "SUCCESS");
            response.put("message", "Email kiểm tra đã được gửi và bàn giao thành công cho SMTP Server của Google!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[ADMIN DIAGNOSTIC] Thử nghiệm gửi email thất bại: ", e);

            response.put("success", false);
            response.put("status", "ERROR");
            response.put("exceptionClass", e.getClass().getName());
            response.put("errorMessage", e.getMessage());

            // Trích xuất Root Cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            response.put("rootCauseClass", rootCause.getClass().getName());
            response.put("rootCauseMessage", rootCause.getMessage());

            // Phân tích nguyên nhân và đưa ra giải pháp cụ thể
            String messageStr = e.getMessage() != null ? e.getMessage() : "";
            String rootMsg = rootCause.getMessage() != null ? rootCause.getMessage() : "";

            if (messageStr.contains("534-5.7.9") || rootMsg.contains("534-5.7.9") || rootMsg.contains("Application-specific password required") || rootMsg.contains("InvalidSecondFactor")) {
                response.put("diagnosis", "LỖI BẮT BUỘC DÙNG MẬT KHẨU ỨNG DỤNG GOOGLE (Google App Password Required - Error 534-5.7.9)");
                response.put("solution", "Tài khoản Google 'kizunatutor@gmail.com' đã bật Xác thực 2 bước (2FA). Google TUYỆT ĐỐI KHÔNG CHO PHÉP dùng mật khẩu tài khoản thông thường (như 'Hoang123@') để gửi thư qua cổng SMTP. Bạn hãy truy cập https://myaccount.google.com/apppasswords, tạo một 'Mật khẩu ứng dụng' (16 ký tự viết liền không dấu cách, ví dụ: 'abcd efgh ijkl mnop'), sau đó điền mật khẩu 16 ký tự đó vào spring.mail.password hoặc biến môi trường MAIL_APP_PASSWORD.");
            } else if (messageStr.contains("535-5.7.8") || rootMsg.contains("535-5.7.8") || rootMsg.contains("BadCredentials") || messageStr.contains("AuthenticationFailedException")) {
                response.put("diagnosis", "LỖI XÁC THỰC MẬT KHẨU GMAIL (Authentication Failed 535-5.7.8)");
                response.put("solution", "Mật khẩu gửi thư không chính xác. Google không cho phép dùng mật khẩu tài khoản thường trên SMTP. Bạn cần tạo 'Mật khẩu ứng dụng' (Google App Password gồm 16 ký tự) tại https://myaccount.google.com/apppasswords (khi đã bật 2FA), sau đó cập nhật vào biến môi trường MAIL_APP_PASSWORD hoặc spring.mail.password.");
            } else if (messageStr.contains("ConnectException") || messageStr.contains("timeout") || rootMsg.contains("timeout")) {
                response.put("diagnosis", "LỖI KẾT NỐI MẠNG ĐẾN SMTP.GMAIL.COM:587 (Connection Timeout)");
                response.put("solution", "Máy chủ không thể mở cổng 587 đến smtp.gmail.com. Hãy kiểm tra tường lửa (Firewall), proxy hoặc cấu hình mạng.");
            } else {
                response.put("diagnosis", "LỖI GỬI EMAIL KHÁC");
                response.put("solution", "Kiểm tra log console chi tiết để xác định nguyên nhân.");
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
