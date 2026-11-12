package com.kizuna.features.notification.service;

import com.kizuna.features.notification.repository.NotificationDispatchRepository;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.Claimed;
import com.kizuna.features.notification.repository.NotificationDispatchRepository.EmailTarget;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Gửi email: chắc chắn chưa gửi thì thử lại có giới hạn, không rõ thì không gửi lại, tôn trọng tùy chọn lúc gửi. */
class NotificationDispatchWorkerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T03:00:00Z"), ZoneOffset.UTC);
    private final NotificationDispatchRepository dispatches = mock(NotificationDispatchRepository.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final NotificationDispatchWorker worker = new NotificationDispatchWorker(dispatches, mail, mock(PlatformTransactionManager.class), CLOCK);
    private final UUID delivery = UUID.randomUUID(), dispatch = UUID.randomUUID(), user = UUID.randomUUID();

    private void target(boolean active, boolean emailOn) {
        when(dispatches.emailTarget(dispatch, user)).thenReturn(Optional.of(new EmailTarget("learner@test.invalid", active, emailOn, "Tiêu đề", "Nội dung")));
    }

    @Test
    void classifiesKnownUnsentTimeoutAndRejection() {
        assertEquals(NotificationDispatchWorker.Outcome.RETRY, NotificationDispatchWorker.classify(new MailAuthenticationException("auth")));
        assertEquals(NotificationDispatchWorker.Outcome.RETRY, NotificationDispatchWorker.classify(new MailSendException("x", new ConnectException("refused"))));
        assertEquals(NotificationDispatchWorker.Outcome.UNKNOWN, NotificationDispatchWorker.classify(new MailSendException("x", new SocketTimeoutException("slow"))));
        assertEquals(NotificationDispatchWorker.Outcome.FAILED, NotificationDispatchWorker.classify(new MailSendException("x", new jakarta.mail.SendFailedException("bad address"))));
    }

    @Test
    void acceptedEmailIsRecorded() {
        target(true, true);
        worker.deliver(new Claimed(delivery, dispatch, user, 1));
        verify(mail).send(any(SimpleMailMessage.class));
        verify(dispatches).finish(delivery, "EMAIL_ACCEPTED", null, null);
    }

    @Test
    void turnedOffEmailOrLockedAccountIsSkippedWithoutSending() {
        target(true, false);
        worker.deliver(new Claimed(delivery, dispatch, user, 1));
        verify(dispatches).finish(delivery, "SKIPPED", "PREFERENCE", null);
        target(false, true);
        worker.deliver(new Claimed(delivery, dispatch, user, 1));
        verify(dispatches).finish(delivery, "SKIPPED", "INACTIVE", null);
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void knownUnsentRetriesThenFailsAfterFourthAttempt() {
        target(true, true);
        doThrow(new MailSendException("x", new ConnectException("refused"))).when(mail).send(any(SimpleMailMessage.class));
        worker.deliver(new Claimed(delivery, dispatch, user, 1));
        verify(dispatches).finish(delivery, "PENDING", "RETRY", OffsetDateTime.now(CLOCK).plusSeconds(30));
        worker.deliver(new Claimed(delivery, dispatch, user, 4));
        verify(dispatches).finish(delivery, "FAILED", "UNAVAILABLE", null);
    }

    @Test
    void timeoutIsUnknownAndNotRetried() {
        target(true, true);
        doThrow(new MailSendException("x", new SocketTimeoutException("slow"))).when(mail).send(any(SimpleMailMessage.class));
        worker.deliver(new Claimed(delivery, dispatch, user, 1));
        verify(dispatches).finish(delivery, "UNKNOWN", "TIMEOUT", null);
        verify(dispatches, never()).finish(eq(delivery), eq("PENDING"), any(), any());
    }
}
