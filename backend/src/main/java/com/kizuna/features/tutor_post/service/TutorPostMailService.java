package com.kizuna.features.tutor_post.service;

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
 * Email kết quả duyệt bài đăng gửi gia sư (UC-129, UC-130). Luôn gửi, không phụ thuộc cài đặt nhận email của FE-12.
 * Gửi sau khi transaction commit, trên luồng nền riêng; lỗi gửi chỉ ghi log và không hoàn tác quyết định.
 */
@Slf4j
@Service
public class TutorPostMailService {

    private static final String SENDER_NAME = "KizunaTutor - Nền Tảng Gia Sư & Học Tiếng Nhật";

    private final JavaMailSender mailSender;
    private final String senderEmail;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "tutor-post-mail");
        thread.setDaemon(true);
        return thread;
    });

    public TutorPostMailService(JavaMailSender mailSender,
                                @Value("${spring.mail.username:kizunatutor@gmail.com}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void approvedAfterCommit(String toEmail, String fullName, String postTitle) {
        afterCommit(toEmail, "Bài đăng của bạn đã được duyệt",
                greeting(fullName)
                        + "<p>Bài đăng <strong>" + escape(postTitle) + "</strong> đã được cán bộ học vụ duyệt "
                        + "và đang hiển thị trên trang Tìm gia sư.</p>"
                        + signature());
    }

    public void rejectedAfterCommit(String toEmail, String fullName, String postTitle, String reason) {
        rejectedAfterCommit(toEmail, fullName, postTitle, reason, null);
    }

    /**
     * FE-16: resubmitAvailableAt = thời điểm được gửi bài mới (bài đầu tiên bị từ chối);
     * null = bản cập nhật bị từ chối, gia sư gửi bản khác được ngay.
     */
    public void rejectedAfterCommit(String toEmail, String fullName, String postTitle, String reason,
                                    java.time.OffsetDateTime resubmitAvailableAt) {
        String next = resubmitAvailableAt == null
                ? "Bạn có thể sửa và gửi bản cập nhật khác ngay."
                : "Bạn có thể gửi bài đăng mới từ " + TutorPostService.formatResubmitTime(resubmitAvailableAt) + ".";
        afterCommit(toEmail, "Bài đăng của bạn không được duyệt",
                greeting(fullName)
                        + "<p>Rất tiếc, bài đăng <strong>" + escape(postTitle) + "</strong> không được duyệt.</p>"
                        + "<p>Lý do: " + escape(reason) + "</p>"
                        + "<p>" + escape(next) + " Nếu cần trao đổi thêm, vui lòng liên hệ bộ phận hỗ trợ của KizunaTutor.</p>"
                        + signature());
    }

    public void revisionRequestedAfterCommit(String toEmail, String fullName, String postTitle, String message) {
        afterCommit(toEmail, "Vui lòng bổ sung bài đăng",
                greeting(fullName)
                        + "<p>Cán bộ học vụ cần bạn sửa bài đăng <strong>" + escape(postTitle) + "</strong>:</p>"
                        + "<blockquote style=\"border-left:3px solid #006194;padding-left:12px;color:#3f4850\">" + escape(message) + "</blockquote>"
                        + "<p>Vui lòng đăng nhập, sửa bài và gửi lại.</p>"
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
            log.info("Đã gửi email kết quả duyệt bài đăng tới {}", toEmail);
        } catch (Exception e) {
            log.error("Gửi email kết quả duyệt bài đăng tới {} thất bại; quyết định vẫn được giữ nguyên", toEmail, e);
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
