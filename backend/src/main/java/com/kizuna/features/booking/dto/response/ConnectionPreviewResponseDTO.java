package com.kizuna.features.booking.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/** Lịch học dự kiến của yêu cầu kết nối: từng buổi cụ thể và buổi nào đã kín. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionPreviewResponseDTO {

    /** Ngày bắt đầu thực tế (không sớm hơn hôm nay). */
    private LocalDate startDate;
    /** Ngày của buổi cuối cùng. */
    private LocalDate endDate;
    private Integer totalSessions;
    /** Số buổi bị kín. 0 = gửi được. */
    private Integer conflictCount;
    /** Thứ Hai của tuần bắt đầu sớm nhất (sau tuần đang chọn) mà tất cả các buổi đều trống; null nếu không có. */
    private LocalDate suggestedStartWeek;
    /** Tuần bắt đầu muộn nhất được chọn. */
    private LocalDate maxStartWeek;

    private List<SessionCheck> sessions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SessionCheck {
        private LocalDate date;
        /** 1 = Chủ nhật, 2 = Thứ Hai ... 7 = Thứ Bảy */
        private Integer dayOfWeek;
        private String dayName;
        /** "HH:mm" */
        private String startTime;
        private String endTime;
        private boolean available;
        /** Lý do bị kín (null nếu trống). */
        private String reason;
    }
}
