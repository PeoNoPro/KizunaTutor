package com.kizuna.features.admin.config;
import com.zaxxer.hikari.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Clock;
@Configuration
public class OperationsConfig {
    @Bean public Clock operationsClock() { return Clock.systemUTC(); }
    @Bean(name="operationsProbePool", destroyMethod="close")
    public ProbePool operationsProbePool(DataSourceProperties properties) {
        HikariConfig c = new HikariConfig();
        c.setJdbcUrl(properties.determineUrl()); c.setUsername(properties.determineUsername());
        c.setPassword(properties.determinePassword()); c.setPoolName("operations-probes");
        // Giữ sẵn một kết nối và nới thời gian: mở kết nối mới tới Supabase có thể lâu hơn 1 giây.
        c.setMaximumPoolSize(3); c.setMinimumIdle(1); c.setConnectionTimeout(2500);
        c.setValidationTimeout(1000); c.setInitializationFailTimeout(-1); c.setReadOnly(true);
        c.addDataSourceProperty("connectTimeout","5"); c.addDataSourceProperty("socketTimeout","5");
        return new ProbePool(new HikariDataSource(c));
    }
    @Bean(name="operationsIdentityJdbc")
    public JdbcTemplate operationsIdentityJdbc(ProbePool pool) {
        JdbcTemplate jdbc = new JdbcTemplate(pool.dataSource()); jdbc.setQueryTimeout(3); return jdbc;
    }
    public record ProbePool(HikariDataSource dataSource) implements AutoCloseable {
        public void close() { dataSource.close(); }
    }
}
