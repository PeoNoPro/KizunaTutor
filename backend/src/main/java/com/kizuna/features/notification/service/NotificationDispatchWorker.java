package com.kizuna.features.notification.service;

import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.Claimed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * FE-12 tác vụ nền (mỗi 5 giây): chốt người nhận cho đợt hẹn giờ đến hạn, rồi gửi email đang chờ.
 * Bản làm gọn cho một máy chủ: giữ chỗ bằng FOR UPDATE SKIP LOCKED, gửi ngoài transaction,
 * lỗi chắc chắn chưa gửi thì thử lại tối đa 4 lần (sau 30 giây, 2 phút, 5 phút); không rõ đã gửi hay chưa thì ghi
 * "chưa xác định" và không gửi lại để tránh người nhận nhận trùng.
 */
@Slf4j
@Component
public class NotificationDispatchWorker {
    static final int MAX_ATTEMPTS = 4;
    static final List<Duration> RETRY_DELAYS = List.of(Duration.ofSeconds(30), Duration.ofMinutes(2), Duration.ofMinutes(5));

    private final NotificationDispatchRepository dispatches;
    private final JavaMailSender mail;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public NotificationDispatchWorker(NotificationDispatchRepository dispatches, JavaMailSender mail, PlatformTransactionManager transactionManager,
                                      @org.springframework.beans.factory.annotation.Qualifier("notificationClock") Clock clock) {
        this.dispatches = dispatches; this.mail = mail; this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setTimeout(30);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 15000)
    public void run() {
        try {
            var touched = new LinkedHashSet<UUID>(dispatches.markInterrupted());
            for (var id : dispatches.dueScheduled(10)) {
                try { transactions.executeWithoutResult(status -> dispatches.freeze(id)); }
                catch (RuntimeException exception) { log.warn("[NOTIFICATION] Chưa chốt được đợt hẹn giờ {}: {}", id, exception.getClass().getSimpleName()); }
            }
            for (var claimed : dispatches.claimEmails(20)) {
                deliver(claimed);
                touched.add(claimed.dispatchId());
            }
            touched.forEach(dispatches::refreshStatus);
        } catch (RuntimeException exception) {
            // Lỗi DB tạm thời: lần chạy sau thử lại; không làm dừng lịch chạy.
            log.warn("[NOTIFICATION] Tác vụ gửi thông báo lỗi: {}", exception.getClass().getSimpleName());
        }
    }

    /** Kết quả gửi một email: trạng thái lượt và có nên thử lại không. */
    enum Outcome { ACCEPTED, RETRY, FAILED, UNKNOWN }

    void deliver(Claimed claimed) {
        var target = dispatches.emailTarget(claimed.dispatchId(), claimed.userId()).orElse(null);
        if (target == null || !target.active()) { dispatches.finish(claimed.deliveryId(), "SKIPPED", "INACTIVE", null); return; }
        if (!target.emailEnabled()) { dispatches.finish(claimed.deliveryId(), "SKIPPED", "PREFERENCE", null); return; }
        if (target.email() == null || target.email().isBlank()) { dispatches.finish(claimed.deliveryId(), "SKIPPED", "NO_EMAIL", null); return; }
        var message = new SimpleMailMessage();
        message.setTo(target.email());
        message.setSubject("[KizunaTutor] " + target.title());
        message.setText(target.content() + "\n\n—\nBạn nhận thư này vì đã bật nhận thông báo qua email trên KizunaTutor."
                + " Có thể tắt trong mục Tùy chọn nhận thông báo.");
        Outcome outcome;
        try { mail.send(message); outcome = Outcome.ACCEPTED; }
        catch (MailException exception) { outcome = classify(exception); }
        catch (RuntimeException exception) { outcome = Outcome.UNKNOWN; }
        switch (outcome) {
            case ACCEPTED -> dispatches.finish(claimed.deliveryId(), "EMAIL_ACCEPTED", null, null);
            case UNKNOWN -> dispatches.finish(claimed.deliveryId(), "UNKNOWN", "TIMEOUT", null);
            case FAILED -> dispatches.finish(claimed.deliveryId(), "FAILED", "REJECTED", null);
            case RETRY -> {
                if (claimed.attempts() >= MAX_ATTEMPTS) dispatches.finish(claimed.deliveryId(), "FAILED", "UNAVAILABLE", null);
                else dispatches.finish(claimed.deliveryId(), "PENDING", "RETRY", OffsetDateTime.now(clock).plus(RETRY_DELAYS.get(claimed.attempts() - 1)));
            }
        }
    }

    /**
     * Chắc chắn chưa gửi (không đăng nhập được, không kết nối được máy chủ thư) → thử lại;
     * máy chủ thư từ chối địa chỉ → thất bại; hết thời gian chờ hoặc lỗi khác → không rõ, không gửi lại.
     */
    static Outcome classify(MailException exception) {
        if (exception instanceof MailAuthenticationException) return Outcome.RETRY;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException) return Outcome.UNKNOWN;
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) return Outcome.RETRY;
            if (cause instanceof jakarta.mail.SendFailedException) return Outcome.FAILED;
        }
        if (exception instanceof MailSendException send) {
            for (var failure : send.getMessageExceptions()) {
                for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                    if (cause instanceof SocketTimeoutException) return Outcome.UNKNOWN;
                    if (cause instanceof ConnectException || cause instanceof UnknownHostException) return Outcome.RETRY;
                    if (cause instanceof jakarta.mail.SendFailedException) return Outcome.FAILED;
                }
            }
        }
        return Outcome.UNKNOWN;
    }
}
