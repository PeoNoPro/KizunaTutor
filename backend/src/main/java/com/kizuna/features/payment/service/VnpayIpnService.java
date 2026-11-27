package com.kizuna.features.payment.service;

import com.kizuna.features.payment.repository.MembershipQuerySpecification;
import com.kizuna.features.payment.repository.PaymentTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Xử lý IPN từ VNPay (API-EXT-02, FR-004). Trả mã phản hồi chuẩn của cổng; kiểm tra theo thứ tự:
 * chữ ký (97) → đơn tồn tại (01) → số tiền (04) → đã xử lý (02) → thành công (00) / lỗi khác (99).
 */
@Slf4j
@Service
public class VnpayIpnService {

    public record IpnReply(String RspCode, String Message) {
    }

    private final VnpaySigner signer;
    private final PaymentResultProcessor processor;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAudit audit;

    public VnpayIpnService(VnpaySigner signer, PaymentResultProcessor processor,
                           PaymentTransactionRepository transactionRepository, PaymentAudit audit) {
        this.signer = signer;
        this.processor = processor;
        this.transactionRepository = transactionRepository;
        this.audit = audit;
    }

    public IpnReply handle(Map<String, String> params) {
        UUID transactionId = MembershipQuerySpecification.parseTransactionKeyword(params.get("vnp_TxnRef"));
        boolean known = transactionId != null && transactionRepository.existsById(transactionId);

        if (!signer.verify(params)) {
            IpnReply reply = new IpnReply("97", "Invalid signature");
            if (known) {
                audit.log(transactionId, PaymentLogWriter.TYPE_IPN, params, response(reply, "INVALID_SIGNATURE"), 200);
            }
            return reply;
        }

        String outcome;
        IpnReply reply;
        try {
            PaymentResultProcessor.Outcome result = processor.process(GatewayResult.fromParams(params));
            outcome = result.name();
            reply = switch (result) {
                case NOT_FOUND -> new IpnReply("01", "Order not found");
                case AMOUNT_MISMATCH -> new IpnReply("04", "Invalid amount");
                case ALREADY_PROCESSED -> new IpnReply("02", "Order already confirmed");
                case ACTIVATED, FAILED, STILL_PENDING -> new IpnReply("00", "Confirm Success");
            };
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN cho giao dịch {}; giao dịch giữ nguyên trạng thái", params.get("vnp_TxnRef"), e);
            outcome = "ERROR";
            reply = new IpnReply("99", "Unknown error");
        }
        if (known) {
            audit.log(transactionId, PaymentLogWriter.TYPE_IPN, params, response(reply, outcome), 200);
        }
        return reply;
    }

    private static Map<String, String> response(IpnReply reply, String outcome) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("RspCode", reply.RspCode());
        map.put("Message", reply.Message());
        map.put("outcome", outcome);
        return map;
    }
}
