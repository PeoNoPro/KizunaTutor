package com.kizuna.features.payment.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kizuna.core.entity.User;
import com.kizuna.core.exception.BadRequestException;
import com.kizuna.core.exception.ResourceNotFoundException;
import com.kizuna.features.payment.dto.response.MembershipViews;
import com.kizuna.features.payment.entity.PaymentProcessingLog;
import com.kizuna.features.payment.entity.PaymentTransaction;
import com.kizuna.features.payment.entity.PaymentTransactionStatus;
import com.kizuna.features.payment.repository.MembershipQuerySpecification;
import com.kizuna.features.payment.repository.PaymentProcessingLogRepository;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import com.kizuna.features.payment.repository.TutorMembershipRepository;
import com.kizuna.features.payment.service.MembershipLabels;
import com.kizuna.features.payment.service.MembershipViewMapper;
import com.kizuna.features.payment.service.PaymentAudit;
import com.kizuna.features.payment.service.PaymentIssueService;
import com.kizuna.features.payment.service.PaymentLogWriter;
import com.kizuna.features.payment.service.PaymentMessages;
import com.kizuna.features.payment.service.PaymentResultProcessor;
import com.kizuna.features.payment.service.VnpayGatewayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chỉ giao dịch PENDING mới đổi trạng thái theo kết quả tra cứu (dùng chung PaymentResultProcessor); SUCCESS/FAILED chỉ
 * được ghi nhật ký (GB-01). Ghi chú điều tra không bao giờ trả ra API phía gia sư (GB-02).
 */
@Slf4j
@Service
public class PaymentIssueServiceImpl implements PaymentIssueService {

    private final PaymentTransactionRepository transactionRepository;
    private final PaymentProcessingLogRepository logRepository;
    private final TutorMembershipRepository membershipRepository;
    private final PaymentResultProcessor processor;
    private final VnpayGatewayClient gatewayClient;
    private final MembershipViewMapper mapper;
    private final PaymentAudit audit;
    private final ObjectMapper objectMapper;

    public PaymentIssueServiceImpl(PaymentTransactionRepository transactionRepository,
                                   PaymentProcessingLogRepository logRepository,
                                   TutorMembershipRepository membershipRepository,
                                   PaymentResultProcessor processor,
                                   VnpayGatewayClient gatewayClient,
                                   MembershipViewMapper mapper,
                                   PaymentAudit audit,
                                   ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.logRepository = logRepository;
        this.membershipRepository = membershipRepository;
        this.processor = processor;
        this.gatewayClient = gatewayClient;
        this.mapper = mapper;
        this.audit = audit;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipViews.PageView<MembershipViews.AdminTransactionRowView> listTransactions(String statusText, String fromText,
                                                                                              String toText, String keyword,
                                                                                              int page, int size) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.length() > 100) {
            throw new BadRequestException("Từ khóa tìm kiếm tối đa 100 ký tự.");
        }
        PaymentTransactionStatus status = parseStatus(statusText);
        LocalDate from = parseDate(fromText);
        LocalDate to = parseDate(toText);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        OffsetDateTime fromTime = from == null ? null : from.atStartOfDay(VnpayGatewayClient.VIETNAM).toOffsetDateTime();
        OffsetDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(VnpayGatewayClient.VIETNAM).toOffsetDateTime();
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<PaymentTransaction> result = transactionRepository.findAll(
                MembershipQuerySpecification.membershipTransactions(status, fromTime, toExclusive, trimmed),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "transactionTime")));
        OffsetDateTime now = OffsetDateTime.now(VnpayGatewayClient.VIETNAM);
        return new MembershipViews.PageView<>(result.getContent().stream().map(tx -> mapper.adminRow(tx, now)).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipViews.AdminTransactionDetailView getDetail(String transactionIdText) {
        PaymentTransaction tx = requireTransaction(transactionIdText);
        LocalDate today = LocalDate.now(VnpayGatewayClient.VIETNAM);
        MembershipViews.MembershipItemView membership = membershipRepository
                .findFirstByTransactionRef(tx.getTransactionId().toString())
                .map(m -> mapper.membership(m, today)).orElse(null);

        List<MembershipViews.LogView> logs = new ArrayList<>();
        List<MembershipViews.NoteView> notes = new ArrayList<>();
        for (PaymentProcessingLog entry : logRepository.findByTransactionTransactionIdOrderByLoggedAtAsc(tx.getTransactionId())) {
            Map<String, Object> request = readJson(entry.getRequestPayload());
            Map<String, Object> response = readJson(entry.getResponsePayload());
            String type = String.valueOf(request.getOrDefault("type", ""));
            if (PaymentLogWriter.TYPE_NOTE.equals(type)) {
                notes.add(new MembershipViews.NoteView(String.valueOf(request.getOrDefault("note", "")),
                        String.valueOf(request.getOrDefault("adminName", "Quản trị viên")), entry.getLoggedAt()));
            } else {
                logs.add(new MembershipViews.LogView(type, MembershipLabels.logType(type), entry.getLoggedAt(),
                        entry.getHttpStatusCode(), summary(request, response)));
            }
        }
        return new MembershipViews.AdminTransactionDetailView(
                mapper.adminRow(tx, OffsetDateTime.now(VnpayGatewayClient.VIETNAM)), membership, logs, notes);
    }

    @Override
    public MembershipViews.GatewayCheckView checkWithGateway(String transactionIdText, User admin, String clientIp) {
        PaymentTransaction tx = requireTransaction(transactionIdText);
        PaymentTransactionStatus before = tx.getStatus();

        VnpayGatewayClient.QueryOutcome query = gatewayClient.queryTransaction(tx, clientIp);
        String outcome;
        if (!query.verified() || query.result() == null) {
            outcome = "UNVERIFIED";
        } else if (!"00".equals(query.queryResponseCode())) {
            outcome = "STILL_PENDING";
        } else if (before != PaymentTransactionStatus.PENDING) {
            outcome = "ALREADY_PROCESSED";
        } else {
            try {
                outcome = processor.process(query.result()).name();
            } catch (Exception e) {
                log.error("Lỗi đồng bộ giao dịch {} theo kết quả tra cứu", tx.getTxnRef(), e);
                outcome = "ERROR";
            }
        }

        Map<String, Object> response = TutorMembershipServiceImpl.queryResponse(query);
        response.put("outcome", outcome);
        response.put("adminName", admin.getFullName());
        audit.log(tx.getTransactionId(), PaymentLogWriter.TYPE_QUERY, query.requestBody(), response, query.httpStatus());

        PaymentTransaction after = transactionRepository.findWithDetailsById(tx.getTransactionId()).orElse(tx);
        boolean stale = mapper.isStale(after, OffsetDateTime.now(VnpayGatewayClient.VIETNAM));
        String gatewayMessage = MembershipLabels.outcome(outcome);
        return new MembershipViews.GatewayCheckView(before.name(), after.getStatus().name(),
                MembershipLabels.transactionStatus(after.getStatus(), stale), query.queryResponseCode(),
                gatewayMessage.isEmpty() ? PaymentMessages.GATEWAY_CHECKED : gatewayMessage);
    }

    @Override
    public MembershipViews.NoteView addNote(String transactionIdText, User admin, String note) {
        PaymentTransaction tx = requireTransaction(transactionIdText);
        String text = note == null ? "" : note.trim();
        if (text.isEmpty() || text.length() > 2000) {
            throw new BadRequestException("Ghi chú phải có từ 1 đến 2.000 ký tự.");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("note", text);
        payload.put("adminId", admin.getUserId() == null ? null : admin.getUserId().toString());
        payload.put("adminName", admin.getFullName());
        audit.log(tx.getTransactionId(), PaymentLogWriter.TYPE_NOTE, payload, null, null);
        return new MembershipViews.NoteView(text, admin.getFullName(), OffsetDateTime.now(VnpayGatewayClient.VIETNAM));
    }

    private PaymentTransaction requireTransaction(String transactionIdText) {
        UUID id = MembershipQuerySpecification.parseTransactionKeyword(transactionIdText);
        if (id == null) {
            throw new ResourceNotFoundException(PaymentMessages.TRANSACTION_NOT_FOUND);
        }
        return transactionRepository.findWithDetailsById(id)
                .filter(tx -> tx.getPlan() != null)
                .orElseThrow(() -> new ResourceNotFoundException(PaymentMessages.TRANSACTION_NOT_FOUND));
    }

    private String summary(Map<String, Object> request, Map<String, Object> response) {
        List<String> parts = new ArrayList<>();
        String outcome = MembershipLabels.outcome(response.get("outcome") == null ? null : String.valueOf(response.get("outcome")));
        if (!outcome.isEmpty()) {
            parts.add(outcome);
        }
        Object responseCode = request.containsKey("vnp_ResponseCode") ? request.get("vnp_ResponseCode") : response.get("vnp_ResponseCode");
        if (responseCode != null) {
            parts.add("Mã phản hồi cổng: " + responseCode);
        }
        Object transactionStatus = request.containsKey("vnp_TransactionStatus")
                ? request.get("vnp_TransactionStatus") : response.get("vnp_TransactionStatus");
        if (transactionStatus != null) {
            parts.add("Mã trạng thái giao dịch: " + transactionStatus);
        }
        if (response.get("adminName") != null) {
            parts.add("Thực hiện bởi " + response.get("adminName"));
        }
        return String.join(" · ", parts);
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() { });
            return map == null ? Map.of() : map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static PaymentTransactionStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            PaymentTransactionStatus status = PaymentTransactionStatus.valueOf(value.trim().toUpperCase());
            if (status == PaymentTransactionStatus.REFUNDED) {
                throw new IllegalArgumentException();
            }
            return status;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Bộ lọc trạng thái không hợp lệ.");
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Ngày không hợp lệ.");
        }
    }
}
