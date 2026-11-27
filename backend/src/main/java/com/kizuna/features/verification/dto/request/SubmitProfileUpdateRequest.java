package com.kizuna.features.verification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Gia sư đã được phê duyệt gửi yêu cầu cập nhật hồ sơ: trình độ JLPT mới (để trống = không đổi),
 * ghi chú cho cán bộ học vụ và mã các giấy tờ đã xác minh muốn gỡ (chỉ bị xóa khi cán bộ chấp nhận).
 * Giấy tờ mới tải lên được gửi kèm tự động. Kiểm tra ở service.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitProfileUpdateRequest {
    private String highestJlptLevel;
    private String note;
    private List<UUID> removedDocumentIds;

    public SubmitProfileUpdateRequest(String highestJlptLevel, String note) {
        this(highestJlptLevel, note, null);
    }
}
