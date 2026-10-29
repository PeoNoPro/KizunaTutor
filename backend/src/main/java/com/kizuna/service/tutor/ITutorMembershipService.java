package com.kizuna.service.tutor;

import com.kizuna.dto.tutor.TutorMembershipDto;
import com.kizuna.entity.MembershipPlan;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service quản lý gói thành viên và thanh toán kích hoạt quyền gia sư (UC 71 - 77, UC 94).
 */
public interface ITutorMembershipService {

    /**
     * Lấy danh sách các gói thành viên đang mở bán (UC 71, 72).
     */
    List<MembershipPlan> getActivePlans();

    /**
     * Lấy thông tin gói thành viên hiện tại của gia sư (UC 76).
     */
    TutorMembershipDto getCurrentMembership(String email);

    /**
     * Tạo URL thanh toán VNPay cho gói thành viên đã chọn (UC 73).
     */
    String createVnPayPaymentUrl(String email, UUID planId, HttpServletRequest request);

    /**
     * Xử lý kết quả trả về từ cổng thanh toán VNPay sau giao dịch (UC 94).
     * Chuyển trạng thái gia sư sang ACTIVE_PAID khi thanh toán thành công.
     *
     * @param vnpParams Danh sách tham số callback từ VNPay
     * @return boolean true nếu giao dịch thành công và đã kích hoạt tài khoản
     */
    boolean processVnPayReturn(Map<String, String> vnpParams);

    /**
     * Kích hoạt gói thành viên trực tiếp (Hỗ trợ môi trường Test & Sandbox).
     */
    TutorMembershipDto directActivateMembershipForTest(String email, UUID planId);
}
