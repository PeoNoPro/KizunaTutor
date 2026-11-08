package com.kizuna.features.booking.service;

import com.kizuna.features.booking.dto.request.CancelTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.CreateTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RescheduleTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.request.RespondTrialBookingRequestDTO;
import com.kizuna.features.booking.dto.response.TrialBookingResponseDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface TrialBookingService {

    /**
     * Học viên đăng ký học thử 1-1 (User Story 1 - P1)
     */
    TrialBookingResponseDTO createTrialBooking(String userEmail, CreateTrialBookingRequestDTO request);

    /**
     * Gia sư tiếp nhận và xử lý yêu cầu: Chấp nhận hoặc Từ chối (User Story 2 - P1)
     */
    TrialBookingResponseDTO respondTrialBooking(String userEmail, UUID bookingId, RespondTrialBookingRequestDTO request);

    /**
     * Lấy danh sách lịch học thử của người dùng hiện tại (User Story 3 - P2)
     */
    Page<TrialBookingResponseDTO> getMyTrialBookings(String userEmail, String status, int page, int size);

    /**
     * Xem chi tiết 1 buổi học thử (User Story 3 - P2)
     */
    TrialBookingResponseDTO getTrialBookingDetail(String userEmail, UUID bookingId);

    /**
     * Hủy buổi học thử trước giờ học (User Story 4 - P3)
     */
    TrialBookingResponseDTO cancelTrialBooking(String userEmail, UUID bookingId, CancelTrialBookingRequestDTO request);

    /**
     * Đổi lịch / dời lịch học thử sang khung giờ mới (SRS 5.3, UC-34)
     */
    TrialBookingResponseDTO rescheduleTrialBooking(String userEmail, UUID bookingId, RescheduleTrialBookingRequestDTO request);

    /**
     * Kiểm tra xem gia sư có lịch học thử nào đang PENDING hoặc CONFIRMED trong ca này không
     */
    boolean hasActiveBookingsInSlot(UUID tutorId, Integer dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);
}

