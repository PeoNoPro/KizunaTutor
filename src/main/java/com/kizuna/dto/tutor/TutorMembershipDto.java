package com.kizuna.dto.tutor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO đại diện thông tin gói thành viên gia sư.
 * Phục vụ UC 71, 72, 73 (Xem và mua gói thành viên).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorMembershipDto {
    private UUID membershipId;
    private UUID planId;
    private String planName;
    private String planCode;
    private Integer durationMonths;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private String paymentStatus;
    private String transactionRef;
}
