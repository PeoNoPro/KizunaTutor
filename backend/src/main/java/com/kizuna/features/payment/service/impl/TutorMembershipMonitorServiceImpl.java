package com.kizuna.features.payment.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.repository.MembershipQuerySpecification;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.MembershipExpiryService;
import com.kizuna.features.payment.service.MembershipLabels;
import com.kizuna.features.payment.service.MembershipViewMapper;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.TutorMembershipMonitorService;
import com.kizuna.features.payment.service.VnpayGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TutorMembershipMonitorServiceImpl implements TutorMembershipMonitorService {

    private final TutorMembershipRepository membershipRepository;
    private final MembershipTutorRepository tutorRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final MembershipExpiryService expiryService;
    private final MembershipViewMapper mapper;

    public TutorMembershipMonitorServiceImpl(TutorMembershipRepository membershipRepository,
                                             MembershipTutorRepository tutorRepository,
                                             PaymentTransactionRepository transactionRepository,
                                             MembershipExpiryService expiryService,
                                             MembershipViewMapper mapper) {
        this.membershipRepository = membershipRepository;
        this.tutorRepository = tutorRepository;
        this.transactionRepository = transactionRepository;
        this.expiryService = expiryService;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipViews.PageView<MembershipViews.TutorMembershipRowView> listMemberships(String keyword, String planIdText,
                                                                                            String statusText, int page, int size) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.length() > 100) {
            throw new BadRequestException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        UUID planId = blank(planIdText) ? null : parseFilterId(planIdText);
        MembershipQuerySpecification.DisplayStatus status = parseStatus(statusText);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LocalDate today = LocalDate.now(VnpayGatewayClient.VIETNAM);

        Page<TutorMembership> result = membershipRepository.findAll(
                MembershipQuerySpecification.latestMembershipPerTutor(trimmedKeyword, planId, status, today),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<MembershipViews.TutorMembershipRowView> items = result.getContent().stream().map(m -> {
            boolean valid = m.getStatus() == TutorMembershipState.ACTIVE && !m.getEndDate().isBefore(today);
            return new MembershipViews.TutorMembershipRowView(m.getTutor().getTutorId(),
                    m.getTutor().getUser().getFullName(), m.getTutor().getUser().getEmail(),
                    m.getPlan().getPlanName(), m.getCreatedAt(),
                    valid ? TutorMembershipState.ACTIVE.name() : TutorMembershipState.EXPIRED.name(),
                    MembershipLabels.membershipStatus(m.getStatus(), m.getStartDate(), m.getEndDate(), today),
                    m.getEndDate());
        }).toList();
        return new MembershipViews.PageView<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public MembershipViews.TutorMembershipDetailView getDetail(String tutorIdText) {
        UUID tutorId = TutorMembershipServiceImpl.parseId(tutorIdText, PaymentMessages.TUTOR_NOT_FOUND);
        tutorRepository.findWithUserByTutorId(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.TUTOR_NOT_FOUND));
        LocalDate today = LocalDate.now(VnpayGatewayClient.VIETNAM);
        expiryService.refresh(tutorId, today);

        Tutor tutor = tutorRepository.findWithUserByTutorId(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.TUTOR_NOT_FOUND));
        List<TutorMembership> records = membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutorId);
        LocalDate expiresOn = records.stream()
                .filter(m -> m.getStatus() == TutorMembershipState.ACTIVE && !m.getEndDate().isBefore(today))
                .map(TutorMembership::getEndDate).max(LocalDate::compareTo).orElse(null);

        List<MembershipViews.TutorPaymentView> payments = null;
        boolean paymentsUnavailable = false;
        try {
            OffsetDateTime now = OffsetDateTime.now(VnpayGatewayClient.VIETNAM);
            payments = transactionRepository
                    .findByUserUserIdAndPlanIsNotNullOrderByTransactionTimeDesc(tutor.getUser().getUserId())
                    .stream().map(tx -> mapper.tutorPayment(tx, now)).toList();
        } catch (Exception e) {
            log.warn("Không tải được lịch sử thanh toán của gia sư {}: {}", tutorId, e.getMessage());
            paymentsUnavailable = true;
        }

        return new MembershipViews.TutorMembershipDetailView(
                new MembershipViews.TutorInfoView(tutor.getTutorId(), tutor.getUser().getFullName(),
                        tutor.getUser().getEmail(), tutor.getPhone() != null ? tutor.getPhone() : tutor.getUser().getPhone(),
                        MembershipLabels.tutorState(tutor.getTutorState())),
                expiresOn,
                records.stream().map(m -> mapper.membership(m, today)).toList(),
                payments, paymentsUnavailable);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static UUID parseFilterId(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Bộ lọc gói không hợp lệ.");
        }
    }

    private static MembershipQuerySpecification.DisplayStatus parseStatus(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            return MembershipQuerySpecification.DisplayStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Bộ lọc trạng thái không hợp lệ.");
        }
    }
}
