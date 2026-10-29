package com.kizuna.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation đánh dấu các API/Controller yêu cầu tài khoản Gia sư phải hoàn tất
 * Giai đoạn 3 (ACTIVE_PAID - Đã mua gói thành viên) mới được phép thực thi.
 *
 * Áp dụng cho: UC 28-30, UC 33-37, UC 41-45, UC 47-57.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireActiveTutor {
}
