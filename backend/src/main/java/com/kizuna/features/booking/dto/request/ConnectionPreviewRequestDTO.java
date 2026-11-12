package com.kizuna.features.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Học viên xem trước lịch học dự kiến (các buổi cụ thể) trước khi gửi yêu cầu kết nối. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionPreviewRequestDTO {

    @NotNull(message = "Mã gia sư không được để trống")
    private UUID tutorId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ca học hằng tuần")
    @Size(max = 7, message = "Bạn chỉ có thể chọn tối đa 7 ca học mỗi tuần")
    @Valid
    private List<ConnectionSlotRequestDTO> slots;

    /** Ngày bắt đầu (thường là Thứ Hai của tuần học viên chọn). Bỏ trống = hôm nay. */
    private LocalDate startDate;

    @Min(value = 1, message = "Số buổi học tối thiểu là 1")
    @Max(value = 12, message = "Số buổi học tối đa là 12")
    private Integer totalSessions;
}
