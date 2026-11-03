package com.kizuna.features.admin.config;

import java.net.URI;
import java.util.Arrays;
import org.springframework.core.env.Environment;

/** Chỉ cho phép phiên quản trị tạm trong profile và cơ sở dữ liệu cục bộ dành riêng. */
public final class OperationsDevLoginSettings {
    private OperationsDevLoginSettings() { }

    public static String validatedEmail(Environment environment, String configuredEmail) {
        if (configuredEmail == null || configuredEmail.isBlank()) return null;
        var profiles = Arrays.asList(environment.getActiveProfiles());
        if (profiles.size() != 1 || !profiles.contains("admin-dev")) {
            throw new IllegalStateException("Phiên quản trị phát triển chỉ được bật với profile admin-dev riêng.");
        }
        if (!"127.0.0.1".equals(environment.getProperty("server.address"))) {
            throw new IllegalStateException("Phiên quản trị phát triển phải chỉ lắng nghe trên máy cục bộ.");
        }
        String url = environment.getProperty("spring.datasource.url", "");
        try {
            if (!url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI database = URI.create(url.substring(5));
            if (!("127.0.0.1".equals(database.getHost()) || "localhost".equals(database.getHost()))
                    || !database.getPath().matches("/[a-z][a-z0-9_]*_admin_dev")
                    || database.getRawQuery() != null || database.getUserInfo() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Phiên quản trị phát triển cần cơ sở dữ liệu PostgreSQL cục bộ riêng có tên kết thúc bằng _admin_dev.");
        }
        return configuredEmail.trim();
    }
}
