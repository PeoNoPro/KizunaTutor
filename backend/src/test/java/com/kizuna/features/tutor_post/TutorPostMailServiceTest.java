package com.kizuna.features.tutor_post;

import com.kizuna.features.tutor_post.service.TutorPostMailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** UC-129, UC-130: email kết quả duyệt bài đăng; lỗi gửi chỉ ghi log. */
class TutorPostMailServiceTest {

    JavaMailSender sender = mock(JavaMailSender.class);
    TutorPostMailService service = new TutorPostMailService(sender, "kizunatutor@gmail.com");

    TutorPostMailServiceTest() {
        when(sender.createMimeMessage()).thenAnswer(inv -> new MimeMessage(Session.getInstance(new Properties())));
    }

    private MimeMessage sent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender, timeout(3000)).send(captor.capture());
        return captor.getValue();
    }

    @Test
    void rejectionEmailGoesToTutorWithEscapedReason() throws Exception {
        service.rejectedAfterCommit("gs@kizuna.test", "Gia sư A", "Bài <N3>", "Thiếu <lộ trình>");

        MimeMessage message = sent();
        assertEquals("gs@kizuna.test", message.getAllRecipients()[0].toString());
        assertEquals("Bài đăng của bạn không được duyệt", message.getSubject());
        message.saveChanges();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        message.writeTo(out);
        String raw = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(raw.contains("<lộ trình>"));
    }

    @Test
    void approvalAndRevisionEmailsAreSent() throws Exception {
        service.approvedAfterCommit("gs@kizuna.test", "Gia sư A", "Bài N3");
        service.revisionRequestedAfterCommit("gs@kizuna.test", null, "Bài N3", "Bổ sung lộ trình");

        verify(sender, timeout(3000).times(2)).send(any(MimeMessage.class));
    }

    @Test
    void missingEmailSendsNothing() {
        service.approvedAfterCommit("  ", "Gia sư A", "Bài N3");
        verify(sender, after(300).never()).send(any(MimeMessage.class));
    }

    @Test
    void sendFailureIsSwallowed() {
        doThrow(new MailSendException("SMTP down")).when(sender).send(any(MimeMessage.class));
        assertDoesNotThrow(() -> service.approvedAfterCommit("gs@kizuna.test", "Gia sư A", "Bài N3"));
        verify(sender, timeout(3000)).send(any(MimeMessage.class));
    }
}
