package com.kizuna.operations;

import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Explicit opt-in test DB only; never falls back to application configuration. */
public final class OperationsTestEnvironment {
    private OperationsTestEnvironment() { }

    public static DataSource dataSource() {
        String url = required("OPERATIONS_TEST_DB_URL");
        if (!url.matches("jdbc:postgresql://[^/]+/[a-zA-Z0-9_]*operations_test(?:\\?.*)?")) {
            throw new IllegalStateException("Cần cơ sở dữ liệu operations_test riêng.");
        }
        return new DriverManagerDataSource(url, required("OPERATIONS_TEST_DB_USERNAME"),
                required("OPERATIONS_TEST_DB_PASSWORD"));
    }

    /** Schema FE-11 (database/seeds/fe11_operations_schema.sql). */
    public static String fe11Schema() throws IOException {
        return Files.readString(Path.of("../database/seeds/fe11_operations_schema.sql"));
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Thiếu biến cấu hình môi trường kiểm thử: " + name);
        }
        return value;
    }
}
