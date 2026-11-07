package com.kizuna.features.admin.config;

import com.kizuna.features.admin.service.SystemSettingService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import java.io.InputStream;

/**
 * Bộ gửi thư dùng chung cho toàn hệ thống (thư mật khẩu nhân viên, OTP đăng ký, kết quả xác minh gia sư,
 * thông báo thanh toán). Mỗi lần gửi: nếu Admin đã cấu hình email gửi thư ở trang Cấu hình hệ thống thì gửi
 * bằng email đó, chưa có thì gửi bằng tài khoản SMTP trong cấu hình server (spring.mail.*) như trước.
 * Các service khác vẫn tiêm JavaMailSender như cũ nên không phải sửa code của họ.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class AdminMailConfiguration {

    @Bean
    @Primary
    public JavaMailSender javaMailSender(MailProperties properties, SystemSettingService settings) {
        return new SwitchingMailSender(serverSender(properties), settings);
    }

    /** Dựng bộ gửi theo spring.mail.* giống cách Spring Boot tự tạo (Spring Boot không tự tạo khi đã có bean này). */
    private static JavaMailSenderImpl serverSender(MailProperties properties) {
        var sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        if (properties.getPort() != null) sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());
        sender.setProtocol(properties.getProtocol());
        if (properties.getDefaultEncoding() != null) sender.setDefaultEncoding(properties.getDefaultEncoding().name());
        sender.getJavaMailProperties().putAll(properties.getProperties());
        return sender;
    }

    static final class SwitchingMailSender implements JavaMailSender {
        private final JavaMailSenderImpl server;
        private final SystemSettingService settings;

        SwitchingMailSender(JavaMailSenderImpl server, SystemSettingService settings) {
            this.server = server;
            this.settings = settings;
        }

        /** Tài khoản Admin cấu hình (nếu có); lỗi đọc cấu hình thì vẫn gửi bằng tài khoản server. */
        private SystemSettingService.MailAccount account() {
            try { return settings.mailAccount(); }
            catch (RuntimeException exception) { return null; }
        }

        private JavaMailSender current(SystemSettingService.MailAccount account) {
            return account == null ? server : SystemSettingService.buildMailSender(account);
        }

        /**
         * Tên hiển thị đọc từ application.properties bị Spring đọc theo bảng mã ISO-8859-1 nên tiếng Việt thành
         * "Ná»n Táº£ng...". Đổi lại về UTF-8; chuỗi vốn đúng thì giữ nguyên.
         */
        static String repairName(String name) {
            if (name == null || name.chars().anyMatch(c -> c > 0xFF)) return name;
            var bytes = name.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
            var decoder = java.nio.charset.StandardCharsets.UTF_8.newDecoder();
            try { return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString(); }
            catch (java.nio.charset.CharacterCodingException exception) { return name; }
        }

        /**
         * Sửa tên hiển thị người gửi; khi dùng tài khoản Admin thì đặt địa chỉ người gửi đúng email đã đăng nhập,
         * vì Gmail, Outlook chỉ cho gửi bằng chính địa chỉ đó.
         */
        private static void prepareSender(MimeMessage message, SystemSettingService.MailAccount account) {
            try {
                String personal = null, address = null;
                var from = message.getFrom();
                if (from != null && from.length > 0 && from[0] instanceof InternetAddress current) { personal = current.getPersonal(); address = current.getAddress(); }
                if (account != null) address = account.username();
                if (address == null) return;
                personal = repairName(personal);
                message.setFrom(new InternetAddress(address, personal == null ? "KizunaTutor" : personal, "UTF-8"));
            } catch (MessagingException | java.io.UnsupportedEncodingException exception) {
                log.warn("[MAIL] Chưa đặt được người gửi: {}", exception.getClass().getSimpleName());
            }
        }

        @Override public MimeMessage createMimeMessage() { return server.createMimeMessage(); }
        @Override public MimeMessage createMimeMessage(InputStream contentStream) throws MailException { return server.createMimeMessage(contentStream); }

        /**
         * Gửi bằng tài khoản Admin; nếu tài khoản đó không đăng nhập hoặc không kết nối được (mật khẩu ứng dụng
         * bị thu hồi, đổi máy chủ...) thì gửi lại bằng tài khoản SMTP của server để OTP, xác minh, thanh toán
         * vẫn đi được.
         */
        @Override
        public void send(MimeMessage... mimeMessages) throws MailException {
            var account = account();
            if (account == null) {
                for (var message : mimeMessages) prepareSender(message, null);
                server.send(mimeMessages);
                return;
            }
            var originalFrom = new jakarta.mail.Address[mimeMessages.length][];
            for (int i = 0; i < mimeMessages.length; i++) {
                originalFrom[i] = fromOf(mimeMessages[i]);
                prepareSender(mimeMessages[i], account);
            }
            try {
                current(account).send(mimeMessages);
            } catch (MailAuthenticationException | MailSendException exception) {
                log.warn("[MAIL] Tài khoản gửi thư do Admin cấu hình lỗi ({}), chuyển sang tài khoản SMTP của server.", exception.getClass().getSimpleName());
                for (int i = 0; i < mimeMessages.length; i++) {
                    restoreFrom(mimeMessages[i], originalFrom[i]);
                    prepareSender(mimeMessages[i], null);
                }
                server.send(mimeMessages);
            }
        }

        private static jakarta.mail.Address[] fromOf(MimeMessage message) {
            try { return message.getFrom(); } catch (MessagingException exception) { return null; }
        }

        private static void restoreFrom(MimeMessage message, jakarta.mail.Address[] from) {
            try {
                if (from == null || from.length == 0) message.removeHeader("From");
                else message.setFrom(from[0]);
            } catch (MessagingException exception) {
                log.warn("[MAIL] Chưa đặt lại được người gửi: {}", exception.getClass().getSimpleName());
            }
        }

        @Override
        public void send(MimeMessagePreparator... preparators) throws MailException {
            var messages = new MimeMessage[preparators.length];
            for (int i = 0; i < preparators.length; i++) {
                messages[i] = createMimeMessage();
                try { preparators[i].prepare(messages[i]); }
                catch (Exception exception) { throw new org.springframework.mail.MailPreparationException(exception); }
            }
            send(messages);
        }

        @Override
        public void send(SimpleMailMessage... simpleMessages) throws MailException {
            var account = account();
            if (account == null) { server.send(simpleMessages); return; }
            var originalFrom = new String[simpleMessages.length];
            for (int i = 0; i < simpleMessages.length; i++) {
                originalFrom[i] = simpleMessages[i].getFrom();
                simpleMessages[i].setFrom(account.username());
            }
            try {
                current(account).send(simpleMessages);
            } catch (MailAuthenticationException | MailSendException exception) {
                log.warn("[MAIL] Tài khoản gửi thư do Admin cấu hình lỗi ({}), chuyển sang tài khoản SMTP của server.", exception.getClass().getSimpleName());
                for (int i = 0; i < simpleMessages.length; i++) simpleMessages[i].setFrom(originalFrom[i]);
                server.send(simpleMessages);
            }
        }
    }
}
