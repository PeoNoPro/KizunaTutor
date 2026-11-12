package com.kizuna.features.user_management.service;

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
 * FE-14: email báo người dùng khi Quản lý đổi trạng thái tài khoản (UC-12 bước 10, AF-1).
 * Gửi sau khi giao dịch commit, trên luồng nền; lỗi gửi chỉ ghi log, không hoàn tác thay đổi.
 * Không bao giờ chứa lý do nội bộ của Quản lý, chỉ chứa lời nhắn Quản lý chủ động gửi.
 */
@Slf4j
@Service
public class UserStatusMailService {

    private static final String SENDER_NAME = "KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật";

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "user-status-mail");
        thread.setDaemon(true);
        return thread;
    });

    public UserStatusMailService(JavaMailSender mailSender,
                                 @Value("${spring.mail.username:kizunatutor@gmail.com}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    /** newStatus: ACTIVE, SUSPENDED hoặc BANNED. message: lời nhắn tùy chọn của Quản lý. */
    public void statusChangedAfterCommit(String toEmail, String fullName, String newStatus, String message) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }
        String subject = subject(newStatus);
        String html = body(fullName, newStatus, message);
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

    static String subject(String newStatus) {
        return switch (newStatus) {
            case "ACTIVE" -> "Tài khoản KizunaTutor của bạn đã được mở lại";
            case "BANNED" -> "Tài khoản KizunaTutor của bạn đã bị khóa vĩnh viễn";
            default -> "Tài khoản KizunaTutor của bạn đã bị tạm khóa";
        };
    }

    static String body(String fullName, String newStatus, String message) {
        String main = switch (newStatus) {
            case "ACTIVE" -> "<p>Tài khoản của bạn trên KizunaTutor đã được khôi phục quyền truy cập. "
                    + "Bạn có thể đăng nhập và sử dụng hệ thống như bình thường.</p>";
            case "BANNED" -> "<p>Tài khoản của bạn trên KizunaTutor đã bị khóa vĩnh viễn và không thể đăng nhập nữa.</p>";
            default -> "<p>Tài khoản của bạn trên KizunaTutor đang bị tạm khóa. "
                    + "Trong thời gian này bạn không thể đăng nhập vào hệ thống.</p>";
        };
        String note = message == null || message.isBlank() ? ""
                : "<p>Lời nhắn từ Quản lý vận hành:</p>"
                + "<blockquote style=\"border-left:3px solid #006194;padding-left:12px;color:#3f4850\">" + escape(message) + "</blockquote>";
        String contact = "ACTIVE".equals(newStatus) ? ""
                : "<p>Nếu bạn cần trao đổi thêm, vui lòng phản hồi email này để được bộ phận hỗ trợ của KizunaTutor giúp đỡ.</p>";
        return "<p>Xin chào " + escape(fullName == null || fullName.isBlank() ? "bạn" : fullName) + ",</p>"
                + main + note + contact
                + "<p>Trân trọng,<br>Ban quản lý vận hành KizunaTutor</p>";
    }

    private void send(String toEmail, String subject, String html) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(senderEmail, SENDER_NAME);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Đã gửi email đổi trạng thái tài khoản tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email đổi trạng thái tài khoản tới {} thất bại; trạng thái mới vẫn được giữ", toEmail, e);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value).replace("\n", "<br>");
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
