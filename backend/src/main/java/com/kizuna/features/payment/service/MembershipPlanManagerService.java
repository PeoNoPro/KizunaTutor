package com.kizuna.features.payment.service;

import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;

import java.util.List;

/**
 * Cấu hình gói, giá và quyền lợi của quản lý vận hành (UC-99, UC-100).
 */
public interface MembershipPlanManagerService {

    List<MembershipViews.PlanManagerView> listPlans();

    MembershipViews.PlanManagerView createPlan(MembershipRequests.CreatePlanRequest request);

    MembershipViews.PlanManagerView updatePlan(String planId, MembershipRequests.UpdatePlanRequest request);

    MembershipViews.PlanManagerView setActive(String planId, boolean active);
}
