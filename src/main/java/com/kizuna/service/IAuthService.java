package com.kizuna.service;

import com.kizuna.dto.auth.LoginRequest;
import com.kizuna.dto.auth.LoginResponse;
import com.kizuna.dto.auth.RegisterRequest;
import com.kizuna.dto.auth.UserSummaryDto;
import jakarta.servlet.http.HttpServletResponse;

public interface IAuthService {
    /**
     * Xác thực tài khoản người dùng, tạo JWT Token và cấu hình cookie/redirectUrl.
     */
    LoginResponse login(LoginRequest request, HttpServletResponse response);

    /**
     * Đăng ký tài khoản người dùng mới, tự động mã hóa mật khẩu, tạo Learner nếu là học viên,
     * cấp phát JWT và cấu hình cookie/redirectUrl.
     */
    LoginResponse register(RegisterRequest request, HttpServletResponse response);

    /**
     * Lấy thông tin tài khoản hiện tại từ Security Context.
     */
    UserSummaryDto getCurrentUser();

    /**
     * Đăng xuất, hủy cookie và Security Context.
     */
    void logout(HttpServletResponse response);

    /**
     * Thực hiện đổi mật khẩu bắt buộc lần đầu cho tài khoản nhân sự được Admin cấp.
     * Kiểm tra mật khẩu tạm, độ mạnh mật khẩu, giải phóng cờ mustChangePassword.
     */
    java.util.Map<String, Object> changePasswordFirstTime(com.kizuna.dto.auth.FirstTimePasswordChangeRequest request, String userEmail);
}
