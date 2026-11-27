package com.kizuna.features.verification.service;

import jakarta.annotation.PreDestroy;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Email kết quả thẩm định gửi gia sư (UC-26, UC-28, BF I.2.1).
 * Gửi sau khi transaction commit, trên luồng nền riêng; lỗi gửi chỉ ghi log và không hoàn tác quyết định (FR-022).
 * Không bao giờ chứa ghi chú nội bộ của cán bộ (GB-02).
 */
@Slf4j
@Service
public class VerificationNotificationService {

    private static final String SENDER_NAME = "KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật";

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "verification-mail");
        thread.setDaemon(true);
        return thread;
    });

    public VerificationNotificationService(JavaMailSender mailSender,
                                           @Value("${spring.mail.username:kizunatutor@gmail.com}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void approvedAfterCommit(String toEmail, String fullName) {
        afterCommit(toEmail, "Hồ sơ gia sư của bạn đã được phê duyệt",
                greeting(fullName)
                        + "<p>Chúc mừng bạn! Hồ sơ xác minh gia sư trên KizunaTutor đã được cán bộ học vụ phê duyệt.</p>"
                        + "<p>Bạn đã đủ điều kiện nhận học viên. Hãy đăng nhập để hoàn thiện hồ sơ giảng dạy và bắt đầu hành trình cùng Kizuna.</p>"
                        + signature());
    }

    public void rejectedAfterCommit(String toEmail, String fullName, String reasonLabel, String feedback) {
        String detail = feedback == null || feedback.isBlank() ? "" : "<p>Nhận xét của cán bộ học vụ: " + escape(feedback) + "</p>";
        afterCommit(toEmail, "Kết quả xét duyệt hồ sơ gia sư của bạn",
                greeting(fullName)
                        + "<p>Rất tiếc, hồ sơ xác minh gia sư của bạn không được chấp nhận.</p>"
                        + "<p>Lý do: <strong>" + escape(reasonLabel) + "</strong></p>"
                        + detail
                        + "<p>Quyết định này là kết quả cuối cùng của đợt xét duyệt. Nếu bạn cần trao đổi thêm, vui lòng liên hệ bộ phận hỗ trợ của KizunaTutor.</p>"
                        + signature());
    }

    public void infoRequestedAfterCommit(String toEmail, String fullName, String message) {
        afterCommit(toEmail, "Vui lòng bổ sung thông tin hồ sơ gia sư",
                greeting(fullName)
                        + "<p>Cán bộ học vụ cần bạn bổ sung thông tin cho hồ sơ xác minh gia sư:</p>"
                        + "<blockquote style=\"border-left:3px solid #006194;padding-left:12px;color:#3f4850\">" + escape(message) + "</blockquote>"
                        + "<p>Vui lòng đăng nhập, cập nhật hồ sơ và nộp lại.</p>"
                        + signature());
    }

    private void afterCommit(String toEmail, String subject, String html) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        Runnable send = () -> executor.execute(() -> send(toEmail, subject, html));
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

    private void send(String toEmail, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Đã gửi email kết quả xác minh tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email kết quả xác minh tới {} thất bại; quyết định vẫn được giữ nguyên", toEmail, e);
        }
    }

    private static String greeting(String fullName) {
        return "<p>Xin chào " + escape(fullName == null ? "bạn" : fullName) + ",</p>";
    }

    private static String signature() {
        return "<p>Trân trọng,<br>Ban học vụ KizunaTutor</p>";
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value).replace("\n", "<br>");
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
