package com.kizuna.service.payment;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service tích hợp cổng thanh toán trực tuyến VNPay (UC 73, 75, 94).
 */
public interface IVnPayService {

    /**
     * Tạo liên kết thanh toán sang cổng VNPay (Sandbox / Production).
     *
     * @param orderInfo Mô tả nội dung thanh toán
     * @param amount Số tiền (VND)
     * @param txnRef Mã giao dịch hệ thống
     * @param ipAddress IP của người dùng gửi yêu cầu
     * @return URL chuyển hướng sang trang thanh toán VNPay
     */
    String createPaymentUrl(String orderInfo, BigDecimal amount, String txnRef, String ipAddress);

    /**
     * Xác thực chữ ký số HMAC-SHA512 từ dữ liệu callback của VNPay.
     *
     * @param fields Tham số phản hồi từ VNPay (bắt đầu bằng vnp_)
     * @return true nếu chữ ký hợp lệ
     */
    boolean validateCallback(Map<String, String> fields);
}
