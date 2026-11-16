package com.kizuna.features.booking.service;

import com.kizuna.features.booking.dto.request.CreateConnectionRequestDTO;
import com.kizuna.features.booking.dto.request.RespondConnectionRequestDTO;
import com.kizuna.features.booking.dto.response.ConnectionRequestResponseDTO;
import org.springframework.data.domain.Page;

import java.util.UUID;

/** Kết nối gia sư trực tiếp, không cần học thử: học viên chọn ca hằng tuần trong lịch rảnh, gia sư duyệt. */
public interface ConnectionRequestService {

    ConnectionRequestResponseDTO createDirectRequest(String learnerEmail, CreateConnectionRequestDTO request);

    Page<ConnectionRequestResponseDTO> getMyDirectRequests(String userEmail, String status, int page, int size);

    ConnectionRequestResponseDTO respond(String tutorEmail, UUID requestId, RespondConnectionRequestDTO request);

    ConnectionRequestResponseDTO cancel(String learnerEmail, UUID requestId);

    /**
     * Chuyển các yêu cầu kết nối trực tiếp gia sư chưa phản hồi quá 24 giờ sang EXPIRED.
     * @return số yêu cầu vừa hết hạn
     */
    int expireStaleDirectRequests();

    /**
     * Gia sư có buổi học kết nối sắp tới (đã đồng ý / đang chờ còn hạn) trong ca hằng tuần này không.
     * dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy.
     */
    boolean hasUpcomingSessionsInSlot(UUID tutorId, Integer dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime);
}
