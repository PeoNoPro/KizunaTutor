package com.kizuna.features.profile.service;

import com.kizuna.features.profile.dto.request.ChangePasswordRequest;

/**
 * Đổi mật khẩu của học viên hoặc gia sư đang đăng nhập (FE-02, contracts/profile-api.md mục 2A).
 */
public interface ProfilePasswordService {

    void changeMyPassword(String email, ChangePasswordRequest request);
}
