package com.kizuna.features.auth.service.impl;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.auth.dto.response.PendingRegistration;
import com.kizuna.features.auth.service.OtpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class OtpServiceImpl implements OtpService {

    private static final int OTP_TTL_SECONDS = 300; // 5 phút
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final Map<String, PendingRegistration> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAndSaveOtp(String email, String fullName, String passwordHash, String role, String targetJlpt) {
        String normalizedEmail = normalizeEmail(email);

        int randomCode = 100000 + secureRandom.nextInt(900000);
        String otp = String.valueOf(randomCode);

        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(OTP_TTL_SECONDS);

        PendingRegistration pending = PendingRegistration.builder()
                .email(normalizedEmail)
                .fullName(fullName.trim())
                .passwordHash(passwordHash)
                .role(role != null && !role.isBlank() ? role : "ROLE_LEARNER")
                .targetJlpt(targetJlpt != null && !targetJlpt.isBlank() ? targetJlpt : "N3")
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
    public PendingRegistration verifyOtp(String email, String otpInput) {
        String normalizedEmail = normalizeEmail(email);
        PendingRegistration pending = otpCache.get(normalizedEmail);

        if (pending == null) {
            throw new BadRequestException("Yêu cầu xác thực không tồn tại hoặc đã hết hiệu lực. Vui lòng đăng ký lại.");
        }

        if (Instant.now().isAfter(pending.getExpiresAt())) {
            otpCache.remove(normalizedEmail);
            throw new BadRequestException("Mã xác thực OTP đã hết hạn (sau 5 phút). Vui lòng yêu cầu mã mới.");
        }

        if (pending.getAttempts() >= MAX_ATTEMPTS) {
            otpCache.remove(normalizedEmail);
            throw new BadRequestException("Bạn đã nhập sai mã OTP quá 5 lần. Yêu cầu đăng ký bị hủy để bảo mật.");
        }

        if (!pending.getOtp().equals(otpInput.trim())) {
            pending.setAttempts(pending.getAttempts() + 1);
            int remaining = MAX_ATTEMPTS - pending.getAttempts();
            throw new BadRequestException("Mã xác thực OTP không chính xác. Bạn còn " + remaining + " lần thử.");
        }

        otpCache.remove(normalizedEmail);
        return pending;
    }

    @Override
    public String resendOtp(String email) {
        String normalizedEmail = normalizeEmail(email);
        PendingRegistration pending = otpCache.get(normalizedEmail);

        if (pending == null) {
            throw new BadRequestException("Phiên đăng ký đã kết thúc. Vui lòng điền lại thông tin đăng ký.");
        }

        Instant now = Instant.now();
        long secondsSinceLastSend = Duration.between(pending.getLastSentAt(), now).getSeconds();
        if (secondsSinceLastSend < RESEND_COOLDOWN_SECONDS) {
            long waitSeconds = RESEND_COOLDOWN_SECONDS - secondsSinceLastSend;
            throw new BadRequestException("Vui lòng đợi thêm " + waitSeconds + " giây trước khi yêu cầu gửi lại mã.");
        }

        int randomCode = 100000 + secureRandom.nextInt(900000);
        String newOtp = String.valueOf(randomCode);

        pending.setOtp(newOtp);
        pending.setExpiresAt(now.plusSeconds(OTP_TTL_SECONDS));
        pending.setLastSentAt(now);
        pending.setAttempts(0);

        log.info("[OTP Service] Đã tạo mã OTP mới cho [{}]: {}", normalizedEmail, newOtp);
        return newOtp;
    }

    @Override
    public PendingRegistration getPendingRegistration(String email) {
        return otpCache.get(normalizeEmail(email));
    }

    private String normalizeEmail(String email) {
        return (email == null) ? "" : email.trim().toLowerCase();
    }
}
