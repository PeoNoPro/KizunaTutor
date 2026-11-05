package com.kizuna.features.admin.service;

import com.kizuna.features.admin.repository.StaffCredentialRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import org.springframework.web.util.HtmlUtils;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Mật khẩu tạm chỉ sinh ngay lúc gửi thư, nên hàng đợi trong DB không chứa bí mật và gửi lại luôn dùng mật khẩu mới. */
@Slf4j @Service @RequiredArgsConstructor
public class StaffCredentialService {
    /** Không phải hash BCrypt nên không mật khẩu nào khớp cho tới khi thư được gửi. */
    public static final String UNUSABLE_HASH="!CREDENTIAL-NOT-ISSUED";
    private static final String ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$%*";
    private static final int LENGTH=16;
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER=new BCryptPasswordEncoder();
    // Một luồng gửi lần lượt để lô lớn không làm quá tải SMTP và hai lượt không xử lý trùng một người.
    private static final ExecutorService MAIL=Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"staff-credential-mail");thread.setDaemon(true);return thread;});
    private final JavaMailSender mailSender;
    private final StaffCredentialRepository queue;
    private final PlatformTransactionManager transactions;
    private final Clock operationsClock;
    @Value("${spring.mail.username:}") private String sender;
    @Value("${app.frontend-url:http://localhost:3000}") private String frontendUrl;

    /** Ghi vào hàng đợi cùng giao dịch tạo tài khoản; rollback thì không có thư nào đi. */
    public void enqueue(UUID user,UUID actor,OffsetDateTime now) {
        queue.enqueue(user,actor,now);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {MAIL.execute(()->process(user));}
        });
    }

    /** Tiếp tục các thư còn chờ, kể cả khi ứng dụng từng khởi động lại giữa chừng. */
    @Scheduled(fixedDelay=60000,initialDelay=30000)
    public void resumePending() {
        try {
            queue.stalePending(OffsetDateTime.now(operationsClock).minusMinutes(2),20).forEach(user->MAIL.execute(()->process(user)));
        } catch(RuntimeException exception) {
            log.warn("[STAFF MAIL] Chưa đọc được hàng đợi email: {}",exception.getClass().getSimpleName());
        }
    }

    private void process(UUID user) {
        try {
            var password=new String[1];
            var recipient=new TransactionTemplate(transactions).execute(status->queue.lockPending(user).map(found->{
                password[0]=temporaryPassword();
                queue.issue(user,ENCODER.encode(password[0]),OffsetDateTime.now(operationsClock));
                return found;
            }).orElse(null));
            if(recipient==null)return;
            boolean sent=send(recipient,password[0]);
            new TransactionTemplate(transactions).executeWithoutResult(status->queue.finish(user,sent?"SENT":"FAILED",OffsetDateTime.now(operationsClock)));
        } catch(RuntimeException exception) {
            log.warn("[STAFF MAIL] Chưa xử lý được hàng đợi: {}",exception.getClass().getSimpleName());
        }
    }

    private static String temporaryPassword() {
        var text=new StringBuilder(LENGTH);
        for(int n=0;n<LENGTH;n++)text.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return text.toString();
    }

    private boolean send(StaffCredentialRepository.Recipient to,String password) {
        try {
            MimeMessage message=mailSender.createMimeMessage();
            var helper=new MimeMessageHelper(message,"UTF-8");
            helper.setFrom(sender.isBlank()?"no-reply@kizunatutor.local":sender,"KizunaTutor");
            helper.setTo(to.email());
            helper.setSubject("[KizunaTutor] Thông tin đăng nhập tài khoản nhân viên");
            helper.setText("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px'>"
                +"<h2 style='color:#0F2545'>Chào mừng bạn đến với KizunaTutor</h2>"
                +"<p>Xin chào <strong>"+HtmlUtils.htmlEscape(to.fullName())+"</strong>,</p>"
                +"<p>Tài khoản nhân viên của bạn đã được tạo. Thông tin đăng nhập lần đầu:</p>"
                +"<p>Email: <strong>"+HtmlUtils.htmlEscape(to.email())+"</strong><br>Mật khẩu tạm: <strong data-temporary-password>"+HtmlUtils.htmlEscape(password)+"</strong></p>"
                +"<p><a href='"+HtmlUtils.htmlEscape(frontendUrl)+"/login'>Đăng nhập KizunaTutor</a></p>"
                +"<p style='color:#68737D;font-size:13px'>Bạn phải đổi mật khẩu ở lần đăng nhập đầu tiên. Vui lòng không chia sẻ thư này cho bất kỳ ai.</p></div>",true);
            mailSender.send(message);
            log.info("[STAFF MAIL] Đã gửi thông tin đăng nhập tới: {}",to.email());
            return true;
        } catch(Exception exception) {
            // Không ghi mật khẩu; Admin thấy trạng thái thất bại và cấp lại từ danh sách.
            log.warn("[STAFF MAIL] Chưa gửi được tới {}: {} - {}",to.email(),exception.getClass().getSimpleName(),exception.getMessage());
            return false;
        }
    }
}
