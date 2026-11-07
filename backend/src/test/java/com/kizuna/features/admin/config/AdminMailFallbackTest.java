package com.kizuna.features.admin.config;

import com.kizuna.features.admin.service.SystemSettingService;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tài khoản gửi thư của Admin hỏng thì thư của hệ thống vẫn đi bằng tài khoản SMTP của server. */
class AdminMailFallbackTest {

    /** Cổng 1 trên máy cục bộ không có máy chủ SMTP nên kết nối bị từ chối ngay. */
    private static final SystemSettingService.MailAccount BROKEN =
            new SystemSettingService.MailAccount("127.0.0.1", 1, "admin-sender@test.invalid", "test-only");

    private static MimeMessage message(String from) throws Exception {
        var message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(new InternetAddress(from, "KizunaTutor", "UTF-8"));
        message.setRecipients(jakarta.mail.Message.RecipientType.TO, "learner@test.invalid");
        message.setSubject("OTP");
        message.setText("123456");
        return message;
    }

    @Test
    void brokenAdminAccountFallsBackToServerSenderWithOriginalFrom() throws Exception {
        var server = mock(JavaMailSenderImpl.class);
        var settings = mock(SystemSettingService.class);
        when(settings.mailAccount()).thenReturn(BROKEN);
        var sender = new AdminMailConfiguration.SwitchingMailSender(server, settings);
        var otp = message("no-reply@server.test");

        sender.send(otp);

        verify(server).send(new MimeMessage[]{otp});
        assertEquals("no-reply@server.test", ((InternetAddress) otp.getFrom()[0]).getAddress());
    }

    @Test
    void brokenAdminAccountFallsBackForSimpleMessages() {
        var server = mock(JavaMailSenderImpl.class);
        var settings = mock(SystemSettingService.class);
        when(settings.mailAccount()).thenReturn(BROKEN);
        var sender = new AdminMailConfiguration.SwitchingMailSender(server, settings);
        var mail = new SimpleMailMessage();
        mail.setFrom("no-reply@server.test");
        mail.setTo("learner@test.invalid");
        mail.setText("123456");

        sender.send(mail);

        verify(server).send(new SimpleMailMessage[]{mail});
        assertEquals("no-reply@server.test", mail.getFrom());
    }

    @Test
    void withoutAdminAccountServerSenderIsUsedDirectly() throws Exception {
        var server = mock(JavaMailSenderImpl.class);
        var settings = mock(SystemSettingService.class);
        when(settings.mailAccount()).thenReturn(null);
        var sender = new AdminMailConfiguration.SwitchingMailSender(server, settings);

        sender.send(message("no-reply@server.test"));

        verify(server, times(1)).send(any(MimeMessage[].class));
    }
}
