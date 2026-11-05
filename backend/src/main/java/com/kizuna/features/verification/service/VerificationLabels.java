package com.kizuna.features.verification.service;

import com.kizuna.features.verification.entity.CredentialType;
import com.kizuna.features.verification.entity.RejectionReason;
import com.kizuna.features.verification.entity.TutorCredentialStatus;
import com.kizuna.features.verification.entity.TutorVerifAction;
import com.kizuna.features.verification.entity.TutorVerificationStatus;

/**
 * Ánh xạ mã nội bộ của FE-03 sang nhãn tiếng Việt hiển thị cho người dùng (Constitution I).
 */
public final class VerificationLabels {

    private VerificationLabels() {
    }

    public static String status(TutorVerificationStatus status) {
        if (status == null) {
            return "Chưa nộp hồ sơ";
        }
        return switch (status) {
            case UNVERIFIED -> "Chưa nộp hồ sơ";
            case PENDING -> "Đang chờ thẩm định";
            case ADDITIONAL_INFO_REQUIRED -> "Cần bổ sung thông tin";
            case APPROVED -> "Đã phê duyệt";
            case REJECTED -> "Bị từ chối";
        };
    }

    public static String documentStatus(TutorCredentialStatus status) {
        if (status == null) {
            return "Chờ thẩm định";
        }
        return switch (status) {
            case PENDING -> "Chờ thẩm định";
            case VERIFIED -> "Đã xác minh";
            case REJECTED -> "Không được chấp nhận";
        };
    }

    public static String credentialType(CredentialType type) {
        if (type == null) {
            return "Khác";
        }
        return switch (type) {
            case IDENTITY_DOCUMENT -> "Giấy tờ tùy thân";
            case JLPT_CERTIFICATE -> "Chứng chỉ JLPT";
            case OTHER_JAPANESE_CERTIFICATE -> "Chứng chỉ tiếng Nhật khác";
            case DEGREE -> "Bằng cấp";
            case TEACHING_CERTIFICATE -> "Chứng chỉ sư phạm";
            case OTHER -> "Khác";
        };
    }

    public static String action(TutorVerifAction action) {
        if (action == null) {
            return "";
        }
        return switch (action) {
            case SUBMIT -> "Nộp hồ sơ";
            case REQUEST_INFO -> "Yêu cầu bổ sung";
            case APPROVE -> "Phê duyệt";
            case REJECT -> "Từ chối";
        };
    }

    public static String rejectionReason(RejectionReason reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason) {
            case INSUFFICIENT_QUALIFICATION -> "Chưa đủ trình độ chuyên môn";
            case INVALID_OR_FORGED_DOCUMENT -> "Giấy tờ không hợp lệ hoặc có dấu hiệu giả mạo";
            case INFORMATION_MISMATCH -> "Thông tin không trùng khớp với giấy tờ";
            case OTHER -> "Lý do khác";
        };
    }

    /**
     * Nhãn nhóm giấy tờ bắt buộc dùng trong danh sách "còn thiếu" khi nộp hồ sơ.
     */
    public static final String MISSING_IDENTITY = "Giấy tờ tùy thân";
    public static final String MISSING_JAPANESE = "Chứng chỉ năng lực tiếng Nhật (JLPT hoặc chứng chỉ tương đương)";
}
