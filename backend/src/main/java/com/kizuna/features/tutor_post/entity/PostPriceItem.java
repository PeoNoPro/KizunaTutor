package com.kizuna.features.tutor_post.entity;

import java.math.BigDecimal;

/**
 * Một mức học phí theo môn học / cấp độ, lưu trong cột JSONB tutor_posts.price_items.
 * level là tên hằng của {@link PriceLevel}.
 */
public record PostPriceItem(String level, BigDecimal hourlyRate) {
}
