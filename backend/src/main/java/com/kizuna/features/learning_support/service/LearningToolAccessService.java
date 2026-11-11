package com.kizuna.features.learning_support.service;

import com.kizuna.core.entity.User;
import com.kizuna.features.learning_support.dto.LearningToolAccessResponse;

/**
 * Quy tắc mở khóa công cụ học: học viên phải được gia sư thêm vào ít nhất một lớp học còn mở.
 * Gia sư và nhân viên không bị giới hạn. Từ điển Kanji, tra cứu viết tay và Kana luôn mở (không đi qua quy tắc này).
 */
public interface LearningToolAccessService {

    String LOCKED_MESSAGE = "Bạn cần được gia sư thêm vào lớp học để sử dụng công cụ học tập này.";
    String LOGIN_REQUIRED_MESSAGE = "Vui lòng đăng nhập để sử dụng công cụ học tập.";

    /** user null = chưa đăng nhập → 401. */
    LearningToolAccessResponse getAccess(User user);

    /** Ném BusinessException 401 (chưa đăng nhập) hoặc 403 (học viên chưa có lớp). */
    void requireAccess(User user);
}
