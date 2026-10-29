package com.kizuna.features.auth.service;


import com.kizuna.core.entity.*;
import com.kizuna.features.auth.dto.PendingRegistration;

public interface IOtpService {

    /**
     * Sinh mã OTP 6 số ngẫu nhiên, lưu thông tin đăng ký vào cache với thời hạn 5 phút.
     *
     * @return Mã OTP 6 chữ số vừa tạo
     */
    String generateAndSaveOtp(String email, String fullName, String passwordHash, String role, String targetJlpt);

    /**
     * Xác minh mã OTP do người dùng nhập.
     * Kiểm tra thời hạn hiệu lực và số lần thử sai (tối đa 5 lần).
     *
     * @return Thông tin PendingRegistration nếu mã khớp chính xác
     */
    PendingRegistration verifyOtp(String email, String inputOtp);

    /**
     * Tạo mã OTP mới và cập nhật thời hạn hiệu lực cho thông tin đăng ký đang chờ.
     * Yêu cầu thời gian giữa 2 lần gửi tối thiểu là 60 giây.
     *
     * @return Mã OTP 6 chữ số mới
     */
    String resendOtp(String email);

    /**
     * Lấy thông tin đăng ký chờ xác thực (nếu còn tồn tại).
     */
    PendingRegistration getPendingRegistration(String email);
}
