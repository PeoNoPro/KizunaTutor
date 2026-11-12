package com.kizuna.features.payment.service.impl;

import com.kizuna.core.entity.Tutor;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ConflictException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.core.exception.ServiceUnavailableException;
import com.kizuna.features.payment.config.VnpayProperties;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.MembershipPlan;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.entity.TutorMembership;
import com.kizuna.features.payment.entity.TutorMembershipState;
import com.kizuna.features.payment.repository.MembershipPlanRepository;
import com.kizuna.features.payment.repository.MembershipQuerySpecification;
import com.kizuna.features.payment.repository.MembershipTutorRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.GatewayResult;
import com.kizuna.features.payment.service.MembershipExpiryService;
import com.kizuna.features.payment.service.MembershipLabels;
import com.kizuna.features.payment.service.MembershipViewMapper;
import com.kizuna.features.payment.service.PaymentAudit;
import com.kizuna.features.payment.service.PaymentLogWriter;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.PaymentResultProcessor;
import com.kizuna.features.payment.service.TutorMembershipService;
import com.kizuna.features.payment.service.VnpayGatewayClient;
import com.kizuna.features.payment.service.VnpaySigner;
import com.kizuna.features.verification.entity.TutorVerificationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Không đặt @Transactional ở mức lớp: mỗi bước (lưu giao dịch, xử lý kết quả, ghi nhật ký) chạy transaction riêng,
 * để nhật ký không bao giờ ghi trong lúc giữ khóa dòng giao dịch.
 */
@Slf4j
@Service
public class TutorMembershipServiceImpl implements TutorMembershipService {

    private final MembershipPlanRepository planRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final TutorMembershipRepository membershipRepository;
    private final MembershipTutorRepository tutorRepository;
    private final MembershipExpiryService expiryService;
    private final PaymentResultProcessor processor;
    private final VnpayGatewayClient gatewayClient;
    private final VnpaySigner signer;
    private final VnpayProperties properties;
    private final MembershipViewMapper mapper;
    private final PaymentAudit audit;

    public TutorMembershipServiceImpl(MembershipPlanRepository planRepository,
                                      PaymentTransactionRepository transactionRepository,
                                      TutorMembershipRepository membershipRepository,
                                      MembershipTutorRepository tutorRepository,
                                      MembershipExpiryService expiryService,
                                      PaymentResultProcessor processor,
                                      VnpayGatewayClient gatewayClient,
                                      VnpaySigner signer,
                                      VnpayProperties properties,
                                      MembershipViewMapper mapper,
                                      PaymentAudit audit) {
        this.planRepository = planRepository;
        this.transactionRepository = transactionRepository;
        this.membershipRepository = membershipRepository;
        this.tutorRepository = tutorRepository;
        this.expiryService = expiryService;
        this.processor = processor;
        this.gatewayClient = gatewayClient;
        this.signer = signer;
        this.properties = properties;
        this.mapper = mapper;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(UUID tutorId, LocalDate today) {
        return membershipRepository.countValidOn(tutorId, TutorMembershipState.ACTIVE, today) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<LocalDate> activeMembershipEndDate(UUID tutorId, LocalDate today) {
        return membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutorId).stream()
                .filter(m -> m.getStatus() == TutorMembershipState.ACTIVE && !m.getEndDate().isBefore(today))
                .map(TutorMembership::getEndDate)
                .max(LocalDate::compareTo);
    }

    @Override
    public List<MembershipViews.PlanView> listPlans() {
        return planRepository.findByIsActiveTrueOrderByDurationMonthsAscPriceAsc().stream().map(mapper::plan).toList();
    }

    /** Hạng gói theo thời hạn: gói dài hơn là gói cao hơn; cùng thời hạn là cùng hạng. */
    private static final Comparator<MembershipPlan> PLAN_TIER =
            Comparator.comparing((MembershipPlan p) -> p.getDurationMonths() == null ? 0 : p.getDurationMonths());

    private Integer ownedDurationMonths(UUID tutorId) {
        MembershipPlan owned = highestOwnedPlan(tutorId);
        return owned == null ? null : owned.getDurationMonths();
    }

    /** Gói cao nhất trong các gói còn hiệu lực hoặc đã gia hạn nối tiếp; null nếu chưa có gói nào còn hạn. */
    private MembershipPlan highestOwnedPlan(UUID tutorId) {
        LocalDate today = today();
        return membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutorId).stream()
                .filter(m -> m.getStatus() == TutorMembershipState.ACTIVE && !m.getEndDate().isBefore(today))
                .map(TutorMembership::getPlan)
                .max(PLAN_TIER)
                .orElse(null);
    }

    @Override
    public MembershipViews.MyMembershipView getMyMembership(User user) {
        Tutor found = requireTutor(user);
        LocalDate today = today();
        expiryService.refresh(found.getTutorId(), today);
        Tutor tutor = requireTutor(user);

        List<TutorMembership> records = membershipRepository.findByTutorTutorIdOrderByStartDateDesc(tutor.getTutorId());
        MembershipViews.MembershipItemView current = null;
        LocalDate expiresOn = null;
        java.util.ArrayList<MembershipViews.MembershipItemView> upcoming = new java.util.ArrayList<>();
        for (TutorMembership m : records) {
            if (m.getStatus() != TutorMembershipState.ACTIVE || m.getEndDate().isBefore(today)) {
                continue;
            }
            if (expiresOn == null || m.getEndDate().isAfter(expiresOn)) {
                expiresOn = m.getEndDate();
            }
            if (m.getStartDate().isAfter(today)) {
                upcoming.add(mapper.membership(m, today));
            } else {
                current = mapper.membership(m, today);
            }
        }
        upcoming.sort(Comparator.comparing(MembershipViews.MembershipItemView::startDate));
        List<MembershipViews.MembershipItemView> history = records.stream().map(m -> mapper.membership(m, today)).toList();

        boolean eligible = tutor.getVerificationStatus() == TutorVerificationStatus.APPROVED;
        return new MembershipViews.MyMembershipView(eligible, eligible ? null : PaymentMessages.MSG34_NOT_APPROVED,
                tutor.getTutorState() == null ? null : tutor.getTutorState().name(),
                MembershipLabels.tutorState(tutor.getTutorState()),
                current, upcoming, expiresOn, history, ownedDurationMonths(tutor.getTutorId()));
    }

    @Override
    public MembershipViews.PaymentCreatedView createPayment(User user, String planIdText, String clientIp) {
        if (!properties.isConfigured()) {
            throw new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE);
        }
        UUID planId = parseId(planIdText, PaymentMessages.PLAN_NOT_FOUND);
        Tutor tutor = requireTutor(user);
        if (tutor.getVerificationStatus() != TutorVerificationStatus.APPROVED) {
            throw new ConflictException(PaymentMessages.MSG34_NOT_APPROVED);
        }
        MembershipPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.PLAN_NOT_FOUND));
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            throw new ConflictException(PaymentMessages.MSG35_PLAN_INACTIVE);
        }
        // Chỉ cho nâng cấp: không mua lại gói đang có hoặc gói thấp hơn khi gói hiện tại còn hạn
        MembershipPlan owned = highestOwnedPlan(tutor.getTutorId());
        if (owned != null && PLAN_TIER.compare(plan, owned) <= 0) {
            throw new ConflictException(PaymentMessages.PLAN_NOT_UPGRADE);
        }

        PaymentTransaction tx = transactionRepository.save(PaymentTransaction.builder()
                .user(tutor.getUser())
                .plan(plan)
                .amount(plan.getPrice())
                .paymentGateway("VNPay")
                .status(PaymentTransactionStatus.PENDING)
                .transactionTime(OffsetDateTime.now(VnpayGatewayClient.VIETNAM).truncatedTo(ChronoUnit.SECONDS))
                .build());

        Map<String, String> params = gatewayClient.paymentParams(tx, clientIp);
        String paymentUrl = gatewayClient.buildPaymentUrl(params);
        audit.log(tx.getTransactionId(), PaymentLogWriter.TYPE_CREATE, params, Map.of("outcome", "URL_CREATED"), null);

        return new MembershipViews.PaymentCreatedView(tx.getTransactionId(), MembershipViewMapper.money(tx.getAmount()),
                plan.getPlanName(), paymentUrl, tx.getTransactionTime().plusMinutes(properties.getExpireMinutes()));
    }

    @Override
    public MembershipViews.PaymentResultView handleReturn(User user, Map<String, String> params, String clientIp) {
        if (!signer.verify(params)) {
            throw new BadRequestException(PaymentMessages.INVALID_RESULT);
        }
        UUID transactionId = MembershipQuerySpecification.parseTransactionKeyword(params.get("vnp_TxnRef"));
        if (transactionId == null) {
            throw new ResourceNotFoundException(PaymentMessages.TRANSACTION_NOT_FOUND);
        }
        PaymentTransaction tx = requireOwnTransaction(user, transactionId);
        audit.log(transactionId, PaymentLogWriter.TYPE_RETURN, params, null, null);

        if (tx.getStatus() == PaymentTransactionStatus.PENDING) {
            GatewayResult result = GatewayResult.fromParams(params);
            try {
                VnpayGatewayClient.QueryOutcome query = gatewayClient.queryTransaction(tx, clientIp);
                audit.log(transactionId, PaymentLogWriter.TYPE_QUERY, query.requestBody(),
                        queryResponse(query), query.httpStatus());
                if (query.verified() && "00".equals(query.queryResponseCode()) && query.result() != null) {
                    result = query.result();
                }
            } catch (ServiceUnavailableException e) {
                log.info("Không tra cứu được giao dịch {}; dùng kết quả trả về đã xác thực chữ ký", tx.getTxnRef());
            }
            try {
                PaymentResultProcessor.Outcome outcome = processor.process(result);
                log.info("Trang kết quả: giao dịch {} → {}", tx.getTxnRef(), outcome);
            } catch (Exception e) {
                log.error("Lỗi xử lý kết quả trả về cho giao dịch {}; giao dịch giữ nguyên", tx.getTxnRef(), e);
            }
            tx = requireOwnTransaction(user, transactionId);
        }
        return resultView(tx);
    }

    @Override
    public MembershipViews.PageView<MembershipViews.PaymentView> listMyPayments(User user, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<PaymentTransaction> result = transactionRepository.findByUserUserIdAndPlanIsNotNull(user.getUserId(),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "transactionTime")));
        OffsetDateTime now = OffsetDateTime.now(VnpayGatewayClient.VIETNAM);
        // Danh sách xếp mới nhất trước: một gói đã có giao dịch mới hơn thì giao dịch cũ không còn "Tiếp tục thanh toán".
        Set<UUID> planWithNewer = new HashSet<>();
        List<MembershipViews.PaymentView> items = new ArrayList<>();
        for (PaymentTransaction tx : result.getContent()) {
            MembershipViews.PaymentView view = mapper.payment(tx, now);
            UUID planId = tx.getPlan() == null ? null : tx.getPlan().getPlanId();
            if (view.resumable() && planId != null && planWithNewer.contains(planId)) {
                view = new MembershipViews.PaymentView(view.transactionId(), view.planName(), view.amount(), view.status(),
                        view.statusLabel(), view.transactionTime(), false);
            }
            if (planId != null) planWithNewer.add(planId);
            items.add(view);
        }
        return new MembershipViews.PageView<>(items,
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public MembershipViews.PaymentResultView checkMyPayment(User user, String transactionIdText, String clientIp) {
        UUID transactionId = parseId(transactionIdText, PaymentMessages.TRANSACTION_NOT_FOUND);
        PaymentTransaction tx = requireOwnTransaction(user, transactionId);
        if (tx.getStatus() != PaymentTransactionStatus.PENDING) {
            return resultView(tx);
        }
        VnpayGatewayClient.QueryOutcome query = gatewayClient.queryTransaction(tx, clientIp);
        Map<String, Object> response = queryResponse(query);
        String outcome = "STILL_PENDING";
        if (query.verified() && "00".equals(query.queryResponseCode()) && query.result() != null) {
            try {
                outcome = processor.process(query.result()).name();
            } catch (Exception e) {
                log.error("Lỗi xử lý kết quả tra cứu cho giao dịch {}; giao dịch giữ nguyên", tx.getTxnRef(), e);
                outcome = "ERROR";
            }
        } else if (!query.verified()) {
            outcome = "UNVERIFIED";
        }
        response.put("outcome", outcome);
        response.put("requestedBy", "TUTOR");
        audit.log(transactionId, PaymentLogWriter.TYPE_QUERY, query.requestBody(), response, query.httpStatus());
        return resultView(requireOwnTransaction(user, transactionId));
    }

    @Override
    public MembershipViews.PaymentCreatedView resumePayment(User user, String transactionIdText, String clientIp) {
        if (!properties.isConfigured()) {
            throw new ServiceUnavailableException(PaymentMessages.MSG37_GATEWAY_UNAVAILABLE);
        }
        UUID transactionId = parseId(transactionIdText, PaymentMessages.TRANSACTION_NOT_FOUND);
        PaymentTransaction tx = requireOwnTransaction(user, transactionId);
        if (tx.getStatus() != PaymentTransactionStatus.PENDING
                || mapper.isStale(tx, OffsetDateTime.now(VnpayGatewayClient.VIETNAM))) {
            throw new ConflictException(PaymentMessages.PAYMENT_EXPIRED);
        }
        // VNPay không cho mở lại một mã đơn đã từng mở ("Giao dịch đang được xử lý hoặc đã quá thời gian thanh toán"),
        // nên không dựng lại URL cũ. Trước hết hỏi cổng xem giao dịch cũ đã có kết quả chưa (tránh thu tiền hai lần);
        // còn chờ thì tạo giao dịch mới cùng gói. Giao dịch cũ giữ PENDING để nếu vẫn được trả ở tab cũ thì IPN/trang
        // kết quả vẫn ghi nhận được; quá 15 phút nó tự hiện "Chưa hoàn tất thanh toán".
        settleFromGateway(tx, clientIp);
        if (requireOwnTransaction(user, transactionId).getStatus() != PaymentTransactionStatus.PENDING) {
            throw new ConflictException(PaymentMessages.PAYMENT_ALREADY_FINISHED);
        }
        return createPayment(user, tx.getPlan().getPlanId().toString(), clientIp);
    }

    /** Tra cứu giao dịch với cổng; kết quả có chữ ký hợp lệ thì xử lý như trang kết quả. Cổng lỗi thì bỏ qua. */
    private void settleFromGateway(PaymentTransaction tx, String clientIp) {
        try {
            VnpayGatewayClient.QueryOutcome query = gatewayClient.queryTransaction(tx, clientIp);
            Map<String, Object> response = queryResponse(query);
            response.put("requestedBy", "TUTOR_RESUME");
            audit.log(tx.getTransactionId(), PaymentLogWriter.TYPE_QUERY, query.requestBody(), response, query.httpStatus());
            if (query.verified() && "00".equals(query.queryResponseCode()) && query.result() != null) {
                processor.process(query.result());
            }
        } catch (ServiceUnavailableException e) {
            log.info("Không tra cứu được giao dịch {} trước khi tạo lại; tạo giao dịch mới", tx.getTxnRef());
        } catch (Exception e) {
            log.error("Lỗi xử lý kết quả tra cứu cho giao dịch {}; giao dịch giữ nguyên", tx.getTxnRef(), e);
        }
    }

    private MembershipViews.PaymentResultView resultView(PaymentTransaction tx) {
        String planName = tx.getPlan() == null ? null : tx.getPlan().getPlanName();
        MembershipViews.PeriodView period = membershipRepository.findFirstByTransactionRef(tx.getTransactionId().toString())
                .map(m -> new MembershipViews.PeriodView(m.getStartDate(), m.getEndDate()))
                .orElse(null);
        String message = switch (tx.getStatus()) {
            case SUCCESS -> period == null
                    ? PaymentMessages.MSG33_PENDING
                    : String.format(PaymentMessages.MSG31_SUCCESS, planName, period.endDate().format(MembershipLabels.DATE_VN));
            case FAILED, REFUNDED -> PaymentMessages.MSG32_FAILED;
            case PENDING -> PaymentMessages.MSG33_PENDING;
        };
        boolean stale = mapper.isStale(tx, OffsetDateTime.now(VnpayGatewayClient.VIETNAM));
        return new MembershipViews.PaymentResultView(tx.getTransactionId(), tx.getStatus().name(),
                MembershipLabels.transactionStatus(tx.getStatus(), stale), message, planName,
                MembershipViewMapper.money(tx.getAmount()), period);
    }

    private PaymentTransaction requireOwnTransaction(User user, UUID transactionId) {
        return transactionRepository.findWithDetailsById(transactionId)
                .filter(tx -> tx.getUser().getUserId().equals(user.getUserId()) && tx.getPlan() != null)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.TRANSACTION_NOT_FOUND));
    }

    private Tutor requireTutor(User user) {
        return tutorRepository.findWithUserByUserId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.TUTOR_PROFILE_MISSING));
    }

    static Map<String, Object> queryResponse(VnpayGatewayClient.QueryOutcome query) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("verified", query.verified());
        map.put("vnp_ResponseCode", query.queryResponseCode());
        map.put("vnp_Message", query.message());
        if (query.result() != null) {
            map.put("vnp_TransactionStatus", query.result().transactionStatus());
            map.put("vnp_TransactionNo", query.result().transactionNo());
        }
        if (!query.verified()) {
            map.put("outcome", "UNVERIFIED");
        }
        return map;
    }

    static UUID parseId(String value, String notFoundMessage) {
        try {
            return UUID.fromString(value == null ? "" : value.trim());
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    private static LocalDate today() {
        return LocalDate.now(VnpayGatewayClient.VIETNAM);
    }
}
