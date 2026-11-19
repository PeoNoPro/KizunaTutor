package com.kizuna.features.booking.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

/**
 * Một ca học hằng tuần học viên chọn khi gửi yêu cầu kết nối trực tiếp.
 * dayOfWeek: 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy (giờ Việt Nam).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionSlotRequestDTO {

    @NotNull(message = "Vui lòng chọn thứ trong tuần cho ca học")
    @Min(value = 1, message = "Thứ trong tuần không hợp lệ")
    @Max(value = 7, message = "Thứ trong tuần không hợp lệ")
    private Integer dayOfWeek;

    @NotNull(message = "Vui lòng chọn giờ bắt đầu cho ca học")
    private LocalTime startTime;

    @NotNull(message = "Vui lòng chọn thời lượng cho ca học")
    private Integer durationMinutes;
}
