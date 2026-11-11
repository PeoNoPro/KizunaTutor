package com.kizuna.features.tutor_search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO dữ liệu tóm tắt của gia sư hiển thị trên thẻ kết quả tìm kiếm (Tutor Card) và hồ sơ chi tiết.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorSummaryResponseDTO {
    private UUID tutorId;
    private Long tutorCode;
    private String fullName;
    private String avatarUrl;
    private String headline;
    /** Tiêu đề bài đăng đã được cán bộ học vụ duyệt (null nếu chưa có). */
    private String postTitle;
    private String bio;
    private String highestJlptLevel;
    private Short experienceYears;
    private BigDecimal hourlyRate;
    private BigDecimal avgRating;
    private Integer totalReviews;
    private String location;
    private boolean isFeatured;
    private boolean isVerified;
    private List<String> specialties;
    private String teachingFormat;
    private List<TutorCredentialDTO> credentials;
    private List<TutorAvailabilityDTO> availabilities;
    private List<TutorAvailabilityDTO> classSchedules;
    private List<TutorBookedSlotDTO> bookedSlots;
    /** Ca hằng tuần đã có học viên nhận qua kết nối trực tiếp (dayOfWeek: 1 = Chủ nhật ... 7 = Thứ Bảy). */
    private List<TutorAvailabilityDTO> lockedSlots;
    private List<TutorReviewDTO> reviews;
}
