package com.kizuna.features.auth.service.impl;

import com.kizuna.features.auth.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:kizunatutor@gmail.com}")
    private String senderEmail;

    @Override
    @Async
    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otp) {
        log.info("[OTP DISPATCH] Mã OTP [{}] gửi tới: [{}] ({})", otp, toEmail, fullName);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật");
            helper.setTo(toEmail);
            helper.setSubject("[KizunaTutor] Mã xác thực đăng ký tài khoản của bạn: " + otp);

            String htmlContent = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;'>"
                    + "<h2 style='color: #0F2545;'>Chào mừng bạn đến với KizunaTutor</h2>"
                    + "<p>Xin chào <strong>" + fullName + "</strong>,</p>"
                    + "<p>Mã xác thực OTP gồm 6 chữ số để kích hoạt tài khoản học tập của bạn là:</p>"
                    + "<div style='font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #006194; margin: 20px 0;'>" + otp + "</div>"
                    + "<p style='color: #68737D; font-size: 13px;'>Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã cho bất kỳ ai.</p>"
                    + "</div>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email OTP đã gửi thành công tới: {}", toEmail);

        } catch (Exception e) {
            log.warn("Không thể gửi email OTP qua SMTP (vui lòng kiểm tra cấu hình Mail): {}", e.getMessage());
            log.info("[DEV FALLBACK] Trong môi trường phát triển (Offline/Sandbox), mã OTP có thể dùng trực tiếp là: {}", otp);
        }
    }
}
