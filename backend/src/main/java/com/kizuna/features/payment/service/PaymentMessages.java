package com.kizuna.features.payment.service;

/**
 * Thông điệp hiển thị FE-09 (spec mục "Thông điệp hệ thống"; MSG31–MSG37 là đề xuất bổ sung SRS — srs-gaps G-05).
 */
public final class PaymentMessages {

    private PaymentMessages() {
    }

    public static final String MSG01_NO_RESULT = "Không tìm thấy kết quả phù hợp. Vui lòng điều chỉnh bộ lọc.";
    public static final String MSG04_UPDATED = "Cập nhật thành công.";
    public static final String MSG31_SUCCESS = "Thanh toán thành công. Gói %s có hiệu lực đến %s.";
    public static final String MSG32_FAILED = "Thanh toán chưa thành công. Bạn có thể thử thanh toán lại.";
    public static final String MSG33_PENDING = "Giao dịch đang được xác nhận với cổng thanh toán. Vui lòng kiểm tra lại sau ít phút.";
    public static final String MSG34_NOT_APPROVED = "Bạn cần được phê duyệt hồ sơ gia sư trước khi đăng ký gói hội viên.";
    public static final String MSG35_PLAN_INACTIVE = "Gói hội viên này hiện không còn mở bán.";
    public static final String MSG36_PRICE_RANGE = "Giá gói phải từ 10.000 đ đến 100.000.000 đ.";
    public static final String MSG37_GATEWAY_UNAVAILABLE = "Cổng thanh toán đang tạm thời không khả dụng. Vui lòng thử lại sau.";

    public static final String PLAN_NOT_FOUND = "Không tìm thấy gói hội viên.";
    public static final String PLAN_NOT_UPGRADE = "Bạn đang sở hữu gói này hoặc gói cao hơn. Chỉ có thể nâng cấp lên gói cao hơn.";
    public static final String PLAN_NAME_DUPLICATE = "Tên gói đã tồn tại. Vui lòng chọn tên khác.";
    public static final String PLAN_CREATED = "Đã tạo gói hội viên.";
    public static final String PLAN_OPENED = "Đã mở bán gói hội viên.";
    public static final String PLAN_CLOSED = "Đã ngừng bán gói hội viên.";
    public static final String TRANSACTION_NOT_FOUND = "Không tìm thấy giao dịch.";
    public static final String TUTOR_NOT_FOUND = "Không tìm thấy gia sư.";
    public static final String TUTOR_PROFILE_MISSING = "Tài khoản của bạn chưa có hồ sơ gia sư.";
    public static final String INVALID_RESULT = "Kết quả thanh toán không hợp lệ.";
    public static final String PAYMENT_CREATED = "Đã tạo giao dịch. Đang chuyển tới cổng thanh toán.";
    public static final String NOTE_SAVED = "Đã lưu ghi chú điều tra.";
    public static final String GATEWAY_CHECKED = "Đã kiểm tra trạng thái với cổng thanh toán.";
    public static final String INVALID_ID = "Mã không hợp lệ.";
    public static final String PAYMENT_EXPIRED = "Giao dịch đã hết hạn thanh toán. Vui lòng chọn gói để tạo giao dịch mới.";
    public static final String PAYMENT_RESUMED = "Đang chuyển tới cổng thanh toán.";
    public static final String PAYMENT_ALREADY_FINISHED = "Giao dịch này đã có kết quả từ cổng thanh toán. Vui lòng tải lại trang để xem.";
}
