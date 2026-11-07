package com.kizuna.features.admin.service;

import com.kizuna.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lưu email gửi thư phải đăng nhập được máy chủ SMTP; không được thì báo lỗi và không lưu. */
class MailSenderVerifyTest {

    @Test
    void unreachableSmtpServerIsRejectedBeforeSaving() {
        var account = new SystemSettingService.MailAccount("127.0.0.1", 1, "admin-sender@test.invalid", "test-only");
        var error = assertThrows(BusinessException.class, () -> SystemSettingService.verifyLogin(account));
        assertEquals(400, error.status());
        assertTrue(error.getMessage().startsWith("Chưa lưu"));
        assertFalse(error.getMessage().contains("test-only"), "Thông báo không được chứa mật khẩu");
    }
}
