package com.kizuna.features.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * FE-14: ghi lần đăng nhập gần nhất vào user_login_activity (migration 037) để Quản lý biết tài khoản
 * đã bao lâu không đăng nhập.
 * - Chạy trong giao dịch riêng (REQUIRES_NEW) vì luồng đăng nhập có thể đang ở giao dịch chỉ đọc.
 * - Không bao giờ làm hỏng việc đăng nhập: lỗi (ví dụ chưa chạy migration 037) chỉ ghi log cảnh báo.
 */
@Slf4j
@Component
public class LoginActivityRecorder {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate newTransaction;

    public LoginActivityRecorder(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Đang ở trong giao dịch (đăng nhập, vừa đăng ký xong) thì ghi sau khi giao dịch đó hoàn tất:
     * tài khoản vừa tạo mới thấy được, và đăng nhập thất bại / bị hoàn tác thì không ghi.
     */
    public void record(UUID userId) {
        if (userId == null) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write(userId);
                }
            });
        } else {
            write(userId);
        }
    }

    private void write(UUID userId) {
        try {
            newTransaction.executeWithoutResult(status -> jdbc.update(
                    "INSERT INTO user_login_activity (user_id, last_login_at) VALUES (?, CURRENT_TIMESTAMP) "
                            + "ON CONFLICT (user_id) DO UPDATE SET last_login_at = EXCLUDED.last_login_at",
                    userId));
        } catch (RuntimeException e) {
            log.warn("Không ghi được lần đăng nhập gần nhất của {}: {}", userId, e.getMessage());
        }
    }
}
