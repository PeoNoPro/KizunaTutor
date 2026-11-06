package com.kizuna.support;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import com.kizuna.core.security.*;
import static org.mockito.Mockito.mock;
@org.springframework.boot.test.context.TestConfiguration @EnableWebMvc @EnableTransactionManagement(proxyTargetClass=true)
@ComponentScan("com.kizuna.features.ticket")
@Import({SecurityConfig.class,com.kizuna.core.exception.GlobalExceptionHandler.class})
public class SupportTicketIntegrationConfig {
 @Bean DataSource dataSource(){return SupportTicketTestEnvironment.dataSource();}
 @Bean PlatformTransactionManager transactionManager(DataSource ds){return new DataSourceTransactionManager(ds);}
 @Bean JwtTokenVerifier jwtTokenVerifier(){return mock(JwtTokenVerifier.class);}
 @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper(){return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();}
}
