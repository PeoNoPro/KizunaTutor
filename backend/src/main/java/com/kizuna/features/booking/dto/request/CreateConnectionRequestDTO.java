package com.kizuna.features.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Học viên gửi yêu cầu kết nối trực tiếp với gia sư (không cần học thử). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConnectionRequestDTO {

    @NotNull(message = "Mã gia sư không được để trống")
    private UUID tutorId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một ca học hằng tuần")
    @Size(max = 7, message = "Bạn chỉ có thể chọn tối đa 7 ca học mỗi tuần")
    @Valid
    private List<ConnectionSlotRequestDTO> slots;

    /** Ngày bắt đầu học (thường là Thứ Hai của tuần học viên đang xem). Bỏ trống = bắt đầu từ hôm nay. */
    private java.time.LocalDate startDate;

    @Size(max = 100, message = "Trình độ hiện tại tối đa 100 ký tự")
    private String currentLevel;

    @Size(max = 500, message = "Mục tiêu học tập tối đa 500 ký tự")
    private String learningGoals;

    @Size(max = 1000, message = "Lời nhắn tối đa 1000 ký tự")
    private String initialMessage;
}
