package com.kizuna.service.tutor;

import com.kizuna.dto.tutor.SaveTutorProfileRequest;
import com.kizuna.dto.tutor.TutorProfileDto;

import java.util.UUID;

/**
 * Service quản lý hồ sơ và quy trình phê duyệt gia sư (UC 9 - 16, UC 20).
 */
public interface ITutorProfileService {

    /**
     * Lấy thông tin hồ sơ gia sư theo email tài khoản đăng nhập.
     */
    TutorProfileDto getProfileByEmail(String email);

    /**
     * Lưu bản nháp hồ sơ gia sư (UC 9, 10, 11, 12).
     */
    TutorProfileDto saveDraft(String email, SaveTutorProfileRequest request);

    /**
     * Nộp hồ sơ lên Ban Học Vụ để thẩm định (UC 13, 16).
     * Chuyển trạng thái sang PENDING_VERIFICATION.
     */
    TutorProfileDto submitProfile(String email, SaveTutorProfileRequest request);

    /**
     * Ban Học Vụ phê duyệt hồ sơ gia sư (UC 20).
     * Chuyển trạng thái sang APPROVED_UNPAID.
     */
    TutorProfileDto approveProfile(UUID tutorId);

    /**
     * Ban Học Vụ từ chối hoặc yêu cầu bổ sung hồ sơ (UC 15, 16).
     * Chuyển trạng thái sang REJECTED kèm lý do/góp ý.
     */
    TutorProfileDto rejectProfile(UUID tutorId, String feedback);
}
