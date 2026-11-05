package com.kizuna.operations;
import com.kizuna.core.security.JwtTokenVerifier;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.mock;

@org.springframework.boot.test.context.TestConfiguration @EnableWebMvc @EnableTransactionManagement(proxyTargetClass=true)
@ComponentScan(basePackages={"com.kizuna.features.admin.service","com.kizuna.features.admin.repository","com.kizuna.features.admin.controller","com.kizuna.features.booking.service"})
@EnableJpaRepositories(basePackages={"com.kizuna.features.admin.repository"})
@Import({com.kizuna.core.security.SecurityConfig.class,com.kizuna.features.admin.config.OperationsWebConfig.class,com.kizuna.core.exception.GlobalExceptionHandler.class,
    com.kizuna.features.tutor_search.service.impl.ConnectionActivityReportServiceImpl.class,com.kizuna.features.tutor_search.repository.ConnectionActivityReportRepository.class,
    com.kizuna.features.booking.repository.BookingActivityReportRepository.class})
public class OperationsIntegrationConfig {
    @Bean public DataSource dataSource() throws Exception {
        DataSource ds=OperationsTestEnvironment.dataSource();
        try(var c=ds.getConnection()) {
            // Dedicated test database was explicitly validated; only test-owned tables below.
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS roles(role_id smallint PRIMARY KEY,role_name varchar(50),description varchar(255),is_active boolean NOT NULL DEFAULT true)");
            c.createStatement().execute("DO $$ BEGIN CREATE TYPE user_account_status_enum AS ENUM ('ACTIVE','INACTIVE','SUSPENDED','PENDING'); EXCEPTION WHEN duplicate_object THEN NULL; END $$");
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS users(user_id uuid PRIMARY KEY,role_id smallint REFERENCES roles(role_id),email text,full_name text,password_hash text,phone text,avatar_url text,account_status user_account_status_enum NOT NULL DEFAULT 'ACTIVE',must_change_password boolean NOT NULL DEFAULT false,deleted_at timestamptz,created_at timestamptz NOT NULL DEFAULT now(),updated_at timestamptz NOT NULL DEFAULT now())");
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS trial_bookings(booking_id uuid PRIMARY KEY)");
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS tutors(tutor_id uuid PRIMARY KEY,user_id uuid REFERENCES users(user_id),tutor_state varchar(30) NOT NULL DEFAULT 'DRAFT_PROFILE')");
            c.createStatement().execute("CREATE TABLE IF NOT EXISTS system_configurations(config_key varchar(100) PRIMARY KEY,config_value text NOT NULL,data_type varchar(20) NOT NULL,description varchar(255),updated_at timestamptz NOT NULL DEFAULT now())");
            c.createStatement().execute("INSERT INTO system_configurations(config_key,config_value,data_type) VALUES ('TRIAL_DEFAULT_DURATION_MINUTES','45','NUMBER'),('BOOKING_MIN_HOURS_IN_ADVANCE','12','NUMBER') ON CONFLICT DO NOTHING");
            c.createStatement().execute(OperationsTestEnvironment.fe11Schema());
            for(String file:java.util.List.of("staff_role_change_schema.sql","fe11_admin_update.sql")) c.createStatement().execute(java.nio.file.Files.readString(java.nio.file.Path.of("../database/seeds/"+file)));
        }
        return ds;
    }
    @Bean public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
        var f=new LocalContainerEntityManagerFactoryBean(); f.setDataSource(ds);
        f.setPackagesToScan("com.kizuna.core.entity","com.kizuna.features");
        f.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); f.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto","none","hibernate.jdbc.time_zone","UTC")); return f;
    }
    @Bean public JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory f) { return new JpaTransactionManager(f); }
    @Bean("operationsIdentityJdbc") public JdbcTemplate identityJdbc(DataSource ds) { var j=new JdbcTemplate(ds); j.setQueryTimeout(2); return j; }
    /** Không gửi thư thật khi test; test đọc mật khẩu từ thư giả để chứng minh hash khớp. */
    @Bean public org.springframework.mail.javamail.JavaMailSender javaMailSender() {
        var sender=mock(org.springframework.mail.javamail.JavaMailSender.class);
        org.mockito.Mockito.when(sender.createMimeMessage()).thenAnswer(call->new jakarta.mail.internet.MimeMessage((jakarta.mail.Session)null));
        return sender;
    }
    @Bean public Clock operationsClock() { return Clock.systemUTC(); }
    @Bean public JwtTokenVerifier jwtTokenVerifier() { return mock(JwtTokenVerifier.class); }
}
