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
 @Bean DataSource dataSource() throws java.io.IOException {
  var ds=SupportTicketTestEnvironment.dataSource();
  new org.springframework.jdbc.core.JdbcTemplate(ds).execute(java.nio.file.Files.readString(java.nio.file.Path.of("../database/seeds/fe10_support_ticket.sql")));
  return ds;
 }
 @Bean PlatformTransactionManager transactionManager(DataSource ds){return new DataSourceTransactionManager(ds);}
 @Bean JwtTokenVerifier jwtTokenVerifier(){return mock(JwtTokenVerifier.class);}
 // The merged dev SecurityConfig also constructs the non-ticket filter chain. Keep those dependencies test-only.
 @Bean JwtAuthenticationFilter jwtAuthenticationFilter(){return new JwtAuthenticationFilter(mock(JwtTokenProvider.class),mock(com.kizuna.core.repository.UserRepository.class));}
 @Bean RestAuthErrorHandlers restAuthErrorHandlers(com.fasterxml.jackson.databind.ObjectMapper mapper){return new RestAuthErrorHandlers(mapper);}
 @Bean com.fasterxml.jackson.databind.ObjectMapper objectMapper(){return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();}
}
