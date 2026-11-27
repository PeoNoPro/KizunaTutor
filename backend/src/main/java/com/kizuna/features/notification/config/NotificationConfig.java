package com.kizuna.features.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** FE-12 dùng JdbcTemplate riêng có giới hạn thời gian truy vấn để lỗi nguồn dữ liệu được báo nhanh. */
@Configuration
public class NotificationConfig {
    @Bean("notificationJdbc")
    public JdbcTemplate notificationJdbc(DataSource dataSource) {
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.setQueryTimeout(5);
        return jdbc;
    }

    @Bean("notificationClock")
    public java.time.Clock notificationClock() { return java.time.Clock.system(java.time.ZoneId.of("Asia/Ho_Chi_Minh")); }
}
