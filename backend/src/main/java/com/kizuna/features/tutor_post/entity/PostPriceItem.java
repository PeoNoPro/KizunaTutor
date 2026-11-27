package com.kizuna.features.tutor_post.entity;

import java.math.BigDecimal;

/**
 * Một mức học phí theo môn học / cấp độ, lưu trong cột JSONB tutor_posts.price_items.
 * level là tên hằng của {@link PriceLevel}.
 *
 * @param sessionMinutes  thời lượng một buổi do gia sư đặt cho môn này (60 / 90 / 120 phút)
 * @param pricePerSession học phí một buổi (giá gia sư nhập)
 * @param hourlyRate      học phí quy ra một giờ = pricePerSession × 60 / sessionMinutes (tự tính),
 *                        giữ để trang Tìm gia sư lọc / sắp xếp giá công bằng giữa các thời lượng khác nhau
 */
public record PostPriceItem(String level, BigDecimal hourlyRate, Integer sessionMinutes, BigDecimal pricePerSession) {

    /** Thời lượng buổi gia sư được chọn (phút). */
    public static final java.util.List<Integer> SESSION_MINUTES = java.util.List.of(60, 90, 120);

    /** Thời lượng dùng khi bài đăng cũ chưa có thời lượng theo môn (khớp migration 024_price_items_session.sql). */
    public static final int DEFAULT_SESSION_MINUTES = 90;

    /** Giá một buổi quy ra một giờ, làm tròn đến đồng. */
    public static BigDecimal hourlyOf(BigDecimal pricePerSession, int sessionMinutes) {
        return pricePerSession.multiply(BigDecimal.valueOf(60))
                .divide(BigDecimal.valueOf(sessionMinutes), 0, java.math.RoundingMode.HALF_UP);
    }
}
