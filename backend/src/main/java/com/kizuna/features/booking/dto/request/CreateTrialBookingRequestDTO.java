package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTrialBookingRequestDTO {

    @NotNull(message = "Mã gia sư không được để trống")
    private UUID tutorId;

    @NotNull(message = "Thời gian bắt đầu học không được để trống")
    private OffsetDateTime scheduledStartTime;

    @Builder.Default
    private Integer durationMinutes = 30;

    @Builder.Default
    private String learningMode = "ONLINE";

    private String locationDetails;

    private String currentLevel;

    private String learningGoals;

    private String initialMessage;

    /** Email liên hệ gửi cho gia sư (để trống = email tài khoản). */
    @jakarta.validation.constraints.Size(max = 255, message = "Email tối đa 255 ký tự")
    private String contactEmail;

    /** Số điện thoại liên hệ gửi cho gia sư (để trống = số trong hồ sơ). */
    @jakarta.validation.constraints.Size(max = 20, message = "Số điện thoại không hợp lệ")
    private String contactPhone;

    /** Học viên đồng ý chia sẻ hồ sơ, email, số điện thoại với gia sư (bắt buộc). */
    private Boolean shareContact;

    /**
     * true = học viên dùng "Đề xuất giờ khác" (giờ ngoài lịch rảnh của gia sư).
     * false/null = chọn từ lịch rảnh; nếu giờ đó nằm ngoài lịch rảnh thì bị từ chối.
     */
    private Boolean offSchedule;
}

