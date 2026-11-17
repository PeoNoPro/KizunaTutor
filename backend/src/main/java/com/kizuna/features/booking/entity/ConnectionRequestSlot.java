package com.kizuna.features.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Ca học hằng tuần học viên chọn trong yêu cầu kết nối trực tiếp (bảng connection_request_slots).
 * dayOfWeek theo quy ước lịch rảnh: 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy (giờ Việt Nam).
 */
@Entity
@Table(name = "connection_request_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionRequestSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "slot_id", updatable = false, nullable = false)
    private UUID slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ConnectionRequest connectionRequest;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
