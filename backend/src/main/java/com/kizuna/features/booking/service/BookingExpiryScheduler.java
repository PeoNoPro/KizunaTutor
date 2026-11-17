package com.kizuna.features.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tác vụ nền cập nhật trạng thái theo thời gian (chạy mỗi 5 phút):
 *  - buổi học thử đã xác nhận mà đã qua giờ kết thúc → "Đã hoàn thành";
 *  - yêu cầu học thử / kết nối trực tiếp gia sư chưa phản hồi quá hạn → "Đã hết hạn".
 * Các API danh sách không tự làm việc này nữa để mở trang nhanh hơn. Gia sư vẫn không đồng ý được
 * yêu cầu đã quá hạn (kiểm tra riêng khi phản hồi), nên trạng thái trễ vài phút không gây sai nghiệp vụ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final TrialBookingService trialBookingService;
    private final ConnectionRequestService connectionRequestService;

    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 30 * 1000L)
    public void refreshTimeBasedStatuses() {
        try {
            trialBookingService.completeEndedTrials();
        } catch (Exception e) {
            log.warn("Không thể tự hoàn thành buổi học thử đã qua giờ: {}", e.getMessage());
        }
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
