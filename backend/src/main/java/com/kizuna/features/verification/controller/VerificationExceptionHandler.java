package com.kizuna.features.verification.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Ánh xạ lỗi của các API FE-03 sang JSON tiếng Việt theo contracts/verification-api.md.
 *
 * Nội dung xử lý nằm ở {@link VerificationErrorResponses}; các controller của feature đã kế thừa lớp đó nên lỗi
 * phát sinh trong controller luôn được xử lý tại chính controller (ưu tiên hơn GlobalExceptionHandler cùng mức
 * @Order). Advice này giữ lại cho lỗi được chuyển về các controller trong gói nhưng không đi qua handler của
 * controller.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.kizuna.features.verification.controller")
public class VerificationExceptionHandler extends VerificationErrorResponses {
}
