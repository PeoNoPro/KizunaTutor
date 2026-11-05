package com.kizuna.features.payment.service;

import com.kizuna.core.entity.TutorState;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembershipState;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Nhãn tiếng Việt cho mọi mã trạng thái FE-09 (không để mã nội bộ lên giao diện).
 */
public final class MembershipLabels {

    public static final DateTimeFormatter DATE_VN = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private MembershipLabels() {
    }

    public static String transactionStatus(PaymentTransactionStatus status, boolean stale) {
        if (status == null) {
            return "Không xác định";
        }
        return switch (status) {
            case PENDING -> stale ? "Chưa hoàn tất thanh toán" : "Chờ thanh toán";
            case SUCCESS -> "Thành công";
            case FAILED -> "Thất bại";
            case REFUNDED -> "Đã hoàn tiền";
        };
    }

    /** Nhãn của một bản ghi hội viên theo ngày hiện tại. */
    public static String membershipStatus(TutorMembershipState status, LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (status == null) {
            return "Không xác định";
        }
        return switch (status) {
            case ACTIVE -> {
                if (endDate != null && endDate.isBefore(today)) {
                    yield "Đã hết hạn";
                }
                if (startDate != null && startDate.isAfter(today)) {
                    yield "Đã gia hạn, bắt đầu từ " + startDate.format(DATE_VN);
                }
                yield "Đang hiệu lực";
            }
            case EXPIRED -> "Đã hết hạn";
            case CANCELLED -> "Đã hủy";
        };
    }

    public static String planActive(boolean active) {
        return active ? "Đang mở bán" : "Ngừng bán";
    }

    public static String tutorState(TutorState state) {
        if (state == null) {
            return "Không xác định";
        }
        return switch (state) {
            case APPROVED_UNPAID -> "Đã duyệt, chưa kích hoạt gói";
            case ACTIVE_PAID -> "Đang hoạt động";
            default -> state.getDisplayName();
        };
    }

    /** Diễn giải kết quả xử lý ghi trong nhật ký (không hiện mã nội bộ). */
    public static String outcome(String outcome) {
        if (outcome == null) {
            return "";
        }
        return switch (outcome) {
            case "ACTIVATED" -> "Đã ghi nhận thanh toán thành công và kích hoạt gói";
            case "FAILED" -> "Đã ghi nhận thanh toán thất bại";
            case "ALREADY_PROCESSED" -> "Giao dịch đã được xử lý trước đó, không thay đổi";
            case "AMOUNT_MISMATCH" -> "Số tiền không khớp, không thay đổi";
            case "NOT_FOUND" -> "Không tìm thấy giao dịch";
            case "STILL_PENDING" -> "Cổng chưa xác nhận kết quả";
            case "INVALID_SIGNATURE" -> "Chữ ký không hợp lệ, không thay đổi";
            case "UNVERIFIED" -> "Phản hồi tra cứu không xác thực được, không thay đổi";
            case "URL_CREATED" -> "Đã tạo đường dẫn thanh toán";
            case "ERROR" -> "Lỗi khi xử lý, giao dịch giữ nguyên trạng thái";
            default -> "";
        };
    }

    public static String logType(String type) {
        if (type == null) {
            return "Khác";
        }
        return switch (type) {
            case PaymentLogWriter.TYPE_CREATE -> "Tạo đường dẫn thanh toán";
            case PaymentLogWriter.TYPE_IPN -> "Cổng gửi kết quả (IPN)";
            case PaymentLogWriter.TYPE_RETURN -> "Gia sư quay về trang kết quả";
            case PaymentLogWriter.TYPE_QUERY -> "Tra cứu trạng thái với cổng";
            case PaymentLogWriter.TYPE_NOTE -> "Ghi chú điều tra";
            default -> "Khác";
        };
    }
}
