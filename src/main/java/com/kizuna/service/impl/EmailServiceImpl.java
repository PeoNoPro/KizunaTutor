package com.kizuna.service.impl;

import com.kizuna.service.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * Triển khai dịch vụ gửi email xác thực OTP qua SMTP Gmail:
 * - Sử dụng JavaMailSender và MimeMessageHelper định dạng HTML chuẩn.
 * - Thực thi bất đồng bộ (@Async) trên luồng thread pool độc lập.
 * - Template email phong cách Nhật Bản tinh tế, hiển thị mã OTP to rõ ràng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:kizunatutor@gmail.com}")
    private String senderEmail;

    @Override
    @Async("mailTaskExecutor")
    public void sendRegistrationOtpEmail(String toEmail, String fullName, String otp) {
        log.info("=================================================================");
        log.info("[OTP DISPATCH] Bắt đầu gửi email mã OTP [{}] đến Gmail: [{}] ({})", otp, toEmail, fullName);
        log.info("=================================================================");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "KizunaTutor - Nền Tảng Học & Gia Sư Tiếng Nhật");
            helper.setTo(toEmail);
            helper.setSubject("[KizunaTutor] Mã xác thực đăng ký tài khoản của bạn: " + otp);

            String htmlContent = buildOtpHtmlTemplate(fullName, otp);
            helper.setText(htmlContent, true);

            log.info("Bắt đầu gửi email thông tin tài khoản đến: {}", toEmail);
            mailSender.send(message);
            log.info("Email đã được bàn giao thành công cho SMTP Server cho địa chỉ: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("LỖI GỬI EMAIL THẤT BẠI (MIME/ENCODING) tới [{}]: ", toEmail, e);
        } catch (Exception e) {
            log.error("LỖI GỬI EMAIL THẤT BẠI: ", e);
            log.info("[DEV FALLBACK] Trong môi trường phát triển (Offline/Sandbox), bạn vẫn có thể dùng mã OTP này trên giao diện: {}", otp);
        }
    }

    @Override
    @Async("mailTaskExecutor")
    public void sendStaffAccountCredentialsEmail(String toEmail, String fullName, String temporaryPassword, String roleBadge) {
        log.info("=================================================================");
        log.info("[STAFF CREDENTIALS DISPATCH] Bắt đầu chuẩn bị email tài khoản tới: [{}] ({}) - Role: [{}]", toEmail, fullName, roleBadge);
        log.info("=================================================================");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail, "KizunaTutor - Ban Quản Trị Hệ Thống");
            helper.setTo(toEmail);
            helper.setSubject("【KizunaTutor】Thông tin tài khoản công việc của bạn tại KizunaTutor");

            String htmlContent = buildStaffCredentialsHtmlTemplate(fullName, toEmail, temporaryPassword, roleBadge);
            helper.setText(htmlContent, true);

            log.info("Bắt đầu gửi email thông tin tài khoản đến: {}", toEmail);
            mailSender.send(message);
            log.info("Email đã được bàn giao thành công cho SMTP Server cho địa chỉ: {}", toEmail);

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("LỖI GỬI EMAIL THẤT BẠI (MIME/ENCODING) tài khoản nhân sự tới [{}]: ", toEmail, e);
        } catch (Exception e) {
            log.error("LỖI GỬI EMAIL THẤT BẠI: ", e);
            log.info("[DEV FALLBACK] Thông tin tài khoản: Email={}, Password={}, Role={}", toEmail, temporaryPassword, roleBadge);
        }
    }

    /**
     * Gửi email kiểm tra chẩn đoán kết nối SMTP trực tiếp (đồng bộ, ném ngoại lệ nếu có lỗi).
     */
    @Override
    public void sendDirectDiagnosticTestEmail(String toEmail) throws Exception {
        log.info("=================================================================");
        log.info("[SMTP DIAGNOSTIC TEST] Kiểm tra kết nối SMTP trực tiếp tới: [{}] từ hòm thư [{}]", toEmail, senderEmail);
        log.info("=================================================================");

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(senderEmail, "KizunaTutor - SMTP Health Check");
        helper.setTo(toEmail);
        helper.setSubject("【KizunaTutor】Email Kiểm Tra Kết Nối SMTP Hệ Thống (" + java.time.LocalDateTime.now() + ")");

        String testContent = "<!DOCTYPE html><html><body>"
                + "<div style='font-family: Arial, sans-serif; padding: 20px; border: 1px solid #E2E8F0; border-radius: 10px; max-width: 500px;'>"
                + "<h2 style='color: #0F172A;'>🎉 Kết Nối SMTP Thành Công!</h2>"
                + "<p>Email này được gửi trực tiếp từ máy chủ KizunaTutor để kiểm tra đường truyền gửi thư Gmail.</p>"
                + "<ul>"
                + "<li><strong>Hòm thư gửi:</strong> " + senderEmail + "</li>"
                + "<li><strong>Hòm thư nhận:</strong> " + toEmail + "</li>"
                + "<li><strong>Thời gian gửi:</strong> " + java.time.LocalDateTime.now() + "</li>"
                + "</ul>"
                + "<p style='color: #16A34A; font-weight: bold;'>Máy chủ SMTP Google đã tiếp nhận và bàn giao thư thành công.</p>"
                + "</div></body></html>";

        helper.setText(testContent, true);

        try {
            log.info("Bắt đầu gửi email thông tin tài khoản đến: {}", toEmail);
            mailSender.send(message);
            log.info("Email đã được bàn giao thành công cho SMTP Server cho địa chỉ: {}", toEmail);
        } catch (Exception e) {
            log.error("LỖI GỬI EMAIL THẤT BẠI: ", e);
            throw e;
        }
    }

    /**
     * Xây dựng giao diện Email HTML trang trọng gửi tài khoản nhân sự
     */
    private String buildStaffCredentialsHtmlTemplate(String fullName, String email, String temporaryPassword, String roleBadge) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName : "Nhân sự mới";

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='UTF-8'>"
                + "  <title>Thông Tin Tài Khoản KizunaTutor</title>"
                + "</head>"
                + "<body style='margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>"
                + "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color: #F8FAFC; padding: 40px 10px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='100%' max-width='600' border='0' cellspacing='0' cellpadding='0' style='max-width: 600px; background-color: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 4px 16px rgba(15, 23, 42, 0.06);'>"
                + "          <!-- Header Banner -->"
                + "          <tr>"
                + "            <td style='background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%); padding: 36px 28px; text-align: center;'>"
                + "              <div style='display: inline-block; width: 50px; height: 50px; line-height: 50px; border-radius: 14px; background: #E11D48; color: #FFFFFF; font-size: 26px; font-weight: bold; margin-bottom: 12px; box-shadow: 0 4px 10px rgba(225, 29, 72, 0.3);'>絆</div>"
                + "              <h1 style='color: #FFFFFF; font-size: 22px; font-weight: 800; margin: 0 0 6px 0; letter-spacing: -0.5px;'>KizunaTutor Portal</h1>"
                + "              <p style='color: #94A3B8; font-size: 13px; margin: 0;'>Hệ Thống Quản Trị & Đào Tạo Tiếng Nhật Trực Tuyến</p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Body Content -->"
                + "          <tr>"
                + "            <td style='padding: 36px 32px; color: #334155; line-height: 1.6;'>"
                + "              <p style='font-size: 16px; margin: 0 0 16px 0;'>Kính gửi <strong style='color: #0F172A;'>" + greetingName + "</strong>,</p>"
                + "              <p style='font-size: 14px; margin: 0 0 24px 0; color: #475569;'>Chào mừng bạn gia nhập đội ngũ vận hành và giảng dạy của KizunaTutor. Ban Quản Trị Hệ Thống đã khởi tạo thành công tài khoản công việc cho bạn với thông tin đăng nhập ban đầu như sau:</p>"
                + "              <!-- Credentials Card -->"
                + "              <div style='background-color: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 12px; padding: 20px 24px; margin-bottom: 24px;'>"
                + "                <table width='100%' border='0' cellspacing='0' cellpadding='0'>"
                + "                  <tr>"
                + "                    <td style='padding: 8px 0; font-size: 13px; color: #64748B; width: 140px; font-weight: 600;'>Tên Đăng Nhập (Email):</td>"
                + "                    <td style='padding: 8px 0; font-size: 14px; color: #0F172A; font-weight: 700;'>" + email + "</td>"
                + "                  </tr>"
                + "                  <tr>"
                + "                    <td style='padding: 8px 0; font-size: 13px; color: #64748B; font-weight: 600;'>Mật Khẩu Tạm Thời:</td>"
                + "                    <td style='padding: 8px 0;'>"
                + "                      <span style='background: #FEF2F2; border: 1px dashed #F87171; color: #DC2626; padding: 4px 10px; border-radius: 6px; font-family: \"Courier New\", monospace; font-size: 15px; font-weight: 800; letter-spacing: 1px;'>" + temporaryPassword + "</span>"
                + "                    </td>"
                + "                  </tr>"
                + "                  <tr>"
                + "                    <td style='padding: 8px 0; font-size: 13px; color: #64748B; font-weight: 600;'>Quyền Hạn / Vai Trò:</td>"
                + "                    <td style='padding: 8px 0; font-size: 14px; color: #1E293B; font-weight: 700;'>" + roleBadge + "</td>"
                + "                  </tr>"
                + "                </table>"
                + "              </div>"
                + "              <!-- Security Warning Box -->"
                + "              <div style='background-color: #FFFBEB; border-left: 4px solid #F59E0B; padding: 14px 18px; border-radius: 0 8px 8px 0; margin-bottom: 28px;'>"
                + "                <div style='display: flex; align-items: flex-start;'>"
                + "                  <span style='font-size: 16px; margin-right: 8px;'>🔒</span>"
                + "                  <p style='margin: 0; font-size: 13px; color: #92400E; font-weight: 500; line-height: 1.5;'>"
                + "                    <strong>Lưu ý bảo mật quan trọng:</strong> Vì lý do an toàn bảo mật, hệ thống sẽ yêu cầu bạn <strong>thay đổi mật khẩu này ngay trong lần đăng nhập đầu tiên</strong> trước khi có thể truy cập Bảng điều khiển công việc."
                + "                  </p>"
                + "                </div>"
                + "              </div>"
                + "              <!-- Call To Action Button -->"
                + "              <div style='text-align: center; margin: 30px 0 20px 0;'>"
                + "                <a href='http://localhost:8080/login' style='display: inline-block; background-color: #0F172A; color: #FFFFFF; text-decoration: none; padding: 14px 36px; border-radius: 10px; font-size: 14px; font-weight: 700; letter-spacing: 0.2px; box-shadow: 0 4px 10px rgba(15, 23, 42, 0.2);'>Đăng Nhập Ngay Vào Hệ Thống →</a>"
                + "              </div>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td style='background-color: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 20px 32px; text-align: center; color: #94A3B8; font-size: 12px;'>"
                + "              <p style='margin: 0 0 4px 0;'>Email này được gửi tự động từ hệ sinh thái học tập <strong>KizunaTutor</strong>.</p>"
                + "              <p style='margin: 0;'>Nếu bạn không phải là người nhận dự kiến, vui lòng bỏ qua thư này hoặc thông báo cho Quản trị viên.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    /**
     * Xây dựng giao diện Email HTML chuyên nghiệp mang thương hiệu KizunaTutor
     */
    private String buildOtpHtmlTemplate(String fullName, String otp) {
        String greetingName = (fullName != null && !fullName.isBlank()) ? fullName : "bạn";

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <meta charset='UTF-8'>"
                + "  <title>Mã Xác Thực KizunaTutor</title>"
                + "</head>"
                + "<body style='margin: 0; padding: 0; background-color: #F1F5F9; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>"
                + "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='background-color: #F1F5F9; padding: 40px 10px;'>"
                + "    <tr>"
                + "      <td align='center'>"
                + "        <table width='100%' max-width='560' border='0' cellspacing='0' cellpadding='0' style='max-width: 560px; background-color: #FFFFFF; border-radius: 16px; border: 1px solid #E2E8F0; overflow: hidden; box-shadow: 0 4px 12px rgba(15, 23, 42, 0.05);'>"
                + "          <!-- Header Banner -->"
                + "          <tr>"
                + "            <td style='background: linear-gradient(135deg, #0F172A 0%, #1E293B 100%); padding: 32px 24px; text-align: center;'>"
                + "              <div style='display: inline-block; width: 48px; height: 48px; line-height: 48px; border-radius: 12px; background: #E11D48; color: #FFFFFF; font-size: 24px; font-weight: bold; margin-bottom: 12px;'>絆</div>"
                + "              <h1 style='color: #FFFFFF; font-size: 22px; font-weight: 800; margin: 0 0 6px 0; letter-spacing: -0.5px;'>KizunaTutor</h1>"
                + "              <p style='color: #94A3B8; font-size: 13px; margin: 0;'>Nền Tảng Kết Nối Gia Sư & Luyện Viết Kanji Trực Tuyến</p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Body Content -->"
                + "          <tr>"
                + "            <td style='padding: 36px 32px; color: #334155; line-height: 1.6;'>"
                + "              <p style='font-size: 15px; margin: 0 0 16px 0;'>Xin chào <strong style='color: #0F172A;'>" + greetingName + "</strong>,</p>"
                + "              <p style='font-size: 14px; margin: 0 0 24px 0; color: #64748B;'>Cảm ơn bạn đã lựa chọn tham gia nền tảng KizunaTutor. Để hoàn tất việc đăng ký và kích hoạt tài khoản của bạn, vui lòng nhập mã xác thực OTP 6 chữ số dưới đây:</p>"
                + "              <!-- Big OTP Box -->"
                + "              <div style='text-align: center; margin: 30px 0;'>"
                + "                <div style='display: inline-block; background-color: #F8FAFC; border: 2px dashed #CBD5E1; border-radius: 12px; padding: 16px 36px; font-family: \"Courier New\", Courier, monospace; font-size: 34px; font-weight: 800; letter-spacing: 10px; color: #0F172A;'>"
                +                    otp
                + "                </div>"
                + "              </div>"
                + "              <!-- Notice Box -->"
                + "              <div style='background-color: #FFF1F2; border-left: 4px solid #E11D48; padding: 12px 16px; border-radius: 0 8px 8px 0; margin-bottom: 24px;'>"
                + "                <p style='margin: 0; font-size: 13px; color: #9F1239; font-weight: 500;'>"
                + "                  ⏳ <strong>Thời hạn hiệu lực:</strong> Mã xác thực trên chỉ có giá trị sử dụng trong vòng <strong>5 phút (300 giây)</strong>."
                + "                </p>"
                + "              </div>"
                + "              <p style='font-size: 13px; color: #64748B; margin: 0 0 8px 0;'>"
                + "                🔒 <strong>Bảo mật tài khoản:</strong> Tuyệt đối không chia sẻ mã này cho bất kỳ ai. Đội ngũ KizunaTutor sẽ không bao giờ liên hệ yêu cầu bạn cung cấp mã xác thực OTP."
                + "              </p>"
                + "              <p style='font-size: 13px; color: #94A3B8; margin: 16px 0 0 0; font-style: italic;'>"
                + "                Nếu bạn không thực hiện yêu cầu đăng ký này, vui lòng bỏ qua email hoặc thông báo ngay cho đội ngũ hỗ trợ của chúng tôi."
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td style='background-color: #F8FAFC; border-top: 1px solid #E2E8F0; padding: 20px 32px; text-align: center; color: #94A3B8; font-size: 12px;'>"
                + "              <p style='margin: 0 0 4px 0;'>Hệ Thống Đào Tạo Tiếng Nhật KizunaTutor</p>"
                + "              <p style='margin: 0;'>Email liên hệ: <a href='mailto:kizunatutor@gmail.com' style='color: #E11D48; text-decoration: none;'>kizunatutor@gmail.com</a></p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }
}
