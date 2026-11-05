package com.kizuna.features.payment.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Email xác nhận kích hoạt gói hội viên (FR-010, actor Google Mail Service).
 * Gửi sau khi transaction commit trên luồng nền riêng; lỗi gửi chỉ ghi log, không hoàn tác việc kích hoạt.
 */
@Slf4j
@Service
public class PaymentNotificationService {

    private static final String SENDER_NAME = "KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật";

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "membership-mail");
        thread.setDaemon(true);
        return thread;
    });

    public PaymentNotificationService(JavaMailSender mailSender,
                                      @Value("${spring.mail.username:kizunatutor@gmail.com}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void membershipActivatedAfterCommit(String toEmail, String fullName, String planName,
                                               LocalDate startDate, LocalDate endDate, long amount) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        String html = "<p>Xin chào " + escape(fullName == null ? "bạn" : fullName) + ",</p>"
                + "<p>Thanh toán gói hội viên của bạn trên KizunaTutor đã thành công.</p>"
                + "<ul>"
                + "<li>Gói: <strong>" + escape(planName) + "</strong></li>"
                + "<li>Hiệu lực: " + startDate.format(MembershipLabels.DATE_VN) + " – " + endDate.format(MembershipLabels.DATE_VN) + "</li>"
                + "<li>Số tiền: " + formatMoney(amount) + "</li>"
                + "</ul>"
                + "<p>Bạn có thể xem gói và lịch sử thanh toán tại mục \"Gói hội viên\" trong khu vực gia sư.</p>"
                + "<p>Trân trọng,<br>Đội ngũ KizunaTutor</p>";
        Runnable send = () -> executor.execute(() -> send(toEmail, "Kích hoạt gói hội viên thành công", html));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

    static String formatMoney(long amount) {
        return String.format("%,d", amount).replace(',', '.') + " đ";
    }

    private void send(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email kích hoạt gói hội viên tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email kích hoạt gói hội viên tới {} thất bại; gói vẫn được kích hoạt", toEmail, e);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
