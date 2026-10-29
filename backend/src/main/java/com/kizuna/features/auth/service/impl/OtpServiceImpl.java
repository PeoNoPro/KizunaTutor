package com.kizuna.features.auth.service.impl;


import com.kizuna.core.entity.*;
import com.kizuna.features.auth.dto.PendingRegistration;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.auth.service.IOtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Triển khai dịch vụ quản lý mã OTP trong bộ nhớ đệm (In-memory Cache):
 * - Quản lý sinh mã 6 số ngẫu nhiên bằng SecureRandom.
 * - Giới hạn thời gian sống (TTL) 300 giây (5 phút).
 * - Kiểm soát số lần thử sai (tối đa 5 lần) để phòng chống tấn công Brute-force.
 * - Kiểm soát thời gian chờ gửi lại (Cooldown 60 giây).
 */
@Slf4j
@Service
public class OtpServiceImpl implements IOtpService {

    private static final int OTP_TTL_SECONDS = 300; // 5 phút
    private static final int MAX_ATTEMPTS = 5;      // Tối đa 5 lần thử sai
    private static final int RESEND_COOLDOWN_SECONDS = 60; // 60 giây giữa các lần gửi lại

    private final Map<String, PendingRegistration> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndSaveOtp(String email, String fullName, String passwordHash, String role, String targetJlpt) {
        String normalizedEmail = normalizeEmail(email);

        // Sinh mã 6 chữ số ngẫu nhiên từ 100000 đến 999999
        int randomCode = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(randomCode);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(OTP_TTL_SECONDS);

        PendingRegistration pending = PendingRegistration.builder()
                .email(normalizedEmail)
                .fullName(fullName.trim())
                .passwordHash(passwordHash)
                .role(role != null && !role.isBlank() ? role : "ROLE_LEARNER")
                .targetJlpt(targetJlpt != null && !targetJlpt.isBlank() ? targetJlpt : "N5")
                .otp(otp)
                .expiresAt(expiresAt)
                .attempts(0)
                .lastSentAt(now)
                .build();

        otpCache.put(normalizedEmail, pending);
        log.info("[OTP Service] Đã tạo mã OTP cho [{}], hiệu lực đến: {}", normalizedEmail, expiresAt);

        return otp;
    }

    @Override
    public PendingRegistration verifyOtp(String email, String inputOtp) {
        String normalizedEmail = normalizeEmail(email);
        PendingRegistration pending = otpCache.get(normalizedEmail);

        if (pending == null) {
            log.warn("[OTP Service] Không tìm thấy phiên đăng ký chờ xác thực cho email: {}", normalizedEmail);
            throw new BadRequestException("Phiên đăng ký không tồn tại hoặc mã OTP đã hết hạn. Vui lòng đăng ký lại.");
        }

        // Kiểm tra thời hạn hiệu lực (5 phút)
        if (Instant.now().isAfter(pending.getExpiresAt())) {
            otpCache.remove(normalizedEmail);
            log.warn("[OTP Service] Mã OTP cho [{}] đã hết hạn", normalizedEmail);
            throw new BadRequestException("Mã xác thực OTP đã hết hạn hiệu lực (5 phút). Vui lòng bấm 'Gửi lại mã'.");
        }

        // Tăng số lần thử
        pending.setAttempts(pending.getAttempts() + 1);

        // Kiểm tra số lần thử sai
        if (pending.getAttempts() > MAX_ATTEMPTS) {
            otpCache.remove(normalizedEmail);
            log.warn("[OTP Service] Email [{}] đã nhập sai quá {} lần. Hủy phiên.", normalizedEmail, MAX_ATTEMPTS);
            throw new BadRequestException("Bạn đã nhập sai mã OTP quá " + MAX_ATTEMPTS + " lần. Phiên xác thực đã bị hủy để bảo vệ an toàn. Vui lòng bắt đầu lại.");
        }

        // So khớp mã OTP
        String cleanInputOtp = inputOtp != null ? inputOtp.trim() : "";
        if (!pending.getOtp().equals(cleanInputOtp)) {
            int remainingAttempts = MAX_ATTEMPTS - pending.getAttempts();
            log.warn("[OTP Service] Email [{}] nhập sai mã OTP ({}/{}). Còn lại: {}",
                    normalizedEmail, pending.getAttempts(), MAX_ATTEMPTS, remainingAttempts);
            throw new BadRequestException("Mã xác thực OTP không chính xác. Bạn còn " + remainingAttempts + " lần thử.");
        }

        // Mã khớp chính xác -> Xóa khỏi cache và trả về thông tin đăng ký
        otpCache.remove(normalizedEmail);
        log.info("[OTP Service] Xác thực OTP thành công cho [{}]", normalizedEmail);
        return pending;
    }

    @Override
    public String resendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        PendingRegistration pending = otpCache.get(normalizedEmail);

        if (pending == null) {
            log.warn("[OTP Service] Yêu cầu gửi lại mã thất bại do không tìm thấy pending registration cho: {}", normalizedEmail);
            throw new BadRequestException("Phiên đăng ký của bạn không tồn tại hoặc đã hết hạn. Vui lòng điền lại form đăng ký.");
        }

        // Kiểm tra thời gian chờ gửi lại (Cooldown 60s)
        Instant now = Instant.now();
        if (pending.getLastSentAt() != null) {
            long elapsedSeconds = Duration.between(pending.getLastSentAt(), now).getSeconds();
            if (elapsedSeconds < RESEND_COOLDOWN_SECONDS) {
                long waitSeconds = RESEND_COOLDOWN_SECONDS - elapsedSeconds;
                log.warn("[OTP Service] Email [{}] gửi lại mã quá nhanh. Cần đợi {}s", normalizedEmail, waitSeconds);
                throw new BadRequestException("Vui lòng đợi thêm " + waitSeconds + " giây trước khi yêu cầu gửi lại mã mới.");
            }
        }

        // Sinh mã mới và làm mới thời hạn
        int randomCode = 100000 + secureRandom.nextInt(900000);
        String newOtp = String.valueOf(randomCode);

        pending.setOtp(newOtp);
        pending.setExpiresAt(now.plusSeconds(OTP_TTL_SECONDS));
        pending.setAttempts(0); // Đặt lại số lần thử
        pending.setLastSentAt(now);

        otpCache.put(normalizedEmail, pending);
        log.info("[OTP Service] Đã tạo mã OTP mới cho [{}]", normalizedEmail);

        return newOtp;
    }

    @Override
    public PendingRegistration getPendingRegistration(String email) {
        return otpCache.get(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new BadRequestException("Địa chỉ email không được để trống");
        }
        return email.trim().toLowerCase();
    }
}
