package com.kizuna.service;

import com.kizuna.dto.auth.LearnerAccessState;
import com.kizuna.entity.User;

import java.util.UUID;

/**
 * Service kiểm soát đặc quyền truy cập phân hệ "Học tập & Luyện tập" và kết nối Learning Space.
 */
public interface ILearningSpaceService {

    /**
     * Xác định chính xác 1 trong 3 trạng thái của tài khoản hiện tại:
     * - UNAUTHENTICATED
     * - UNASSIGNED_LEARNER
     * - ACTIVE_OFFICIAL_LEARNER
     */
    LearnerAccessState checkCurrentUserAccessState();

    /**
     * Kiểm tra trạng thái rào cản cho đối tượng User cụ thể.
     */
    LearnerAccessState checkUserAccessState(User user);

    /**
     * Kiểm tra nhanh Learner có thuộc Learning Space nào đang hoạt động không.
     */
    boolean isStudentAssignedToAnyTutor(UUID userId);

    /**
     * Xử lý nhập mã mời của Gia sư để kích hoạt Không gian học tập (Mở khóa từ Trạng thái 2 lên 3).
     */
    boolean joinSpaceByInviteCode(String inviteCode);
}
