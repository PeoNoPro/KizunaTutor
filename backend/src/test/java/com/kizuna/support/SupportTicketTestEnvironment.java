package com.kizuna.support;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Explicit opt-in: never falls back to the application's development database. */
public final class SupportTicketTestEnvironment {
    private SupportTicketTestEnvironment() {}
    public static DataSource dataSource() {
        String url=required("TICKET_TEST_DB_URL");
        if(!url.matches("jdbc:postgresql://[^/]+/[a-zA-Z0-9_]+_ticket_test"))
            throw new IllegalStateException("Cần cơ sở dữ liệu ticket_test riêng, không có tham số kết nối bổ sung.");
        return new DriverManagerDataSource(url,required("TICKET_TEST_DB_USERNAME"),required("TICKET_TEST_DB_PASSWORD"));
    }
    private static String required(String name) {
        String value=System.getenv(name);
        if(value==null || value.isBlank()) throw new IllegalStateException("Thiếu biến kiểm thử: "+name);
        return value;
    }
}
