package com.kizuna.features.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tác vụ nền: định kỳ chuyển các yêu cầu học thử / kết nối trực tiếp gia sư chưa phản hồi quá hạn sang "Đã hết hạn".
 * Danh sách yêu cầu cũng tự kiểm tra hết hạn mỗi khi được mở, nên dữ liệu vẫn đúng nếu tác vụ này chưa kịp chạy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final TrialBookingService trialBookingService;
    private final ConnectionRequestService connectionRequestService;

    @Scheduled(fixedDelay = 10 * 60 * 1000L, initialDelay = 60 * 1000L)
    public void expireStaleRequests() {
        try {
            trialBookingService.expireStalePendingTrials();
        } catch (Exception e) {
            log.warn("Không thể tự hết hạn yêu cầu học thử: {}", e.getMessage());
        }
        try {
            connectionRequestService.expireStaleDirectRequests();
        } catch (Exception e) {
            log.warn("Không thể tự hết hạn yêu cầu kết nối: {}", e.getMessage());
        }
    }
}
