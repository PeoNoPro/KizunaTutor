package com.kizuna.features.payment.service;

import java.util.Map;

/**
 * Kết quả thanh toán đã xác thực chữ ký, đến từ IPN, trang trả về hoặc tra cứu querydr.
 *
 * @param amountTimes100 vnp_Amount (số tiền × 100); null nếu cổng không trả
 */
public record GatewayResult(String txnRef, String responseCode, String transactionStatus,
                            String transactionNo, Long amountTimes100, String bankCode) {

    /** Thành công khi vnp_ResponseCode = 00 và vnp_TransactionStatus = 00. */
    public boolean isSuccess() {
        return "00".equals(responseCode) && "00".equals(transactionStatus);
    }

    /** Cổng xác nhận giao dịch chưa hoàn tất (đang chờ / đang xử lý). */
    public boolean isStillPending() {
        return "01".equals(transactionStatus) || "05".equals(transactionStatus);
    }

    public static GatewayResult fromParams(Map<String, String> params) {
        return new GatewayResult(
                params.get("vnp_TxnRef"),
                params.get("vnp_ResponseCode"),
                params.get("vnp_TransactionStatus"),
                params.get("vnp_TransactionNo"),
                parseLong(params.get("vnp_Amount")),
                params.get("vnp_BankCode"));
    }

    static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
