package com.kizuna.features.payment.service;

import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;

import java.util.List;

/**
 * Cấu hình gói, giá và quyền lợi của quản lý vận hành (UC-99, UC-100).
 */
public interface MembershipPlanAdminService {

    List<MembershipViews.PlanAdminView> listPlans();

    MembershipViews.PlanAdminView createPlan(MembershipRequests.CreatePlanRequest request);

    MembershipViews.PlanAdminView updatePlan(String planId, MembershipRequests.UpdatePlanRequest request);

    MembershipViews.PlanAdminView setActive(String planId, boolean active);
}
