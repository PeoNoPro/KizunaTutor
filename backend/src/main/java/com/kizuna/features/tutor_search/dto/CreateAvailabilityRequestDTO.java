package com.kizuna.features.tutor_search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAvailabilityRequestDTO {

    /**
     * Ngày trong tuần: 1 = Chủ nhật, 2 = Thứ hai, 3 = Thứ ba, 4 = Thứ tư, 5 = Thứ năm, 6 = Thứ sáu, 7 = Thứ bảy
     */
    @NotNull(message = "Ngày trong tuần không được để trống")
    @Min(value = 1, message = "Ngày trong tuần phải từ 1 (Chủ nhật) đến 7 (Thứ bảy)")
    @Max(value = 7, message = "Ngày trong tuần phải từ 1 (Chủ nhật) đến 7 (Thứ bảy)")
    private Integer dayOfWeek;

    @NotBlank(message = "Giờ bắt đầu không được để trống")
    private String startTime;

    @NotBlank(message = "Giờ kết thúc không được để trống")
    private String endTime;

    @Builder.Default
    private Boolean isActive = true;
}

