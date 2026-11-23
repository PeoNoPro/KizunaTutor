package com.kizuna.features.auth;

import com.kizuna.features.auth.service.LoginActivityRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** FE-14: ghi lần đăng nhập gần nhất — ghi đúng bảng, sau khi giao dịch hoàn tất, và không bao giờ làm hỏng đăng nhập. */
class LoginActivityRecorderTest {

    private static final UUID USER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JdbcTemplate jdbc;
    private PlatformTransactionManager transactions;
    private LoginActivityRecorder recorder;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        recorder = new LoginActivityRecorder(jdbc, transactions);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void ngoaiGiaoDich_ghiNgay_vaoUserLoginActivity_trongGiaoDichRieng() {
        recorder.record(USER);

        verify(jdbc).update(contains("INSERT INTO user_login_activity"), eq(USER));
        verify(transactions).getTransaction(argThat(d -> d.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
    }

    @Test
    void trongGiaoDich_chiGhiSauKhiGiaoDichHoanTat() {
        TransactionSynchronizationManager.initSynchronization();

        recorder.record(USER);
        verifyNoInteractions(jdbc);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(jdbc).update(contains("INSERT INTO user_login_activity"), eq(USER));
    }

    @Test
    void loiGhi_vdChuaChayMigration_khongLamHongDangNhap() {
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(new org.springframework.jdbc.BadSqlGrammarException(
                "insert", "INSERT INTO user_login_activity", new java.sql.SQLException("relation does not exist")));

        assertThatCode(() -> recorder.record(USER)).doesNotThrowAnyException();
    }

    @Test
    void khongCoNguoiDung_khongGhi() {
        recorder.record(null);
        verifyNoInteractions(jdbc, transactions);
    }
}
