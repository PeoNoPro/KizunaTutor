package com.kizuna.features.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Một buổi học cụ thể (theo ngày, giờ Việt Nam) của yêu cầu kết nối trực tiếp (bảng connection_request_sessions).
 * Trạng thái buổi đi theo trạng thái yêu cầu: PENDING giữ chỗ trong thời hạn phản hồi, ACCEPTED đã xác nhận.
 */
@Entity
@Table(name = "connection_request_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ConnectionRequest connectionRequest;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
