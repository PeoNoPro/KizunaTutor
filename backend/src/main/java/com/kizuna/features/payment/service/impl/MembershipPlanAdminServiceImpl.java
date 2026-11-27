package com.kizuna.features.payment.service.impl;

import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.payment.dto.request.MembershipRequests;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.repository.MembershipPlanRepository;
import com.kizuna.features.payment.service.MembershipPlanAdminService;
import com.kizuna.features.payment.service.MembershipViewMapper;
import com.kizuna.features.payment.service.PaymentMessages;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Giá mới chỉ áp dụng cho giao dịch tạo sau thời điểm lưu (FR-015): giao dịch và gói đã mua lưu số tiền riêng.
 * Số tháng không đổi sau khi tạo; không xóa gói (FR-014).
 */
@Service
public class MembershipPlanAdminServiceImpl implements MembershipPlanAdminService {

    static final long MIN_PRICE = 10_000L;
    static final long MAX_PRICE = 100_000_000L;
    static final int MAX_BENEFITS = 20;
    static final int MAX_BENEFIT_LENGTH = 200;
    static final String BENEFIT_INVALID = "Mỗi gói có tối đa 20 quyền lợi, mỗi quyền lợi tối đa 200 ký tự.";

    private final MembershipPlanRepository planRepository;
    private final MembershipViewMapper mapper;

    public MembershipPlanAdminServiceImpl(MembershipPlanRepository planRepository, MembershipViewMapper mapper) {
        this.planRepository = planRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipViews.PlanAdminView> listPlans() {
        return planRepository.findAllByOrderByDurationMonthsAscPriceAsc().stream().map(mapper::planAdmin).toList();
    }

    @Override
    @Transactional
    public MembershipViews.PlanAdminView createPlan(MembershipRequests.CreatePlanRequest request) {
        String name = normalizeName(request.planName());
        long price = validatePrice(request.price());
        List<String> benefits = normalizeBenefits(request.benefits());
        if (planRepository.existsByPlanNameIgnoreCase(name)) {
            throw new ConflictException(PaymentMessages.PLAN_NAME_DUPLICATE);
        }
        UUID planId = UUID.randomUUID();
        planRepository.insertPlan(planId, name, request.durationMonths(), BigDecimal.valueOf(price),
                mapper.benefitsJson(benefits));
        return mapper.planAdmin(load(planId));
    }

    @Override
    @Transactional
    public MembershipViews.PlanAdminView updatePlan(String planIdText, MembershipRequests.UpdatePlanRequest request) {
        UUID planId = TutorMembershipServiceImpl.parseId(planIdText, PaymentMessages.PLAN_NOT_FOUND);
        load(planId);
        String name = normalizeName(request.planName());
        long price = validatePrice(request.price());
        List<String> benefits = normalizeBenefits(request.benefits());
        if (planRepository.existsByPlanNameIgnoreCaseAndPlanIdNot(name, planId)) {
            throw new ConflictException(PaymentMessages.PLAN_NAME_DUPLICATE);
        }
        planRepository.updatePlan(planId, name, BigDecimal.valueOf(price), mapper.benefitsJson(benefits));
        return mapper.planAdmin(load(planId));
    }

    @Override
    @Transactional
    public MembershipViews.PlanAdminView setActive(String planIdText, boolean active) {
        UUID planId = TutorMembershipServiceImpl.parseId(planIdText, PaymentMessages.PLAN_NOT_FOUND);
        MembershipPlan plan = load(planId);
        if (Boolean.TRUE.equals(plan.getIsActive()) != active) {
            planRepository.updateActive(planId, active);
        }
        return mapper.planAdmin(load(planId));
    }

    private MembershipPlan load(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.PLAN_NOT_FOUND));
    }

    static String normalizeName(String value) {
        String name = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập tên gói.");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Tên gói tối đa 100 ký tự.");
        }
        return name;
    }

    static long validatePrice(Long price) {
        if (price == null || price < MIN_PRICE || price > MAX_PRICE) {
            throw new BadRequestException(PaymentMessages.MSG36_PRICE_RANGE);
        }
        return price;
    }

    static List<String> normalizeBenefits(List<String> benefits) {
        List<String> result = new ArrayList<>();
        if (benefits == null) {
            return result;
        }
        for (String benefit : benefits) {
            String text = benefit == null ? "" : benefit.trim();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_BENEFIT_LENGTH) {
                throw new BadRequestException(BENEFIT_INVALID);
            }
            result.add(text);
        }
        if (result.size() > MAX_BENEFITS) {
            throw new BadRequestException(BENEFIT_INVALID);
        }
        return result;
    }
}
