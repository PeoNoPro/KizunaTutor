package com.kizuna.core.exception;
public class BusinessException extends RuntimeException {
    private final int status;
    public BusinessException(int status, String message) { super(message); this.status = status; }
    public int status() { return status; }
    public static BusinessException invalid() { return new BusinessException(400, "Vui lòng kiểm tra thông tin đã nhập."); }
    public static BusinessException conflict() { return new BusinessException(409, "Dữ liệu đã thay đổi. Vui lòng tải lại trước khi lưu."); }
    public static BusinessException missing() { return new BusinessException(404, "Không tìm thấy nội dung bạn yêu cầu."); }
}
