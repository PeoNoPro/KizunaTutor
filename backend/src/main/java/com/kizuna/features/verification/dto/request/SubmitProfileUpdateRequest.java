package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gia sư đã được phê duyệt gửi yêu cầu cập nhật hồ sơ: trình độ JLPT mới (để trống = không đổi)
 * và ghi chú cho cán bộ học vụ. Giấy tờ mới tải lên được gửi kèm tự động. Kiểm tra ở service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitProfileUpdateRequest {
    private String highestJlptLevel;
    private String note;
}
