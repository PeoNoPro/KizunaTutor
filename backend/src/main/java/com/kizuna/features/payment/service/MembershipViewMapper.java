package com.kizuna.features.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.features.payment.config.VnpayProperties;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Chuyển entity sang dữ liệu trả về FE-09.
 */
@Component
public class MembershipViewMapper {

    private final ObjectMapper objectMapper;
    private final VnpayProperties properties;

    public MembershipViewMapper(ObjectMapper objectMapper, VnpayProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public static long money(BigDecimal amount) {
        return amount == null ? 0L : amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public List<String> benefits(String featuresJson) {
        if (featuresJson == null || featuresJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(featuresJson, new TypeReference<List<String>>() { });
            return values == null ? List.of() : values.stream().filter(v -> v != null && !v.isBlank()).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    public String benefitsJson(List<String> benefits) {
        try {
            return objectMapper.writeValueAsString(benefits == null ? List.of() : benefits);
        } catch (Exception e) {
            return "[]";
        }
    }

    public MembershipViews.PlanView plan(MembershipPlan plan) {
        return new MembershipViews.PlanView(plan.getPlanId(), plan.getPlanName(), plan.getDurationMonths(),
                money(plan.getPrice()), benefits(plan.getFeaturesJson()));
    }

    public MembershipViews.PlanAdminView planAdmin(MembershipPlan plan) {
        boolean active = Boolean.TRUE.equals(plan.getIsActive());
        return new MembershipViews.PlanAdminView(plan.getPlanId(), plan.getPlanName(), plan.getDurationMonths(),
                money(plan.getPrice()), benefits(plan.getFeaturesJson()), active, MembershipLabels.planActive(active),
                plan.getCreatedAt());
    }

    public MembershipViews.MembershipItemView membership(TutorMembership m, LocalDate today) {
        boolean running = m.getStatus() == TutorMembershipState.ACTIVE
                && !m.getStartDate().isAfter(today) && !m.getEndDate().isBefore(today);
        Long daysLeft = running ? ChronoUnit.DAYS.between(today, m.getEndDate()) + 1 : null;
        String status = m.getStatus() == TutorMembershipState.ACTIVE && m.getEndDate().isBefore(today)
                ? TutorMembershipState.EXPIRED.name() : m.getStatus().name();
        return new MembershipViews.MembershipItemView(m.getMembershipId(),
                m.getPlan() == null ? null : m.getPlan().getPlanName(),
                m.getStartDate(), m.getEndDate(), status,
                MembershipLabels.membershipStatus(m.getStatus(), m.getStartDate(), m.getEndDate(), today),
                money(m.getAmount()), daysLeft, m.getCreatedAt());
    }

    public boolean isStale(PaymentTransaction tx, OffsetDateTime now) {
        return tx.getStatus() == PaymentTransactionStatus.PENDING && tx.getTransactionTime() != null
                && tx.getTransactionTime().plusMinutes(properties.getExpireMinutes()).isBefore(now);
    }

    public MembershipViews.PaymentView payment(PaymentTransaction tx, OffsetDateTime now) {
        return new MembershipViews.PaymentView(tx.getTransactionId(),
                tx.getPlan() == null ? null : tx.getPlan().getPlanName(), money(tx.getAmount()),
                tx.getStatus().name(), MembershipLabels.transactionStatus(tx.getStatus(), isStale(tx, now)),
                tx.getTransactionTime(), tx.getStatus() == PaymentTransactionStatus.PENDING && !isStale(tx, now));
    }

    public MembershipViews.TutorPaymentView tutorPayment(PaymentTransaction tx, OffsetDateTime now) {
        return new MembershipViews.TutorPaymentView(tx.getTransactionId(),
                tx.getPlan() == null ? null : tx.getPlan().getPlanName(), money(tx.getAmount()),
                tx.getStatus().name(), MembershipLabels.transactionStatus(tx.getStatus(), isStale(tx, now)),
                tx.getTransactionTime(), tx.getGatewayTransactionId());
    }

    public MembershipViews.AdminTransactionRowView adminRow(PaymentTransaction tx, OffsetDateTime now) {
        boolean stale = isStale(tx, now);
        return new MembershipViews.AdminTransactionRowView(tx.getTransactionId(), tx.getTxnRef(),
                tx.getUser().getFullName(), tx.getUser().getEmail(),
                tx.getPlan() == null ? null : tx.getPlan().getPlanName(), money(tx.getAmount()),
                tx.getStatus().name(), MembershipLabels.transactionStatus(tx.getStatus(), stale), stale,
                tx.getTransactionTime(), tx.getGatewayTransactionId());
    }
}
