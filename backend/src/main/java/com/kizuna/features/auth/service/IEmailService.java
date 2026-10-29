package com.kizuna.features.auth.service;


import com.kizuna.core.entity.*;
public interface IEmailService {

    /**
     * Gửi email chứa mã xác thực OTP 6 số đến Gmail của người dùng với mẫu HTML chuẩn thương hiệu.
     * Được thực thi bất đồng bộ (@Async) để không làm chậm luồng xử lý API.
     *
     * @param toEmail   Địa chỉ email người nhận
     * @param fullName  Họ và tên người nhận
     * @param otp       Mã OTP 6 chữ số
     */
    void sendRegistrationOtpEmail(String toEmail, String fullName, String otp);

    /**
     * Gửi email thông tin tài khoản công việc mới cho nhân sự / người dùng kèm mật khẩu tạm thời.
     * Được thực thi bất đồng bộ (@Async) từ kizunatutor@gmail.com.
     *
     * @param toEmail           Địa chỉ email người nhận
     * @param fullName          Họ và tên nhân sự
     * @param temporaryPassword Mật khẩu tạm thời được cấp
     * @param roleBadge         Tên hiển thị tiếng Việt của Vai trò
     */
    void sendStaffAccountCredentialsEmail(String toEmail, String fullName, String temporaryPassword, String roleBadge);

    /**
     * Gửi email kiểm tra chẩn đoán kết nối SMTP trực tiếp (đồng bộ, không dùng @Async).
     * Dùng cho API chẩn đoán POST /api/admin/test-email để bắt lỗi và hiển thị StackTrace tức thì.
     *
     * @param toEmail Địa chỉ email nhận test
     * @throws Exception Ngoại lệ nếu SMTP thất bại
     */
    void sendDirectDiagnosticTestEmail(String toEmail) throws Exception;
}
