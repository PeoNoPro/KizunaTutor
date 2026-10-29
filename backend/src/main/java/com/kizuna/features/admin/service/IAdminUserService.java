package com.kizuna.features.admin.service;


import com.kizuna.core.entity.*;
import com.kizuna.features.admin.dto.*;
import com.kizuna.core.entity.User;

import java.util.Map;
import java.util.UUID;

public interface IAdminUserService {

    /**
     * Lấy danh sách người dùng có phân trang, tìm kiếm và lọc theo vai trò, trạng thái.
     */
    UserPageResponse getUsers(int page, int size, String keyword, String role, String status, String sortField, String sortDir, User currentAdmin);

    /**
     * Lấy thông tin chi tiết một tài khoản theo ID.
     */
    UserDTO getUserById(UUID userId, User currentAdmin);

    /**
     * Khởi tạo tài khoản nhân viên / người dùng mới.
     */
    UserDTO createUser(CreateUserRequest request, User currentAdmin);

    /**
     * Cập nhật thông tin và thay đổi vai trò (phân quyền).
     */
    UserDTO updateUser(UUID userId, UpdateUserRoleRequest request, User currentAdmin);

    /**
     * Khóa hoặc Mở khóa tài khoản người dùng (ngăn chặn tự khóa chính mình).
     */
    UserDTO updateStatus(UUID userId, String newStatus, User currentAdmin);

    /**
     * Xóa mềm tài khoản người dùng (ngăn chặn tự xóa chính mình).
     */
    void deleteUser(UUID userId, User currentAdmin);

    /**
     * Cấp lại mật khẩu tạm thời mới và gửi lại email thông tin tài khoản cho người dùng.
     */
    UserDTO resendCredentials(UUID userId, User currentAdmin);

    /**
     * Lấy 4 chỉ số KPI tổng hợp cho Admin Dashboard.
     */
    Map<String, Long> getKpiStats();
}
