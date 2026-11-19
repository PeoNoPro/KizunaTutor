package com.kizuna.features.auth;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.features.auth.dto.response.PendingRegistration;
import com.kizuna.features.auth.service.impl.OtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test Suite for OtpServiceImpl.
 * Covers UC-02 & UC-02.OTP (OTP Generation, 60s Cooldown, 5 Attempts Limit, 5-minute Expiry, Verification).
 */
class OtpServiceImplTest {

    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        otpService = new OtpServiceImpl();
    }

    @Nested
    @DisplayName("generateAndSaveOtp Tests")
    class GenerateOtpTests {

        @Test
        @DisplayName("Happy Path: Tạo mã OTP 6 chữ số hợp lệ, lưu thông tin đăng ký chờ")
        void generateOtp_happyPath_shouldReturn6DigitOtp() {
            // [Arrange] Given registration info
            String email = "learner@kizuna.vn";
            String fullName = "Yamada Taro";
            String hash = "pw-hash-123";

            // [Act] When generateAndSaveOtp is called
            String otp = otpService.generateAndSaveOtp(email, fullName, hash, "ROLE_LEARNER", "N3");

            // [Assert] Then OTP has exactly 6 digits and pending registration is saved
            assertNotNull(otp);
            assertEquals(6, otp.length());
            assertTrue(otp.matches("\\d{6}"));

            PendingRegistration pending = otpService.getPendingRegistration(email);
            assertNotNull(pending);
            assertEquals(email, pending.getEmail());
            assertEquals("Yamada Taro", pending.getFullName());
            assertEquals(otp, pending.getOtp());
            assertEquals(0, pending.getAttempts());
            assertTrue(pending.getExpiresAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("Boundary Case: Chuỗi email có khoảng trắng và chữ hoa được chuẩn hóa thành lowercase")
        void generateOtp_whenEmailHasSpacesAndUpper_shouldNormalize() {
            // [Arrange] Given un-normalized email
            String rawEmail = "  LEARNER@Kizuna.VN  ";

            // [Act] When generating OTP
            String otp = otpService.generateAndSaveOtp(rawEmail, "Taro", "hash", "ROLE_LEARNER", "N3");

            // [Assert] Then email is retrieved in normalized lowercase form
            assertNotNull(otp);
            assertNotNull(otpService.getPendingRegistration("learner@kizuna.vn"));
        }
    }

    @Nested
    @DisplayName("verifyOtp Tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("Happy Path: Xác thực thành công khi mã OTP nhập khớp")
        void verifyOtp_happyPath_shouldReturnPendingAndRemoveFromCache() {
            // [Arrange] Given generated OTP
            String email = "test@kizuna.vn";
            String otp = otpService.generateAndSaveOtp(email, "Test User", "hash", "ROLE_LEARNER", "N2");

            // [Act] When verifying with matching OTP
            PendingRegistration result = otpService.verifyOtp(email, otp);

            // [Assert] Then verification succeeds and pending entry is cleared
            assertNotNull(result);
            assertEquals(email, result.getEmail());
            assertNull(otpService.getPendingRegistration(email), "Sau khi xác thực thành công, OTP phải được xóa khỏi cache");
        }

        @Test
        @DisplayName("Abnormal Case: Xác thực thất bại khi email không có phiên đăng ký nào trong cache")
        void verifyOtp_whenPendingNotFound_shouldThrowBadRequestException() {
            // [Arrange] Given email without pending registration

            // [Act & Assert] When verifying -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.verifyOtp("notfound@kizuna.vn", "123456"));
            assertThat(ex.getMessage()).contains("Yêu cầu xác thực không tồn tại");
        }

        @Test
        @DisplayName("Abnormal Case: Nhập sai mã OTP -> Tăng số lần thử (attempts) và báo số lần thử còn lại")
        void verifyOtp_whenWrongOtp_shouldIncrementAttemptsAndThrow() {
            // [Arrange] Given generated OTP
            String email = "wrong@kizuna.vn";
            String correctOtp = otpService.generateAndSaveOtp(email, "Wrong User", "hash", "ROLE_LEARNER", "N3");
            String wrongOtp = "000000".equals(correctOtp) ? "111111" : "000000";

            // [Act & Assert] When entering wrong OTP -> Throws BadRequestException with remaining attempts
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.verifyOtp(email, wrongOtp));
            assertThat(ex.getMessage()).contains("Mã xác thực OTP không chính xác. Bạn còn 4 lần thử.");

            PendingRegistration pending = otpService.getPendingRegistration(email);
            assertNotNull(pending);
            assertEquals(1, pending.getAttempts());
        }

        @Test
        @DisplayName("Boundary Case: Nhập sai vượt quá 5 lần (Max Attempts) -> Hủy yêu cầu đăng ký và xóa khỏi cache")
        void verifyOtp_whenMaxAttemptsExceeded_shouldLockAndRemove() {
            // [Arrange] Given pending registration with 5 previous wrong attempts
            String email = "maxattempts@kizuna.vn";
            otpService.generateAndSaveOtp(email, "Max User", "hash", "ROLE_LEARNER", "N3");
            PendingRegistration pending = otpService.getPendingRegistration(email);
            pending.setAttempts(5);

            // [Act & Assert] When verifying again -> Throws BadRequestException and removes from cache
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.verifyOtp(email, "999999"));
            assertThat(ex.getMessage()).contains("Bạn đã nhập sai mã OTP quá 5 lần");
            assertNull(otpService.getPendingRegistration(email));
        }

        @Test
        @DisplayName("Boundary Case: Mã OTP đã hết hạn sau 5 phút (TTL expired) -> Xóa khỏi cache và báo lỗi")
        void verifyOtp_whenExpired_shouldThrowBadRequestException() {
            // [Arrange] Given pending registration with expiresAt in the past
            String email = "expired@kizuna.vn";
            otpService.generateAndSaveOtp(email, "Expired User", "hash", "ROLE_LEARNER", "N3");
            PendingRegistration pending = otpService.getPendingRegistration(email);
            pending.setExpiresAt(Instant.now().minusSeconds(10));

            // [Act & Assert] When verifying expired OTP -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.verifyOtp(email, pending.getOtp()));
            assertThat(ex.getMessage()).contains("Mã xác thực OTP đã hết hạn (sau 5 phút)");
            assertNull(otpService.getPendingRegistration(email));
        }
    }

    @Nested
    @DisplayName("resendOtp Tests")
    class ResendOtpTests {

        @Test
        @DisplayName("Happy Path: Gửi lại OTP thành công sau khi đã hết thời gian cooldown 60 giây")
        void resendOtp_happyPath_shouldGenerateNewOtpAfterCooldown() {
            // [Arrange] Given pending registration with lastSentAt set 65 seconds ago
            String email = "resend@kizuna.vn";
            String oldOtp = otpService.generateAndSaveOtp(email, "Resend User", "hash", "ROLE_LEARNER", "N3");
            PendingRegistration pending = otpService.getPendingRegistration(email);
            pending.setLastSentAt(Instant.now().minusSeconds(65));
            pending.setAttempts(3);

            // [Act] When resending OTP
            String newOtp = otpService.resendOtp(email);

            // [Assert] Then a new OTP is issued, attempts reset to 0, and expiresAt refreshed
            assertNotNull(newOtp);
            assertEquals(6, newOtp.length());
            assertEquals(0, pending.getAttempts());
            assertTrue(pending.getExpiresAt().isAfter(Instant.now()));
        }

        @Test
        @DisplayName("Boundary Case: Yêu cầu gửi lại khi chưa hết 60 giây cooldown -> Chặn và báo số giây cần đợi")
        void resendOtp_whenInCooldown_shouldThrowBadRequestException() {
            // [Arrange] Given pending registration sent 10 seconds ago (50s cooldown remaining)
            String email = "cooldown@kizuna.vn";
            otpService.generateAndSaveOtp(email, "Cooldown User", "hash", "ROLE_LEARNER", "N3");
            PendingRegistration pending = otpService.getPendingRegistration(email);
            pending.setLastSentAt(Instant.now().minusSeconds(10));

            // [Act & Assert] When calling resendOtp -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.resendOtp(email));
            assertThat(ex.getMessage()).contains("Vui lòng đợi thêm");
            assertThat(ex.getMessage()).contains("giây trước khi yêu cầu gửi lại mã");
        }

        @Test
        @DisplayName("Abnormal Case: Yêu cầu gửi lại OTP khi phiên đăng ký không tồn tại -> Báo lỗi BadRequestException")
        void resendOtp_whenPendingNotFound_shouldThrowBadRequestException() {
            // [Arrange] Given unknown email

            // [Act & Assert] When calling resendOtp -> Throws BadRequestException
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> otpService.resendOtp("unknown@kizuna.vn"));
            assertThat(ex.getMessage()).contains("Phiên đăng ký đã kết thúc");
        }
    }
}
